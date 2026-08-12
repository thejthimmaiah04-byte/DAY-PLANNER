package com.liana.dayplanner.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.widget.RemoteViews
import androidx.core.content.res.ResourcesCompat
import com.liana.dayplanner.MainActivity
import com.liana.dayplanner.R
import com.liana.dayplanner.data.AppDatabase
import com.liana.dayplanner.data.AppSettings
import com.liana.dayplanner.data.Priority
import com.liana.dayplanner.data.SettingsStore
import com.liana.dayplanner.data.Task
import com.liana.dayplanner.planner.DayPlanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin

class ArcWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, mgr: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = AppDatabase.get(context).taskDao().getAll()
                val settings = SettingsStore.current(context)
                val clockBmp = renderClock(context, tasks, settings)
                val tasksBmp = renderTasks(context, tasks)
                val views = RemoteViews(context.packageName, R.layout.widget_arc)
                views.setImageViewBitmap(R.id.widget_clock_image, clockBmp)
                views.setImageViewBitmap(R.id.widget_tasks_image, tasksBmp)

                // Tap the clock -> jump straight into Calendar (widgets can't detect
                // swipe gestures, so a direct tap is the closest reliable equivalent)
                views.setOnClickPendingIntent(R.id.widget_clock_image,
                    PendingIntent.getActivity(context, 930001,
                        Intent(context, MainActivity::class.java)
                            .putExtra("openScreen", "calendar")
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
                // Tap the task list -> open the app on Today
                views.setOnClickPendingIntent(R.id.widget_tasks_image,
                    PendingIntent.getActivity(context, 930002,
                        Intent(context, MainActivity::class.java)
                            .putExtra("openScreen", "today")
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))

                ids.forEach { mgr.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        fun requestUpdate(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, ArcWidget::class.java))
            if (ids.isEmpty()) return
            val intent = Intent(context, ArcWidget::class.java)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            context.sendBroadcast(intent)
        }

        private val INK = Color.parseColor("#F20B0C10")
        private val SURFACE = Color.parseColor("#FF13141A")
        private val HAIRLINE = Color.parseColor("#FF262832")
        private val DIM_PETAL = Color.parseColor("#FF2B2D36")
        private val IVORY = Color.parseColor("#FFF2EEE5")
        private val IVORY_DIM = Color.parseColor("#FF9C9CA6")
        private val IVORY_FAINT = Color.parseColor("#FF5E5F6A")
        private val GOLD = Color.parseColor("#FFD4B476")
        private val ON_GOLD = Color.parseColor("#FF1C1505")

        private fun priColor(p: Priority) = when (p) {
            Priority.URGENT -> Color.parseColor("#FFC96F5A")
            Priority.HIGH -> GOLD
            Priority.MEDIUM -> Color.parseColor("#FF8FA98F")
            Priority.LOW -> Color.parseColor("#FF7C8493")
        }

        private fun lerpColor(from: Int, to: Int, t: Float): Int {
            val f = t.coerceIn(0f, 1f)
            fun ch(shift: Int) = ((from shr shift) and 0xFF) + (((to shr shift) and 0xFF) - ((from shr shift) and 0xFF)) * f
            val a = ch(24).toInt(); val r = ch(16).toInt(); val g = ch(8).toInt(); val b = ch(0).toInt()
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }

        private fun loadFonts(context: Context) = Triple(
            runCatching { ResourcesCompat.getFont(context, R.font.fraunces_semibold) }.getOrNull() ?: Typeface.SERIF,
            runCatching { ResourcesCompat.getFont(context, R.font.inter_medium) }.getOrNull() ?: Typeface.SANS_SERIF,
            runCatching { ResourcesCompat.getFont(context, R.font.inter_semibold) }.getOrNull() ?: Typeface.DEFAULT_BOLD
        )

        /** Top half: a 12-petal radial day clock, matching the in-app DayClock. */
        fun renderClock(context: Context, tasks: List<Task>, settings: AppSettings): Bitmap {
            val w = 800; val h = 780
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val (serif, sans, sansBold) = loadFonts(context)

            val bgPath = Path().apply {
                addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()),
                    floatArrayOf(56f,56f, 56f,56f, 0f,0f, 0f,0f), Path.Direction.CW)
            }
            c.drawPath(bgPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK })

            val today = LocalDate.now()
            val p = Paint(Paint.ANTI_ALIAS_FLAG)

            // header: month/year + logo mark, same as before
            p.typeface = serif; p.textSize = 46f; p.color = IVORY
            val monthTxt = today.format(java.time.format.DateTimeFormatter.ofPattern(
                "MMMM", java.util.Locale.ENGLISH))
            c.drawText(monthTxt, 40f, 84f, p)
            p.typeface = sans; p.textSize = 26f; p.color = IVORY_FAINT
            val monthW = Paint(p).apply { typeface = serif; textSize = 46f }.measureText(monthTxt)
            c.drawText("${today.year}", 40f + monthW + 16f, 84f, p)
            p.typeface = sansBold; p.textSize = 24f; p.color = GOLD; p.letterSpacing = 0.25f
            val name = "ARC"; val nameW = p.measureText(name)
            c.drawText(name, w - 40f - nameW, 80f, p)
            p.letterSpacing = 0f
            val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = GOLD; style = Paint.Style.STROKE; strokeWidth = 4.5f; strokeCap = Paint.Cap.ROUND }
            val ax = w - 40f - nameW - 42f
            c.drawArc(RectF(ax, 52f, ax + 26f, 78f), 300f, 300f, false, arcPaint)

            // occupancy per 2-hour slot from the real plan
            val plan = DayPlanner.plan(tasks, today, settings = settings)
            val occ = FloatArray(12)
            for (b in plan.blocks) {
                for (i in 0 until 12) {
                    val slotStart = i * 120; val slotEnd = slotStart + 120
                    val overlap = (minOf(b.endMin, slotEnd) - maxOf(b.startMin, slotStart)).coerceAtLeast(0)
                    occ[i] += overlap / 120f
                }
            }
            for (i in occ.indices) occ[i] = occ[i].coerceIn(0f, 1f)
            val totalMin = plan.blocks.sumOf { it.endMin - it.startMin }
            val nowMin = LocalTime.now().let { it.hour * 60 + it.minute }
            val currentSlot = (nowMin / 120).coerceIn(0, 11)

            val cx = w / 2f; val cy = 470f
            val midR = 210f; val stroke = 92f
            val oval = RectF(cx - midR, cy - midR, cx + midR, cy + midR)
            val gapDeg = 8f

            for (i in 0 until 12) {
                val centerAngle = -90f + i * 30f
                val startAngle = centerAngle - 15f + gapDeg / 2
                val sweep = 30f - gapDeg
                val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND
                    color = lerpColor(DIM_PETAL, GOLD, occ[i])
                }
                c.drawArc(oval, startAngle, sweep, false, ring)
                if (i == currentSlot) {
                    val hi = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        style = Paint.Style.STROKE; strokeWidth = 3f; color = GOLD }
                    val r2 = midR + stroke / 2 + 8f
                    c.drawArc(RectF(cx - r2, cy - r2, cx + r2, cy + r2), startAngle, sweep, false, hi)
                }
                // hour label: always a black pill with white text, for
                // consistent contrast regardless of the petal's brightness
                val labelAngle = Math.toRadians(centerAngle.toDouble())
                val lx = cx + (midR * cos(labelAngle)).toFloat()
                val ly = cy + (midR * sin(labelAngle)).toFloat()
                p.typeface = sans; p.textSize = 22f
                val label = "%02d".format(i * 2)
                val textW = p.measureText(label)
                val boxW = textW + 20f; val boxH = 34f
                c.drawRoundRect(
                    RectF(lx - boxW / 2f, ly - boxH / 2f, lx + boxW / 2f, ly + boxH / 2f),
                    8f, 8f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK })
                p.color = Color.WHITE
                c.drawText(label, lx - textW / 2f, ly + 8f, p)
            }

            // now marker
            val nowAngle = Math.toRadians((-90f + nowMin / 1440f * 360f).toDouble())
            val nowR = midR + stroke / 2 + 26f
            c.drawCircle(cx + (nowR * cos(nowAngle)).toFloat(), cy + (nowR * sin(nowAngle)).toFloat(),
                9f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GOLD })

            // center summary
            val hoursLabel = if (totalMin >= 60)
                "${totalMin / 60}h" + (if (totalMin % 60 != 0) " ${totalMin % 60}m" else "")
                else "${totalMin}m"
            p.typeface = serif; p.textSize = 44f; p.color = GOLD
            c.drawText(hoursLabel, cx - p.measureText(hoursLabel) / 2f, cy - 2f, p)
            p.typeface = sans; p.textSize = 20f; p.color = IVORY_DIM
            val occLabel = "occupied"
            c.drawText(occLabel, cx - p.measureText(occLabel) / 2f, cy + 26f, p)

            return bmp
        }

        /** Bottom half: today's task list, unchanged from before. */
        fun renderTasks(context: Context, tasks: List<Task>): Bitmap {
            val w = 800; val h = 780
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val (_, sans, sansBold) = loadFonts(context)

            val bgPath = Path().apply {
                addRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()),
                    floatArrayOf(0f,0f, 0f,0f, 56f,56f, 56f,56f), Path.Direction.CW)
            }
            c.drawPath(bgPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INK })
            c.drawLine(40f, 0f, w - 40f, 0f, Paint().apply { color = HAIRLINE; strokeWidth = 2f })

            val today = LocalDate.now()
            val p = Paint(Paint.ANTI_ALIAS_FLAG)

            val dayTasks = tasks.filter {
                it.dueEpochDay != null &&
                (it.dueEpochDay == today.toEpochDay() ||
                 (it.dueEpochDay < today.toEpochDay() && !it.isDone))
            }.sortedWith(compareBy<Task> { it.isDone }.thenByDescending { it.priority.weight })
            val done = dayTasks.count { it.isDone }

            p.typeface = sansBold; p.textSize = 26f; p.color = IVORY_FAINT; p.letterSpacing = 0.15f
            c.drawText("TODAY", 40f, 48f, p)
            p.color = GOLD
            c.drawText("$done OF ${dayTasks.size} DONE", 40f + p.measureText("TODAY  "), 48f, p)
            p.letterSpacing = 0f

            if (dayTasks.isEmpty()) {
                p.typeface = sans; p.textSize = 30f; p.color = IVORY_FAINT
                c.drawText("A clear day", 40f, 130f, p)
            } else {
                val maxRows = 6
                val rowTop = 78f
                val taskRowH = 92f
                dayTasks.take(maxRows).forEachIndexed { i, t ->
                    val top = rowTop + i * (taskRowH + 14f)
                    val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SURFACE }
                    c.drawRoundRect(RectF(40f, top, w - 40f, top + taskRowH), 26f, 26f, card)
                    val ringCx = 88f; val ringCy = top + taskRowH / 2f
                    if (t.isDone) {
                        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GOLD }
                        c.drawCircle(ringCx, ringCy, 20f, fill)
                        val tick = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = ON_GOLD; style = Paint.Style.STROKE
                            strokeWidth = 5f; strokeCap = Paint.Cap.ROUND }
                        c.drawLine(ringCx - 9f, ringCy, ringCx - 2f, ringCy + 8f, tick)
                        c.drawLine(ringCx - 2f, ringCy + 8f, ringCx + 10f, ringCy - 7f, tick)
                    } else {
                        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = priColor(t.priority); style = Paint.Style.STROKE; strokeWidth = 4f }
                        c.drawCircle(ringCx, ringCy, 20f, ring)
                    }
                    p.typeface = sans; p.textSize = 32f
                    p.color = if (t.isDone) IVORY_FAINT else IVORY
                    var title = t.title
                    val maxW = w - 40f - 140f - (if (t.fixedTime != null) 130f else 40f)
                    while (p.measureText(title) > maxW && title.length > 3)
                        title = title.dropLast(4) + "…"
                    c.drawText(title, 128f, ringCy + 11f, p)
                    if (t.isDone) {
                        val strike = Paint().apply { color = IVORY_FAINT; strokeWidth = 3f }
                        c.drawLine(128f, ringCy, 128f + p.measureText(title), ringCy, strike)
                    }
                    if (t.fixedTime != null && !t.isDone) {
                        p.typeface = sansBold; p.textSize = 26f; p.color = GOLD
                        val tw = p.measureText(t.fixedTime)
                        c.drawText(t.fixedTime, w - 60f - tw, ringCy + 9f, p)
                    }
                }
                if (dayTasks.size > maxRows) {
                    p.typeface = sans; p.textSize = 26f; p.color = IVORY_FAINT
                    val more = "+ ${dayTasks.size - maxRows} more in ARC"
                    c.drawText(more, (w - p.measureText(more)) / 2f,
                        rowTop + maxRows * (taskRowH + 14f) + 30f, p)
                }
            }
            return bmp
        }
    }
}
