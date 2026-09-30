/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.rsyntaxtextarea.demo;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.font.TextAttribute;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.fife.ui.rsyntaxtextarea.*;
import org.fife.ui.rtextarea.FoldIndicatorStyle;
import org.fife.ui.rtextarea.Gutter;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.LineNumberFormatter;
import org.fife.ui.rtextarea.LineNumberList;


/**
 * 演示程序使用的根面板。applet 与独立应用共用同一套 UI。
 *
 * @author Robert Futrell
 * @version 1.0
 */
public class DemoRootPane extends JRootPane implements HyperlinkListener,
											SyntaxConstants {

	private RTextScrollPane scrollPane;
	private RSyntaxTextArea textArea;


	/**
	 * 构造根面板：创建文本区、滚动面板、错误条与菜单栏。
	 */
	DemoRootPane() {
		textArea = createTextArea();
		setText("JavaExample.txt");
		textArea.setSyntaxEditingStyle(SYNTAX_STYLE_D);
		scrollPane = new RTextScrollPane(textArea, true);
		Gutter gutter = scrollPane.getGutter();
		gutter.setBookmarkingEnabled(true);
		URL url = getClass().getResource("bookmark.png");
		gutter.setBookmarkIcon(new ImageIcon(url));
		getContentPane().add(scrollPane);
		ErrorStrip errorStrip = new ErrorStrip(textArea);
		//errorStrip.setBackground(java.awt.Color.blue);
		getContentPane().add(errorStrip, BorderLayout.LINE_END);
		setJMenuBar(createMenuBar());
	}


	/**
	 * 向"外观"菜单中添加一个 LookAndFeel 选项。
	 *
	 * @param info LookAndFeel 信息。
	 * @param bg 单选按钮组，保证同一时刻只选中一项。
	 * @param menu 目标菜单。
	 */
	private void addLookAndFeelItem(UIManager.LookAndFeelInfo info, ButtonGroup bg,
									JMenu menu) {
		LookAndFeelAction a = new LookAndFeelAction(info);
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(a);
		bg.add(item);
		menu.add(item);
	}


	/**
	 * 向"语言"菜单中添加一个语法风格选项。
	 *
	 * @param name 菜单项显示名称。
	 * @param res 选中后加载到文本区的示例资源文件名。
	 * @param style 对应的语法风格常量。
	 * @param bg 单选按钮组。
	 * @param menu 目标菜单。
	 */
	private void addSyntaxItem(String name, String res, String style,
			ButtonGroup bg, JMenu menu) {
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(
				new ChangeSyntaxStyleAction(name, res, style));
		bg.add(item);
		menu.add(item);
	}


	/**
	 * 向"主题"菜单中添加一个主题选项。
	 *
	 * @param name 菜单项显示名称。
	 * @param themeXml 主题 XML 文件名。
	 * @param bg 单选按钮组。
	 * @param menu 目标菜单。
	 */
	private void addThemeItem(String name, String themeXml, ButtonGroup bg,
			JMenu menu) {
		JRadioButtonMenuItem item = new JRadioButtonMenuItem(
				new ThemeAction(name, themeXml));
		bg.add(item);
		menu.add(item);
	}


	/**
	 * 创建一个"按指定主题复制为带样式文本"的动作。
	 *
	 * @param themeName 主题名称，对应 themes 目录下的 XML 文件。
	 * @return 对应的动作。
	 * @throws IOException 如果主题资源加载失败。
	 */
	private static Action createCopyAsStyledTextAction(String themeName) throws IOException {
		String resource = "/org/fife/ui/rsyntaxtextarea/themes/" + themeName + ".xml";
		Theme theme = Theme.load(DemoRootPane.class.getResourceAsStream(resource));
		return new RSyntaxTextAreaEditorKit.CopyCutAsStyledTextAction(themeName, theme, false);
	}


	/**
	 * 创建菜单栏。
	 *
	 * @return 菜单栏。
	 */
	private JMenuBar createMenuBar() {

		JMenuBar mb = new JMenuBar();

		JMenu menu = new JMenu("语言");
		ButtonGroup bg = new ButtonGroup();
		addSyntaxItem("无", "NoneExample.txt", SYNTAX_STYLE_NONE, bg, menu);
		addSyntaxItem("6502 Assembler", "Assembler6502.txt", SYNTAX_STYLE_ASSEMBLER_6502, bg, menu);
		addSyntaxItem("ActionScript", "ActionScriptExample.txt", SYNTAX_STYLE_ACTIONSCRIPT, bg, menu);
		addSyntaxItem("C",    "CExample.txt", SYNTAX_STYLE_C, bg, menu);
		addSyntaxItem("C#",    "CSharpExample.txt", SYNTAX_STYLE_CSHARP, bg, menu);
		addSyntaxItem("Clojure",  "ClojureExample.txt", SYNTAX_STYLE_CLOJURE, bg, menu);
		addSyntaxItem("CSS",  "CssExample.txt", SYNTAX_STYLE_CSS, bg, menu);
		addSyntaxItem("Dockerfile", "DockerfileExample.txt", SYNTAX_STYLE_DOCKERFILE, bg, menu);
		addSyntaxItem("Go", "GoExample.txt", SYNTAX_STYLE_GO, bg, menu);
		addSyntaxItem("Handlebars", "HandlebarsExample.txt", SYNTAX_STYLE_HANDLEBARS, bg, menu);
		addSyntaxItem("Hosts", "HostsExample.txt", SYNTAX_STYLE_HOSTS, bg, menu);
		addSyntaxItem("HTML", "HtmlExample.txt", SYNTAX_STYLE_HTML, bg, menu);
		addSyntaxItem("INI", "IniExample.txt", SYNTAX_STYLE_INI, bg, menu);
		addSyntaxItem("Java", "JavaExample.txt", SYNTAX_STYLE_JAVA, bg, menu);
		addSyntaxItem("JavaScript", "JavaScriptExample.txt", SYNTAX_STYLE_JAVASCRIPT, bg, menu);
		addSyntaxItem("JSP", "JspExample.txt", SYNTAX_STYLE_JSP, bg, menu);
		addSyntaxItem("JSON", "JsonExample.txt", SYNTAX_STYLE_JSON_WITH_COMMENTS, bg, menu);
		addSyntaxItem("Kotlin", "KotlinExample.txt", SYNTAX_STYLE_KOTLIN, bg, menu);
		addSyntaxItem("LaTeX", "LatexExample.txt", SYNTAX_STYLE_LATEX, bg, menu);
		addSyntaxItem("Less", "LessExample.txt", SYNTAX_STYLE_LESS, bg, menu);
		addSyntaxItem("Markdown", "MarkdownExample.txt", SYNTAX_STYLE_MARKDOWN, bg, menu);
		addSyntaxItem("Perl", "PerlExample.txt", SYNTAX_STYLE_PERL, bg, menu);
		addSyntaxItem("PHP",  "PhpExample.txt", SYNTAX_STYLE_PHP, bg, menu);
		addSyntaxItem("PowerShell",  "PowershellExample.txt", SYNTAX_STYLE_POWERSHELL, bg, menu);
		addSyntaxItem("Proto", "ProtoExample.txt", SYNTAX_STYLE_PROTO, bg, menu);
		addSyntaxItem("Python",  "PythonExample.txt", SYNTAX_STYLE_PYTHON, bg, menu);
		addSyntaxItem("Ruby", "RubyExample.txt", SYNTAX_STYLE_RUBY, bg, menu);
		addSyntaxItem("Rust", "RustExample.txt", SYNTAX_STYLE_RUST, bg, menu);
		addSyntaxItem("SQL",  "SQLExample.txt", SYNTAX_STYLE_SQL, bg, menu);
		addSyntaxItem("TypeScript", "TypeScriptExample.txt", SYNTAX_STYLE_TYPESCRIPT, bg, menu);
		addSyntaxItem("VHDL", "VhdlExample.txt", SYNTAX_STYLE_VHDL, bg, menu);
		addSyntaxItem("XML",  "XMLExample.txt", SYNTAX_STYLE_XML, bg, menu);
		addSyntaxItem("YAML", "YamlExample.txt", SYNTAX_STYLE_YAML, bg, menu);
		menu.getItem(2).setSelected(true);
		mb.add(menu);

		menu = new JMenu("视图");
		JMenu foldStyleSubMenu = new JMenu("折叠区域样式");
		JRadioButtonMenuItem classicStyleItem = new JRadioButtonMenuItem(
			new FoldStyleAction(FoldIndicatorStyle.CLASSIC));
		JRadioButtonMenuItem modernStyleItem = new JRadioButtonMenuItem(new FoldStyleAction(FoldIndicatorStyle.MODERN));
		modernStyleItem.setSelected(true);
		bg = new ButtonGroup();
		bg.add(classicStyleItem);
		bg.add(modernStyleItem);
		foldStyleSubMenu.add(classicStyleItem);
		foldStyleSubMenu.add(modernStyleItem);
		menu.add(foldStyleSubMenu);
		JMenu lineNumberFormatSubMenu = new JMenu("行号格式");
		JRadioButtonMenuItem normalStyleItem = new JRadioButtonMenuItem(
			new LineNumberFormatAction("常规", LineNumberList.DEFAULT_LINE_NUMBER_FORMATTER));
		JRadioButtonMenuItem hinduArabicStyleItem = new JRadioButtonMenuItem(
			new LineNumberFormatAction("阿拉伯数字", new HinduArabicLineNumberFormatter()));
		normalStyleItem.setSelected(true);
		bg = new ButtonGroup();
		bg.add(normalStyleItem);
		bg.add(hinduArabicStyleItem);
		lineNumberFormatSubMenu.add(normalStyleItem);
		lineNumberFormatSubMenu.add(hinduArabicStyleItem);
		menu.add(lineNumberFormatSubMenu);
		JCheckBoxMenuItem cbItem = new JCheckBoxMenuItem(new CodeFoldingAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new ViewLineHighlightAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new ViewLineNumbersAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new AnimateBracketMatchingAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new BookmarksAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new WordWrapAction());
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new MarkOccurrencesAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new TabLinesAction());
		menu.add(cbItem);
		mb.add(menu);

		menu = new JMenu("字体");
		cbItem = new JCheckBoxMenuItem(new ToggleAntiAliasingAction());
		cbItem.setSelected(true);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new ToggleFractionalFontMetricsAction());
		cbItem.setSelected(false);
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new ToggleKerningAction());
		menu.add(cbItem);
		cbItem = new JCheckBoxMenuItem(new ToggleLigatureSupportAction());
		menu.add(cbItem);
		mb.add(menu);

		menu = new JMenu("外观");
		bg = new ButtonGroup();
		UIManager.LookAndFeelInfo[] infos = UIManager.getInstalledLookAndFeels();
		for (UIManager.LookAndFeelInfo info : infos) {
			addLookAndFeelItem(info, bg, menu);
		}
		mb.add(menu);

		bg = new ButtonGroup();
		menu = new JMenu("主题");
		addThemeItem("默认", "default.xml", bg, menu);
		addThemeItem("默认（跟随系统）", "default-alt.xml", bg, menu);
		addThemeItem("深色", "dark.xml", bg, menu);
		addThemeItem("Druid", "druid.xml", bg, menu);
		addThemeItem("Monokai", "monokai.xml", bg, menu);
		addThemeItem("Eclipse", "eclipse.xml", bg, menu);
		addThemeItem("IDEA", "idea.xml", bg, menu);
		addThemeItem("Visual Studio", "vs.xml", bg, menu);
		mb.add(menu);

		menu = new JMenu("帮助");
		JMenuItem item = new JMenuItem(new AboutAction());
		menu.add(item);
		mb.add(menu);

		return mb;

	}


	/**
	 * 创建本应用使用的文本区。
	 *
	 * @return 文本区。
	 */
	private RSyntaxTextArea createTextArea() {

		RSyntaxTextArea textArea = new RSyntaxTextArea(25, 70);
		textArea.setTabSize(3);
		textArea.setCaretPosition(0);
		textArea.addHyperlinkListener(this);
		textArea.requestFocusInWindow();
		textArea.setMarkOccurrences(true);
		textArea.setCodeFoldingEnabled(true);
		textArea.setClearWhitespaceLinesEnabled(false);

		InputMap im = textArea.getInputMap();
		ActionMap am = textArea.getActionMap();
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "decreaseFontSize");
		am.put("decreaseFontSize", new RSyntaxTextAreaEditorKit.DecreaseFontSizeAction());
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0), "increaseFontSize");
		am.put("increaseFontSize", new RSyntaxTextAreaEditorKit.IncreaseFontSizeAction());

		int ctrlShift = InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;
		im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, ctrlShift), "copyAsStyledText");
		am.put("copyAsStyledText", new RSyntaxTextAreaEditorKit.CopyCutAsStyledTextAction(false));

		try {

			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_M, ctrlShift), "copyAsStyledTextMonokai");
			am.put("copyAsStyledTextMonokai", createCopyAsStyledTextAction("monokai"));

			im.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, ctrlShift), "copyAsStyledTextEclipse");
			am.put("copyAsStyledTextEclipse", createCopyAsStyledTextAction("eclipse"));
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		// 本演示允许分别切换 LookAndFeel 与 RSyntaxTextArea 主题，
		// 因此将该属性设为 true，使括号匹配弹出框的观感更好。
		// 若应用中开发者能保证 RSTA 主题与 LookAndFeel 的明暗始终一致，
		// 则可以省略该属性。
		System.setProperty(MatchedBracketPopup.PROPERTY_CONSIDER_TEXTAREA_BACKGROUND, "true");

		return textArea;
	}


	/**
	 * 让文本区获得焦点。
	 */
	void focusTextArea() {
		textArea.requestFocusInWindow();
	}


	/**
	 * 当文本区中的超链接被点击时调用。
	 *
	 * @param e 事件。
	 */
	@Override
	public void hyperlinkUpdate(HyperlinkEvent e) {
		if (e.getEventType()==HyperlinkEvent.EventType.ACTIVATED) {
			URL url = e.getURL();
			if (url==null) {
				UIManager.getLookAndFeel().provideErrorFeedback(null);
			}
			else {
				JOptionPane.showMessageDialog(this,
									"点击的链接：\n" + url);
			}
		}
	}


	/**
	 * 将文本区的内容设置为指定资源中的内容。
	 *
	 * @param resource 要加载的资源。
	 */
	private void setText(String resource) {
		BufferedReader r;
		try {
			r = new BufferedReader(new InputStreamReader(
					getClass().getResourceAsStream(resource), StandardCharsets.UTF_8));
			textArea.read(r, null);
			r.close();
			textArea.setCaretPosition(0);
			textArea.discardAllEdits();
		} catch (RuntimeException re) {
			throw re; // FindBugs
		} catch (Exception e) { // Never happens
			textArea.setText("在此输入内容，即可看到语法高亮效果");
		}
	}

	/**
	 * 显示"关于"对话框。
	 */
	private class AboutAction extends AbstractAction {

		AboutAction() {
			putValue(NAME, "关于 RSyntaxTextArea...");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			JOptionPane.showMessageDialog(DemoRootPane.this,
					"<html><b>RSyntaxTextArea</b> —— 一个 Swing 语法高亮文本组件" +
					"<br>基于修改版 BSD 许可证发布",
					"关于 RSyntaxTextArea",
					JOptionPane.INFORMATION_MESSAGE);
		}

	}

	/**
	 * 切换是否启用括号匹配动画。
	 */
	private class AnimateBracketMatchingAction extends AbstractAction {

		AnimateBracketMatchingAction() {
			putValue(NAME, "括号匹配动画");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			textArea.setAnimateBracketMatching(
						!textArea.getAnimateBracketMatching());
		}

	}

	/**
	 * 切换是否启用书签。
	 */
	private class BookmarksAction extends AbstractAction {

		BookmarksAction() {
			putValue(NAME, "书签");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			scrollPane.setIconRowHeaderEnabled(
							!scrollPane.isIconRowHeaderEnabled());
		}

	}

	/**
	 * 将语法风格切换为新的值。
	 */
	private class ChangeSyntaxStyleAction extends AbstractAction {

		private String res;
		private String style;

		ChangeSyntaxStyleAction(String name, String res, String style) {
			putValue(NAME, name);
			this.res = res;
			this.style = style;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			setText(res);
			textArea.setCaretPosition(0);
			textArea.setSyntaxEditingStyle(style);
		}

	}

	/**
	 * 切换是否启用代码折叠。
	 */
	private class CodeFoldingAction extends AbstractAction {

		CodeFoldingAction() {
			putValue(NAME, "代码折叠");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			textArea.setCodeFoldingEnabled(!textArea.isCodeFoldingEnabled());
		}

	}

	/**
	 * 更改行号栏中折叠指示区域的外观。
	 */
	private class FoldStyleAction extends AbstractAction {

		private final FoldIndicatorStyle style;

		FoldStyleAction(FoldIndicatorStyle style) {
			this.style = style;
			// 折叠样式只有固定的两种，这里直接给出中文名称
			String name;
			if (style == FoldIndicatorStyle.CLASSIC) {
				name = "经典";
			}
			else {
				name = "现代";
			}
			putValue(NAME, name);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			scrollPane.getGutter().setFoldIndicatorStyle(style);
		}

	}

	/**
	 * 更改行号的显示方式。
	 */
	private class LineNumberFormatAction extends AbstractAction {

		private final LineNumberFormatter formatter;

		LineNumberFormatAction(String name, LineNumberFormatter formatter) {
			this.formatter = formatter;
			putValue(NAME, name);
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			scrollPane.getGutter().setLineNumberFormatter(formatter);
		}

	}

	/**
	 * 更改演示程序的外观（LookAndFeel）。
	 */
	private class LookAndFeelAction extends AbstractAction {

		private UIManager.LookAndFeelInfo info;

		LookAndFeelAction(UIManager.LookAndFeelInfo info) {
			putValue(NAME, info.getName());
			this.info = info;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			try {
				UIManager.setLookAndFeel(info.getClassName());
				SwingUtilities.updateComponentTreeUI(DemoRootPane.this);
			} catch (RuntimeException re) {
				throw re; // FindBugs
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	/**
	 * 切换是否启用"标记相同内容"。
	 */
	private class MarkOccurrencesAction extends AbstractAction {

		MarkOccurrencesAction() {
			putValue(NAME, "标记相同内容");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			textArea.setMarkOccurrences(!textArea.getMarkOccurrences());
		}

	}

	/**
	 * 切换是否启用"制表符竖线"。
	 */
	private class TabLinesAction extends AbstractAction {

		private boolean selected;

		TabLinesAction() {
			putValue(NAME, "制表符竖线");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			selected = !selected;
			textArea.setPaintTabLines(selected);
		}

	}

	/**
	 * 更改主题。
	 */
	private class ThemeAction extends AbstractAction {

		private String xml;

		ThemeAction(String name, String xml) {
			putValue(NAME, name);
			this.xml = xml;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			InputStream in = getClass().
				getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/" + xml);
			try {
				// Keep the text area's font since it has our e.g. ligature hints
				Theme theme = Theme.load(in, textArea.getFont());
				theme.apply(textArea);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}

	}

	/**
	 * 切换抗锯齿。
	 */
	private class ToggleAntiAliasingAction extends AbstractAction {

		ToggleAntiAliasingAction() {
			putValue(NAME, "抗锯齿");
			int defaultModifier = getToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_DOWN_MASK;
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_A, defaultModifier));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			textArea.setAntiAliasingEnabled(!textArea.getAntiAliasingEnabled());
		}

	}

	/**
	 * 切换小数字体度量（通常不需要改动该项）。
	 */
	private class ToggleFractionalFontMetricsAction extends AbstractAction {

		ToggleFractionalFontMetricsAction() {
			putValue(NAME, "小数字体度量");
			int defaultModifier = getToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_DOWN_MASK;
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F, defaultModifier));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			textArea.setFractionalFontMetricsEnabled(!textArea.getFractionalFontMetricsEnabled());
		}

	}

	/**
	 * 切换字距调整（kerning）。注意：在较老的 JVM 上可能较慢（见 XXX）。
	 */
	private class ToggleKerningAction extends AbstractAction {

		ToggleKerningAction() {
			putValue(NAME, "字距调整");
			int defaultModifier = getToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_DOWN_MASK;
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_1, defaultModifier));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Font font = textArea.getFont();
			Integer prev = (Integer)font.getAttributes().get(TextAttribute.KERNING);
			int toggledValue = TextAttribute.KERNING_ON.equals(prev) ? -1 : TextAttribute.KERNING_ON;
			Map<TextAttribute, Object> attrs = new HashMap<>();
			attrs.put(TextAttribute.KERNING, toggledValue);
			textArea.setFont(font.deriveFont(attrs));
		}

	}

	/**
	 * 切换连字（ligature）支持。注意：在较老的 JVM 上可能较慢（见 XXX）。
	 */
	private class ToggleLigatureSupportAction extends AbstractAction {

		ToggleLigatureSupportAction() {
			putValue(NAME, "连字支持");
			int defaultModifier = getToolkit().getMenuShortcutKeyMask() | InputEvent.SHIFT_DOWN_MASK;
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_2, defaultModifier));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			Font font = textArea.getFont();
			Integer prev = (Integer)font.getAttributes().get(TextAttribute.LIGATURES);
			int toggledValue = TextAttribute.LIGATURES_ON.equals(prev) ? -1 : TextAttribute.LIGATURES_ON;
			Map<TextAttribute, Object> attrs = new HashMap<>();
			attrs.put(TextAttribute.LIGATURES, toggledValue);
			textArea.setFont(font.deriveFont(attrs));
		}

	}

	/**
	 * 切换是否高亮当前行。
	 */
	private class ViewLineHighlightAction extends AbstractAction {

		ViewLineHighlightAction() {
			putValue(NAME, "高亮当前行");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			textArea.setHighlightCurrentLine(
					!textArea.getHighlightCurrentLine());
		}

	}

	/**
	 * 切换行号是否可见。
	 */
	private class ViewLineNumbersAction extends AbstractAction {

		ViewLineNumbersAction() {
			putValue(NAME, "行号");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			scrollPane.setLineNumbersEnabled(
					!scrollPane.getLineNumbersEnabled());
		}

	}

	/**
	 * 切换自动换行。
	 */
	private class WordWrapAction extends AbstractAction {

		WordWrapAction() {
			putValue(NAME, "自动换行");
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			textArea.setLineWrap(!textArea.getLineWrap());
		}

	}

	/**
	 * 将行号格式化为阿拉伯数字。
	 */
	private static final class HinduArabicLineNumberFormatter implements LineNumberFormatter {
		private static final String[] NUMERALS = {
			"٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩"
		};

		@Override
		public String format(int lineNumber) {
			if (lineNumber == 0) {
				return NUMERALS[0];
			}

			StringBuilder sb = new StringBuilder();
			while (lineNumber > 0) {
				int digit = lineNumber % 10;
				sb.insert(0, NUMERALS[digit]);
				lineNumber /= 10;
			}

			return sb.toString();
		}

		@Override
		public int getMaxLength(int maxLineNumber) {
			return String.valueOf(maxLineNumber).length();
		}
	}


}
