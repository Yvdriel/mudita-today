package com.mosquishe.today.ui.nav

object Routes {
    const val TODAY = "today"
    const val UPCOMING = "upcoming"
    const val ANYTIME = "anytime"
    const val LOGBOOK = "logbook"
    const val SETTINGS = "settings"

    // taskId 0 = new task. default = epoch-day to pre-fill the scheduled date (-1 = none).
    const val DETAIL = "detail/{taskId}?default={default}"
    fun detail(taskId: Long, defaultEpochDay: Long? = null): String =
        "detail/$taskId?default=${defaultEpochDay ?: -1L}"

    val TAB_ROUTES = setOf(TODAY, UPCOMING, ANYTIME, LOGBOOK)
}
