# RSyntaxTextArea（中文文档）

![Java Build](https://github.com/bobbylight/RSyntaxTextArea/actions/workflows/gradle.yml/badge.svg)
![Java Build](https://github.com/bobbylight/RSyntaxTextArea/actions/workflows/codeql-analysis.yml/badge.svg)
![Maven Central](https://maven-badges.sml.io/sonatype-central/com.fifesoft/rsyntaxtextarea/badge.svg)
[![codecov](https://codecov.io/gh/bobbylight/RSyntaxTextArea/graph/badge.svg?token=6YxJwHpfCp)](https://codecov.io/gh/bobbylight/RSyntaxTextArea)

> 本文档是 [README.md](./README.md) 的中文版本，内容与英文版保持一致。
> 代码注释、演示资源等仍以英文为准，仅在必要时补充中文说明。

## 项目介绍

RSyntaxTextArea 是一个面向 Java Swing 应用的、可定制的语法高亮文本组件。开箱即用支持
**50 多种编程语言**的语法高亮、代码折叠、查找与替换，并通过附加库提供代码补全和拼写检查。
如需支持更多语言，可以借助 [JFlex](http://jflex.de) 等工具[自行扩展](https://github.com/bobbylight/RSyntaxTextArea/wiki)。

RSyntaxTextArea 采用 [BSD 3-Clause 许可证](https://raw.githubusercontent.com/bobbylight/RSyntaxTextArea/refs/heads/master/RSyntaxTextArea/src/main/resources/META-INF/LICENSE) 发布。
更多信息请访问项目主页 [http://bobbylight.github.io/RSyntaxTextArea/](http://bobbylight.github.io/RSyntaxTextArea/)。

可通过 [Maven Central 仓库](https://central.sonatype.com/artifact/com.fifesoft/rsyntaxtextarea) 获取
（`com.fifesoft:rsyntaxtextarea:XXX`）。开发中未发布版本的 SNAPSHOT 构建托管在
[Sonatype](https://central.sonatype.com/repository/maven-snapshots/)。

功能总览与代码深入解析请看 [项目 wiki](https://github.com/bobbylight/RSyntaxTextArea/wiki)。

## 构建

RSyntaxTextArea 使用 [Gradle](http://gradle.org/) 构建。若要编译源码、运行全部单元测试并生成 jar 包，执行：

    ./gradlew build --warning-mode all

RSTA 3.0 及更高版本**编译需要 Java 17**，但可在 Java 8 上运行。
如果需要兼容 Java 6，请使用 2.6.x 版本。

> 本仓库当前基线为 **3.6.3**，`gradle.properties` 中 `javaLanguageVersion=8`，
> 后续会按 issue 流程升级到 4.0.1（见 issue #4）。

## 演示程序

`RSyntaxTextAreaDemo` 子模块中包含几个简单的演示应用。运行"主"演示（展示多种语言的
语法高亮与代码折叠，以及若干常用配置选项）：

```bash
./gradlew RSyntaxTextAreaDemo:run
```

## 使用示例

RSyntaxTextArea 本身就是 `JTextComponent` 的子类，因此可以轻松嵌入任意 Swing 应用：

```java
import javax.swing.*;
import java.awt.BorderLayout;

import org.fife.ui.rtextarea.*;
import org.fife.ui.rsyntaxtextarea.*;

public class TextEditorDemo extends JFrame {

    public TextEditorDemo() {

        JPanel cp = new JPanel(new BorderLayout());

        RSyntaxTextArea textArea = new RSyntaxTextArea(20, 60);
        textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
        textArea.setCodeFoldingEnabled(true);
        RTextScrollPane sp = new RTextScrollPane(textArea);
        cp.add(sp);

        setContentPane(cp);
        setTitle("Text Editor Demo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);

    }

    public static void main(String[] args) {
        // 所有 Swing 应用都应在 EDT 上启动
        SwingUtilities.invokeLater(() -> new TextEditorDemo().setVisible(true));
    }

}
```

## 姊妹项目

RSyntaxTextArea 自带语法高亮、代码折叠以及许多其他功能，但在打造代码编辑器时往往还想更进一步。
以下是一些提供更多复杂功能的小型附加库：

* [AutoComplete](https://github.com/bobbylight/AutoComplete) —— 为 RSyntaxTextArea（或任何其他 `JTextComponent`）添加代码补全。
* [RSTALanguageSupport](https://github.com/bobbylight/RSTALanguageSupport) —— 为 RSTA 提供以下语言的代码补全：Java、JavaScript、HTML、PHP、JSP、Perl、C、Unix Shell，基于 RSTA 与 AutoComplete 构建。
* [SpellChecker](https://github.com/bobbylight/SpellChecker) —— 为 RSyntaxTextArea 添加波浪线拼写检查。
* [RSTAUI](https://github.com/bobbylight/RSTAUI) —— 文本编辑应用常用的对话框：查找、替换、跳转到行、文件属性。

> 注意：自动补全**不在** RSyntaxTextArea 核心库内，需额外引入 AutoComplete。

## 获取帮助

* 在 GitHub 上提交 issue
* 查阅 [项目 wiki](https://github.com/bobbylight/RSyntaxTextArea/wiki)
* 查看[项目主页](http://bobbylight.github.io/RSyntaxTextArea/)

## 关于本仓库

本仓库 fork 自 [bobbylight/RSyntaxTextArea](https://github.com/bobbylight/RSyntaxTextArea)，
用于为期 16 周的个人开发实践（v0.1.0 → v1.0.0），规划在四个大版本中逐步引入 AI 相关能力。

约定：

* 上游已有代码与注释保持英文，避免与上游产生海量 diff；
* 本项目**新增或修改**的代码，注释与文档使用中文；
* 中文文档以 `*.zh-CN.*` 并列文件形式提供，不覆盖英文原文。
