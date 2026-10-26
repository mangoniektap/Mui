package com.mui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiPalette

/** 当前表单的提交回调（嵌套控件可读取；null = 不在 MuiForm 内） */
val LocalMuiFormSubmit = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * 表单容器（form.tsx 的 Form）：Column + 提交回调。
 * 输入 = 提交回调 + 内容槽；输出 = LocalMuiFormSubmit 提供给嵌套控件（如输入框 ImeAction.Done 触发）。
 * 硬约束：禁 material3；web 的 react-hook-form 状态管理未移植，字段值/校验由调用方自持。
 * TODO: web 的 FormProvider / useFormContext 上下文状态机未移植。
 *
 * @param onSubmit 提交回调（经 LocalMuiFormSubmit 广播给嵌套控件）
 * @param modifier 布局修饰
 * @param content 表单内容（建议若干 MuiFormField）
 */
@Composable
fun MuiForm(
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalMuiFormSubmit provides onSubmit) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}

/**
 * 表单字段行（FormField + FormItem + FormLabel + FormControl + FormMessage 的组合）：
 * 标签 + 控件槽 + 错误消息的纵向组，间距 grid gap-2 = 8dp；出错时标签转 destructive、控件转 invalid。
 * 输入 = 字段名 / 标签 / 错误文本；输出 = 控件槽由调用方渲染（控件自收 value/onValueChange）。
 *
 * @param name 字段名（与 web 的 Controller name 对应，仅作标识）
 * @param label 标签文本
 * @param modifier 布局修饰
 * @param error 错误文本（null = 无错，不渲染消息行）
 * @param content 控件槽（放 MuiInput / MuiSelect 等；invalid 时控件读 error != null）
 */
@Composable
fun MuiFormField(
    name: String,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CompositionLocalProvider(LocalMuiFormFieldName provides name) {
            MuiLabel(text = label)
            content()
            MuiFormMessage(message = error)
        }
    }
}

/** 当前 MuiFormField 的字段名（嵌套控件可读取作标识） */
val LocalMuiFormFieldName = staticCompositionLocalOf { "" }

/**
 * 表单消息（FormMessage）：text-sm text-destructive；空消息不占位。
 *
 * @param message 消息文本（null/空 = 不渲染）
 * @param modifier 布局修饰
 */
@Composable
fun MuiFormMessage(
    message: String?,
    modifier: Modifier = Modifier,
) {
    if (message.isNullOrEmpty()) return
    MuiText(
        text = message,
        modifier = modifier,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = LocalMuiPalette.current.destructive,
    )
}

/**
 * 表单描述（FormDescription）：text-sm text-muted-foreground。
 *
 * @param text 描述文本
 * @param modifier 布局修饰
 */
@Composable
fun MuiFormDescription(
    text: String,
    modifier: Modifier = Modifier,
) {
    MuiText(text = text, modifier = modifier, fontSize = 14.sp, lineHeight = 20.sp, color = LocalMuiPalette.current.mutedForeground)
}
