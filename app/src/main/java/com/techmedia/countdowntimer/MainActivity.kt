package com.techmedia.countdowntimer

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.techmedia.countdowntimer.databinding.ActivityMainBinding
import com.techmedia.countdowntimer.util.NotificationUtil
import com.techmedia.countdowntimer.util.PrefUtil
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        fun setAlarm(context: Context, nowSeconds: Long, secondsRemaining: Long): Long {
            val wakeUpTime = (nowSeconds + secondsRemaining) * 1000
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, wakeUpTime, pendingIntent)
            PrefUtil.setAlarmSetTime(nowSeconds, context)
            return wakeUpTime
        }

        fun removeAlarm(context: Context) {
            val intent = Intent(context, TimerExpiredReceiver::class.java)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0)
            alarmManager.cancel(pendingIntent)
            PrefUtil.setAlarmSetTime(0, context)
        }

        val nowSeconds: Long get() = Calendar.getInstance().timeInMillis / 1000

    }


    enum class TimerState {
        STOPPED, PAUSED, RUNNING
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var timer: CountDownTimer
    private var timerLengthSeconds = 0L
    private var timerState = TimerState.STOPPED
    private var secondsRemaining = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setIcon(R.drawable.ic_timer)
        supportActionBar?.title = "   Timer"

//        Button Events
        binding.fabStart.setOnClickListener {
            startTimer()
            timerState = TimerState.RUNNING
//            Disable the current Button
            updateButtons()
        }
        binding.fabPause.setOnClickListener {
            timer.cancel()
            timerState = TimerState.PAUSED
//            Disable the current Button
            updateButtons()
        }

        binding.fabStop.setOnClickListener {
            timer.cancel()
            timerState = TimerState.STOPPED
//            Disable the current Button
            onTimerFinished()
        }
    }


    override fun onResume() {
        super.onResume()
        initTimer()
        removeAlarm(this)
        // hide notification
        NotificationUtil.hideTimerNotification(this)
//        Update the buttons and the countdownUI whenever the user navigate back to the app
        updateButtons()
        updateCountdownUI()

    }

    private fun initTimer() {
        timerState = PrefUtil.getTimerState(this)
        if (timerState == TimerState.STOPPED)
            setNewTimerLength()
        else {
            setPreviousTimerLength()
        }
        secondsRemaining =
            if (timerState == TimerState.RUNNING || timerState == TimerState.PAUSED) {
                PrefUtil.getSecondsRemaining(this)
            } else {
                timerLengthSeconds
            }

        val alarmSetTime = PrefUtil.getAlarmSetTime(this)
        if (alarmSetTime > 0) {
            secondsRemaining -= nowSeconds - alarmSetTime
        }
        if (secondsRemaining <= 0) {
            onTimerFinished()
        } else if (timerState == TimerState.RUNNING) {
            startTimer()
            updateButtons()
            updateCountdownUI()
        }
    }

    override fun onPause() {
        super.onPause()
        if (timerState == TimerState.RUNNING) {
            timer.cancel()
            val wakeUpTime = setAlarm(this, nowSeconds, secondsRemaining)
            //show notification
            NotificationUtil.showTimerRunning(this, wakeUpTime)


        } else if (timerState == TimerState.PAUSED) {
            //show notification
            NotificationUtil.showTimerPaused(this)
        }

        PrefUtil.setPreviousTimerLengthSeconds(timerLengthSeconds, this)
        PrefUtil.setSecondsRemaining(secondsRemaining, this)
        PrefUtil.setTimerState(timerState, this)
    }

    private fun onTimerFinished() {
        timerState = TimerState.STOPPED
        setNewTimerLength()
        binding.progressLayout.progressCountdown.progress = 0

        PrefUtil.setSecondsRemaining(timerLengthSeconds, this)
        secondsRemaining = timerLengthSeconds
        updateButtons()
        updateCountdownUI()
    }

    private fun startTimer() {
        timerState = TimerState.RUNNING
        timer = object : CountDownTimer(secondsRemaining * 1000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                secondsRemaining = millisUntilFinished / 1000
                updateCountdownUI()
            }

            override fun onFinish() {
                onTimerFinished()
                loadConfeti()
                Toast.makeText(this@MainActivity, "Countdown Completed", Toast.LENGTH_LONG).show()
            }

        }.start()
    }


    private fun setNewTimerLength() {
        val lengthInMinutes = PrefUtil.getTimerLength(this)
        timerLengthSeconds = (lengthInMinutes * 60L)
        binding.progressLayout.progressCountdown.max = timerLengthSeconds.toInt()


    }

    private fun setPreviousTimerLength() {
        timerLengthSeconds = PrefUtil.getPreviousTimerLengthSeconds(this)
        binding.progressLayout.progressCountdown.max = timerLengthSeconds.toInt()
    }

    @SuppressLint("SetTextI18n")
    private fun updateCountdownUI() {
        val minutesUntilFinished = secondsRemaining / 60
        val secondsInMinutesUntilFinished = secondsRemaining - minutesUntilFinished * 60
        val secondsString = secondsInMinutesUntilFinished.toString()
        binding.progressLayout.countdownText.text = "$minutesUntilFinished:${
            if (secondsString.length == 2) secondsString
            else "0$secondsString"

        }"
        binding.progressLayout.progressCountdown.progress =
            (timerLengthSeconds - secondsRemaining).toInt()
    }

    // handling Button Behaviours
    private fun updateButtons() {
        when (timerState) {
            TimerState.RUNNING -> {
                binding.fabStart.isEnabled = false
                binding.fabPause.isEnabled = true
                binding.fabStop.isEnabled = true
            }

            TimerState.PAUSED -> {
                binding.fabStart.isEnabled = true
                binding.fabPause.isEnabled = false
                binding.fabStop.isEnabled = false
            }
            TimerState.STOPPED -> {
                binding.fabStart.isEnabled = true
                binding.fabPause.isEnabled = false
                binding.fabStop.isEnabled = false
            }
        }
    }

    //    Handles the congratulations flower for completing the Timer set
    private fun loadConfeti() {
        binding.viewKonfetti.build()
            .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
            .setDirection(0.0, 359.0)
            .setSpeed(1f, 5f)
            .setFadeOutEnabled(true)
            .setTimeToLive(2000L)
            .addShapes(Shape.Square, Shape.Circle)
            .addSizes(Size(12))
            .setPosition(-50f, binding.viewKonfetti.width + 50f, -50f, -50f)
            .streamFor(300, 5000L)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                Intent(this, SettingsActivity::class.java).also {
                    startActivity(it)
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}