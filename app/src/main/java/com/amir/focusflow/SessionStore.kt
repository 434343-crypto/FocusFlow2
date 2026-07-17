package com.amir.focusflow

import android.content.Context

/** Mirrors localStorage 'focusflow_data' behaviour from the original web app. */
object SessionStore {
    private const val PREFS = "focusflow_prefs"

    fun loadSessions(context: Context): List<Session> {
        val sessions = ScheduleGenerator.generate()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        for (s in sessions) {
            if (s.type == "study") {
                prefs.getString("name_${s.id}", null)?.let { s.name = it }
            }
            s.enabled = prefs.getBoolean("enabled_${s.id}", true)
        }
        return sessions
    }

    fun setEnabled(context: Context, sessionId: String, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean("enabled_$sessionId", enabled).apply()
    }

    fun setName(context: Context, sessionId: String, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("name_$sessionId", name).apply()
    }
}
