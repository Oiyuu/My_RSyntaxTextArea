/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor.llm;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * 增量解析服务端发送过来的 SSE（Server-Sent Events）数据流。
 *
 * <p>之所以要专门写一个类而不是简单地按行切分：HTTP 的分块边界与 SSE 的行边界并不对齐，
 * 一行可能被切成两次到达。本类内部维护一块残留缓冲区，只有凑出完整一行才向上回调。</p>
 *
 * <p>数据格式形如：</p>
 * <pre>
 * data: {"choices":[{"delta":{"content":"你"}}]}
 * data: {"choices":[{"delta":{"content":"好"}}]}
 * data: [DONE]
 * </pre>
 *
 * @author Oiyuu
 * @version 0.2.0
 */
public final class SseParser {

	private final StringBuilder buffer = new StringBuilder();
	private final Consumer<String> onDelta;
	private final Runnable onDone;
	private boolean done;

	/**
	 * 创建一个解析器。
	 *
	 * @param onDelta 每解析出一段增量文本时回调
	 * @param onDone 收到 {@code [DONE]} 标记时回调
	 */
	public SseParser(Consumer<String> onDelta, Runnable onDone) {
		this.onDelta = Objects.requireNonNull(onDelta, "onDelta 不可为 null");
		this.onDone = Objects.requireNonNull(onDone, "onDone 不可为 null");
	}

	/**
	 * 追加一段新到达的原始数据。
	 *
	 * @param chunk 本次收到的文本，可为 {@code null}
	 */
	public void feed(String chunk) {
		if (chunk == null || chunk.isEmpty() || done) {
			return;
		}
		buffer.append(chunk);
		int newline = buffer.indexOf("\n");
		while (newline >= 0) {
			String line = buffer.substring(0, newline).trim();
			buffer.delete(0, newline + 1);
			handleLine(line);
			if (done) {
				return;
			}
			newline = buffer.indexOf("\n");
		}
	}

	/**
	 * 处理一整行 SSE 内容。
	 *
	 * @param line 已去除首尾空白的行
	 */
	private void handleLine(String line) {
		if (line.isEmpty()) {
			return;
		}
		if (!line.startsWith("data:")) {
			return;
		}
		String payload = line.substring("data:".length()).trim();
		if (payload.isEmpty()) {
			return;
		}
		if (isDoneMarker(payload)) {
			done = true;
			onDone.run();
			return;
		}
		String delta = extractDelta(payload);
		if (delta != null && !delta.isEmpty()) {
			onDelta.accept(delta);
		}
	}

	/**
	 * 判断载荷是否为流结束标记。
	 *
	 * @param payload {@code data:} 之后的内容
	 * @return 是结束标记时返回 {@code true}
	 */
	private boolean isDoneMarker(String payload) {
		return "[DONE]".equals(payload);
	}

	/**
	 * 从单个 SSE 帧中取出增量文本。
	 *
	 * 解析失败时返回 {@code null}：服务端偶尔会在正常帧之间插入心跳注释，
	 * 静默跳过比让整个请求失败更合理。
	 *
	 * @param payload 单个帧的 JSON 文本
	 * @return 增量文本，解析失败或不存在时为 {@code null}
	 */
	private String extractDelta(String payload) {
		try {
			JsonElement root = JsonParser.parseString(payload);
			if (!root.isJsonObject()) {
				return null;
			}
			JsonObject choicesFirst = firstChoice(root.getAsJsonObject());
			if (choicesFirst == null) {
				return null;
			}
			JsonElement delta = choicesFirst.get("delta");
			if (delta == null || !delta.isJsonObject()) {
				return null;
			}
			JsonElement content = delta.getAsJsonObject().get("content");
			if (content == null || content.isJsonNull()) {
				return null;
			}
			return content.getAsString();
		}
		catch (RuntimeException ex) {
			return null;
		}
	}

	/**
	 * 取第一个选项对象。
	 *
	 * @param root 响应根对象
	 * @return 第一个选项，不存在时为 {@code null}
	 */
	private JsonObject firstChoice(JsonObject root) {
		JsonElement choices = root.get("choices");
		if (choices == null || !choices.isJsonArray() || choices.getAsJsonArray().isEmpty()) {
			return null;
		}
		JsonElement first = choices.getAsJsonArray().get(0);
		return first.isJsonObject() ? first.getAsJsonObject() : null;
	}

	/**
	 * 返回是否已经收到结束标记。
	 *
	 * @return 已结束时返回 {@code true}
	 */
	public boolean isDone() {
		return done;
	}
}
