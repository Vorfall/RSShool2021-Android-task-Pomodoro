package com.rsschool.pomodoro

fun Long.displayTime(): String {
    return if (this <= 0L) START_TIME
    else "%02d:%02d:%02d".format(
        this / 1000 / 3600,
        this / 1000 % 3600 / 60,
        this / 1000 % 60
    )
}

const val START_TIME = "00:00:00"
const val INTERVAL = 100L

const val INVALID = "INVALID"
const val COMMAND_START = "COMMAND_START"
const val COMMAND_STOP = "COMMAND_STOP"
const val COMMAND_ID = "COMMAND_ID"

const val TIMER_INIT_TIME = "TIMER_INIT_TIME"
const val TIMER_START_TIME = "TIMER_START_TIME"
const val TIMER_OPERATION_TIME = "TIMER_OPERATING_TIME"
const val LABEL = "LABEL"