package com.zts.sanshi.domain

import java.time.DayOfWeek
import java.time.LocalDate

fun formatChineseDate(date: LocalDate): String {
    val week = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
    }
    return "${date.monthValue}月${date.dayOfMonth}日 · $week"
}
