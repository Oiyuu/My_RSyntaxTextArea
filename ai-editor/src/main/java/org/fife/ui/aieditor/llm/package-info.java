/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */

/**
 * 与大语言模型交互的客户端层。
 *
 * 本包刻意只暴露抽象：调用方只依赖 {@link org.fife.ui.aieditor.llm.LlmClient}
 * 与 {@link org.fife.ui.aieditor.llm.LlmRequest}，从而在更换厂商或本地模型时无需改动业务代码。
 *
 * <p>所有实现都必须满足两个硬性要求：立即返回（绝不阻塞调用线程），且支持取消。</p>
 *
 * @author Oiyuu
 * @version 0.2.0
 */
package org.fife.ui.aieditor.llm;
