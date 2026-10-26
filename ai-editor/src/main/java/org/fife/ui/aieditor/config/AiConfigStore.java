/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 把 {@link AiConfig} 持久化到 {@code ~/.ai-editor/config.json}。
 *
 * <p>设计上遵循一条原则：<strong>配置文件损坏永远不能导致应用启动失败</strong>。
 * 因此任何读取或解析异常都会被降级为"使用默认配置"，同时把损坏文件另存为
 * {@code config.json.broken}，方便事后排查而不至于丢数据。</p>
 *
 * @author Oiyuu
 * @version 0.2.0
 */
public final class AiConfigStore {

	private static final Logger LOGGER = Logger.getLogger(AiConfigStore.class.getName());

	/** 配置文件所在目录名。 */
	private static final String DIR_NAME = ".ai-editor";

	/** 配置文件名。 */
	private static final String FILE_NAME = "config.json";

	/** 损坏文件的备份后缀。 */
	private static final String BROKEN_SUFFIX = ".broken";

	private final Path path;
	private final Gson gson;

	/**
	 * 使用默认位置 {@code ~/.ai-editor/config.json} 创建读写器。
	 */
	public AiConfigStore() {
		this(defaultPath());
	}

	/**
	 * 创建指向指定文件的读写器，便于测试。
	 *
	 * @param path 配置文件路径，不可为 {@code null}
	 */
	public AiConfigStore(Path path) {
		this.path = java.util.Objects.requireNonNull(path, "path 不可为 null");
		this.gson = new GsonBuilder().setPrettyPrinting().create();
	}

	/**
	 * 返回默认配置文件路径。
	 *
	 * @return {@code ~/.ai-editor/config.json}
	 */
	public static Path defaultPath() {
		String home = System.getProperty("user.home");
		return Paths.get(home == null ? "." : home, DIR_NAME, FILE_NAME);
	}

	/**
	 * 读取配置。
	 *
	 * @return 配置对象；文件不存在、无法读取或格式非法时返回默认配置
	 */
	public AiConfig load() {
		if (!Files.exists(path)) {
			return new AiConfig();
		}
		try {
			String json = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
			if (json.trim().isEmpty()) {
				return new AiConfig();
			}
			AiConfig parsed = gson.fromJson(json, AiConfig.class);
			return parsed == null ? new AiConfig() : normalize(parsed);
		}
		catch (JsonSyntaxException ex) {
			LOGGER.log(Level.WARNING, "配置文件格式非法，已备份为 " + path + BROKEN_SUFFIX + " 并使用默认配置", ex);
			backupBrokenFile();
			return new AiConfig();
		}
		catch (IOException ex) {
			LOGGER.log(Level.WARNING, "配置文件读取失败，使用默认配置", ex);
			return new AiConfig();
		}
	}

	/**
	 * 写出配置。
	 *
	 * 保存失败时不向上抛异常：这只是偏好设置，不值得让整个应用崩溃。
	 *
	 * @param config 待保存的配置，不可为 {@code null}
	 */
	public void save(AiConfig config) {
		try {
			Path parent = path.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			String json = gson.toJson(config);
			Files.write(path, json.getBytes(StandardCharsets.UTF_8));
			restrictPermissions();
		}
		catch (IOException ex) {
			LOGGER.log(Level.WARNING, "配置文件写入失败：" + path, ex);
		}
	}

	/**
	 * 尽量收紧文件权限为仅本人可读写。
	 *
	 * Windows 文件系统通常不支持 POSIX 权限，此时会抛异常并被忽略，
	 * 因此这里仅作为"能收紧就收紧"的尽力而为处理。
	 */
	private void restrictPermissions() {
		try {
			Set<PosixFilePermission> perms = new HashSet<>();
			perms.add(PosixFilePermission.OWNER_READ);
			perms.add(PosixFilePermission.OWNER_WRITE);
			Files.setPosixFilePermissions(path, perms);
		}
		catch (IOException | UnsupportedOperationException ex) {
			LOGGER.log(Level.FINE, "当前文件系统不支持收紧权限，已跳过", ex);
		}
	}

	/**
	 * 把损坏的配置文件改名备份。
	 */
	private void backupBrokenFile() {
		try {
			Path backup = Paths.get(path.toString() + BROKEN_SUFFIX);
			Files.move(path, backup, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
		}
		catch (IOException ex) {
			LOGGER.log(Level.WARNING, "无法备份损坏的配置文件，已保留原文件", ex);
		}
	}

	/**
	 * 校验从磁盘读到的值，把明显非法的取值拉回合法范围。
	 *
	 * 手写这个函数是值得的：用户很可能会手工编辑这个 JSON，
	 * 一个负数超时会让后续每个请求都立刻失败。
	 *
	 * @param config 解析得到的配置
	 * @return 修正后的同一个对象
	 */
	private static AiConfig normalize(AiConfig config) {
		if (config.getMaxTokens() <= 0) {
			config.setMaxTokens(new AiConfig().getMaxTokens());
		}
		if (config.getConnectTimeoutMs() <= 0) {
			config.setConnectTimeoutMs(new AiConfig().getConnectTimeoutMs());
		}
		if (config.getRequestTimeoutMs() <= 0) {
			config.setRequestTimeoutMs(new AiConfig().getRequestTimeoutMs());
		}
		if (config.getBaseUrl() == null || config.getBaseUrl().trim().isEmpty()) {
			config.setBaseUrl(new AiConfig().getBaseUrl());
		}
		if (config.getModel() == null || config.getModel().trim().isEmpty()) {
			config.setModel(new AiConfig().getModel());
		}
		return config;
	}

	/**
	 * 返回本读写器使用的配置文件路径。
	 *
	 * @return 配置文件路径
	 */
	public Path getPath() {
		return path;
	}
}
