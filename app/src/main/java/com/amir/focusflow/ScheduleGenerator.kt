package com.amir.focusflow

/**
 * Exact port of generateSchedule() from the original FocusFlow HTML file:
 * - Lunch at 13:00 (780 min) for 45 min, Dinner at 21:00 (1260 min) for 30 min
 * - Study blocks up to 105 min, followed by a 15 min break that gets merged
 *   into the study session (shown as one card, but rings twice: once at
 *   study start, once at break start).
 */
object ScheduleGenerator {

    data class Meal(val name: String, val start: Int, val dur: Int)

    fun generate(): List<Session> {
        val sessions = mutableListOf<Session>()
        var studyCounter = 1
        var t = 0
        val meals = listOf(Meal("Lunch", 780, 45), Meal("Dinner", 1260, 30))

        while (t < 1440) {
            var mealHit = false
            for (m in meals) {
                if (t == m.start) {
                    sessions.add(
                        Session(
                            id = "m$t", type = "meal", name = m.name,
                            start = t, end = t + m.dur, enabled = true
                        )
                    )
                    t += m.dur
                    mealHit = true
                    break
                }
            }
            if (mealHit) continue

            var next = 1440
            for (m in meals) if (m.start > t) next = minOf(next, m.start)

            val studyStart = t
            val studyDur = minOf(105, next - t)
            if (studyDur > 0) {
                sessions.add(
                    Session(
                        id = "s$t", type = "study", name = "Study ${studyCounter++}",
                        start = t, end = t + studyDur, enabled = true, breakMin = 0
                    )
                )
                t += studyDur
            }

            val breakDur = minOf(15, next - t)
            if (breakDur > 0) {
                val lastStudy = sessions.lastOrNull()
                if (lastStudy != null && lastStudy.type == "study" && lastStudy.start == studyStart) {
                    lastStudy.breakStart = t
                    lastStudy.end += breakDur
                    lastStudy.breakMin += breakDur
                } else {
                    sessions.add(
                        Session(
                            id = "b$t", type = "break", name = "Break",
                            start = t, end = t + breakDur, enabled = true
                        )
                    )
                }
                t += breakDur
            }
        }
        return sessions
    }
}
