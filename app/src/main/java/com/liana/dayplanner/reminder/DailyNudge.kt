package com.liana.dayplanner.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.liana.dayplanner.MainActivity
import com.liana.dayplanner.R
import com.liana.dayplanner.data.AppDatabase
import com.liana.dayplanner.data.VisionCheckStore
import com.liana.dayplanner.data.WeeklyReflectionStore
import com.liana.dayplanner.planner.DayPlanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Reliable daily notifications via exact alarms:
 *  - 06:00  "Here is how your day looks today" with a Read-aloud action
 *  - 21:00  wind-down summary
 * Each firing reschedules the next occurrence; BootReceiver re-arms after reboot.
 */
object DailyNudge {
    const val CHANNEL_ID = "arc_daily"
    private const val MORNING_REQ = 900001
    private const val EVENING_REQ = 900002

    fun scheduleAll(context: Context) {
        val s = com.liana.dayplanner.data.SettingsStore.current(context)
        schedule(context, LocalTime.of(s.morningHour, s.morningMinute), MORNING_REQ, "morning")
        schedule(context, LocalTime.of(s.eveningHour, s.eveningMinute), EVENING_REQ, "evening")
    }

    private fun schedule(context: Context, at: LocalTime, req: Int, kind: String) {
        val now = LocalDateTime.now()
        var next = LocalDateTime.of(LocalDate.now(), at)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(context, DailyNudgeReceiver::class.java).putExtra("kind", kind)
        val pi = PendingIntent.getBroadcast(context, req, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val canExact = if (Build.VERSION.SDK_INT >= 31) am.canScheduleExactAlarms() else true
        if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pi)
        else am.setWindow(AlarmManager.RTC_WAKEUP, millis, 15 * 60 * 1000L, pi)
    }

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_ID, "Daily briefings", NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Morning brief and evening wind-down" })
    }
}

class DailyNudgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra("kind") ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DailyNudge.ensureChannel(context)
                val tasks = AppDatabase.get(context).taskDao().getAll()
                val settings = com.liana.dayplanner.data.SettingsStore.current(context)
                val plan = DayPlanner.plan(tasks, LocalDate.now(), settings = settings)
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                if (kind == "morning") {
                    val uniq = plan.blocks.distinctBy { it.task.id }.size
                    val body = if (uniq == 0 && plan.triage.isEmpty())
                        "A clear day ahead. Tap to plan it."
                    else "You have $uniq planned " + (if (uniq == 1) "task" else "tasks") +
                         (if (plan.triage.isNotEmpty()) " and ${plan.triage.size} to triage" else "") + "."
                    val openPi = PendingIntent.getActivity(context, 910001,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    val briefPi = PendingIntent.getActivity(context, 910002,
                        Intent(context, MainActivity::class.java)
                            .putExtra("startBrief", true)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    nm.notify(910000, NotificationCompat.Builder(context, DailyNudge.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Here is how your day looks today")
                        .setContentText(body)
                        .setContentIntent(openPi)
                        .addAction(0, "Read aloud", briefPi)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .build())
                } else {
                    val uniq = plan.blocks.distinctBy { it.task.id }.size
                    val total = uniq + plan.overflow.size + plan.completed.size
                    val tomorrow = DayPlanner.plan(tasks, LocalDate.now().plusDays(1), settings = settings)

                    val isSunday = LocalDate.now().dayOfWeek == DayOfWeek.SUNDAY
                    val needsReflection = isSunday && !WeeklyReflectionStore.hasReflectionThisWeek(context)
                    val visionDays = VisionCheckStore.daysSinceLastReview(context)
                    val needsVisionNudge = visionDays != null && visionDays >= 30

                    var body = "You completed ${plan.completed.size} of $total today."
                    tomorrow.blocks.firstOrNull()?.let { b ->
                        val h = b.startMin / 60; val m = b.startMin % 60
                        body += " Tomorrow starts with \"${b.task.title}\" at %02d:%02d.".format(h, m)
                    }
                    if (needsVisionNudge) {
                        body += " Your vision hasn't been reviewed in $visionDays days — worth a look."
                    }

                    val openPi = PendingIntent.getActivity(context, 920001,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

                    val builder = NotificationCompat.Builder(context, DailyNudge.CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(if (needsReflection) "Time to reflect on your week" else "Winding down")
                        .setContentText(body)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                        .setContentIntent(openPi)
                        .setAutoCancel(true)

                    if (needsReflection) {
                        val reflectPi = PendingIntent.getActivity(context, 930001,
                            Intent(context, MainActivity::class.java)
                                .putExtra("openScreen", "reflection")
                                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                        builder.addAction(0, "Reflect on week", reflectPi)
                    }

                    nm.notify(920000, builder.build())
                }
            } finally {
                DailyNudge.scheduleAll(context)   // arm tomorrow's alarm
                pending.finish()
            }
        }
    }
}
