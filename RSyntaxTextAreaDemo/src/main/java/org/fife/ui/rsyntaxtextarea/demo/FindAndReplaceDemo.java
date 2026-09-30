package org.fife.ui.rsyntaxtextarea.demo;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;

/**
 * 一个简单的示例，演示如何在 RSyntaxTextArea 中执行查找与替换。
 * 工具栏并不怎么友好，这里只是为了展示 API 的用法。
 */
public final class FindAndReplaceDemo extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;

	private RSyntaxTextArea textArea;
	private JTextField searchField;
	private JCheckBox regexCB;
	private JCheckBox matchCaseCB;

	private FindAndReplaceDemo() {

		JPanel cp = new JPanel(new BorderLayout());

		textArea = new RSyntaxTextArea(20, 60);
		textArea.setText("one two three one\ntwo three one two\nthree one two three");
		textArea.setCaretPosition(0);
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		textArea.setCodeFoldingEnabled(true);
		RTextScrollPane sp = new RTextScrollPane(textArea);
		cp.add(sp);

		// 创建带搜索选项的工具栏
		JToolBar toolBar = new JToolBar();
		searchField = new JTextField(30);
		toolBar.add(searchField);
		final JButton nextButton = new JButton("查找下一个");
		nextButton.setActionCommand("FindNext");
		nextButton.addActionListener(this);
		toolBar.add(nextButton);
		JButton prevButton = new JButton("查找上一个");
		prevButton.setActionCommand("FindPrev");
		prevButton.addActionListener(this);
		toolBar.add(prevButton);
		regexCB = new JCheckBox("正则表达式");
		toolBar.add(regexCB);
		matchCaseCB = new JCheckBox("区分大小写");
		toolBar.add(matchCaseCB);
		cp.add(toolBar, BorderLayout.NORTH);

		// 当搜索框获得焦点时，回车向后搜索，Shift + 回车向前搜索
		InputMap im = searchField.getInputMap();
		ActionMap am = searchField.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "searchForward");
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_MASK), "searchBackward");
		am.put("searchForward", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				nextButton.doClick(0);
			}
		});
		am.put("searchBackward", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				prevButton.doClick(0);
			}
		});

		// 让 Ctrl+F / Cmd+F 聚焦搜索框
		int defaultMod = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		im = textArea.getInputMap();
		am = textArea.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, defaultMod), "doSearch");
		am.put("doSearch", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				searchField.requestFocusInWindow();
			}
		});

		setContentPane(cp);
		setTitle("查找与替换演示");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		pack();
		setLocationByPlatform(true);

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		// "FindNext" 表示向后搜索，"FindPrev" 表示向前搜索
		String command = e.getActionCommand();
		boolean forward = "FindNext".equals(command);

		// 创建定义搜索参数的对象
		SearchContext context = new SearchContext();
		String text = searchField.getText();
		if (text.isEmpty()) {
			return;
		}
		context.setSearchFor(text);
		context.setMatchCase(matchCaseCB.isSelected());
		context.setRegularExpression(regexCB.isSelected());
		context.setSearchForward(forward);
		context.setWholeWord(false);

		boolean found = SearchEngine.find(textArea, context).wasFound();
		if (!found) {
			JOptionPane.showMessageDialog(this, "未找到指定文本");
		}

	}

	public static void main(String[] args) {
		// 所有 Swing 应用都应在 EDT 上启动
		SwingUtilities.invokeLater(() -> {
			try {
				String laf = UIManager.getSystemLookAndFeelClassName();
				UIManager.setLookAndFeel(laf);
			} catch (Exception e) { /* 不会发生 */ }
			FindAndReplaceDemo demo = new FindAndReplaceDemo();
			demo.setVisible(true);
			demo.textArea.requestFocusInWindow();
		});
	}

}
