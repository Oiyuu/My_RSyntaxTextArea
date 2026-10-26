/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor.llm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 一次 LLM 请求的描述。
 *
 * 本项目统一使用"系统提示 + 用户提示"两段式结构，不保留多轮历史：
 * 代码编辑场景里每个请求都携带自己的上下文片段，历史线程由对话面板在外部自行管理。
 *
 * <p>实例不可变，通过 {@link #builder()} 创建。</p>
 *
 * @author Oiyuu
 * @version 0.2.0
 */
public final class LlmRequest {

	private final String model;
	private final String systemPrompt;
	private final String userPrompt;
	private final double temperature;
	private final int maxTokens;
	private final List<String> stop;

	/**
	 * 用构建器组装实例。
	 *
	 * @param builder 构建器，不可为 {@code null}
	 */
	private LlmRequest(Builder builder) {
		Objects.requireNonNull(builder, "builder 不可为 null");
		this.model = builder.model;
		this.systemPrompt = builder.systemPrompt;
		this.userPrompt = builder.userPrompt;
		this.temperature = builder.temperature;
		this.maxTokens = builder.maxTokens;
		this.stop = Collections.unmodifiableList(new ArrayList<>(builder.stop));
	}

	/**
	 * 创建一个新的构建器。
	 *
	 * @return 已填入默认值的构建器
	 */
	public static Builder builder() {
		return new Builder();
	}

	/**
	 * 返回要使用的模型名。
	 *
	 * @return 模型名
	 */
	public String getModel() {
		return model;
	}

	/**
	 * 返回系统提示。
	 *
	 * @return 系统提示，可能为 {@code null}
	 */
	public String getSystemPrompt() {
		return systemPrompt;
	}

	/**
	 * 返回用户提示。
	 *
	 * @return 用户提示
	 */
	public String getUserPrompt() {
		return userPrompt;
	}

	/**
	 * 返回采样温度。
	 *
	 * @return 温度值
	 */
	public double getTemperature() {
		return temperature;
	}

	/**
	 * 返回本次允许生成的最大 token 数。
	 *
	 * @return 最大 token 数
	 */
	public int getMaxTokens() {
		return maxTokens;
	}

	/**
	 * 返回停止词列表。
	 *
	 * @return 停止词列表，永不为 {@code null}
	 */
	public List<String> getStop() {
		return stop;
	}

	/**
	 * {@link LlmRequest} 的构建器。
	 *
	 * 默认值刻意偏向"稳定"：温度取 {@code 0.0}，
	 * 因为代码相关输出若每次都不同，反复跑测试的成本会非常高。
	 */
	public static final class Builder {

		private String model = "";
		private String systemPrompt;
		private String userPrompt = "";
		private double temperature;
		private int maxTokens = 1024;
		private final List<String> stop = new ArrayList<>();

		/**
		 * 构造默认构建器。构造函数本身不设限制，所有字段均有默认值。
		 */
		Builder() {
			// 所有字段使用默认值，无需额外初始化
		}

		/**
		 * 设置模型名。
		 *
		 * @param model 模型名，例如 {@code gpt-4o-mini}
		 * @return 当前构建器，便于链式调用
		 */
		public Builder model(String model) {
			this.model = model;
			return this;
		}

		/**
		 * 设置系统提示，用于约束模型的输出格式。
		 *
		 * @param systemPrompt 系统提示，传 {@code null} 表示不使用
		 * @return 当前构建器，便于链式调用
		 */
		public Builder systemPrompt(String systemPrompt) {
			this.systemPrompt = systemPrompt;
			return this;
		}

		/**
		 * 设置用户提示，即真正要模型处理的内容。
		 *
		 * @param userPrompt 用户提示，不可为 {@code null}
		 * @return 当前构建器，便于链式调用
		 */
		public Builder userPrompt(String userPrompt) {
			this.userPrompt = Objects.requireNonNull(userPrompt, "userPrompt 不可为 null");
			return this;
		}

		/**
		 * 设置采样温度。做代码审查与生成时建议设为一个较低值。
		 *
		 * @param temperature 温度值，建议取 0.0 到 1.0
		 * @return 当前构建器，便于链式调用
		 */
		public Builder temperature(double temperature) {
			this.temperature = temperature;
			return this;
		}

		/**
		 * 设置最大生成 token 数，是控制成本与延迟的主要手段。
		 *
		 * @param maxTokens 最大 token 数，必须大于 0
		 * @return 当前构建器，便于链式调用
		 */
		public Builder maxTokens(int maxTokens) {
			this.maxTokens = maxTokens;
			return this;
		}

		/**
		 * 追加一个停止词。
		 *
		 * @param stopWord 停止词
		 * @return 当前构建器，便于链式调用
		 */
		public Builder addStop(String stopWord) {
			stop.add(stopWord);
			return this;
		}

		/**
		 * 构建不可变的请求对象。
		 *
		 * @return 请求对象
		 */
		public LlmRequest build() {
			return new LlmRequest(this);
		}
	}
}
