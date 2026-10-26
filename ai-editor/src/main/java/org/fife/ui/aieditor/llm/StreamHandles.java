/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor.llm;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link StreamHandle} 的常用实现工厂。
 *
 * @author Oiyuu
 * @version 0.2.0
 */
public final class StreamHandles {

	/**
	 * 工具类不允许被实例化。
	 */
	private StreamHandles() {
	}

	/**
	 * 返回一个什么都不做的句柄，用于请求根本没发出去的场景。
	 *
	 * 典型场景是未配置 API Key：此时错误已通过 {@code onError} 告知调用方，
	 * 后续界面若不慎调用 {@code cancel()} 也不应抛异常。
	 *
	 * @return 已完成状态且取消方法为空操作的句柄
	 */
	public static StreamHandle noop() {
		return new NoOpStreamHandle();
	}

	/**
	 * 把一个异步任务包装成句柄。
	 *
	 * @param future 底层任务，不可为 {@code null}
	 * @return 与之联动的句柄
	 */
	public static StreamHandle of(CompletableFuture<?> future) {
		return new FutureStreamHandle(future);
	}

	/**
	 * 什么也不做的句柄实现。
	 */
	private static final class NoOpStreamHandle implements StreamHandle {

		@Override
		public void cancel() {
			// 没有任务需要取消
		}

		@Override
		public boolean isDone() {
			return true;
		}
	}

	/**
	 * 基于异步任务的句柄实现。
	 *
	 * 用 {@link AtomicBoolean} 记录取消状态：即使底层任务已经完成，
	 * 调用 {@code cancel()} 也不应该抛异常，这是 Swing 里频繁随时间调用的操作。
	 */
	private static final class FutureStreamHandle implements StreamHandle {

		private final CompletableFuture<?> future;
		private final AtomicBoolean cancelled = new AtomicBoolean();

		/**
		 * 包装一个异步任务。
		 *
		 * @param future 底层任务
		 */
		FutureStreamHandle(CompletableFuture<?> future) {
			this.future = Objects.requireNonNull(future, "future 不可为 null");
		}

		@Override
		public void cancel() {
			cancelled.set(true);
			future.cancel(true);
		}

		@Override
		public boolean isDone() {
			return cancelled.get() || future.isDone();
		}
	}
}
