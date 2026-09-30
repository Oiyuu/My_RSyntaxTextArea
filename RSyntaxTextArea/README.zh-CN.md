# RSyntaxTextArea（中文文档）

> 对应英文原文：[README.md](./README.md)

本模块包含 RSyntaxTextArea 的实际源代码。

## 目录结构

* `src/main/java` —— 库源码，主包为 `org.fife.ui.rsyntaxtextarea`（语法高亮编辑器）
  与 `org.fife.ui.rtextarea`（基础文本区组件）。
* `src/main/resources` —— 主题（XML）、国际化资源与 `META-INF/LICENSE`。
* `src/main/dist` —— 随发布包一起分发的 `readme.txt`（已有中文版 `readme.zh-CN.txt`）。
* `src/test` —— 单元测试与测试资源。

## 构建

在仓库根目录执行：

```bash
./gradlew :RSyntaxTextArea:build
```

产物 jar 位于 `RSyntaxTextArea/build/libs/`。

## 说明

本模块为上游核心库，代码与注释保持英文，以便与上游同步并降低升级成本；
中文说明统一放在本文件及 `src/main/dist/readme.zh-CN.txt`。
