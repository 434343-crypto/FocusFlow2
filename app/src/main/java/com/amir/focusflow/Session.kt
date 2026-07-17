package com.amir.focusflow

/**
 * type: "study", "break", "meal"
 * start / end / breakStart are minutes-from-midnight (0..1440)
 */
data class Session(
    val id: String,
    val type: String,
    var name: String,
    val start: Int,
    var end: Int,
    var breakStart: Int? = null,
    var breakMin: Int = 0,
    var enabled: Boolean = true
)

fun minutesToTime(m: Int): String {
    val h = (m / 60) % 24
    val mm = m % 60
    return String.format("%02d:%02d", h, mm)
}
