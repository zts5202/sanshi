package com.zts.sanshi.data

import com.zts.sanshi.domain.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskCodecTest {
    @Test
    fun roundTripsPunctuationAndNewlines() {
        val tasks = listOf(
            Task(id = "a", text = "买牛奶 \"全脂\"\n和面包", done = false),
            Task(id = "b", text = "三事", done = true),
        )
        val decoded = TaskCodec.decode(TaskCodec.encode(tasks))
        assertEquals(tasks, decoded)
    }

    @Test
    fun blankAndCorruptPayloadsDecodeToEmpty() {
        assertTrue(TaskCodec.decode(null).isEmpty())
        assertTrue(TaskCodec.decode("").isEmpty())
        assertTrue(TaskCodec.decode("{not-json").isEmpty())
    }

    @Test
    fun dropsBlankEntriesAndKeepsAtMostThree() {
        val raw = """
            [
              {"id":"","text":"空","done":false},
              {"id":"1","text":"一","done":false},
              {"id":"2","text":"二","done":true},
              {"id":"3","text":"三","done":false},
              {"id":"4","text":"四","done":false}
            ]
        """.trimIndent()
        assertEquals(listOf("一", "二", "三"), TaskCodec.decode(raw).map { it.text })
    }
}
