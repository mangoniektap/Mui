package com.mui.components

import androidx.compose.foundation.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mui.token.MuiPalette
import com.mui.theme.LocalIsDark
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.muiElevation
import com.mui.theme.muiFocusRing
import com.mui.theme.rememberMuiInteractionSource

/** 外观档位（toggle.tsx 的 variant） */
enum class ToggleVariant { Default, Outline }

/** 尺寸档位（toggle.tsx 的 size） */
enum class ToggleSize { Default, Sm, Lg }

/**
 * 按压式开关钮：rounded-md text-sm font-medium；hover = bg-muted、on 态 = bg-accent；
 * outline 档 = border-input + xs 阴影；聚焦 3dp 环；disabled 半透明。
 * 输入 = pressed / defaultPressed / onPressedChange（受控非受控与 web 对齐）；
 * 输出 = onPressedChange。硬约束：禁 material3；视觉全走令牌。
 *
 * @param pressed 受控 on 态（null = 非受控，走 defaultPressed）
 * @param onPressedChange 状态变化回调（null = 只读展示）
 * @param modifier 布局修饰
 * @param defaultPressed 初始 on 态（非受控时用）
 * @param variant 外观档位（默认 Default）
 * @param size 尺寸档位（默认 Default）
 * @param disabled 禁用：半透明且不响应
 * @param invalid 校验失败态：聚焦环转 destructive
 * @param interactionSource 交互源（悬停/聚焦状态出口）
 * @param content 钮内内容（Row 作用域；文字用 MuiText，颜色自动取内容色）
 */
@Composable
fun MuiToggle(
    pressed: Boolean? = null,
    onPressedChange: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier,
    defaultPressed: Boolean = false,
    variant: ToggleVariant = ToggleVariant.Default,
    size: ToggleSize = ToggleSize.Default,
    disabled: Boolean = false,
    invalid: Boolean = false,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
    content: @Composable RowScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    val isDark = LocalIsDark.current
    val hovered by interactionSource.collectIsHoveredAsState()

    var inner by remember { mutableStateOf(defaultPressed) }
    val isOn = pressed ?: inner

    val surface = toggleSurface(variant, isOn, hovered, palette, isDark)
    val shape = RoundedCornerShape(MuiRadius.md)

    val sizeModifier = when (size) {
        ToggleSize.Default -> Modifier.defaultMinSize(minHeight = 36.dp, minWidth = 36.dp).padding(horizontal = 8.dp)
        ToggleSize.Sm -> Modifier.defaultMinSize(minHeight = 32.dp, minWidth = 32.dp).padding(horizontal = 6.dp)
        ToggleSize.Lg -> Modifier.defaultMinSize(minHeight = 40.dp, minWidth = 40.dp).padding(horizontal = 10.dp)
    }

    fun toggle() {
        val next = !isOn
        if (pressed == null) inner = next
        onPressedChange?.invoke(next)
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(surface.background, shape)
            .then(if (surface.showBorder) Modifier.border(1.dp, surface.borderColor, shape) else Modifier)
            .then(if (variant == ToggleVariant.Outline) Modifier.muiElevation("2xs", shape) else Modifier)
            .muiFocusRing(interactionSource, shape, invalid)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = !disabled && onPressedChange != null,
                onClick = { toggle() },
            )
            .then(sizeModifier)
            .then(if (disabled) Modifier.alpha(0.5f) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalMuiContentColor provides surface.foreground) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                content()
            }
        }
    }
}

/** 一档按压钮的解析外观 */
private data class ToggleSurface(
    val background: Color,
    val foreground: Color,
    val borderColor: Color,
    val showBorder: Boolean,
)

/** variant × on/hover → 颜色（on 优先，其次 hover） */
private fun toggleSurface(
    variant: ToggleVariant,
    isOn: Boolean,
    hovered: Boolean,
    palette: MuiPalette,
    isDark: Boolean,
): ToggleSurface {
    fun frac(c: Color, f: Float) = c.copy(alpha = c.alpha * f)
    return when {
        isOn -> ToggleSurface(
            background = if (isDark) frac(palette.accent, 1f) else palette.accent,
            foreground = palette.accentForeground,
            borderColor = if (variant == ToggleVariant.Outline) palette.input else Color.Transparent,
            showBorder = variant == ToggleVariant.Outline,
        )
        hovered -> when (variant) {
            // web：default 档 hover = bg-muted；outline 档 hover = bg-accent
            ToggleVariant.Default -> ToggleSurface(palette.muted, palette.mutedForeground, Color.Transparent, false)
            ToggleVariant.Outline -> ToggleSurface(palette.accent, palette.accentForeground, palette.input, true)
        }
        else -> when (variant) {
            ToggleVariant.Default -> ToggleSurface(Color.Transparent, palette.foreground, Color.Transparent, false)
            ToggleVariant.Outline -> ToggleSurface(
                background = if (isDark) frac(palette.input, 0.30f) else Color.Transparent,
                foreground = palette.foreground,
                borderColor = if (isDark) palette.input else palette.border,
                showBorder = true,
            )
        }
    }
}
