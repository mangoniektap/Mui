package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette
import com.mui.token.MuiRadius

/** 警告条外观档位（alert.tsx 的 variant） */
enum class AlertVariant { Default, Destructive }

/**
 * 警告条：rounded-lg 描边容器，图标槽 + 标题 + 描述两行结构。
 * 输入：外观档位、可选图标槽、标题与描述文本；输出：纯展示组件（无交互回调）。
 * 硬约束：颜色全走 LocalMuiPalette；destructive 档标题/图标转 destructive、描述 90% 透明。
 *
 * @param variant 外观档位（Default = 卡面色文案 / Destructive = 危险红文案）
 * @param icon 图标槽（null = 无图标列；有图标时左列 16dp、与文本列间距 12dp）
 * @param title 标题文本（null = 不显示；单行截断，font-medium）
 * @param description 描述文本（null = 不显示；Default 档为 muted-foreground）
 * @param modifier 布局修饰
 */
@Composable
fun MuiAlert(
    modifier: Modifier = Modifier,
    variant: AlertVariant = AlertVariant.Default,
    icon: (@Composable () -> Unit)? = null,
    title: String? = null,
    description: String? = null,
) {
    val palette = LocalMuiPalette.current
    val shape = RoundedCornerShape(MuiRadius.lg)
    val contentColor = if (variant == AlertVariant.Destructive) {
        palette.destructive
    } else {
        palette.cardForeground
    }
    val descriptionColor = if (variant == AlertVariant.Destructive) {
        palette.destructive.copy(alpha = palette.destructive.alpha * 0.90f)
    } else {
        palette.mutedForeground
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.card, shape)
            .border(1.dp, palette.border, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) {
            CompositionLocalProvider(LocalMuiContentColor provides contentColor) {
                Box(modifier = Modifier.size(16.dp)) { icon() }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (title != null) {
                CompositionLocalProvider(LocalMuiContentColor provides contentColor) {
                    MuiText(
                        text = title,
                        modifier = Modifier.defaultMinSize(minHeight = 16.dp),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (description != null) {
                CompositionLocalProvider(LocalMuiContentColor provides descriptionColor) {
                    MuiText(text = description, fontSize = 14.sp, lineHeight = 20.sp)
                }
            }
        }
    }
}
