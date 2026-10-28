package com.zts.sanshi.data

import java.time.LocalDate
import java.time.ZoneId

fun interface DayClock {
    fun today(): LocalDate
}

object SystemDayClock : DayClock {
    override fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())
}

fun interface TaskIds {
    fun newId(): String
}

object UuidTaskIds : TaskIds {
    override fun newId(): String = java.util.UUID.randomUUID().toString()
}
