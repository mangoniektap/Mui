package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiPalette
import com.mui.theme.rememberMuiInteractionSource
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * 月历（calendar.tsx 的单月视图）：星期表头 + 6×7 网格；选中日 = bg-primary 胶囊、
 * 今日 = 描边 outline、月外日期 = muted；左右翻月按钮用 Canvas 箭头。
 * 输入 = selected/onSelect + 月份状态（内部维护当前显示月）；输出 = onSelect。
 * 硬约束：禁 material3；视觉全走令牌；单元格 32dp（--cell-size = spacing(8)）。
 * TODO: web 的区间选择（range_start/range_end/range_middle）与多选未移植，仅单选。
 * TODO: web 的 captionLayout 下拉（年/月选择器）未移植，仅 label 文本。
 * TODO: web 的 disabled 日期（min/max 等 modifiers）未移植。
 * TODO: web 的 RTL 翻转与 showWeekNumber 未移植。
 *
 * @param selected 受控选中日（null = 非受控，内部维护）
 * @param onSelect 点击某天的回调
 * @param modifier 布局修饰
 * @param initialMonth 初始显示月份（默认今天所在月）
 * @param showOutsideDays 是否显示月外日期（默认 true，对齐 web）
 * @param disabled 返回 true 的日期不可选（null = 全可选）
 */
@Composable
fun MuiCalendar(
    selected: LocalDate? = null,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    initialMonth: YearMonth = YearMonth.now(),
    showOutsideDays: Boolean = true,
    disabled: ((LocalDate) -> Boolean)? = null,
) {
    val palette = LocalMuiPalette.current
    var displayedMonth by remember(initialMonth) { mutableStateOf(initialMonth) }
    var innerSelected by remember { mutableStateOf<LocalDate?>(null) }
    val effectiveSelected = selected ?: innerSelected
    val today = remember { LocalDate.now() }

    // 每周首日跟随 locale（对齐 DayPicker 默认）
    val firstDayOfWeek = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }
    val weekdayLabels = remember(firstDayOfWeek) {
        val names = DayOfWeek.entries.associateWith {
            it.getDisplayName(JavaTextStyle.NARROW, Locale.getDefault())
        }
        (0 until 7).map { offset -> names[DayOfWeek.of(((firstDayOfWeek.value - 1 + offset) % 7) + 1)] ?: "" }
    }
    // 网格起点：显示月首日往前推到每周首日；固定 6 行 × 7 列
    val gridStart = remember(displayedMonth, firstDayOfWeek) {
        val first = displayedMonth.atDay(1)
        val back = (first.dayOfWeek.value + 7 - firstDayOfWeek.value) % 7
        first.minusDays(back.toLong())
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // 头部：上翻月 + 月份标题 + 下翻月（nav 绝对居中，这里用等宽三段近似）
        Row(verticalAlignment = Alignment.CenterVertically) {
            MuiCalendarNavButton(
                direction = CalendarNavDirection.Previous,
                onClick = { displayedMonth = displayedMonth.minusMonths(1) },
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                MuiText(
                    text = formatMonth(displayedMonth),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                )
            }
            MuiCalendarNavButton(
                direction = CalendarNavDirection.Next,
                onClick = { displayedMonth = displayedMonth.plusMonths(1) },
            )
        }
        // 星期表头（text-[0.8rem] ≈ 13sp text-muted-foreground）
        Row {
            for (label in weekdayLabels) {
                Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    MuiText(text = label, fontSize = 13.sp, lineHeight = 16.sp, color = palette.mutedForeground, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        // 6×7 日期网格（week mt-2 = 行距 8dp；格间 border-collapse 无间距）
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(6) { week ->
                Row {
                    repeat(7) { dayOfWeek ->
                        val date = gridStart.plusDays((week * 7 + dayOfWeek).toLong())
                        val isOutside = !YearMonth.from(date).equals(displayedMonth)
                        val isSelected = date == effectiveSelected
                        val isToday = date == today
                        val isHidden = isOutsideNotShown(showOutsideDays, isOutside)
                        val isDisabled = isHidden || disabled?.invoke(date) == true
                        MuiCalendarDayCell(
                            dayText = date.dayOfMonth.toString(),
                            selected = isSelected,
                            today = isToday,
                            outside = isOutside,
                            hidden = isHidden,
                            disabled = isDisabled,
                            onClick = {
                                if (selected == null) innerSelected = date
                                onSelect(date)
                            },
                        )
                    }
                }
            }
        }
    }
}

/** 月外日期且不显示月外时该格隐藏 */
private fun isOutsideNotShown(showOutsideDays: Boolean, isOutside: Boolean) = !showOutsideDays && isOutside

/** 月份标题文本（TODO: web 用 toLocaleString short 格式化，此处用本地数字拼装） */
private fun formatMonth(month: YearMonth): String = "${month.year}年${month.monthValue}月"

/**
 * 单日格：32dp 方格；选中 = bg-primary 胶囊 + primaryForeground；今日 = border 描边（胶囊）；
 * 月外 = muted-foreground；禁用 = 半透明；隐藏（不显示月外）= 空占位。
 *
 * @param dayText 显示的日数字
 * @param selected 是否选中
 * @param today 是否今天
 * @param outside 是否月外日期
 * @param hidden 是否隐藏（showOutsideDays=false 时的月外格）
 * @param disabled 是否禁用
 * @param onClick 点击回调
 */
@Composable
private fun MuiCalendarDayCell(
    dayText: String,
    selected: Boolean,
    today: Boolean,
    outside: Boolean,
    hidden: Boolean,
    disabled: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalMuiPalette.current
    val shape = RoundedCornerShape(50)
    val background = if (selected) palette.primary else Color.Transparent
    val textColor = when {
        selected -> palette.primaryForeground
        disabled || outside -> palette.mutedForeground
        else -> palette.foreground
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .then(if (!hidden) Modifier.alpha(if (disabled) 0.5f else 1f) else Modifier)
            .clip(shape)
            .background(background, shape)
            .then(if (today && !selected) Modifier.border(1.dp, palette.border, shape) else Modifier)
            .clickable(
                interactionSource = rememberMuiInteractionSource(),
                indication = null,
                enabled = !disabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (!hidden) {
            MuiText(text = dayText, fontSize = 13.sp, lineHeight = 16.sp, color = textColor, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

/** 翻月方向（calendar.tsx 的 Chevron orientation） */
private enum class CalendarNavDirection { Previous, Next }

/**
 * 翻月按钮：32dp ghost 图标钮（buttonVariants ghost + size-(--cell-size)），Canvas 画箭头。
 *
 * @param direction 翻月方向（Previous = 左箭头 / Next = 右箭头）
 * @param onClick 点击回调
 * @param interactionSource 交互源（聚焦/悬停出口）
 */
@Composable
private fun MuiCalendarNavButton(
    direction: CalendarNavDirection,
    onClick: () -> Unit,
) {
    val palette = LocalMuiPalette.current
    val interactionSource = rememberMuiInteractionSource()
    val hovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (hovered) palette.muted else Color.Transparent, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
            drawChevron(direction, palette.foreground)
        }
    }
}

/** Canvas 箭头：两段折线组成 < 或 > */
private fun DrawScope.drawChevron(direction: CalendarNavDirection, color: Color) {
    val w = size.width
    val h = size.height
    val tipX = if (direction == CalendarNavDirection.Previous) w * 0.40f else w * 0.60f
    val stroke = 1.5.dp.toPx()
    drawLine(color, Offset(tipX, h * 0.25f), if (direction == CalendarNavDirection.Previous) Offset(w * 0.60f, h / 2f) else Offset(w * 0.40f, h / 2f), strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(color, if (direction == CalendarNavDirection.Previous) Offset(w * 0.60f, h / 2f) else Offset(w * 0.40f, h / 2f), Offset(tipX, h * 0.75f), strokeWidth = stroke, cap = StrokeCap.Round)
}
