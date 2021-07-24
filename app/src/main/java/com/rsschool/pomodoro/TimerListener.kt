package com.rsschool.pomodoro

interface TimerListener {
    fun add(initTime: Long, label: String)
    fun delete(id: Int)
    fun start(id: Int)
    fun stop(id: Int)
    fun finish(id: Int)
}