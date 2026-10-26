/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor.llm;

import java.util.Objects;

/**
 * LLM 的一次完整响应。
 *
 * <p>刻意不解析厂商返回的完整结构，只取本项目真正需要的三个字段：
 * 这样上游增删字段时不会导致解析失败。</p>
 *
 * @author Oiyuu
 * @version 0.2.0
 */
public final class LlmResponse {

	private final String content;
	private final String finishReason;
	private final int totalTokens;

	/**
	 * 构造一个响应对象。
	 *
	 * @param content 模型生成的文本，可为 {@code null}
	 * @param finishReason 结束原因，例如 {@code stop} 或 {@code length}
	 * @param totalTokens 本次请求消耗的总 token 数
	 */
	public LlmResponse(String content, String finishReason, int totalTokens) {
		this.content = content;
		this.finishReason = finishReason;
		this.totalTokens = totalTokens;
	}

	/**
	 * 创建一个只关心内容的响应，用于测试与 Mock。
	 *
	 * @param content 模型生成的文本
	 * @return 响应对象
	 */
	public static LlmResponse of(String content) {
		return new LlmResponse(content, "stop", 0);
	}

	/**
	 * 返回模型生成的文本。
	 *
	 * @return 生成内容，可能为 {@code null}
	 */
	public String getContent() {
		return content;
	}

	/**
	 * 返回结束原因。
	 *
	 * @return 结束原因，可能为 {@code null}
	 */
	public String getFinishReason() {
		return finishReason;
	}

	/**
	 * 返回消耗的总 token 数。
	 *
	 * @return token 数，未知时为 0
	 */
	public int getTotalTokens() {
		return totalTokens;
	}

	/**
	 * 返回生成内容的非空形式。
	 *
	 * 部分厂商在到达上限或内容被拦截时会返回 {@code null}，
	 * 而调用方通常不想为此写一堆判空逻辑，因此提供本方法。
	 *
	 * @return 生成内容，永不为 {@code null}
	 */
	public String getContentOrEmpty() {
		return content == null ? "" : content;
	}

	/**
	 * 判断本次输出是否因为触及长度上限而被截断。
	 *
	 * 被截断的诊断 JSON 往往是不完整的，业务侧应据此决定是否降级处理。
	 *
	 * @return 截断时返回 {@code true}
	 */
	public boolean isTruncated() {
		return Objects.equals("length", finishReason);
	}
}
