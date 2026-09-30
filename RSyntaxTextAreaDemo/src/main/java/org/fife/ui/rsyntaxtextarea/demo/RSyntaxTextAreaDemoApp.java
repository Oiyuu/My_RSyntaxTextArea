package org.fife.ui.rsyntaxtextarea.demo;

import java.awt.*;
import javax.swing.*;


/**
 * 演示程序的独立运行版本。
 *
 * @author Robert Futrell
 * @version 1.0
 */
public final class RSyntaxTextAreaDemoApp extends JFrame {


	private RSyntaxTextAreaDemoApp() {
		setRootPane(new DemoRootPane());
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setTitle("RSyntaxTextArea 演示程序");
		pack();
	}


	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> {
			try {
				UIManager.setLookAndFeel(UIManager.
										getSystemLookAndFeelClassName());
			} catch (Exception e) {
				e.printStackTrace(); // 不会发生
			}
			Toolkit.getDefaultToolkit().setDynamicLayout(true);
			new RSyntaxTextAreaDemoApp().setVisible(true);
		});
	}


}
