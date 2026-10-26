/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor.llm;

/**
 * LLM 调用失败时抛出的统一异常。
 *
 * 把超时、限流、鉴权失败、解析失败等一概收敛为本类型，
 * 界面层只需在 status 上显示 {@link #getMessage()} 即可，不必区分底层是 HTTP 还是 JSON 的问题。
 *
 * @author Oiyuu
 * @version 0.2.0
 */
public class LlmException extends Exception {

	private static final long serialVersionUID = 1L;

	private final Kind kind;

	/**
	 * 构造一个异常。
	 *
	 * @param kind 失败类型
	 * @param message 面向用户的错误描述
	 */
	public LlmException(Kind kind, String message) {
		this(kind, message, null);
	}

	/**
	 * 构造一个带原因的异常。
	 *
	 * @param kind 失败类型
	 * @param message 面向用户的错误描述
	 * @param cause 原始异常
	 */
	public LlmException(Kind kind, String message, Throwable cause) {
		super(message, cause);
		this.kind = kind;
	}

	/**
	 * 返回失败类型。
	 *
	 * @return 失败类型
	 */
	public Kind getKind() {
		return kind;
	}

	/**
	 * 失败类型，供调用方决定是否需要重试或提示用户。
	 */
	public enum Kind {

		/** 未配置 API Key，或 Key 未被服务端接受。 */
		AUTHENTICATION,

		/** 连接超时或读取超时。 */
		TIMEOUT,

		/** 被服务端限流，可以稍后重试。 */
		RATE_LIMITED,

		/** 服务端返回了 5xx，属于临时性故障。 */
		SERVER_ERROR,

		/** 响应体不是合法的 JSON，或缺失必要字段。 */
		MALFORMED_RESPONSE,

		/** 请求被取消。这不是一种真正的错误，界面层通常直接忽略。 */
		CANCELLED,

		/** 其他未归类的失败。 */
		UNKNOWN
	}
}
