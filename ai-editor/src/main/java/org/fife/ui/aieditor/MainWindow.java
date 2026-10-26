/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;

/**
 * AI 编辑器的主窗口，负责装配菜单栏、中央编辑区与底部状态栏。
 *
 * 后续各期功能的挂载点都在这里：
 * <ul>
 *   <li>左侧文件树与多标签页（#12）</li>
 *   <li>AI 代码审查 Parser 与对话侧栏（#16、#18）</li>
 *   <li>主题切换与偏好持久化（#13）</li>
 * </ul>
 *
 * @author Oiyuu
 * @version 0.2.0
 */
public class MainWindow extends JFrame {

	private static final long serialVersionUID = 1L;

	private final EditorPanel editorPanel;
	private final StatusBar statusBar;

	/**
	 * 创建主窗口。必须在 EDT 上调用。
	 */
	public MainWindow() {
		super("AI 编辑器");

		this.editorPanel = new EditorPanel();
		this.statusBar = new StatusBar();

		setJMenuBar(createMenuBar());
		add(createContentPane(), BorderLayout.CENTER);
		add(statusBar, BorderLayout.SOUTH);

		statusBar.attachTo(editorPanel.getTextArea());

		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setSize(new Dimension(1100, 720));
		setMinimumSize(new Dimension(900, 600));
		setLocationRelativeTo(null);
	}

	/**
	 * 装配中央内容区。
	 *
	 * 使用 {@link JSplitPane} 是为 #18 的 AI 对话侧栏预留位置，
	 * 当前阶段右侧尚不存在，因此该 divider 处于折叠状态。
	 *
	 * @return 内容面板
	 */
	private JSplitPane createContentPane() {
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, editorPanel, null);
		splitPane.setDividerSize(0);
		splitPane.setEnabled(false);
		splitPane.setResizeWeight(1.0);
		return splitPane;
	}

	/**
	 * 创建菜单栏。
	 *
	 * 当前仅提供最小可用的文件与视图菜单，AI 相关菜单项会在后续 issue 中补充。
	 *
	 * @return 菜单栏
	 */
	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu fileMenu = new JMenu("文件");
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.add(createDisabledItem("新建", KeyEvent.VK_N));
		fileMenu.add(createDisabledItem("打开…", KeyEvent.VK_O));
		fileMenu.add(createDisabledItem("保存", KeyEvent.VK_S));
		fileMenu.addSeparator();
		fileMenu.add(createDisabledItem("退出", KeyEvent.VK_X));
		menuBar.add(fileMenu);

		JMenu viewMenu = new JMenu("视图");
		viewMenu.setMnemonic(KeyEvent.VK_V);
		JCheckBoxMenuItem wrapItem = new JCheckBoxMenuItem("自动换行", false);
		wrapItem.addActionListener(event -> editorPanel.setLineWrapEnabled(wrapItem.isSelected()));
		viewMenu.add(wrapItem);
		menuBar.add(viewMenu);

		JMenu helpMenu = new JMenu("帮助");
		helpMenu.setMnemonic(KeyEvent.VK_H);
		helpMenu.add(createDisabledItem("关于", KeyEvent.VK_A));
		menuBar.add(helpMenu);

		return menuBar;
	}

	/**
	 * 创建一个尚未实现的菜单项。
	 *
	 * 先在界面上占位，避免用户点击后毫无反应；
	 * 真正的功能由后续 issue 接入。
	 *
	 * @param text 菜单项文字
	 * @param mnemonic 助记符
	 * @return 已禁用并带标准快捷键提示的菜单项
	 */
	private JMenuItem createDisabledItem(String text, int mnemonic) {
		JMenuItem item = new JMenuItem(text, mnemonic);
		item.setEnabled(false);
		item.setAccelerator(KeyStroke.getKeyStroke(mnemonic, menuShortcutMask()));
		item.setHorizontalTextPosition(SwingConstants.LEFT);
		return item;
	}

	/**
	 * 返回当前平台的菜单快捷键掩码。
	 *
	 * @return Windows/Linux 为 {@code Ctrl}，macOS 为 {@code Command}
	 */
	private static int menuShortcutMask() {
		return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
	}

	/**
	 * 返回中央编辑器面板。供后续 issue 挂载 AI 能力时使用。
	 *
	 * @return 编辑器面板，永不为 {@code null}
	 */
	public final EditorPanel getEditorPanel() {
		return editorPanel;
	}

	/**
	 * 返回底部状态栏。供后续 issue 显示 AI 请求状态时使用。
	 *
	 * @return 状态栏，永不为 {@code null}
	 */
	public final StatusBar getStatusBar() {
		return statusBar;
	}
}
