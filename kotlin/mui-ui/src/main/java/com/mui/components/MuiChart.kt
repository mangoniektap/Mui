package com.mui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiElevation
import com.mui.token.MuiRadius
import kotlin.math.floor
import kotlin.math.roundToInt

/** 图表形态（chart.tsx 的最小子集） */
enum class MuiChartType { Line, Bar, Pie }

/**
 * 最小图表：Canvas 折线/柱状/饼图（数据 List<Float>）。
 * 输入数据 + 形态；输出按 palette.chart1..chart5 着色的图。
 * 硬约束：颜色循环取 chart1..5；饼图从 12 点方向顺时针。
 *
 * @param data 数据系列（正值；空/全零时输出空图）
 * @param modifier 布局修饰（尺寸由调用方给）
 * @param type 图表形态（默认 Line）
 */
@Composable
fun MuiChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    type: MuiChartType = MuiChartType.Line,
) {
    val palette = LocalMuiPalette.current
    val seriesColors = listOf(palette.chart1, palette.chart2, palette.chart3, palette.chart4, palette.chart5)
    val chartData = data.filter { it.isFinite() && it >= 0f }
    val maxV = chartData.maxOrNull() ?: 0f

    Canvas(modifier = modifier.fillMaxSize()) {
        if (chartData.isEmpty() || maxV <= 0f) return@Canvas
        when (type) {
            MuiChartType.Line -> drawLineChart(chartData, maxV, seriesColors[0])
            MuiChartType.Bar -> drawBarChart(chartData, maxV, seriesColors)
            MuiChartType.Pie -> drawPieChart(chartData, seriesColors)
        }
    }
}

/** 折线：2dp chart1 折线，上下留 8dp */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLineChart(
    data: List<Float>,
    maxV: Float,
    color: Color,
) {
    val pad = 8.dp.toPx()
    val stepX = if (data.size == 1) 0f else (size.width - pad * 2) / (data.size - 1)
    val path = Path()
    data.forEachIndexed { i, v ->
        val x = pad + stepX * i
        val y = size.height - pad - (v / maxV) * (size.height - pad * 2)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
}

/** 柱状：槽宽均分，柱占 60%，颜色循环 chart1..5 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBarChart(
    data: List<Float>,
    maxV: Float,
    colors: List<Color>,
) {
    val slot = size.width / data.size
    val barWidth = slot * 0.6f
    data.forEachIndexed { i, v ->
        val h = (v / maxV) * size.height
        drawRoundRect(
            color = colors[i % colors.size],
            topLeft = Offset(slot * i + (slot - barWidth) / 2, size.height - h),
            size = Size(barWidth, h),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
        )
    }
}

/** 饼图：比例扇区，颜色循环 chart1..5，从 12 点方向顺时针 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPieChart(
    data: List<Float>,
    colors: List<Color>,
) {
    val total = data.sum()
    if (total <= 0f) return
    val diameter = minOf(size.width, size.height)
    val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
    var start = -90f
    data.forEachIndexed { i, v ->
        val sweep = v / total * 360f
        drawArc(
            color = colors[i % colors.size],
            startAngle = start,
            sweepAngle = sweep,
            useCenter = true,
            topLeft = topLeft,
            size = Size(diameter, diameter),
        )
        start += sweep
    }
}

/** 一条数据系列及其图例名，颜色可按主题覆盖。 */
data class MuiChartSeries(
    val label: String,
    val values: List<Float>,
    val color: Color = Color.Unspecified,
    val darkColor: Color = Color.Unspecified,
)

/**
 * 多系列图表，按 Web ChartContainer 的令牌绘制坐标、网格、悬停提示和图例。
 *
 * @param categories 每个数据索引对应的横轴标签。缺少标签时显示序号。
 * @param series 各系列的数值。NaN 或无穷值跳过，折线会在缺失值处断开。
 * @param type 画折线、分组柱或饼图。饼图读取第一条系列的非负数值。
 */
@Composable
fun MuiChart(
    categories: List<String>,
    series: List<MuiChartSeries>,
    modifier: Modifier = Modifier,
    type: MuiChartType = MuiChartType.Line,
    showLegend: Boolean = true,
    showGrid: Boolean = true,
    formatValue: (Float) -> String = { it.toString() },
) {
    val palette = LocalMuiPalette.current
    val dark = LocalIsDark.current
    val defaults = listOf(palette.chart1, palette.chart2, palette.chart3, palette.chart4, palette.chart5)
    val colors = series.mapIndexed { index, item ->
        when {
            dark && item.darkColor != Color.Unspecified -> item.darkColor
            item.color != Color.Unspecified -> item.color
            else -> defaults[index % defaults.size]
        }
    }
    val points = maxOf(categories.size, series.maxOfOrNull { it.values.size } ?: 0)
    val yRange = remember(series) {
        var low = 0f
        var high = 0f
        series.forEach { item ->
            item.values.forEach {
                if (it.isFinite()) {
                    low = minOf(low, it)
                    high = maxOf(high, it)
                }
            }
        }
        low..if (high == low) low + 1f else high
    }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = palette.mutedForeground, fontSize = 12.sp)
    var active by remember(points, type) { mutableIntStateOf(-1) }

    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Canvas(
                Modifier.fillMaxSize().pointerInput(type, points) {
                    if (type == MuiChartType.Pie || points <= 0) return@pointerInput
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pointer = event.changes.firstOrNull() ?: continue
                            if (event.type == PointerEventType.Exit || event.type == PointerEventType.Release) {
                                active = -1
                            } else if (event.type == PointerEventType.Move || event.type == PointerEventType.Press || event.type == PointerEventType.Enter) {
                                val plotWidth = size.width - 36.dp.toPx() - 8.dp.toPx()
                                if (plotWidth > 0f) {
                                    val position = (pointer.position.x - 36.dp.toPx()) / plotWidth
                                    active = if (position in 0f..1f) floor(position * points).toInt().coerceIn(0, points - 1) else -1
                                }
                            }
                        }
                    }
                },
            ) {
                if (points <= 0 || series.isEmpty()) return@Canvas
                if (type == MuiChartType.Pie) {
                    drawPieChart(series.first().values.map { if (it.isFinite()) it.coerceAtLeast(0f) else 0f }, defaults)
                    return@Canvas
                }

                val left = 36.dp.toPx()
                val top = 8.dp.toPx()
                val width = size.width - left - top
                val height = size.height - top - 20.dp.toPx()
                if (width <= 0f || height <= 0f) return@Canvas
                val slot = width / points
                fun x(index: Int): Float = left + slot * (index + 0.5f)
                fun y(value: Float): Float = top + height * (yRange.endInclusive - value) / (yRange.endInclusive - yRange.start)
                val baseline = y(0f)
                for (tick in 0..4) {
                    val fraction = tick / 4f
                    val value = yRange.start + (yRange.endInclusive - yRange.start) * fraction
                    val line = y(value)
                    if (showGrid) drawLine(palette.border.copy(alpha = palette.border.alpha * 0.5f), Offset(left, line), Offset(left + width, line))
                    val label = textMeasurer.measure(formatValue(value), style = labelStyle, maxLines = 1)
                    if (label.size.width + 4.dp.toPx() <= left) {
                        drawText(label, topLeft = Offset(left - 4.dp.toPx() - label.size.width, line - label.size.height / 2f))
                    }
                }
                val labelStep = (48.dp.toPx() / slot).roundToInt().coerceAtLeast(1)
                for (index in categories.indices step labelStep) {
                    val label = textMeasurer.measure(categories[index], style = labelStyle, maxLines = 1)
                    val start = (x(index) - label.size.width / 2f).coerceIn(0f, (size.width - label.size.width).coerceAtLeast(0f))
                    drawText(label, topLeft = Offset(start, top + height + 3.dp.toPx()))
                }
                if (active in 0 until points) {
                    if (type == MuiChartType.Bar) {
                        drawRect(palette.muted, Offset(x(active) - slot / 2f, top), Size(slot, height))
                    } else {
                        drawLine(palette.border, Offset(x(active), top), Offset(x(active), top + height))
                    }
                }
                series.forEachIndexed { seriesIndex, item ->
                    val color = colors[seriesIndex]
                    val path = Path()
                    var connected = false
                    val barWidth = slot * 0.72f / series.size
                    item.values.forEachIndexed pointsLoop@ { index, value ->
                        if (!value.isFinite()) {
                            connected = false
                            return@pointsLoop
                        }
                        val point = Offset(x(index), y(value))
                        if (type == MuiChartType.Line) {
                            if (connected) path.lineTo(point.x, point.y) else path.moveTo(point.x, point.y)
                            connected = true
                            if (index == active || item.values.size == 1) drawCircle(color, 3.dp.toPx(), point)
                        } else {
                            val barStart = x(index) - slot * 0.36f + seriesIndex * barWidth
                            drawRoundRect(
                                color, Offset(barStart, minOf(baseline, point.y)),
                                Size(barWidth, kotlin.math.abs(baseline - point.y)),
                                CornerRadius(minOf(2.dp.toPx(), barWidth / 2f)),
                            )
                        }
                    }
                    if (type == MuiChartType.Line) drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                }
            }

            if (active in 0 until points && type != MuiChartType.Pie) {
                val shape = RoundedCornerShape(MuiRadius.lg)
                Column(
                    Modifier.align(Alignment.TopEnd)
                        .padding(8.dp)
                        .muiElevation("xl", shape)
                        .background(palette.background, shape)
                        .border(1.dp, palette.border.copy(alpha = palette.border.alpha * 0.5f), shape)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MuiText(categories.getOrNull(active) ?: (active + 1).toString(), fontSize = 12.sp)
                    series.forEachIndexed { index, item ->
                        val value = item.values.getOrNull(active)?.takeIf { it.isFinite() }
                        if (value != null) ChartLabel(item.label, colors[index], formatValue(value))
                    }
                }
            }
        }
        if (showLegend && series.isNotEmpty()) {
            Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (type == MuiChartType.Pie) {
                    categories.forEachIndexed { index, label -> ChartLabel(label, defaults[index % defaults.size]) }
                } else {
                    series.forEachIndexed { index, item -> ChartLabel(item.label, colors[index]) }
                }
            }
        }
    }
}

@Composable
private fun ChartLabel(label: String, color: Color, value: String? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(color, RoundedCornerShape(2.dp)))
        MuiText(label, fontSize = 12.sp, color = LocalMuiPalette.current.mutedForeground)
        if (value != null) MuiText(value, fontSize = 12.sp)
    }
}
