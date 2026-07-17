package com.amir.focusflow

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.amir.focusflow.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: SessionAdapter
    private var sessions: MutableList<Session> = mutableListOf()
    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val tick = object : Runnable {
        override fun run() {
            updateClockAndStats()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sessions = SessionStore.loadSessions(this).toMutableList()

        adapter = SessionAdapter(
            sessions,
            onToggle = { s, enabled -> SessionStore.setEnabled(this, s.id, enabled); rescheduleAlarms() },
            onRename = { s, current -> showRenameDialog(s, current) }
        )
        binding.timelineList.layoutManager = LinearLayoutManager(this)
        binding.timelineList.adapter = adapter

        binding.resetBtn.setOnClickListener { confirmReset() }

        requestPermissionsIfNeeded()
        rescheduleAlarms()
        updateStats()
    }

    override fun onResume() {
        super.onResume()
        handler.post(tick)
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    private fun requestPermissionsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("Exact alarm permission")
                    .setMessage("For alarms to ring exactly on time, please allow \"Alarms & reminders\" for FocusFlow in the next screen.")
                    .setPositiveButton("OK") { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    }
                    .setCancelable(true)
                    .show()
            }
        }
    }

    private fun rescheduleAlarms() {
        AlarmScheduler.rescheduleAll(this)
    }

    private fun showRenameDialog(s: Session, current: String) {
        val input = EditText(this).apply { setText(current) }
        AlertDialog.Builder(this)
            .setTitle("Session name")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    s.name = newName
                    SessionStore.setName(this, s.id, newName)
                    adapter.notifyDataSetChanged()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("Reset all changes?")
            .setPositiveButton("Reset") { _, _ ->
                getSharedPreferences("focusflow_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                sessions.clear()
                sessions.addAll(SessionStore.loadSessions(this))
                adapter.notifyDataSetChanged()
                rescheduleAlarms()
                updateStats()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getNowMinutes(): Int {
        val cal = Calendar.getInstance()
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    }

    private fun updateClockAndStats() {
        binding.currentTime.text = timeFormat.format(Calendar.getInstance().time)

        val now = getNowMinutes()
        var active = -1
        for (i in sessions.indices) {
            val s = sessions[i]
            if (now >= s.start && now < s.end) {
                active = i
                break
            }
        }
        if (active != adapter.activeIndex) adapter.activeIndex = active

        if (active >= 0) {
            val left = sessions[active].end - now
            binding.countdown.text = String.format("%02d:00", left)
        } else {
            binding.countdown.text = "--:--"
        }

        val pct = (now * 100 / 1440).coerceIn(0, 100)
        binding.dayProgressLabel.text = "$pct%"
    }

    private fun updateStats() {
        var study = 0
        var brk = 0
        for (s in sessions) {
            if (!s.enabled) continue
            when (s.type) {
                "study" -> {
                    study += (s.end - s.start) - s.breakMin
                    brk += s.breakMin
                }
                "break" -> brk += s.end - s.start
            }
        }
        binding.totalStudy.text = "${study / 60}h ${study % 60}m"
        binding.totalBreak.text = "${brk / 60}h ${brk % 60}m"
    }
}
