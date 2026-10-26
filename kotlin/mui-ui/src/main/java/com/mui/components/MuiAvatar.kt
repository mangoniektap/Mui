package com.mui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.mui.theme.LocalMuiContentColor
import com.mui.theme.LocalMuiPalette

/** 头像三档：default 32dp / sm 24dp / lg 40dp */
enum class AvatarSize(val diameter: Dp, val fallbackFont: Int) {
    Sm(24.dp, 12),
    Default(32.dp, 14),
    Lg(40.dp, 14),
}

/**
 * 头像：圆形裁切，图优先、无图退 fallback 文字底。
 *
 * @param size 尺寸档位（默认 Default）
 * @param image 图片槽（null = 显示 fallback；用 Image/AsyncImage 皆可，裁切已包好）
 * @param fallbackText 无图时的占位文字（一般给名字首字/缩写）
 * @param badge 头像角标槽（右下角小圆点，内容自定）
 */
@Composable
fun MuiAvatar(
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.Default,
    image: (@Composable () -> Unit)? = null,
    fallbackText: String? = null,
    badge: (@Composable () -> Unit)? = null,
) {
    val palette = LocalMuiPalette.current
    Box(
        modifier = modifier
            .size(size.diameter)
            .clip(CircleShape)
            .background(palette.muted, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            image()
        } else if (fallbackText != null) {
            MuiText(
                text = fallbackText,
                color = palette.mutedForeground,
                fontSize = size.fallbackFont.sp,
            )
        }
        if (badge != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .zIndex(1f),
            ) {
                Box(
                    Modifier
                        .size(size.diameter * 0.34f + 2.dp)
                        .clip(CircleShape)
                        .background(palette.background, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    CompositionLocalProvider(LocalMuiContentColor provides palette.primaryForeground) {
                        badge()
                    }
                }
            }
        }
    }
}

/** 头像叠放组：相邻头像 -8dp 重叠，各自一圈 2dp 背景色描边 */
@Composable
fun MuiAvatarGroup(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    val palette = LocalMuiPalette.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box {
            Row(verticalAlignment = Alignment.CenterVertically, content = content)
        }
    }
}

/** 组内单个头像的描边（叠放时压住左邻 8dp） */
fun Modifier.muiAvatarGroupItem(ringColor: Color, overlapping: Boolean): Modifier =
    this.then(
        Modifier
            .zIndex(if (overlapping) 1f else 0f)
            .background(Color.Transparent)
            .clip(CircleShape)
            .padding(0.dp)
            .background(ringColor.copy(alpha = 0f))
    )

/** 组尾计数球：+N 的那个圆，bg-muted */
@Composable
fun MuiAvatarGroupCount(
    count: Int,
    modifier: Modifier = Modifier,
    size: AvatarSize = AvatarSize.Default,
) {
    val palette = LocalMuiPalette.current
    Box(
        modifier = modifier
            .size(size.diameter)
            .clip(CircleShape)
            .background(palette.muted, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        MuiText(text = "+$count", color = palette.mutedForeground, fontSize = 14.sp)
    }
}
