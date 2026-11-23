package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiPalette
import com.mui.theme.muiElevation
import com.mui.token.MuiRadius

/**
 * 确认对话框：模态 Dialog + 黑 50% 遮罩 + 居中面板（bg-background、border、lg 阴影、rounded-lg p-6），
 * 标题/描述 + 底部「取消(Outline) / 确认(Default)」右对齐按钮排，点按钮后关闭并回调。
 * 输入：受控/非受控 open、标题/描述、按钮文案与回调；输出：模态浮层。
 * 硬约束：点击遮罩保持打开，返回键或按钮关闭；视觉值全走令牌。
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
    MuiAlertDialog(
        size = MuiAlertDialogSize.Default,
        open = open, defaultOpen = defaultOpen, onOpenChange = onOpenChange,
        modifier = modifier, title = title, description = description, cancelText = cancelText,
        confirmText = confirmText, onCancel = onCancel, onConfirm = onConfirm, content = content,
    )
}

enum class MuiAlertDialogSize { Default, Sm }

/** 默认尺寸随窗口宽度调整内容对齐，Sm 按 Web 使用窄面板和双列操作区。 */
@Composable
fun MuiAlertDialog(
    size: MuiAlertDialogSize,
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
    media: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit = {},
) {
    var inner by remember { mutableStateOf(defaultOpen) }
    val isOpen = open ?: inner

    val palette = LocalMuiPalette.current
    val shape = RoundedCornerShape(MuiRadius.lg)
    val small = size == MuiAlertDialogSize.Sm
    val narrow = LocalConfiguration.current.screenWidthDp < 640
    val centered = small || narrow

    fun setOpen(value: Boolean) {
        if (open == null) inner = value
        onOpenChange(value)
    }

    MuiCenteredDialog(isOpen, onDismiss = { setOpen(false) }, dismissOnBackdrop = false) {
        Column(
            modifier = modifier
                .padding(horizontal = 16.dp)
                .widthIn(max = if (small) 320.dp else 512.dp)
                .fillMaxWidth()
                .background(palette.background, shape)
                .border(1.dp, palette.border, shape)
                .muiElevation("lg", shape)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val header: @Composable () -> Unit = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
                ) {
                    if (centered && media != null) MuiAlertMedia(media)
                    val alignment = if (centered) TextAlign.Center else TextAlign.Start
                    if (title != null) {
                        MuiText(title, fontSize = 18.sp, lineHeight = 28.sp,
                            fontWeight = FontWeight.SemiBold, textAlign = alignment)
                    }
                    if (description != null) {
                        MuiText(description, fontSize = 14.sp, lineHeight = 20.sp,
                            color = palette.mutedForeground, textAlign = alignment)
                    }
                }
            }
            if (!centered && media != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    MuiAlertMedia(media)
                    Box(Modifier.weight(1f)) { header() }
                }
            } else {
                Box(Modifier.fillMaxWidth(), contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart) {
                    header()
                }
            }
            content()

            val cancelButton: @Composable (Modifier) -> Unit = { bounds ->
                if (cancelText != null) {
                    MuiButton(
                        onClick = {
                            onCancel?.invoke()
                            setOpen(false)
                        },
                        modifier = bounds,
                        variant = ButtonVariant.Outline,
                    ) { MuiText(cancelText) }
                }
            }
            val confirmButton: @Composable (Modifier) -> Unit = { bounds ->
                if (confirmText != null) {
                    MuiButton(
                        onClick = {
                            onConfirm?.invoke()
                            setOpen(false)
                        },
                        modifier = bounds,
                    ) { MuiText(confirmText) }
                }
            }
            if (cancelText != null || confirmText != null) {
                if (narrow && !small) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        confirmButton(Modifier.fillMaxWidth())
                        cancelButton(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                        cancelButton(if (small) Modifier.weight(1f) else Modifier)
                        confirmButton(if (small) Modifier.weight(1f) else Modifier)
                    }
                }
            }
        }
    }
}

@Composable
private fun MuiAlertMedia(content: @Composable () -> Unit) {
    Box(
        Modifier.size(64.dp).background(LocalMuiPalette.current.muted, RoundedCornerShape(MuiRadius.md)),
        contentAlignment = Alignment.Center,
    ) { content() }
}
