package com.mui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mui.theme.LocalMuiPalette

// TODO: 坐标轴、网格线、图例与 tooltip 未移植（web 走 recharts）。
// TODO: 多系列数据未支持（web 的 ChartConfig 多 key），只画单列表。

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
    val chartData = data.filter { it >= 0f }
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
