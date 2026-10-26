/*
 * This library is distributed under a modified BSD license.  See the included
 * LICENSE file for details.
 */
package org.fife.ui.aieditor.llm;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link SseParser} 的单元测试。
 *
 * @author Oiyuu
 * @version 0.2.0
 */
class SseParserTest {

	@Test
	void testSingleCompleteFrame() {
		List<String> deltas = new ArrayList<>();
		AtomicInteger doneCount = new AtomicInteger();
		SseParser parser = new SseParser(deltas::add, doneCount::incrementAndGet);

		parser.feed("data: {\"choices\":[{\"delta\":{\"content\":\"你好\"}}]}\n");

		assertEquals(1, deltas.size(), "应解析出一段增量");
		assertEquals("你好", deltas.get(0));
		assertEquals(0, doneCount.get(), "未收到 [DONE] 时不应触发结束回调");
	}

	@Test
	void testLineSplitAcrossChunks() {
		List<String> deltas = new ArrayList<>();
		SseParser parser = new SseParser(deltas::add, () -> { });

		// 同一行被 HTTP 分块切成三次到达，这是流式解析最常见的踩坑点。
		parser.feed("data: {\"choices\":[{\"delt");
		parser.feed("a\":{\"content\":\"分块");
		parser.feed("到达\"}}]}\n");

		assertEquals(1, deltas.size(), "跨块的行应只产出一整段增量");
		assertEquals("分块到达", deltas.get(0));
	}

	@Test
	void testDoneMarkerStopsProcessing() {
		List<String> deltas = new ArrayList<>();
		AtomicInteger doneCount = new AtomicInteger();
		SseParser parser = new SseParser(deltas::add, doneCount::incrementAndGet);

		parser.feed("data: {\"choices\":[{\"delta\":{\"content\":\"前\"}}]}\n");
		parser.feed("data: [DONE]\n");
		parser.feed("data: {\"choices\":[{\"delta\":{\"content\":\"后\"}}]}\n");

		assertEquals(1, deltas.size(), "[DONE] 之后的数据必须被丢弃");
		assertEquals("前", deltas.get(0));
		assertEquals(1, doneCount.get());
		assertTrue(parser.isDone());
	}

	@Test
	void testMultipleFramesInOneChunk() {
		List<String> deltas = new ArrayList<>();
		SseParser parser = new SseParser(deltas::add, () -> { });

		parser.feed("data: {\"choices\":[{\"delta\":{\"content\":\"甲\"}}]}\n" +
				"data: {\"choices\":[{\"delta\":{\"content\":\"乙\"}}]}\n" +
				"data: {\"choices\":[{\"delta\":{\"content\":\"丙\"}}]}\n");

		assertEquals(3, deltas.size(), "一次收到多行时应逐行解析");
		assertEquals("甲乙丙", String.join("", deltas));
	}

	@Test
	void testMalformedFrameIsIgnored() {
		List<String> deltas = new ArrayList<>();
		SseParser parser = new SseParser(deltas::add, () -> { });

		parser.feed("data: 这不是 JSON\n");
		parser.feed(": 心跳注释\n");
		parser.feed("data: {\"choices\":[{\"delta\":{\"content\":\"仍然可用\"}}]}\n");

		assertEquals(1, deltas.size(), "异常帧应跳过，不影响后续帧");
		assertEquals("仍然可用", deltas.get(0));
		assertFalse(parser.isDone());
	}

	@Test
	void testEmptyDeltaIgnored() {
		List<String> deltas = new ArrayList<>();
		SseParser parser = new SseParser(deltas::add, () -> { });

		// 首条消息通常只有 role，没有 content，不应回调空字符串。
		parser.feed("data: {\"choices\":[{\"delta\":{\"role\":\"assistant\"}}]}\n");

		assertTrue(deltas.isEmpty(), "不含文本的增量不应触发回调");
	}
}
