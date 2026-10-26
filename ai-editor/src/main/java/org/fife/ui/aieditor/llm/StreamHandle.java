/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor.llm;

/**
 * 流式请求的句柄，用于中途取消与查询状态。
 *
 * @author Oiyuu
 * @version 0.2.0
 */
public interface StreamHandle {

	/**
	 * 取消本次请求。
	 *
	 * 实现必须保证：调用后不再触发任何增量或完成回调，
	 * 这是 "取消义后在界面上依然追加文字" 这类问题的根源所在。
	 */
	void cancel();

	/**
	 * 查询本次请求是否已经结束。
	 *
	 * @return 已完成、已失败或已取消时返回 {@code true}
	 */
	boolean isDone();
}
