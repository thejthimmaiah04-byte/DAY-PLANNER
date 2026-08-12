package com.liana.dayplanner.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.liana.dayplanner.planner.PlannedBlock
import com.liana.dayplanner.ui.theme.Champagne
import com.liana.dayplanner.ui.theme.IvoryDim
import com.liana.dayplanner.ui.theme.Terracotta
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.sin

/**
 * A 12-petal radial clock (each petal = a 2-hour slot of the day). A petal's
 * brightness shows how much of that slot is occupied by scheduled tasks —
 * dim = free, warm gold = fully booked. Inspired by a Plutchik-style flower.
 */
@Composable
fun DayClock(
    blocks: List<PlannedBlock>,
    modifier: Modifier = Modifier,
    urgentPendingCount: Int = 0
) {
    // occupancy fraction per 2-hour slot (12 slots covering 24h)
    val occ = FloatArray(12)
    for (b in blocks) {
        for (i in 0 until 12) {
            val slotStart = i * 120
            val slotEnd = slotStart + 120
            val overlap = (minOf(b.endMin, slotEnd) - maxOf(b.startMin, slotStart)).coerceAtLeast(0)
            occ[i] += overlap / 120f
        }
    }
    for (i in occ.indices) occ[i] = occ[i].coerceIn(0f, 1f)

    val totalMin = blocks.sumOf { it.endMin - it.startMin }
    val nowMin = LocalTime.now().let { it.hour * 60 + it.minute }
    val currentSlot = (nowMin / 120).coerceIn(0, 11)

    // Sized so the outermost element (now-marker dot) always stays inside
    // the box with margin to spare — nothing gets clipped at any density.
    val boxSize = 320.dp
    val strokeDp = 44.dp
    val midRadiusDp = 100.dp
    val density = LocalDensity.current

    val dimPetal = Color(0xFF2B2D36)
    val gapDeg = 9f

    Box(modifier.size(boxSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(boxSize)) {
            val stroke = with(density) { strokeDp.toPx() }
            val midR = with(density) { midRadiusDp.toPx() }
            val center = Offset(size.width / 2, size.height / 2)
            val topLeft = Offset(center.x - midR, center.y - midR)
            val arcSize = Size(midR * 2, midR * 2)

            for (i in 0 until 12) {
                val centerAngle = -90f + i * 30f
                val startAngle = centerAngle - 15f + gapDeg / 2
                val sweep = 30f - gapDeg
                val color = lerp(dimPetal, Champagne, occ[i])
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                // ring highlight on the current slot — red when urgent tasks are pending
                if (i == currentSlot) {
                    val ringColor = if (urgentPendingCount > 0) Terracotta else Champagne
                    drawArc(
                        color = ringColor,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = Offset(center.x - (midR + stroke / 2 + 5f),
                            center.y - (midR + stroke / 2 + 5f)),
                        size = Size((midR + stroke / 2 + 5f) * 2, (midR + stroke / 2 + 5f) * 2),
                        style = Stroke(width = if (urgentPendingCount > 0) 3f else 2f)
                    )
                }
            }

            // "now" marker dot just outside the ring
            val nowAngle = Math.toRadians((-90f + nowMin / 1440f * 360f).toDouble())
            val nowR = midR + stroke / 2 + 14f
            drawCircle(
                Champagne,
                radius = 5f,
                center = Offset(
                    center.x + (nowR * cos(nowAngle)).toFloat(),
                    center.y + (nowR * sin(nowAngle)).toFloat()
                )
            )
        }

        // hour labels: always a black pill with white text, for consistent
        // contrast whether the petal underneath is dim or fully gold
        for (i in 0 until 12) {
            val centerAngle = Math.toRadians((-90f + i * 30f).toDouble())
            val x = midRadiusDp * cos(centerAngle).toFloat()
            val y = midRadiusDp * sin(centerAngle).toFloat()
            Box(
                Modifier
                    .offset(x = x, y = y)
                    .background(Color.Black, RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "%02d".format(i * 2),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }

        // center summary
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (totalMin >= 60) "${totalMin / 60}h${if (totalMin % 60 != 0) " ${totalMin % 60}m" else ""}"
                else "${totalMin}m",
                style = MaterialTheme.typography.headlineMedium,
                color = Champagne
            )
            Text("occupied", style = MaterialTheme.typography.labelMedium, color = IvoryDim)
        }
    }
}
