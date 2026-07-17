package com.amir.focusflow

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {

    private const val MIDNIGHT_REFRESH_ID = 999999

    /** Cancels & re-schedules every alarm for today (called on app launch, boot, and toggle changes). */
    fun rescheduleAll(context: Context) {
        val sessions = SessionStore.loadSessions(context)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val now = Calendar.getInstance()
        val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        for (s in sessions) {
            if (!s.enabled || s.type == "meal") continue

            // Ring at the session's start time (if still in the future today)
            scheduleAt(context, am, s.start, alarmIdFor(s.id, "start"), "${s.name} is starting", nowMinutes)

            // A merged study+break block rings again when the break portion begins
            if (s.breakStart != null) {
                scheduleAt(context, am, s.breakStart!!, alarmIdFor(s.id, "break"), "Break time!", nowMinutes)
            }
        }

        scheduleMidnightRefresh(context, am)
    }

    private fun scheduleAt(
        context: Context, am: AlarmManager, minuteOfDay: Int, alarmId: Int, label: String, nowMinutes: Int
    ) {
        if (minuteOfDay <= nowMinutes) return // already passed today

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, minuteOfDay / 60)
            set(Calendar.MINUTE, minuteOfDay % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_LABEL, label)
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        val pi = PendingIntent.getBroadcast(
            context, alarmId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            // Fallback: inexact, still fires close to on time
            am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } else {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, pi), pi)
        }
    }

    /** Every midnight, regenerate the day's alarms so the schedule repeats daily. */
    private fun scheduleMidnightRefresh(context: Context, am: AlarmManager) {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 1)
            set(Calendar.SECOND, 0)
        }
        val intent = Intent(context, BootReceiver::class.java) // reuse: just calls rescheduleAll
        val pi = PendingIntent.getBroadcast(
            context, MIDNIGHT_REFRESH_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        } else {
            am.setAlarmClock(AlarmManager.AlarmClockInfo(cal.timeInMillis, pi), pi)
        }
    }

    private fun alarmIdFor(sessionId: String, suffix: String): Int {
        return (sessionId + suffix).hashCode() and 0x7fffffff
    }
}
