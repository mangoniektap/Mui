package com.mui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection

// TODO: web 的 DirectionProvider 只作用于 radix 组件树；这里同时覆盖 androidx 的 LocalLayoutDirection（影响所有布局）。

/** 布局方向（direction.tsx 的 dir） */
enum class MuiDirection { Ltr, Rtl }

/** 当前方向（默认 Ltr；useDirection 对应物） */
val LocalMuiDirection = staticCompositionLocalOf { MuiDirection.Ltr }

/**
 * 方向提供者：组合局部覆盖文本/布局方向，默认 Ltr。
 * 输入方向 + 内容槽；输出方向生效的组合子树。
 * 硬约束：同时覆盖 LocalMuiDirection 与 androidx LocalLayoutDirection。
 *
 * @param direction 方向（默认 Ltr）
 * @param content 方向作用域内容
 */
@Composable
fun MuiDirectionProvider(
    direction: MuiDirection = MuiDirection.Ltr,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalMuiDirection provides direction,
        LocalLayoutDirection provides when (direction) {
            MuiDirection.Ltr -> androidx.compose.ui.unit.LayoutDirection.Ltr
            MuiDirection.Rtl -> androidx.compose.ui.unit.LayoutDirection.Rtl
        },
        content = content,
    )
}

/** 当前方向快捷读取（useDirection 对应物） */
@Composable
fun muiDirection(): MuiDirection = LocalMuiDirection.current
