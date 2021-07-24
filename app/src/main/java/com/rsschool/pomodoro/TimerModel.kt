package com.rsschool.pomodoro

data class TimerModel(
    val id: Int,
    val label: String,
    var initTime: Long,
    var startTime: Long,
    var operationTime: Long,
    var status: TimerStatus
)

enum class TimerStatus {
    ACTIVE, STOPPED, FINISHED
}