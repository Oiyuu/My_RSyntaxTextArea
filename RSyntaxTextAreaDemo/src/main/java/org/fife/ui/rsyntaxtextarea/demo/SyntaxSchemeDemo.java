package org.fife.ui.rsyntaxtextarea.demo;

import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import javax.swing.*;

import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;

/**
 * 一个简单的示例，演示如何修改 RSyntaxTextArea 中使用的字体与颜色。
 * 有两种做法：通过 Java API，或通过 XML 文件。推荐后者，因为它更模块化，
 * 并且能让你的用户在应用中自行定制 RSTA。<p>
 *
 * 项目主页：http://fifesoft.com/rsyntaxtextarea<br>
 * 下载地址：https://sourceforge.net/projects/rsyntaxtextarea
 */
public final class SyntaxSchemeDemo extends JFrame implements ActionListener {

   private static final long serialVersionUID = 1L;

   private RSyntaxTextArea textArea;

   private static final String TEXT = "public class ExampleSource {\n\n" +
         "   // Check out the crazy modified styles!\n" +
         "   public static void main(String[] args) {\n" +
         "      System.out.println(\"Hello, world!\");\n" +
	   	 "   }\n\n" +
         "}\n";

   private SyntaxSchemeDemo() {

      JPanel cp = new JPanel(new BorderLayout());

      textArea = new RSyntaxTextArea(20, 60);
      textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
      textArea.setCodeFoldingEnabled(true);
      textArea.setAntiAliasingEnabled(true);
      RTextScrollPane sp = new RTextScrollPane(textArea);
      cp.add(sp);

      textArea.setText(TEXT);

      JMenuBar mb = new JMenuBar();
      JMenu menu = new JMenu("文件");
      mb.add(menu);
      JMenuItem changeStyleProgrammaticallyItem = new JMenuItem(
            "通过代码修改样式");
      changeStyleProgrammaticallyItem
            .setActionCommand("ChangeProgrammatically");
      changeStyleProgrammaticallyItem.addActionListener(this);
      menu.add(changeStyleProgrammaticallyItem);
      JMenuItem changeStyleViaThemesItem = new JMenuItem(
            "通过主题 XML 修改样式");
      changeStyleViaThemesItem.setActionCommand("ChangeViaThemes");
      changeStyleViaThemesItem.addActionListener(this);
      menu.add(changeStyleViaThemesItem);
      setJMenuBar(mb);

      setContentPane(cp);
      setTitle("语法配色方案演示");
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      pack();
      setLocationRelativeTo(null);

   }

   /**
    * 监听菜单项的选中事件，并执行相应的操作。
    */
   @Override
   public void actionPerformed(ActionEvent e) {
      String command = e.getActionCommand();
      if ("ChangeProgrammatically".equals(command)) {
         changeStyleProgrammatically();
      } else if ("ChangeViaThemes".equals(command)) {
         changeStyleViaThemeXml();
      }
   }

   /**
    * 通过代码修改编辑器中使用的样式。
	* 这里故意做了一些奇怪的选择，用于展示不同类型的 token
	* 可以拥有不同的字体等。
    */
   private void changeStyleProgrammatically() {

      // 为所有 token 类型设置字体
      setFont(textArea, new Font("Comic Sans MS", Font.PLAIN, 16));

      // 这里那里改几处样式
      SyntaxScheme scheme = textArea.getSyntaxScheme();
      scheme.getStyle(Token.RESERVED_WORD).background = Color.pink;
      scheme.getStyle(Token.DATA_TYPE).foreground = Color.blue;
      scheme.getStyle(Token.LITERAL_STRING_DOUBLE_QUOTE).underline = true;
      scheme.getStyle(Token.COMMENT_EOL).font = new Font("Georgia",
            Font.ITALIC, 18);

      textArea.revalidate();

   }

   /**
    * 通过 XML 文件描述来修改编辑器使用的样式。
    * 由于简单且模块化，推荐使用该方法。
    */
   private void changeStyleViaThemeXml() {
      try {
         Theme theme = Theme.load(getClass().getResourceAsStream(
               "/org/fife/ui/rsyntaxtextarea/themes/eclipse.xml"));
         theme.apply(textArea);
      } catch (IOException ioe) { // 不会发生
         ioe.printStackTrace();
      }
   }

   /**
    * 为所有 token 类型设置字体。
    *
    * @param textArea 要修改的文本区。
    * @param font 要使用的字体。
    */
   private static void setFont(RSyntaxTextArea textArea, Font font) {
      if (font != null) {
         SyntaxScheme ss = textArea.getSyntaxScheme();
         ss = (SyntaxScheme) ss.clone();
         for (int i = 0; i < ss.getStyleCount(); i++) {
            if (ss.getStyle(i) != null) {
               ss.getStyle(i).font = font;
            }
         }
         textArea.setSyntaxScheme(ss);
         textArea.setFont(font);
      }
   }

   public static void main(String[] args) {
      // 所有 Swing 应用都应在 EDT 上启动
      SwingUtilities.invokeLater(() -> new SyntaxSchemeDemo().setVisible(true));
   }

}
