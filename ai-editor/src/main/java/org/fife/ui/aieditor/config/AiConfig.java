/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor.config;

import java.util.Locale;
import java.util.Objects;

/**
 * AI 能力的全部可调参数。
 *
 * <p>默认取值偏向"省钱、稳定、快"：
 * 温度默认为 {@code 0.0}，因为同一段代码反复跑出不同诊断会大幅增加调试成本；
 * 默认模型选择轻量型号，AI 功能的绝大多数场景并不需要最强模型。</p>
 *
 * @author Oiyuu
 * @version 0.2.0
 */
public final class AiConfig {

	/** 用于注入 API Key 的环境变量名，优先级高于配置文件。 */
	public static final String ENV_API_KEY = "AI_EDITOR_API_KEY";

	/** 用于覆盖服务地址的环境变量名，便于内网或代理场景。 */
	public static final String ENV_BASE_URL = "AI_EDITOR_BASE_URL";

	private String baseUrl = "https://api.openai.com/v1";
	private String model = "gpt-4o-mini";
	private String apiKey = "";
	private double temperature;
	private int maxTokens = 1024;
	private int connectTimeoutMs = 5000;
	private int requestTimeoutMs = 60000;
	private boolean enabled = true;
	private boolean streamEnabled = true;

	/**
	 * 创建一个使用默认值的配置。
	 */
	public AiConfig() {
		// 全部字段已在声明处初始化为默认值
	}

	/**
	 * 解析实际生效的 API Key。
	 *
	 * 环境变量优先于配置文件：演示与 CI 场景下不便把密钥写到磁盘上，
	 * 同时也避免开发者把本地配置误当成"已配置"而忘记设置环境变量。
	 *
	 * @return 生效的密钥，未配置时返回空字符串
	 */
	public String resolveApiKey() {
		String fromEnv = System.getenv(ENV_API_KEY);
		if (fromEnv != null && !fromEnv.trim().isEmpty()) {
			return fromEnv.trim();
		}
		return apiKey == null ? "" : apiKey.trim();
	}

	/**
	 * 解析实际生效的服务地址，环境变量优先。
	 *
	 * @return 服务地址，永不为 {@code null}
	 */
	public String resolveBaseUrl() {
		String fromEnv = System.getenv(ENV_BASE_URL);
		if (fromEnv != null && !fromEnv.trim().isEmpty()) {
			return fromEnv.trim();
		}
		return baseUrl;
	}

	/**
	 * 判断是否已经具备调用条件。
	 *
	 * @return 已配置非空密钥且功能开关打开时返回 {@code true}
	 */
	public boolean isReady() {
		return enabled && !resolveApiKey().isEmpty();
	}

	/**
	 * 返回可安全用于日志与界面展示的信息。
	 *
	 * <strong>绝不能返回完整密钥</strong>：异常栈和日志文件常被随手贴到处 Issue 里，
	 * 一旦泄漏就是永久性的。
	 *
	 * @return 脱敏后的描述，例如 {@code https://api.openai.com/v1 • gpt-4o-mini • sk-…7d21}
	 */
	public String describeSafe() {
		return resolveBaseUrl() + " • " + model + " • " + maskKey(resolveApiKey());
	}

	/**
	 * 把密钥脱敏为只保留后四位的形式。
	 *
	 * @param key 原始密钥，可为 {@code null} 或空
	 * @return 脱敏结果，未配置时返回 {@code 未配置}
	 */
	private static String maskKey(String key) {
		if (key == null || key.isEmpty()) {
			return "未配置";
		}
		if (key.length() <= 4) {
			return "sk-…" + key;
		}
		String tail = key.substring(key.length() - 4);
		return "sk-…" + tail.toLowerCase(Locale.ROOT);
	}

	/**
	 * 返回服务地址。
	 *
	 * @return 服务地址
	 */
	public String getBaseUrl() {
		return baseUrl;
	}

	/**
	 * 设置服务地址，须去掉尾部斜杠。
	 *
	 * @param baseUrl 服务地址，例如 {@code https://api.openai.com/v1}
	 */
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = trimTrailingSlash(baseUrl);
	}

	/**
	 * 返回模型名。
	 *
	 * @return 模型名
	 */
	public String getModel() {
		return model;
	}

	/**
	 * 设置模型名。
	 *
	 * @param model 模型名
	 */
	public void setModel(String model) {
		this.model = Objects.requireNonNull(model, "model 不可为 null");
	}

	/**
	 * 返回配置文件中保存的密钥。
	 *
	 * @return 密钥，可能为空
	 */
	public String getApiKey() {
		return apiKey;
	}

	/**
	 * 设置密钥。本值会落到磁盘，仅在用户明确选择"记住密钥"时调用。
	 *
	 * @param apiKey 密钥，传 {@code null} 视为清空
	 */
	public void setApiKey(String apiKey) {
		this.apiKey = apiKey == null ? "" : apiKey.trim();
	}

	/**
	 * 返回默认采样温度。
	 *
	 * @return 温度值
	 */
	public double getTemperature() {
		return temperature;
	}

	/**
	 * 设置默认采样温度。
	 *
	 * @param temperature 温度值
	 */
	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

	/**
	 * 返回默认最大 token 数。
	 *
	 * @return 最大 token 数
	 */
	public int getMaxTokens() {
		return maxTokens;
	}

	/**
	 * 设置默认最大 token 数。
	 *
	 * @param maxTokens 最大 token 数，必须大于 0
	 */
	public void setMaxTokens(int maxTokens) {
		this.maxTokens = maxTokens;
	}

	/**
	 * 返回连接超时。
	 *
	 * @return 连接超时毫秒数
	 */
	public int getConnectTimeoutMs() {
		return connectTimeoutMs;
	}

	/**
	 * 设置连接超时。
	 *
	 * @param connectTimeoutMs 毫秒数，必须大于 0
	 */
	public void setConnectTimeoutMs(int connectTimeoutMs) {
		this.connectTimeoutMs = connectTimeoutMs;
	}

	/**
	 * 返回整体请求超时。
	 *
	 * @return 请求超时毫秒数
	 */
	public int getRequestTimeoutMs() {
		return requestTimeoutMs;
	}

	/**
	 * 设置整体请求超时。这类上限必须存在，否则一次卡住的请求会让界面永久显示"思考中"。
	 *
	 * @param requestTimeoutMs 毫秒数，必须大于 0
	 */
	public void setRequestTimeoutMs(int requestTimeoutMs) {
		this.requestTimeoutMs = requestTimeoutMs;
	}

	/**
	 * 返回是否启用 AI 功能总开关。
	 *
	 * @return 启用时返回 {@code true}
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * 设置是否启用 AI 功能。
	 *
	 * @param enabled 启用则为 {@code true}
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * 返回是否启用流式输出。
	 *
	 * @return 启用时返回 {@code true}
	 */
	public boolean isStreamEnabled() {
		return streamEnabled;
	}

	/**
	 * 设置是否启用流式输出。部分兼容实现不支持 SSE，需要能够关闭。
	 *
	 * @param streamEnabled 启用则为 {@code true}
	 */
	public void setStreamEnabled(boolean streamEnabled) {
		this.streamEnabled = streamEnabled;
	}

	/**
	 * 去掉地址尾部的斜杠，避免拼接出 {@code //chat/completions} 这类路径。
	 *
	 * @param url 原始地址
	 * @return 规范化后的地址
	 */
	private static String trimTrailingSlash(String url) {
		if (url == null) {
			return "";
		}
		String trimmed = url.trim();
		while (trimmed.endsWith("/")) {
			trimmed = trimmed.substring(0, trimmed.length() - 1);
		}
		return trimmed;
	}
}
