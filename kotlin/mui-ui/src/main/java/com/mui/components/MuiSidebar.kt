package com.mui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.rememberMuiInteractionSource
import com.mui.token.MuiRadius

// TODO: web 的移动端 Sheet 抽屉形态、cookie 持久化、Cmd/Ctrl+B 快捷键未移植。
// TODO: web 的 SidebarMenuBadge/MenuSkeleton/MenuSub/Tooltip 提示未移植。
// TODO: web 的 variant（floating/inset）未移植，只做经典 sidebar。

/** 侧栏展开宽度（16rem） */
private val SidebarWidthExpanded = 256.dp

/** 侧栏收起宽度（图标位） */
private val SidebarWidthCollapsed = 64.dp

/** 侧栏贴边（sidebar.tsx 的 side） */
enum class SidebarSide { Left, Right }

/** 菜单钮尺寸档位（sidebar.tsx 的 menu button size） */
enum class SidebarMenuButtonSize { Default, Sm, Lg }

/** 侧栏状态：展开/收起 + 切换 */
class MuiSidebarState(open: Boolean = true) {
    var open by mutableStateOf(open)

    /** 切换展开态 */
    fun toggle() {
        open = !open
    }
}

/** 创建并记住侧栏状态 */
@Composable
fun rememberMuiSidebarState(open: Boolean = true): MuiSidebarState =
    remember { MuiSidebarState(open) }

/** 当前侧栏状态（Provider 外为 null） */
val LocalMuiSidebarState = staticCompositionLocalOf<MuiSidebarState?> { null }

/** 读取侧栏状态（未在 Provider 内时抛错，对齐 web 的 useSidebar） */
@Composable
fun rememberSidebarRequired(): MuiSidebarState =
    LocalMuiSidebarState.current
        ?: throw IllegalStateException("MuiSidebar components must be used within MuiSidebarProvider.")

/**
 * 侧栏提供者：组合局部注入侧栏状态。
 * 输入状态 + 内容槽；输出状态生效的横排骨架（侧栏 + 主区由调用方排）。
 *
 * @param state 侧栏状态
 * @param modifier 布局修饰
 * @param content 主内容（Row 作用域：MuiSidebar + 主区）
 */
@Composable
fun MuiSidebarProvider(
    state: MuiSidebarState,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    CompositionLocalProvider(LocalMuiSidebarState provides state) {
        Row(modifier = modifier.fillMaxSize(), content = content)
    }
}

/**
 * 侧栏本体：宽度 256dp↔64dp 动画，bg-sidebar，贴边侧 1dp 描边。
 * 输入侧别；输出可折叠侧栏列。
 * 硬约束：宽度动画 200ms linear（web 的 duration-200 ease-linear）。
 *
 * @param modifier 布局修饰
 * @param side 贴边（默认 Left）
 * @param content 侧栏内容（建议 SidebarHeader / SidebarContent / SidebarFooter）
 */
@Composable
fun MuiSidebar(
    modifier: Modifier = Modifier,
    side: SidebarSide = SidebarSide.Left,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = rememberSidebarRequired()
    val palette = LocalMuiPalette.current
    val width by animateDpAsState(
        targetValue = if (state.open) SidebarWidthExpanded else SidebarWidthCollapsed,
        animationSpec = tween(durationMillis = 200),
    )
    Column(
        modifier = modifier
            .width(width)
            .fillMaxSize()
            .background(palette.sidebar)
            .drawEdgeBorder(side, palette.sidebarBorder),
        content = content,
    )
}

/** 贴边侧 1dp 描边（left 侧画右缘 / right 侧画左缘） */
private fun Modifier.drawEdgeBorder(side: SidebarSide, color: Color): Modifier = drawBehind {
    val w = 1.dp.toPx()
    when (side) {
        SidebarSide.Left -> drawRect(
            color,
            topLeft = Offset(size.width - w, 0f),
            size = androidx.compose.ui.geometry.Size(w, size.height),
        )
        SidebarSide.Right -> drawRect(
            color,
            topLeft = Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(w, size.height),
        )
    }
}

/**
 * 侧栏触发钮：ghost 图标钮，点击切换展开态（Canvas 画 panel-left 图标）。
 *
 * @param modifier 布局修饰
 * @param onClick 附加点击回调（先于切换执行）
 */
@Composable
fun MuiSidebarTrigger(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val state = rememberSidebarRequired()
    val palette = LocalMuiPalette.current
    val interactionSource = rememberMuiInteractionSource()
    val hovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(RoundedCornerShape(MuiRadius.md))
            .background(
                if (hovered) palette.sidebarAccent else Color.Transparent,
                RoundedCornerShape(MuiRadius.md),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) {
                onClick?.invoke()
                state.toggle()
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            val stroke = 1.5.dp.toPx()
            val color = palette.sidebarForeground
            drawRoundRect(
                color = color,
                style = Stroke(width = stroke),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            )
            val x = size.width / 3f
            drawLine(
                color = color,
                start = Offset(x, 0f),
                end = Offset(x, size.height),
                strokeWidth = stroke,
            )
        }
    }
}

/**
 * 侧栏头区：p-2 纵向列。
 *
 * @param modifier 布局修饰
 * @param content 头区内容
 */
@Composable
fun MuiSidebarHeader(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/**
 * 侧栏尾区：p-2 纵向列。
 *
 * @param modifier 布局修饰
 * @param content 尾区内容
 */
@Composable
fun MuiSidebarFooter(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/**
 * 侧栏分隔线：mx-2 的 sidebarBorder 横线。
 *
 * @param modifier 布局修饰
 */
@Composable
fun MuiSidebarSeparator(
    modifier: Modifier = Modifier,
) {
    val palette = LocalMuiPalette.current
    Row(modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.sidebarBorder),
        )
    }
}

/**
 * 侧栏内容区：占满剩余高的纵向滚动列（gap-2）。
 *
 * @param modifier 布局修饰
 * @param content 内容（若干 MuiSidebarGroup）
 */
@Composable
fun MuiSidebarContent(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/**
 * 侧栏分组：p-2 纵向列。
 *
 * @param modifier 布局修饰
 * @param content 组内容（GroupLabel + Menu）
 */
@Composable
fun MuiSidebarGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

/**
 * 分组标签：h-8 px-2 text-xs font-medium 半透明。
 *
 * @param modifier 布局修饰
 * @param text 标签文本
 */
@Composable
fun MuiSidebarGroupLabel(
    modifier: Modifier = Modifier,
    text: String,
) {
    val palette = LocalMuiPalette.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 32.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        MuiText(
            text = text,
            color = palette.sidebarForeground.copy(alpha = palette.sidebarForeground.alpha * 0.70f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * 侧栏菜单：gap-1 纵向列表。
 *
 * @param modifier 布局修饰
 * @param content 菜单内容（若干 MuiSidebarMenuItem）
 */
@Composable
fun MuiSidebarMenu(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        content = content,
    )
}

/**
 * 侧栏菜单条目容器：占满一行的列表项。
 *
 * @param modifier 布局修饰
 * @param content 条目内容（建议 MuiSidebarMenuButton）
 */
@Composable
fun MuiSidebarMenuItem(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        content()
    }
}

/**
 * 菜单钮：h-8 rounded-md p-2，hover/激活 bg-sidebar-accent（激活态加粗）。
 * 输入点击回调 + 激活态 + 尺寸；输出整宽菜单钮。
 * 硬约束：激活态 bg-accent + accentForeground + font-medium（收起态图标模式由调用方裁内容）。
 *
 * @param onClick 点击回调
 * @param modifier 布局修饰
 * @param active 激活态（bg-sidebar-accent 常亮）
 * @param size 尺寸档位（Default = h-8 / Sm = h-7 / Lg = h-12）
 * @param interactionSource 交互源
 * @param content 钮内容（Row 作用域：Canvas 图标 + MuiText）
 */
@Composable
fun MuiSidebarMenuButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    size: SidebarMenuButtonSize = SidebarMenuButtonSize.Default,
    interactionSource: MutableInteractionSource = rememberMuiInteractionSource(),
    content: @Composable RowScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    val hovered by interactionSource.collectIsHoveredAsState()
    val highlight = active || hovered
    val background = if (highlight) palette.sidebarAccent else Color.Transparent
    val foreground = if (highlight) palette.sidebarAccentForeground else palette.sidebarForeground
    val height = when (size) {
        SidebarMenuButtonSize.Default -> 32.dp
        SidebarMenuButtonSize.Sm -> 28.dp
        SidebarMenuButtonSize.Lg -> 48.dp
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = height)
            .clip(RoundedCornerShape(MuiRadius.md))
            .background(background, RoundedCornerShape(MuiRadius.md))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CompositionLocalProvider(LocalMuiContentColor provides foreground) {
            content()
        }
    }
}
