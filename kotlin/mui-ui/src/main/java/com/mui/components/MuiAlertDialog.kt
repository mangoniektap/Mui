package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
private val overlayScrim = Color(0x80000000)

/**
 * 确认对话框：模态 Dialog + 黑 50% 遮罩 + 居中面板（bg-background、border、lg 阴影、rounded-lg p-6），
 * 标题/描述 + 底部「取消(Outline) / 确认(Default)」右对齐按钮排，点按钮后关闭并回调。
 * 输入：受控/非受控 open、标题/描述、按钮文案与回调；输出：模态浮层。
 * 硬约束：点外部与返回键不关闭（对齐 radix alert-dialog，只能经按钮关闭）；视觉值全走令牌。
 *
 * TODO: web 的移动端 footer 纵向倒序（flex-col-reverse）与 size=sm 双列排布未移植（取 sm 横向右对齐）。
 *
 * @param open 受控开关（null = 非受控，走 defaultOpen）
 * @param defaultOpen 初始开关状态（非受控时用）
 * @param onOpenChange 开关变化回调（取消/确认都会以 false 触发）
 * @param title 标题文本（18sp SemiBold，null = 不显示）
 * @param description 描述文本（14sp muted-foreground，null = 不显示）
 * @param cancelText 取消按钮文案（null = 不显示取消钮）
 * @param confirmText 确认按钮文案（null = 不显示确认钮）
 * @param onCancel 取消按钮点击回调（先于关闭）
 * @param onConfirm 确认按钮点击回调（先于关闭）
 * @param modifier 布局修饰（应用于面板）
 * @param content 标题与 footer 之间的自定义内容槽
 */
@Composable
fun MuiAlertDialog(
    open: Boolean? = null,
    defaultOpen: Boolean = false,
    onOpenChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    title: String? = null,
    description: String? = null,
    cancelText: String? = "取消",
    confirmText: String? = "确认",
    onCancel: (() -> Unit)? = null,
    onConfirm: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
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
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = false,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(overlayScrim),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .widthIn(max = 512.dp)
                    .background(palette.background, shape)
                    .border(1.dp, palette.border, shape)
                    .muiElevation("lg", shape)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (title != null) {
                        MuiText(
                            text = title,
                            fontSize = 18.sp,
                            lineHeight = 28.sp,
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
                if (cancelText != null || confirmText != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        if (cancelText != null) {
                            MuiButton(
                                onClick = {
                                    onCancel?.invoke()
                                    setOpen(false)
                                },
                                variant = ButtonVariant.Outline,
                            ) {
                                MuiText(cancelText, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        if (confirmText != null) {
                            MuiButton(
                                onClick = {
                                    onConfirm?.invoke()
                                    setOpen(false)
                                },
                                variant = ButtonVariant.Default,
                            ) {
                                MuiText(confirmText, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
