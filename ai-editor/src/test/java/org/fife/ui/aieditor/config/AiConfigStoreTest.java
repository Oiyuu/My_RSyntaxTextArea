/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link AiConfig} 与 {@link AiConfigStore} 的单元测试。
 *
 * @author Oiyuu
 * @version 0.2.0
 */
class AiConfigStoreTest {

	@Test
	void testDefaultsAreSafe() {
		AiConfig config = new AiConfig();

		assertFalse(config.isReady(), "没有任何密钥时不应处于可用状态");
		assertEquals("", config.resolveApiKey());
		assertEquals(0.0, config.getTemperature(), 0.0001, "默认温度应为 0，保证输出稳定");
		assertTrue(config.getRequestTimeoutMs() > 0, "必须存在超时上限");
	}

	@Test
	void testMaskKeyNeverLeaksFullSecret() {
		AiConfig config = new AiConfig();
		config.setApiKey("sk-secretkey1234");

		String description = config.describeSafe();

		assertFalse(description.contains("secretkey1234"), "描述信息不得包含密钥正文");
		assertTrue(description.contains("1234"), "可以保留后四位便于用户辨认");
		assertNotEquals(config.resolveApiKey(), description);
	}

	@Test
	void testMissingKeyDescribedAsUnset() {
		AiConfig config = new AiConfig();
		assertTrue(config.describeSafe().contains("未配置"), "未配置时应给出明确提示");
	}

	@Test
	void testSaveThenLoadRoundTrip(@TempDir Path tempDir) {
		Path file = tempDir.resolve("config.json");
		AiConfigStore store = new AiConfigStore(file);

		AiConfig config = new AiConfig();
		config.setModel("qwen-plus");
		config.setMaxTokens(2048);
		config.setBaseUrl("https://example.com/v1/");
		store.save(config);

		AiConfig loaded = store.load();

		assertEquals("qwen-plus", loaded.getModel());
		assertEquals(2048, loaded.getMaxTokens());
		assertEquals("https://example.com/v1", loaded.getBaseUrl(), "尾部斜杠应被去掉");
	}

	@Test
	void testMissingFileYieldsDefaults(@TempDir Path tempDir) {
		AiConfig loaded = new AiConfigStore(tempDir.resolve("none.json")).load();
		assertEquals("gpt-4o-mini", loaded.getModel(), "文件缺失时应返回默认配置");
	}

	@Test
	void testBrokenJsonFallsBackToDefaults(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("config.json");
		Files.write(file, "{ 这不是合法 JSON ]".getBytes(StandardCharsets.UTF_8));

		AiConfig loaded = new AiConfigStore(file).load();

		assertEquals("gpt-4o-mini", loaded.getModel(), "配置损坏不能导致启动失败");
		assertTrue(Files.exists(Paths.get(file.toString() + ".broken")), "损坏文件应被备份以便排查");
	}

	@Test
	void testEmptyFileYieldsDefaults(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("config.json");
		Files.write(file, "".getBytes(StandardCharsets.UTF_8));
		assertEquals("gpt-4o-mini", new AiConfigStore(file).load().getModel());
	}

	@Test
	void testInvalidNumbersAreNormalized(@TempDir Path tempDir) throws Exception {
		Path file = tempDir.resolve("config.json");
		Files.write(file, "{\"maxTokens\":-5,\"model\":\"\",\"requestTimeoutMs\":0}"
				.getBytes(StandardCharsets.UTF_8));

		AiConfig loaded = new AiConfigStore(file).load();

		assertTrue(loaded.getMaxTokens() > 0, "负数 token 上限必须被修正");
		assertTrue(loaded.getRequestTimeoutMs() > 0, "零超时会让请求立刻失败");
		assertFalse(loaded.getModel().isEmpty(), "空模型名应回退为默认值");
	}

	@Test
	void testDefaultPathPointsInsideHome() {
		Path path = AiConfigStore.defaultPath();

		assertNotNull(path);
		assertEquals("config.json", path.getFileName().toString());
		assertEquals(".ai-editor", path.getParent().getFileName().toString());
	}

	@Test
	void testSaveFailureDoesNotThrow() {
		// 故意指向一个不可能写入的路径（把文件名当成目录）
		Path impossible = Paths.get("Z:", "no-such-drive", "config.json");
		AiConfigStore store = new AiConfigStore(impossible);

		store.save(new AiConfig());   // 不应抛出异常

		assertTrue(true, "保存失败应被降级处理，而不是让调用方崩溃");
	}
}
