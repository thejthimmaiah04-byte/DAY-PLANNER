package com.liana.dayplanner.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.liana.dayplanner.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(context).taskDao()
                dao.tasksWithFutureReminders(System.currentTimeMillis()).forEach { task ->
                    ReminderScheduler.schedule(context, task)
                }
                DailyNudge.scheduleAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}
