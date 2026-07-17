package com.amir.focusflow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Triggered either on device boot, or as the daily midnight refresh
        // scheduled by AlarmScheduler — both cases just regenerate today's alarms.
        AlarmScheduler.rescheduleAll(context)
    }
}
