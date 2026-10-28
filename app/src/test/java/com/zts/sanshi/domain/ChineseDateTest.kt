package com.zts.sanshi.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ChineseDateTest {
    @Test
    fun formatsMonthDayAndWeekday() {
        assertEquals("10月28日 · 周三", formatChineseDate(LocalDate.of(2026, 10, 28)))
        assertEquals("10月27日 · 周二", formatChineseDate(LocalDate.of(2026, 10, 27)))
        assertEquals("1月1日 · 周五", formatChineseDate(LocalDate.of(2027, 1, 1)))
    }
}
