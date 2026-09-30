package com.example.thwlogin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class WorkTimeNotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_STOP_WORK_TRACKING = "com.example.thwlogin.ACTION_STOP_WORK_TRACKING"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_STOP_WORK_TRACKING) {
            val entry = WorkTimeManager.stopTracking(context)
            WorkTimeNotificationHelper.cancelNotification(context)

            if (entry != null) {
                val formatted = WorkTimeManager.formatMinutesToHours(entry.durationMinutes)
                Toast.makeText(context, "Dienst beendet ($formatted erfasst)", Toast.LENGTH_LONG).show()
            }
        }
    }
}
