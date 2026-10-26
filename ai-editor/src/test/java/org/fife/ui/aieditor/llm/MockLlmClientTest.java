/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor.llm;

import org.fife.ui.aieditor.config.AiConfig;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MockLlmClient} 的行为测试，验证它确实能满足上层功能对"延迟、分片、失败"的模拟需求。
 *
 * 这些测试同时也是 v0.2.0 后续三个 AI 功能依赖的契约：取消之后不得再回调。
 *
 * @author Oiyuu
 * @version 0.2.0
 */
class MockLlmClientTest {

	private static LlmRequest sampleRequest() {
		return LlmRequest.builder().model("mock").userPrompt("写个 HelloWorld").build();
	}

	@Test
	void testNonStreamReturnsFixedText() throws Exception {
		MockLlmClient client = MockLlmClient.builder().responseText("审查通过").build();

		LlmResponse response = client.complete(sampleRequest()).get(2, TimeUnit.SECONDS);

		assertEquals("审查通过", response.getContent());
		assertFalse(response.isTruncated());
	}

	@Test
	void testFailureIsReportedAsException() {
		MockLlmClient client = MockLlmClient.builder()
				.failure(new IllegalStateException("模拟失败"))
				.build();

		CompletableFuture<LlmResponse> future = client.complete(sampleRequest());

		ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get(2, TimeUnit.SECONDS));
		assertEquals("模拟失败", ex.getCause().getMessage());
	}

	@Test
	void testStreamConcatenatesChunks() throws Exception {
		MockLlmClient client = MockLlmClient.builder()
				.addChunk("你").addChunk("好").addChunk("世界")
				.build();

		CompletableFuture<LlmResponse> done = new CompletableFuture<>();
		AtomicReference<String> streaming = new AtomicReference<>("");
		AtomicBoolean completed = new AtomicBoolean();

		StreamHandle handle = client.completeStream(sampleRequest(),
				chunk -> streaming.updateAndGet(old -> old + chunk),
				response -> {
					completed.set(true);
					done.complete(response);
				},
				done::completeExceptionally);

		LlmResponse response = done.get(5, TimeUnit.SECONDS);

		assertTrue(completed.get(), "应触发完成回调");
		assertEquals("你好世界", streaming.get(), "增量拼接结果应完整");
		assertEquals("你好世界", response.getContent());
		assertTrue(handle.isDone());
	}

	@Test
	void testNoFurtherCallbacksAfterCancel() throws Exception {
		MockLlmClient client = MockLlmClient.builder()
				.delayMs(150)
				.addChunk("甲").addChunk("乙").addChunk("丙")
				.build();

		AtomicInteger chunks = new AtomicInteger();
		AtomicBoolean completed = new AtomicBoolean();

		StreamHandle handle = client.completeStream(sampleRequest(),
				chunk -> chunks.incrementAndGet(),
				response -> completed.set(true),
				error -> { });

		handle.cancel();
		Thread.sleep(600);   // 远大于整条流的耗时，确认迟到的回调被吞掉

		assertTrue(handle.isDone());
		assertFalse(completed.get(), "取消后不应再触发完成回调");
		assertTrue(chunks.get() < 3, "取消后不应继续推送剩余分片，实际收到 " + chunks.get() + " 片");
	}

	@Test
	void testStreamFailureGoesToErrorCallback() {
		MockLlmClient client = MockLlmClient.builder()
				.failure(new IllegalStateException("断网"))
				.build();

		AtomicReference<Throwable> error = new AtomicReference<>();
		AtomicBoolean completed = new AtomicBoolean();

		client.completeStream(sampleRequest(), chunk -> { },
				response -> completed.set(true), error::set);

		assertInstanceOf(IllegalStateException.class, error.get());
		assertFalse(completed.get(), "失败时不应再触发完成回调");
	}

	@Test
	void testUnconfiguredClientFailsFast() {
		AiConfig config = new AiConfig();
		config.setApiKey("");
		OpenAiCompatibleClient client = new OpenAiCompatibleClient(config);

		CompletableFuture<LlmResponse> future = client.complete(sampleRequest());

		ExecutionException ex = assertThrows(ExecutionException.class, () -> future.get(2, TimeUnit.SECONDS));
		LlmException cause = assertInstanceOf(LlmException.class, ex.getCause());
		assertEquals(LlmException.Kind.AUTHENTICATION, cause.getKind());
		assertTrue(cause.getMessage().contains("API Key"), "错误信息应指引用户去配置密钥");
	}

	@Test
	void testUnconfiguredClientStreamReportsError() {
		OpenAiCompatibleClient client = new OpenAiCompatibleClient(new AiConfig());

		AtomicReference<Throwable> error = new AtomicReference<>();
		AtomicBoolean completed = new AtomicBoolean();
		AtomicBoolean chunked = new AtomicBoolean();

		StreamHandle handle = client.completeStream(sampleRequest(),
				chunk -> chunked.set(true),
				response -> completed.set(true),
				error::set);

		assertTrue(handle.isDone(), "未配置时应返回已结束的句柄");
		assertFalse(chunked.get());
		assertFalse(completed.get());
		assertInstanceOf(LlmException.class, error.get());
	}
}
