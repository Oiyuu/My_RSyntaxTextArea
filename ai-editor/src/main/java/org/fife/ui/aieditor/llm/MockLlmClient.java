/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor.llm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * 用于测试的假客户端，完全不触网。
 *
 * <p>它能模拟真实场景下最容易出问题的三种情况：</p>
 * <ul>
 *   <li><strong>延迟</strong>——用来验证界面在等待期间没有卡死</li>
 *   <li><strong>流式分片</strong>——包括把同一个字符拆在两片里送出，用于验证增量拼接</li>
 *   <li><strong>失败</strong>——用于验证异常不会冒泡到 EDT 引发崩溃</li>
 * </ul>
 *
 * @author Oiyuu
 * @version 0.2.0
 */
public final class MockLlmClient implements LlmClient {

	private final String responseText;
	private final long delayMs;
	private final List<String> chunks;
	private final RuntimeException failure;

	/**
	 * 用构建器组装实例。
	 *
	 * @param builder 构建器
	 */
	private MockLlmClient(Builder builder) {
		this.responseText = builder.responseText;
		this.delayMs = builder.delayMs;
		this.chunks = Collections.unmodifiableList(new ArrayList<>(builder.chunks));
		this.failure = builder.failure;
	}

	/**
	 * 创建一个构建器。
	 *
	 * @return 构建器
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 返回非流式请求的固定响应。
	 *
	 * @param request 请求内容
	 * @return 固定响应；若配置了失败则异常完成
	 */
	@Override
	public CompletableFuture<LlmResponse> complete(LlmRequest request) {
		Objects.requireNonNull(request, "request 不可为 null");
		if (failure != null) {
			return CompletableFuture.failedFuture(failure);
		}
		if (delayMs <= 0) {
			return CompletableFuture.completedFuture(LlmResponse.of(responseText));
		}
		CompletableFuture<LlmResponse> target = new CompletableFuture<>();
		CompletableFuture.runAsync(() -> { },
						CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS))
				.thenRun(() -> target.complete(LlmResponse.of(responseText)));
		return target;
	}

	/**
	 * 按预先设定的分片逐个推送增量文本。
	 *
	 * @param request 请求内容
	 * @param onChunk 增量文本回调
	 * @param onComplete 完成回调
	 * @param onError 失败回调
	 * @return 句柄，取消后不再推送任何回调
	 */
	@Override
	public StreamHandle completeStream(LlmRequest request, Consumer<String> onChunk,
			Consumer<LlmResponse> onComplete, Consumer<Throwable> onError) {
		Objects.requireNonNull(request, "request 不可为 null");
		Objects.requireNonNull(onChunk, "onChunk 不可为 null");
		Objects.requireNonNull(onComplete, "onComplete 不可为 null");
		Objects.requireNonNull(onError, "onError 不可为 null");

		CancellableStreamHandle handle = new CancellableStreamHandle();
		if (failure != null) {
			onError.accept(failure);
			handle.done.set(true);
			return handle;
		}
		CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
		for (String chunk : chunks) {
			chain = chain.thenRunAsync(() -> {
				if (!handle.done.get()) {
					onChunk.accept(chunk);
				}
			}, CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS));
		}
		CompletableFuture<Void> finalChain = chain;
		finalChain.thenRun(() -> {
			if (handle.done.compareAndSet(false, true)) {
				onComplete.accept(LlmResponse.of(concatenate(chunks)));
			}
		});
		handle.future = finalChain;
		return handle;
	}

	/**
	 * 拼接所有分片。
	 *
	 * @param chunks 分片列表
	 * @return 拼接结果
	 */
	private static String concatenate(List<String> chunks) {
		StringBuilder builder = new StringBuilder();
		chunks.forEach(builder::append);
		return builder.toString();
	}

	/**
	 * {@link MockLlmClient} 的构建器。
	 */
	public static final class Builder {

		private String responseText = "";
		private long delayMs;
		private final List<String> chunks = new ArrayList<>();
		private RuntimeException failure;

		/**
		 * 构造默认构建器。
		 */
		Builder() {
			// 使用默认值即可
		}

		/**
		 * 设置非流式请求的返回内容。
		 *
		 * @param responseText 返回内容
		 * @return 当前构建器
		 */
		public Builder responseText(String responseText) {
			this.responseText = responseText;
			return this;
		}

		/**
		 * 设置每次回调之间的延迟，用于测试超时与界面响应。
		 *
		 * @param delayMs 毫秒数
		 * @return 当前构建器
		 */
		public Builder delayMs(long delayMs) {
			this.delayMs = delayMs;
			return this;
		}

		/**
		 * 追加一个流式分片。
		 *
		 * @param chunk 分片内容
		 * @return 当前构建器
		 */
		public Builder addChunk(String chunk) {
			chunks.add(chunk);
			return this;
		}

		/**
		 * 设置要抛出的失败。
		 *
		 * @param failure 失败异常
		 * @return 当前构建器
		 */
		public Builder failure(RuntimeException failure) {
			this.failure = failure;
			return this;
		}

		/**
		 * 构建客户端实例。
		 *
		 * @return 客户端实例
		 */
		public MockLlmClient build() {
			return new MockLlmClient(this);
		}
	}

	/**
	 * 可取消的句柄实现，兼顾 "取消后不再回调" 这条契约。
	 */
	private final class CancellableStreamHandle implements StreamHandle {

		private final AtomicBoolean done = new AtomicBoolean();
		private CompletableFuture<Void> future;

		@Override
		public void cancel() {
			done.set(true);
			if (future != null) {
				future.cancel(true);
			}
		}

		@Override
		public boolean isDone() {
			return done.get() || (future != null && future.isDone());
		}
	}
}
