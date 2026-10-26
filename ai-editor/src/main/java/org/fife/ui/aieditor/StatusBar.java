/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Locale;

/**
 * 底部状态栏，显示光标位置、语法风格与临时提示信息。
 *
 * 后续 #16 / #18 会复用 {@link #setMessage(String)} 展示 AI 请求状态
 * （例如 "AI 正在思考…"）。
 *
 * @author Oiyuu
 * @version 0.2.0
 */
public class StatusBar extends JPanel implements CaretListener {

	private static final long serialVersionUID = 1L;

	/** 状态栏的高度。 */
	private static final int HEIGHT = 22;

	private final JLabel caretPositionLabel;
	private final JLabel syntaxStyleLabel;
	private final JLabel messageLabel;

	/**
	 * 创建状态栏。必须在 EDT 上调用。
	 */
	public StatusBar() {
		super(new BorderLayout());

		this.caretPositionLabel = createInfoLabel("行 1, 列 1", 120);
		this.syntaxStyleLabel = createInfoLabel("-", 90);
		this.messageLabel = new JLabel("就绪");

		// 右侧两个定宽标签放在同一个容器里，避免 BorderLayout 同位置互相挤占。
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.LINE_AXIS));
		rightPanel.add(syntaxStyleLabel);
		rightPanel.add(caretPositionLabel);

		messageLabel.setHorizontalAlignment(SwingConstants.LEFT);
		add(messageLabel, BorderLayout.CENTER);
		add(rightPanel, BorderLayout.LINE_END);

		setPreferredSize(new Dimension(10, HEIGHT));
		setMinimumSize(new Dimension(10, HEIGHT));
	}

	/**
	 * 创建一个固定宽度的信息标签。
	 *
	 * @param initialText 初始文字
	 * @param width 期望宽度（像素）
	 * @return 已配置好的标签
	 */
	private static JLabel createInfoLabel(String initialText, int width) {
		JLabel label = new JLabel(initialText);
		label.setHorizontalAlignment(SwingConstants.CENTER);
		label.setPreferredSize(new Dimension(width, HEIGHT));
		label.setMaximumSize(new Dimension(width, HEIGHT));
		return label;
	}

	/**
	 * 监听指定编辑区的光标变化，实时更新行列号与语法风格。
	 *
	 * @param textArea 要监听的文本区域
	 */
	public void attachTo(RSyntaxTextArea textArea) {
		textArea.addCaretListener(this);
		syntaxStyleLabel.setText(shortStyleName(textArea.getSyntaxEditingStyle()));
		updatePosition(textArea);
	}

	/**
	 * 光标移动时的回调。运行在 EDT 上。
	 *
	 * @param event 光标事件
	 */
	@Override
	public void caretUpdate(CaretEvent event) {
		if (event.getSource() instanceof RSyntaxTextArea) {
			updatePosition((RSyntaxTextArea) event.getSource());
		}
	}

	/**
	 * 重新计算并显示行列号。
	 *
	 * @param textArea 当前编辑区
	 */
	private void updatePosition(RSyntaxTextArea textArea) {
		int caret = textArea.getCaretPosition();
		try {
			int line = textArea.getLineOfOffset(caret);
			int lineStart = textArea.getLineStartOffset(line);
			caretPositionLabel.setText("行 " + (line + 1) + ", 列 " + (caret - lineStart + 1));
		}
		catch (Exception ex) {
			// 文档正在被并发修改时偏移可能越界（BadLocationException），忽略本次刷新即可。
			caretPositionLabel.setText("行 -, 列 -");
		}
	}

	/**
	 * 显示一条临时提示信息。
	 *
	 * @param message 提示文字；传 {@code null} 或空白则显示为 "就绪"
	 */
	public void setMessage(String message) {
		if (message == null || message.trim().isEmpty()) {
			messageLabel.setText("就绪");
		}
		else {
			messageLabel.setText(message);
		}
	}

	/**
	 * 把完整的语法风格名缩写为适合状态栏宽度的短名。
	 *
	 * @param style 语法风格键，例如 {@code text/java}
	 * @return 缩写后的显示名，例如 {@code JAVA}
	 */
	private static String shortStyleName(String style) {
		if (style == null) {
			return "-";
		}
		int slash = style.indexOf('/');
		String name = slash >= 0 ? style.substring(slash + 1) : style;
		return name.toUpperCase(Locale.ROOT);
	}
}
