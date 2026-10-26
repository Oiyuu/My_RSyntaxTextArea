/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ai-editor 应用的启动入口。
 *
 * 本类只负责三件事：安装外观、创建主窗口、把控制权交给 Swing 事件派发线程（EDT）。
 * 任何业务逻辑都不应写在这里。
 *
 * @author Oiyuu
 * @version 0.2.0
 */
public final class App {

	private static final Logger LOGGER = Logger.getLogger(App.class.getName());

	/**
	 * 工具类不允许被实例化。
	 */
	private App() {
	}

	/**
	 * 应用主入口。此处仅做线程切换，真正的初始化在 EDT 上完成。
	 *
	 * @param args 命令行参数，当前版本忽略
	 */
	public static void main(String[] args) {
		SwingUtilities.invokeLater(App::createAndShowWindow);
	}

	/**
	 * 安装系统外观并显示主窗口。本方法应在 EDT 上调用。
	 */
	private static void createAndShowWindow() {
		installSystemLookAndFeel();
		MainWindow window = new MainWindow();
		window.setVisible(true);
	}

	/**
	 * 安装当前系统的外观风格。
	 *
	 * 失败时并不致命——回退到 Swing 默认外观即可正常运行，
	 * 因此这里只记录一条警告日志。
	 */
	private static void installSystemLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (ReflectiveOperationException | UnsupportedLookAndFeelException ex) {
			LOGGER.log(Level.WARNING, "无法安装系统外观，已回退为默认外观", ex);
		}
	}
}
