package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiPalette
import com.mui.theme.rememberMuiInteractionSource
import com.mui.theme.muiFocusRing
import com.mui.token.MuiRadius
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale

/** 单日、多日和日期区间对应 Web DayPicker 的三种选择模式。 */
sealed interface MuiCalendarSelection {
    data class Single(val date: LocalDate? = null) : MuiCalendarSelection
    data class Multiple(val dates: Set<LocalDate> = emptySet()) : MuiCalendarSelection
    data class Range(val start: LocalDate? = null, val end: LocalDate? = null) : MuiCalendarSelection
}

enum class MuiCalendarCaption { Label, Dropdown, DropdownMonths, DropdownYears }

/** 月历的单选便捷接口，保留既有的受控和非受控调用。 */
@Composable
fun MuiCalendar(
    selected: LocalDate? = null,
    onSelect: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    initialMonth: YearMonth = YearMonth.now(),
    showOutsideDays: Boolean = true,
    disabled: ((LocalDate) -> Boolean)? = null,
) {
    var innerSelected by remember { mutableStateOf<LocalDate?>(null) }
    MuiCalendar(
        selection = MuiCalendarSelection.Single(selected ?: innerSelected),
        onSelectionChange = {
            val date = (it as MuiCalendarSelection.Single).date
            if (selected == null) innerSelected = date
            if (date != null) onSelect(date)
        },
        modifier = modifier,
        initialMonth = initialMonth,
        showOutsideDays = showOutsideDays,
        disabled = disabled,
        required = true,
    )
}

/**
 * 月历的可组合选择接口，支持区间、多选、周数与月份或年份下拉。
 *
 * @param selection 当前受控选择。
 * @param onSelectionChange 选择变化回调。
 * @param min 最少选择天数。区间模式按两个端点之间的天数计算。
 * @param max 最多选择天数。0 表示不限制。
 * @param months 可浏览的月份范围。启用下拉且未传入范围时，默认最近 100 年到本年年底。
 */
@Composable
fun MuiCalendar(
    selection: MuiCalendarSelection,
    onSelectionChange: (MuiCalendarSelection) -> Unit,
    modifier: Modifier = Modifier,
    initialMonth: YearMonth = YearMonth.now(),
    showOutsideDays: Boolean = true,
    disabled: ((LocalDate) -> Boolean)? = null,
    caption: MuiCalendarCaption = MuiCalendarCaption.Label,
    showWeekNumber: Boolean = false,
    fixedWeeks: Boolean = false,
    required: Boolean = false,
    min: Int = 0,
    max: Int = 0,
    months: ClosedRange<YearMonth>? = null,
) {
    val palette = LocalMuiPalette.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val locale = Locale.getDefault()
    val weekFields = WeekFields.of(locale)
    val today = LocalDate.now()
    val defaultMonths = YearMonth.of(today.year - 100, 1)..YearMonth.of(today.year, 12)
    val bounds = months?.takeUnless { it.isEmpty() } ?: defaultMonths.takeIf { caption != MuiCalendarCaption.Label }
    var displayed by remember(initialMonth) { mutableStateOf(initialMonth) }
    val displayedMonth = bounds?.let { displayed.coerceIn(it.start, it.endInclusive) } ?: displayed
    val first = displayedMonth.atDay(1)
    val leadingDays = (first.dayOfWeek.value + 7 - weekFields.firstDayOfWeek.value) % 7
    val gridStart = first.minusDays(leadingDays.toLong())
    val rowCount = if (fixedWeeks) 6 else (leadingDays + displayedMonth.lengthOfMonth() + 6) / 7
    val weekdayLabels = (0 until 7).map { offset ->
        gridStart.plusDays(offset.toLong()).dayOfWeek.getDisplayName(JavaTextStyle.SHORT, locale)
    }

    Column(modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.width(32.dp * if (showWeekNumber) 8 else 7),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MuiCalendarNavButton(CalendarNavDirection.Previous, rtl, bounds == null || displayedMonth > bounds.start) {
                displayed = displayedMonth.minusMonths(1)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                when {
                    caption == MuiCalendarCaption.Label || bounds == null -> {
                        val month = displayedMonth.month.getDisplayName(JavaTextStyle.FULL, locale)
                        MuiText("$month ${displayedMonth.year}", fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    }
                    else -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (caption == MuiCalendarCaption.Dropdown || caption == MuiCalendarCaption.DropdownMonths) {
                                val options = remember(bounds, displayedMonth.year, locale) {
                                    (1..12).map { YearMonth.of(displayedMonth.year, it) }
                                        .filter { it in bounds }
                                        .map { SelectOption(it.monthValue.toString(), it.month.getDisplayName(JavaTextStyle.SHORT, locale)) }
                                }
                                MuiSelect(
                                    options = options,
                                    value = displayedMonth.monthValue.toString(),
                                    onValueChange = { displayed = displayedMonth.withMonth(it.toInt()) },
                                    triggerSize = SelectTriggerSize.Sm,
                                )
                            } else {
                                MuiText(displayedMonth.month.getDisplayName(JavaTextStyle.SHORT, locale))
                            }
                            if (caption == MuiCalendarCaption.Dropdown || caption == MuiCalendarCaption.DropdownYears) {
                                MuiSelect(
                                    options = remember(bounds) { (bounds.start.year..bounds.endInclusive.year).map { SelectOption(it.toString(), it.toString()) } },
                                    value = displayedMonth.year.toString(),
                                    onValueChange = { displayed = displayedMonth.withYear(it.toInt()) },
                                    triggerSize = SelectTriggerSize.Sm,
                                )
                            } else {
                                MuiText(displayedMonth.year.toString())
                            }
                        }
                    }
                }
            }
            MuiCalendarNavButton(CalendarNavDirection.Next, rtl, bounds == null || displayedMonth < bounds.endInclusive) {
                displayed = displayedMonth.plusMonths(1)
            }
        }
        Row {
            val headers = if (showWeekNumber) listOf("") + weekdayLabels else weekdayLabels
            headers.forEach { label ->
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                    MuiText(label, fontSize = 13.sp, color = palette.mutedForeground, maxLines = 1)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(rowCount) { week ->
                Row {
                    if (showWeekNumber) {
                        val weekNumber = gridStart.plusWeeks(week.toLong()).get(weekFields.weekOfWeekBasedYear())
                        Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                            MuiText(weekNumber.toString(), fontSize = 13.sp, color = palette.mutedForeground)
                        }
                    }
                    repeat(7) { weekday ->
                        val date = gridStart.plusDays((week * 7 + weekday).toLong())
                        val outside = YearMonth.from(date) != displayedMonth
                        val hidden = outside && !showOutsideDays
                        if (hidden) {
                            Box(Modifier.size(32.dp))
                        } else {
                            MuiCalendarDayCell(
                                dayText = date.dayOfMonth.toString(),
                                selected = selection.contains(date),
                                rangeMiddle = selection is MuiCalendarSelection.Range &&
                                    selection.start != null && selection.end != null &&
                                    date > selection.start && date < selection.end,
                                today = date == today,
                                outside = outside,
                                disabled = disabled?.invoke(date) == true,
                            ) {
                                val next = selection.select(date, required, min, max)
                                if (next != selection || required && selection is MuiCalendarSelection.Single) {
                                    onSelectionChange(next)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun MuiCalendarSelection.contains(date: LocalDate) = when (this) {
    is MuiCalendarSelection.Single -> this.date == date
    is MuiCalendarSelection.Multiple -> date in dates
    is MuiCalendarSelection.Range -> if (start != null && end != null) date in start..end else date == start
}

/** 按 DayPicker 的数量和跨度约束推进单选、多选或区间状态。 */
private fun MuiCalendarSelection.select(date: LocalDate, required: Boolean, min: Int, max: Int): MuiCalendarSelection =
    when (this) {
        is MuiCalendarSelection.Single -> copy(date = date.takeUnless { !required && it == this.date })
        is MuiCalendarSelection.Multiple -> when {
            date in dates -> if (dates.size <= maxOf(min, if (required) 1 else 0)) this else copy(dates = dates - date)
            max > 0 && dates.size >= max -> this
            else -> copy(dates = dates + date)
        }
        is MuiCalendarSelection.Range -> {
            val next = when {
                start == null -> MuiCalendarSelection.Range(date, if (min > 0) null else date)
                end == null && date == start -> if (min == 0) copy(end = date) else if (required) this else MuiCalendarSelection.Range()
                end == null -> MuiCalendarSelection.Range(minOf(start, date), maxOf(start, date))
                start == date && end == date -> if (required) this else MuiCalendarSelection.Range()
                date == start || date == end -> MuiCalendarSelection.Range(date, if (min > 0) null else date)
                date < start -> copy(start = date)
                else -> copy(end = date)
            }
            val days = if (next.start == null || next.end == null) null else ChronoUnit.DAYS.between(next.start, next.end)
            if (days != null && (max > 0 && days > max || min > 1 && days < min)) MuiCalendarSelection.Range(date) else next
        }
    }

/**
 * 单日格使用 32dp 方格，选中端点用 primary，区间中段和今天用 accent。
 *
 * @param dayText 显示的日数字
 * @param selected 是否选中
 * @param today 是否今天
 * @param outside 是否月外日期
 * @param disabled 是否禁用
 * @param onClick 点击回调
 */
@Composable
private fun MuiCalendarDayCell(
    dayText: String,
    selected: Boolean,
    rangeMiddle: Boolean,
    today: Boolean,
    outside: Boolean,
    disabled: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalMuiPalette.current
    val interaction = rememberMuiInteractionSource()
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(if (rangeMiddle) 0.dp else MuiRadius.md)
    val background = when {
        rangeMiddle || hovered && !selected -> palette.accent
        selected -> palette.primary
        today -> palette.accent
        else -> Color.Transparent
    }
    val textColor = when {
        rangeMiddle -> palette.accentForeground
        selected -> palette.primaryForeground
        disabled || outside -> palette.mutedForeground
        today -> palette.accentForeground
        else -> palette.foreground
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .alpha(if (disabled) 0.5f else 1f)
            .muiFocusRing(interaction, shape)
            .clip(shape)
            .background(background, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = !disabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        MuiText(text = dayText, fontSize = 14.sp, lineHeight = 16.sp, color = textColor, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
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
    rtl: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalMuiPalette.current
    val interactionSource = rememberMuiInteractionSource()
    val hovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .size(32.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .muiFocusRing(interactionSource, RoundedCornerShape(MuiRadius.md))
            .background(if (hovered) palette.accent else Color.Transparent, RoundedCornerShape(MuiRadius.md))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
            val actual = if (!rtl) direction else if (direction == CalendarNavDirection.Previous) CalendarNavDirection.Next else CalendarNavDirection.Previous
            drawChevron(actual, palette.foreground)
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
