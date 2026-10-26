/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor.llm;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.fife.ui.aieditor.config.AiConfig;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OpenAI 兼容接口的客户端实现。
 *
 * <p>之所以叫"兼容"而不是"AOpenAI 客户端"：这个 chat/completions 请求/响应格式已被
 * 大量服务商与本地推理框架（ADS、Azure、DeepSeek、Ollama、vLLM 等）支持，
 * 只要能改 baseUrl，本类就应当能直接用。</p>
 *
 * @author Oiyuu
 * @version 0.2.0
 */
public class OpenAiCompatibleClient implements LlmClient {

	private static final Logger LOGGER = Logger.getLogger(OpenAiCompatibleClient.class.getName());

	/** chat/completions 端点路径。 */
	private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

	/** 成功状态码。 */
	private static final int STATUS_OK = 200;

	/** 被限流的状态码。 */
	private static final int STATUS_TOO_MANY_REQUESTS = 429;

	/** 除首呼之外最多重试的次数，总计最多发起 3 次请求。 */
	private static final int MAX_RETRY_COUNT = 2;

	/** 重试退避的基础毫秒数，实际等待为 base * 2^attempt。 */
	private static final long RETRY_BASE_DELAY_MS = 500L;

	/** 日志中代替响应体的占位文字，避免用户代码被写进日志。 */
	private static final String REDACTED = "<已省略，可能包含用户代码>";

	private final AiConfig config;
	private final HttpClient httpClient;
	private final Gson gson;

	/**
	 * 使用默认的 {@link HttpClient} 创建客户端。
	 *
	 * @param config 配置对象，不可为 {@code null}
	 */
	public OpenAiCompatibleClient(AiConfig config) {
		this(config, createHttpClient(Objects.requireNonNull(config, "config 不可为 null")));
	}

	/**
	 * 使用外部提供的 {@link HttpClient} 创建客户端，便于测试替换。
	 *
	 * @param config 配置对象，不可为 {@code null}
	 * @param httpClient HTTP 客户端，不可为 {@code null}
	 */
	OpenAiCompatibleClient(AiConfig config, HttpClient httpClient) {
		this.config = Objects.requireNonNull(config, "config 不可为 null");
		this.httpClient = Objects.requireNonNull(httpClient, "httpClient 不可为 null");
		this.gson = new Gson();
	}

	/**
	 * 创建带连接超时的 HTTP 客户端。
	 *
	 * 每次请求都 new 一个客户端会创建新的线程池，长期运行会泄漏，
	 * 因此这里刻意只创建一次。
	 *
	 * @param config 配置对象
	 * @return HTTP 客户端
	 */
	private static HttpClient createHttpClient(AiConfig config) {
		return HttpClient.newBuilder()
				.connectTimeout(Duration.ofMillis(config.getConnectTimeoutMs()))
				.build();
	}

	/**
	 * 发起一次非流式请求。
	 *
	 * @param request 请求内容，不可为 {@code null}
	 * @return 异步结果；失败时以 {@link LlmException} 异常完成
	 */
	@Override
	public CompletableFuture<LlmResponse> complete(LlmRequest request) {
		Objects.requireNonNull(request, "request 不可为 null");
		if (!config.isReady()) {
			return CompletableFuture.failedFuture(notConfigured());
		}
		CompletableFuture<LlmResponse> target = new CompletableFuture<>();
		send(request, target, 0);
		return target;
	}

	/**
	 * 发起一次流式请求。
	 *
	 * @param request 请求内容，不可为 {@code null}
	 * @param onChunk 增量文本回调
	 * @param onComplete 完成回调
	 * @param onError 失败回调
	 * @return 流句柄，可用于取消
	 */
	@Override
	public StreamHandle completeStream(LlmRequest request, Consumer<String> onChunk,
			Consumer<LlmResponse> onComplete, Consumer<Throwable> onError) {
		Objects.requireNonNull(request, "request 不可为 null");
		Objects.requireNonNull(onChunk, "onChunk 不可为 null");
		Objects.requireNonNull(onComplete, "onComplete 不可为 null");
		Objects.requireNonNull(onError, "onError 不可为 null");
		if (!config.isReady()) {
			onError.accept(notConfigured());
			return StreamHandles.noop();
		}

		HttpRequest httpRequest = buildRequest(request, true);
		CompletableFuture<HttpResponse<Flow.Publisher<List<ByteBuffer>>>> future =
				httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofPublisher());

		StreamAccumulator accumulator = new StreamAccumulator(onChunk, onComplete, onError);
		future.whenComplete((response, error) -> {
			if (error != null) {
				accumulator.fail(mapThrowable(error));
				return;
			}
			int status = response.statusCode();
			if (status != STATUS_OK) {
				accumulator.fail(failureFor(status));
				return;
			}
			response.body().subscribe(accumulator);
		});
		return StreamHandles.of(future);
	}

	/**
	 * 发起一次请求并异步回填结果。
	 *
	 * @param request 请求内容
	 * @param target 待回填的结果
	 * @param attempt 当前是第几次尝试（从 0 开始）
	 */
	private void send(LlmRequest request, CompletableFuture<LlmResponse> target, int attempt) {
		HttpRequest httpRequest = buildRequest(request, false);
		httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
				.whenComplete((response, error) -> onResponse(request, target, attempt, response, error));
	}

	/**
	 * 处理响应或传输错误，必要时安排重试。
	 *
	 * @param request 原请求，重试时使用
	 * @param target 待回填的结果
	 * @param attempt 当前尝试次数
	 * @param response 响应对象，传输失败时为 {@code null}
	 * @param error 传输层异常，成功时为 {@code null}
	 */
	private void onResponse(LlmRequest request, CompletableFuture<LlmResponse> target, int attempt,
			HttpResponse<String> response, Throwable error) {
		if (target.isDone()) {
			return;
		}
		if (error != null) {
			handleTransportError(request, target, attempt, error);
			return;
		}
		int status = response.statusCode();
		if (status == STATUS_OK) {
			parseBody(response, target);
			return;
		}
		handleHttpError(request, target, attempt, status);
	}

	/**
	 * 处理传输层的失败，超时与网络抖动允许重试。
	 *
	 * @param request 原请求
	 * @param target 待回填的结果
	 * @param attempt 当前尝试次数
	 * @param error 传输层异常
	 */
	private void handleTransportError(LlmRequest request, CompletableFuture<LlmResponse> target, int attempt,
			Throwable error) {
		LlmException failure = mapThrowable(error);
		if (attempt < MAX_RETRY_COUNT && failure.getKind() == LlmException.Kind.TIMEOUT) {
			LOGGER.log(Level.FINE, "请求超时，准备重试；响应体 " + REDACTED);
			scheduleRetry(request, target, attempt);
			return;
		}
		target.completeExceptionally(failure);
	}

	/**
	 * 处理 HTTP 状态码错误。
	 *
	 * 只对 429 与 5xx 重试：这两类属于临时性问题。
	 * 4xx 重试毫无意义，还会浪费用户的额度。
	 *
	 * @param request 原请求
	 * @param target 待回填的结果
	 * @param attempt 当前尝试次数
	 * @param status HTTP 状态码
	 */
	private void handleHttpError(LlmRequest request, CompletableFuture<LlmResponse> target, int attempt, int status) {
		LlmException failure = failureFor(status);
		boolean retriable = failure.getKind() == LlmException.Kind.RATE_LIMITED ||
				failure.getKind() == LlmException.Kind.SERVER_ERROR;
		if (attempt < MAX_RETRY_COUNT && retriable) {
			LOGGER.log(Level.FINE, "收到状态码 " + status + "，准备重试；响应体 " + REDACTED);
			scheduleRetry(request, target, attempt);
			return;
		}
		target.completeExceptionally(failure);
	}

	/**
	 * 指数退避后重试。
	 *
	 * @param request 原请求
	 * @param target 待回填的结果
	 * @param attempt 当前尝试次数
	 */
	private void scheduleRetry(LlmRequest request, CompletableFuture<LlmResponse> target, int attempt) {
		long delay = RETRY_BASE_DELAY_MS << attempt;
		CompletableFuture<Void> tick = CompletableFuture.runAsync(() -> { },
				CompletableFuture.delayedExecutor(delay, TimeUnit.MILLISECONDS));
		tick.thenAccept(ignored -> send(request, target, attempt + 1));
	}

	/**
	 * 解析成功响应体。
	 *
	 * @param response HTTP 响应
	 * @param target 待回填的结果
	 */
	private void parseBody(HttpResponse<String> response, CompletableFuture<LlmResponse> target) {
		try {
			target.complete(parseCompletion(response.body()));
		}
		catch (JsonSyntaxException ex) {
			target.completeExceptionally(new LlmException(LlmException.Kind.MALFORMED_RESPONSE,
					"服务端返回的内容不是合法 JSON", ex));
		}
		catch (RuntimeException ex) {
			target.completeExceptionally(new LlmException(LlmException.Kind.MALFORMED_RESPONSE,
					"服务端返回的内容缺少必要字段", ex));
		}
	}

	/**
	 * 从 chat/completions 的响应体中提取结果。
	 *
	 * 只取需要的字段，而不是反序列化出完整 POJO：
	 * 上游增删字段是常态，忽略它比跟着崩溃更合理。
	 *
	 * @param body 响应体 JSON
	 * @return 解析结果
	 */
	private LlmResponse parseCompletion(String body) {
		JsonObject root = gson.fromJson(body, JsonObject.class);
		if (root == null) {
			throw new IllegalStateException("响应体为空");
		}
		String content = null;
		String finishReason = null;
		JsonArray choices = root.getAsJsonArray("choices");
		if (choices != null && !choices.isEmpty()) {
			JsonObject first = choices.get(0).getAsJsonObject();
			JsonObject message = first.getAsJsonObject("message");
			if (message != null) {
				JsonElement text = message.get("content");
				if (text != null && !text.isJsonNull()) {
					content = text.getAsString();
				}
			}
			JsonElement reason = first.get("finish_reason");
			if (reason != null && !reason.isJsonNull()) {
				finishReason = reason.getAsString();
			}
		}
		int totalTokens = 0;
		JsonObject usage = root.getAsJsonObject("usage");
		if (usage != null) {
			JsonElement total = usage.get("total_tokens");
			if (total != null && !total.isJsonNull()) {
				totalTokens = total.getAsInt();
			}
		}
		return new LlmResponse(content, finishReason, totalTokens);
	}

	/**
	 * 构造 HTTP 请求。
	 *
	 * @param request 逻辑请求
	 * @param stream 是否要求流式返回
	 * @return HTTP 请求对象
	 */
	private HttpRequest buildRequest(LlmRequest request, boolean stream) {
		String url = config.resolveBaseUrl() + CHAT_COMPLETIONS_PATH;
		String body = gson.toJson(buildPayload(request, stream));
		return HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofMillis(config.getRequestTimeoutMs()))
				.header("Content-Type", "application/json")
				.header("Authorization", "Bearer " + config.resolveApiKey())
				.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
				.build();
	}

	/**
	 * 构造请求体。
	 *
	 * @param request 逻辑请求
	 * @param stream 是否要求流式返回
	 * @return 请求体 JSON 对象
	 */
	private JsonObject buildPayload(LlmRequest request, boolean stream) {
		JsonArray messages = new JsonArray();
		if (request.getSystemPrompt() != null && !request.getSystemPrompt().trim().isEmpty()) {
			messages.add(message("system", request.getSystemPrompt()));
		}
		messages.add(message("user", request.getUserPrompt()));

		JsonObject root = new JsonObject();
		root.addProperty("model", request.getModel());
		root.add("messages", messages);
		root.addProperty("temperature", request.getTemperature());
		root.addProperty("max_tokens", request.getMaxTokens());
		root.addProperty("stream", stream);
		if (!request.getStop().isEmpty()) {
			JsonArray stops = new JsonArray();
			request.getStop().forEach(stops::add);
			root.add("stop", stops);
		}
		return root;
	}

	/**
	 * 构造一条消息。
	 *
	 * @param role 角色
	 * @param text 内容
	 * @return 消息 JSON 对象
	 */
	private JsonObject message(String role, String text) {
		JsonObject entry = new JsonObject();
		entry.addProperty("role", role);
		entry.addProperty("content", text);
		return entry;
	}

	/**
	 * 返回尚未配置密钥时的统一异常。
	 *
	 * @return 鉴权类异常
	 */
	private static LlmException notConfigured() {
		return new LlmException(LlmException.Kind.AUTHENTICATION, "尚未配置 API Key，请先在偏好设置中配置");
	}

	/**
	 * 根据状态码生成异常。
	 *
	 * @param status HTTP 状态码
	 * @return 对应的异常
	 */
	private static LlmException failureFor(int status) {
		switch (status) {
			case 401:
			case 403:
				return new LlmException(LlmException.Kind.AUTHENTICATION, "API Key 无效或无权访问该模型");
			case STATUS_TOO_MANY_REQUESTS:
				return new LlmException(LlmException.Kind.RATE_LIMITED, "请求过于频繁，已被服务端限流");
			case 404:
				return new LlmException(LlmException.Kind.UNKNOWN, "接口地址不存在，请检查服务地址与模型名");
			default:
				if (status >= 500) {
					return new LlmException(LlmException.Kind.SERVER_ERROR, "AI 服务暂时不可用（" + status + "）");
				}
				return new LlmException(LlmException.Kind.UNKNOWN, "请求失败（" + status + "）");
		}
	}

	/**
	 * 把底层异常映射为统一的 {@link LlmException}。
	 *
	 * @param error 原始异常
	 * @return 统一异常
	 */
	private static LlmException mapThrowable(Throwable error) {
		Throwable cause = error instanceof java.util.concurrent.CompletionException ? error.getCause() : error;
		if (cause instanceof java.net.http.HttpTimeoutException) {
			return new LlmException(LlmException.Kind.TIMEOUT, "请求超时，请检查网络或适当放宽超时设置", cause);
		}
		if (cause instanceof java.net.http.HttpConnectTimeoutException) {
			return new LlmException(LlmException.Kind.TIMEOUT, "连接超时，请检查服务地址是否可达", cause);
		}
		if (cause instanceof java.io.InterruptedIOException || cause instanceof InterruptedException ||
				cause instanceof java.util.concurrent.CancellationException) {
			return new LlmException(LlmException.Kind.CANCELLED, "请求已取消", cause);
		}
		return new LlmException(LlmException.Kind.UNKNOWN, "网络请求失败：" + cause.getMessage(), cause);
	}

	/**
	 * 流式响应的累加器，把字节流翻译成增量文本回调。
	 *
	 * <p>这里有两个容易出错的细节：</p>
	 * <ul>
	 *   <li>一个 UTF-8 字符可能被拆在两个 TCP 包里，直接解码会产生乱码，
	 *       因此内部保留未消费的字节直到凑够</li>
	 *   <li>取消之后必须停止向回调推送数据，否则界面会出现"停了还在写字"</li>
	 * </ul>
	 */
	private static final class StreamAccumulator implements Flow.Subscriber<List<ByteBuffer>> {

		private final Consumer<String> onChunk;
		private final Consumer<LlmResponse> onComplete;
		private final Consumer<Throwable> onError;
		private final AtomicBoolean finished = new AtomicBoolean();
		private final StringBuilder content = new StringBuilder();
		private final ByteArrayOutputStream pending = new ByteArrayOutputStream();
		private final CharsetDecoder decoder;
		private final SseParser parser;
		private volatile Flow.Subscription subscription;

		/**
		 * 创建累加器。
		 *
		 * @param onChunk 增量文本回调
		 * @param onComplete 完成回调
		 * @param onError 失败回调
		 */
		StreamAccumulator(Consumer<String> onChunk, Consumer<LlmResponse> onComplete, Consumer<Throwable> onError) {
			this.onChunk = onChunk;
			this.onComplete = onComplete;
			this.onError = onError;
			this.decoder = StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPLACE)
					.onUnmappableCharacter(CodingErrorAction.REPLACE);
			this.parser = new SseParser(this::emitDelta, this::emitDone);
		}

		@Override
		public void onSubscribe(Flow.Subscription subscription) {
			this.subscription = subscription;
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(List<ByteBuffer> buffers) {
			if (finished.get()) {
				unsubscribe();
				return;
			}
			for (ByteBuffer buffer : buffers) {
				byte[] bytes = new byte[buffer.remaining()];
				buffer.get(bytes);
				pending.write(bytes, 0, bytes.length);
			}
			drainPending();
		}

		@Override
		public void onError(Throwable throwable) {
			fail(mapThrowable(throwable));
		}

		@Override
		public void onComplete() {
			emitDone();
		}

		/**
		 * 把已累积的字节尽量解码成文本，保留末尾不完整的字符。
		 */
		private void drainPending() {
			ByteBuffer buffer = ByteBuffer.wrap(pending.toByteArray());
			try {
				CharBuffer decoded = decoder.decode(buffer);
				int consumed = buffer.position();
				byte[] all = pending.toByteArray();
				pending.reset();
				pending.write(all, consumed, all.length - consumed);
				parser.feed(decoded.toString());
			}
			catch (CharacterCodingException ex) {
				fail(new LlmException(LlmException.Kind.MALFORMED_RESPONSE, "响应流不是合法的 UTF-8 文本", ex));
			}
		}

		/**
		 * 收到一段增量文本时转发给回调，同时累加以备最终返回。
		 *
		 * @param delta 增量文本
		 */
		private void emitDelta(String delta) {
			if (finished.get()) {
				return;
			}
			content.append(delta);
			onChunk.accept(delta);
		}

		/**
		 * 流结束时调用完成回调。
		 *
		 * 服务端若提前断开，SSE 的 [DONE] 可能根本到不了，
		 * 这里仍然补一次完成回调，避免界面停留在"正在思考"。
		 */
		private void emitDone() {
			if (finished.compareAndSet(false, true)) {
				unsubscribe();
				onComplete.accept(new LlmResponse(content.toString(), "stop", 0));
			}
		}

		/**
		 * 以失败结束本次请求。
		 *
		 * @param failure 失败异常
		 */
		private void fail(LlmException failure) {
			if (finished.compareAndSet(false, true)) {
				unsubscribe();
				if (failure.getKind() != LlmException.Kind.CANCELLED) {
					onError.accept(failure);
				}
			}
		}

		/**
		 * 取消订阅，切断后续数据。
		 */
		private void unsubscribe() {
			if (subscription != null) {
				subscription.cancel();
			}
		}
	}
}
