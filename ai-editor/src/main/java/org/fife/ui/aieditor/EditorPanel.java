/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor;

import org.fife.ui.rsyntaxtextarea.ErrorStrip;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * 单个编辑器视图，包含一个 {@link RSyntaxTextArea} 及其滚动容器与错误条。
 *
 * 这是所有 AI 能力的承载主体：
 * <ul>
 *   <li>#16 的 AI 代码审查会调用 {@code textArea.addParser(...)}，
 *       其结果自动出现在 {@link ErrorStrip} 上</li>
 *   <li>#17 的 Ghost Text 需要在编辑区组件上自绘</li>
 *   <li>#18 需要从这里提取选中文本作为上下文</li>
 * </ul>
 *
 * @author Oiyuu
 * @version 0.2.0
 */
public class EditorPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final RSyntaxTextArea textArea;
	private final RTextScrollPane scrollPane;
	private final ErrorStrip errorStrip;

	/**
	 * 创建一个使用 Java 语法高亮的编辑器面板。必须在 EDT 上调用。
	 */
	public EditorPanel() {
		super(new BorderLayout());

		this.textArea = createTextArea();
		this.scrollPane = new RTextScrollPane(textArea);
		this.errorStrip = new ErrorStrip(textArea);

		add(scrollPane, BorderLayout.CENTER);
		add(errorStrip, BorderLayout.LINE_END);
		setBorder(BorderFactory.createEmptyBorder());
	}

	/**
	 * 创建并配置编辑组件。
	 *
	 * 代码编辑器默认关闭自动换行：换行会让屏幕坐标与模型坐标不再一一对应，
	 * 后续 #17 的 Ghost Text 定位将因此变得不可靠。
	 *
	 * @return 已配置好的文本区域
	 */
	private RSyntaxTextArea createTextArea() {
		RSyntaxTextArea area = new RSyntaxTextArea();
		area.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		area.setCodeFoldingEnabled(true);
		area.setAntiAliasingEnabled(true);
		area.setLineWrap(false);
		area.setTabsEmulated(true);
		area.setTabSize(4);
		return area;
	}

	/**
	 * 返回本面板的编辑组件。
	 *
	 * @return 文本区域，永不为 {@code null}
	 */
	public RSyntaxTextArea getTextArea() {
		return textArea;
	}

	/**
	 * 设置是否启用自动换行。
	 *
	 * 开启换行时线性坐标换算不可靠，因此 #17 的 Ghost Text 会自动禁用，
	 * 界面上不会给出补偿。
	 *
	 * @param enabled 若为 {@code true} 则按窗口宽度自动换行
	 */
	public void setLineWrapEnabled(boolean enabled) {
		textArea.setLineWrap(enabled);
	}

	/**
	 * 返回当前是否启用了自动换行。
	 *
	 * @return 启用时返回 {@code true}
	 */
	public boolean isLineWrapEnabled() {
		return textArea.getLineWrap();
	}

	/**
	 * 用给定文本替换文档内容，并重置撤销历史。
	 *
	 * 读取文件时应优先使用 {@link RSyntaxTextArea#read(java.io.Reader, Object)}，
	 * 本方法仅用于首次填充的默认内容。
	 *
	 * @param text 初始显示的文本
	 */
	public void setDocumentText(String text) {
		textArea.setText(text);
		textArea.discardAllEdits();
		textArea.setCaretPosition(0);
	}
}
