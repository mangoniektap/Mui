package com.mui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mui.theme.rememberMuiInteractionSource
import kotlin.math.roundToInt

// TODO: web 的 Esc 关闭与方向键导航未移植（键盘交互可选）

/**
 * 上下文菜单：触发器槽长按（combinedClickable.onLongClick）或右键打开 Popup 菜单。
 * 菜单条目复用 MuiDropdownMenu 系（MuiMenuSurface / MuiDropdownMenuItem / …）。
 * 输入 = 触发器槽 + 菜单条目 lambda；输出 = 展开态回调与条目点击。
 * 硬约束：无 ripple；Popup focusable 点外关闭；菜单表面走 MuiMenuSurface 配方。
 *
 * @param trigger 触发器槽（任意内容；长按 / 右键触发打开）
 * @param modifier 触发器容器的布局修饰
 * @param open 受控展开态（null = 非受控，内部自持）
 * @param onOpenChange 展开态变化回调（受控时必传；点外部关闭也走这里）
 * @param content 菜单内容（Column 作用域：组合菜单条目；关闭菜单用 onOpenChange(false)）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MuiContextMenu(
    trigger: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    open: Boolean? = null,
    onOpenChange: ((Boolean) -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    var inner by remember { mutableStateOf(false) }
    val isOpen = open ?: inner

    fun setOpen(next: Boolean) {
        if (open == null) inner = next
        onOpenChange?.invoke(next)
    }

    var pressOffset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(0 to 0) }
    val interactionSource = rememberMuiInteractionSource()

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it.width to it.height }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { /* 右键/长按专属表面：普通点击不响应 */ },
                onLongClick = { setOpen(true) },
            )
            .pointerInput(Unit) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.pressed }
                        if (change != null && event.buttons.isSecondaryPressed) {
                            pressOffset = change.position
                            setOpen(true)
                            change.consume()
                            break
                        }
                        if (event.changes.none { it.pressed }) break
                    }
                }
            },
    ) {
        trigger()
        if (isOpen) {
            val x = pressOffset.x.roundToInt().coerceIn(0, containerSize.first)
            val y = pressOffset.y.roundToInt().coerceIn(0, containerSize.second)
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(x, y),
                onDismissRequest = { setOpen(false) },
                properties = PopupProperties(focusable = true),
            ) {
                MuiMenuSurface(content = content)
            }
        }
    }
}
