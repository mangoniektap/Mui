package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius
import com.mui.theme.muiElevation
import com.mui.theme.rememberMuiInteractionSource

// TODO: web 的相邻菜单悬停切换（打开状态下 hover 其它触发器直接换菜单）未移植

/** Menubar 内部共享态：同一时刻只允许一个顶级菜单展开 */
class MuiMenubarState {
    var openMenuLabel by mutableStateOf<String?>(null)
}

/** Menubar 共享态的 CompositionLocal（MuiMenubar 内部提供，值为 null = 菜单独立管理展开态） */
val LocalMuiMenubarState = staticCompositionLocalOf<MuiMenubarState?> { null }

/**
 * 菜单栏：h-9 rounded-md border bg-background p-1 shadow-xs 的横向一排顶级菜单。
 * 输入 = 若干 MuiMenubarMenu；输出 = 菜单项点击。
 * 硬约束：同一时刻最多展开一个菜单；菜单表面走 MuiMenuSurface 配方。
 *
 * @param modifier 布局修饰
 * @param content 菜单栏内容（Row 作用域：排布 MuiMenubarMenu）
 */
@Composable
fun MuiMenubar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    val shape = RoundedCornerShape(MuiRadius.md)
    CompositionLocalProvider(LocalMuiMenubarState provides remember { MuiMenubarState() }) {
        Row(
            modifier = modifier
                .height(36.dp)
                .background(palette.background, shape)
                .border(1.dp, palette.border, shape)
                .muiElevation("xs", shape)
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            content = content,
        )
    }
}

/**
 * 顶级菜单：px-2 py-1 text-sm font-medium rounded-sm 的触发器 + 点击展开的 Popup 菜单。
 *
 * @param label 菜单标题（也用作互斥展开的键）
 * @param modifier 布局修饰
 * @param enabled 可用性（false = 半透明且不可点）
 * @param content 菜单内容（Column 作用域：组合 MuiDropdownMenu 系条目）
 */
@Composable
fun RowScope.MuiMenubarMenu(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    val menubarState = LocalMuiMenubarState.current
    var fallbackOpen by remember { mutableStateOf(false) }
    val open = menubarState?.openMenuLabel == label || fallbackOpen
    var triggerHeight by remember { mutableIntStateOf(0) }
    val interactionSource = rememberMuiInteractionSource()
    val hovered by interactionSource.collectIsHoveredAsState()

    fun setOpen(next: Boolean) {
        if (menubarState != null) {
            menubarState.openMenuLabel = if (next) label else null
        } else {
            fallbackOpen = next
        }
    }

    val shape = RoundedCornerShape(MuiRadius.sm)
    val background = when {
        open -> palette.accent
        hovered -> palette.accent
        else -> androidx.compose.ui.graphics.Color.Transparent
    }
    val contentColor = if (open || hovered) palette.accentForeground else androidx.compose.ui.graphics.Color.Unspecified

    Box(modifier.onSizeChanged { triggerHeight = it.height }) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(background, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                ) { setOpen(!open) }
                .alpha(if (enabled) 1f else 0.5f)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            CompositionLocalProvider(LocalMuiContentColor provides contentColor) {
                MuiText(text = label, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
            }
        }
        if (open) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, triggerHeight + with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.roundToPx() }),
                onDismissRequest = { setOpen(false) },
                properties = PopupProperties(focusable = true),
            ) {
                MuiMenuSurface(content = content)
            }
        }
    }
}
