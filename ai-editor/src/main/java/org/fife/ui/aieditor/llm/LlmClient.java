/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor.llm;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 大语言模型客户端的统一抽象。
 *
 * 本项目的所有 AI 能力（代码审查、行内补全、悬浮解释、对话侧栏）都通过本接口发起请求，
 * 从而把"用哪家模型""怎么鉴权""如何重试"这些细节收敛到一个地方。
 *
 * <p><strong>实现契约：</strong></p>
 * <ul>
 *   <li>立即返回，绝不阻塞调用线程（Swing 项目里阻塞 EDT 会直接卡死界面）</li>
 *   <li>失败一律通过 {@code CompletableFuture} 的异常完成，不抛受检异常</li>
 *   <li>取消后不得再触发任何回调</li>
 * </ul>
 *
 * @author Oiyuu
 * @version 0.2.0
 */
public interface LlmClient {

	/**
	 * 发起一次非流式请求，等待模型完整输出后一次性返回。
	 *
	 * @param request 请求内容，不可为 {@code null}
	 * @return 异步结果；失败时以 {@link LlmException} 异常完成
	 */
	CompletableFuture<LlmResponse> complete(LlmRequest request);

	/**
	 * 发起一次流式请求，模型每产出一段就回调一次。
	 *
	 * 流式之所以重要，在于体感：代码生成的等待时间往往以秒计，
	 * 若等到全部完成才显示，用户会以为界面卡死了。
	 *
	 * @param request 请求内容，不可为 {@code null}
	 * @param onChunk 每收到一段增量文本时回调一次，参数为增量内容
	 * @param onComplete 全部完成时的回调，参数为完整响应
	 * @param onError 失败时的回调，参数通常是 {@link LlmException}
	 * @return 流句柄，可用于中途取消
	 */
	StreamHandle completeStream(LlmRequest request, Consumer<String> onChunk, Consumer<LlmResponse> onComplete,
			Consumer<Throwable> onError);
}
