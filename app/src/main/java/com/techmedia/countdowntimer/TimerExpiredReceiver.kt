package com.techmedia.countdowntimer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.techmedia.countdowntimer.util.NotificationUtil
import com.techmedia.countdowntimer.util.PrefUtil

class TimerExpiredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // show notification
        NotificationUtil.showTimerFinished(context)

        PrefUtil.setTimerState(MainActivity.TimerState.STOPPED, context)
        PrefUtil.setAlarmSetTime(0, context)
    }
}