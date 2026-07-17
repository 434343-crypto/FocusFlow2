package com.amir.focusflow

import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.amir.focusflow.databinding.ActivityAlarmBinding

class AlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmBinding
    private var correctAnswer: Int = 0
    private var ringtone: android.media.Ringtone? = null
    private val handler = Handler(Looper.getMainLooper())
    private var vibrator: android.os.Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val label = intent.getStringExtra(AlarmReceiver.EXTRA_LABEL) ?: "Time to switch!"
        binding.modalSessionName.text = label

        generateProblem()
        startAlarmSound()

        binding.submitBtn.setOnClickListener { checkAnswer() }
        binding.mathAnswer.setOnEditorActionListener { _, _, _ ->
            checkAnswer()
            true
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun generateProblem() {
        val (q, a) = MathBank.random()
        correctAnswer = a
        binding.mathQuestion.text = q
        binding.mathAnswer.setText("")
    }

    private fun checkAnswer() {
        val text = binding.mathAnswer.text?.toString()?.trim()
        val ans = text?.toIntOrNull()
        if (ans != null && ans == correctAnswer) {
            stopAlarmSound()
            finish()
        } else {
            binding.mathAnswer.error = "Wrong, try again"
        }
    }

    private fun startAlarmSound() {
        try {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(this, uri)
            ringtone?.audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            ringtone?.play()
        } catch (_: Exception) {
        }

        vibrator = getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
        val pattern = longArrayOf(0, 500, 300, 500, 300)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(android.os.VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    private fun stopAlarmSound() {
        ringtone?.stop()
        vibrator?.cancel()
    }

    override fun onDestroy() {
        stopAlarmSound()
        super.onDestroy()
    }

    override fun onBackPressed() {
        // Block back button — must solve the math problem to dismiss, same as the web version.
    }
}
