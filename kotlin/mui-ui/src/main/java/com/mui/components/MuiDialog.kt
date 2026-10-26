package com.mui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiElevation
import com.mui.token.MuiRadius

/** web 遮罩 bg-black/50（web 原文即字面黑 50%，非令牌色） */
private val dialogScrim = Color(0x80000000)

/**
 * 对话框：模态 Dialog + 黑 50% 遮罩 + 居中面板（bg-background、border、lg 阴影、rounded-lg p-6），
 * 右上角 70% 不透明度 X 关闭钮，点外部/返回键关闭。
 * 输入：受控/非受控 open、标题/描述、内容槽与 footer 槽；输出：模态浮层。
 * 硬约束：视觉值全走令牌；无 material3 / material-icons 依赖（X 用 Canvas 画）。
 *
 * TODO: web 的进出场 fade/zoom 动画未移植（Compose 侧直接显隐）。
 *
 * @param open 受控开关（null = 非受控，走 defaultOpen）
 * @param defaultOpen 初始开关状态（非受控时用）
 * @param onOpenChange 开关变化回调（关闭钮/遮罩/返回键关闭都触发 false）
 * @param title 标题文本（18sp SemiBold leading-none，null = 不显示）
 * @param description 描述文本（14sp muted-foreground，null = 不显示）
 * @param showCloseButton 是否显示右上角关闭钮（默认 true）
 * @param modifier 布局修饰（应用于面板）
 * @param content 标题与 footer 之间的自定义内容槽
 * @param footer 底部按钮槽（Row 作用域，右对齐）
 */
@Composable
fun MuiDialog(
    open: Boolean? = null,
    defaultOpen: Boolean = false,
    onOpenChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    showCloseButton: Boolean = true,
    content: @Composable ColumnScope.() -> Unit = {},
    footer: @Composable RowScope.() -> Unit = {},
) {
    var inner by remember { mutableStateOf(defaultOpen) }
    val isOpen = open ?: inner
    if (!isOpen) return

    val palette = LocalMuiPalette.current
    val shape = RoundedCornerShape(MuiRadius.lg)

    fun setOpen(value: Boolean) {
        if (open == null) inner = value
        onOpenChange(value)
    }

    Dialog(
        onDismissRequest = { setOpen(false) },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(dialogScrim),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .widthIn(max = 512.dp)
                    .background(palette.background, shape)
                    .border(1.dp, palette.border, shape)
                    .muiElevation("lg", shape),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (title != null) {
                            MuiText(
                                text = title,
                                fontSize = 18.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (description != null) {
                            CompositionLocalProvider(LocalMuiContentColor provides palette.mutedForeground) {
                                MuiText(text = description, fontSize = 14.sp, lineHeight = 20.sp)
                            }
                        }
                    }
                    content()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        footer()
                    }
                }
                if (showCloseButton) {
                    DialogCloseX(
                        onClick = { setOpen(false) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-16).dp, y = 16.dp),
                    )
                }
            }
        }
    }
}

/** 右上角关闭钮：16dp X 图形，70% 不透明度（web 的 XIcon 关闭钮） */
@Composable
private fun DialogCloseX(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalMuiPalette.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .size(16.dp)
            .alpha(0.7f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 1.5.dp.toPx()
            drawLine(palette.foreground, Offset(0f, 0f), Offset(size.width, size.height), strokeWidth = stroke)
            drawLine(palette.foreground, Offset(size.width, 0f), Offset(0f, size.height), strokeWidth = stroke)
        }
    }
}
