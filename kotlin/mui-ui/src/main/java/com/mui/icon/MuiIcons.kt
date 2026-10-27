package com.mui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * 图标集：lucide 1.61 的逐字移植（与 web 侧 `lucide-react` 同一套图形）。
 *
 * 输入输出：每个成员是 24×24 viewport、2dp 圆头描边的 [ImageVector]，
 * 由 `Icon(imageVector, tint = ...)` 换色，尺寸由调用方 modifier 给。
 *
 * 硬约束：
 * - 图形是**描边**不是填充：fill 为 null，stroke 取 SolidColor(Color.Black)，靠 ColorFilter 换色。
 *   所以不要给它套 fill 风格的用法，也不要在外面再叠一层描边。
 * - 名称与 lucide 的 kebab-case 一一对应（refresh-cw ↔ RefreshCw），web 与 kotlin 必须同名同形。
 * - 本文件由 tools/gen_mui_icons.mjs 生成，勿手改；要改图形请改脚本里的 ICONS 清单后重跑。
 *
 * Icon data from lucide (https://lucide.dev), ISC License.
 */

/** 单个图标外观：24 网格 + 2 描边 + 圆头圆角，全部图标共用，改这里就是改全套 */
private fun build(name: String, vararg paths: String): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    paths.forEach { d ->
        builder.addPath(
            pathData = addPathNodes(d),
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return builder.build()
}

object MuiIcons {

    /** lucide: arrow-down */
    val ArrowDown: ImageVector = build(
        "ArrowDown",
        "M 12 5 v 14",
        "m 19 12 -7 7 -7 -7",
    )

    /** lucide: arrow-left */
    val ArrowLeft: ImageVector = build(
        "ArrowLeft",
        "m 12 19 -7 -7 7 -7",
        "M 19 12 H 5",
    )

    /** lucide: arrow-right */
    val ArrowRight: ImageVector = build(
        "ArrowRight",
        "M 5 12 h 14",
        "m 12 5 7 7 -7 7",
    )

    /** lucide: bold */
    val Bold: ImageVector = build(
        "Bold",
        "M 6 12 h 9 a 4 4 0 0 1 0 8 H 7 a 1 1 0 0 1 -1 -1 V 5 a 1 1 0 0 1 1 -1 h 7 a 4 4 0 0 1 0 8",
    )

    /** lucide: bot */
    val Bot: ImageVector = build(
        "Bot",
        "M 12 8 V 4 H 8",
        "M 6 8 h 12 a 2 2 0 0 1 2 2 v 8 a 2 2 0 0 1 -2 2 h -12 a 2 2 0 0 1 -2 -2 v -8 a 2 2 0 0 1 2 -2 z",
        "M 2 14 h 2",
        "M 20 14 h 2",
        "M 15 13 v 2",
        "M 9 13 v 2",
    )

    /** lucide: calendar */
    val Calendar: ImageVector = build(
        "Calendar",
        "M 8 2 v 3",
        "M 16 2 v 3",
        "M 5 3 h 14 a 2 2 0 0 1 2 2 v 14 a 2 2 0 0 1 -2 2 h -14 a 2 2 0 0 1 -2 -2 v -14  " +
        "a 2 2 0 0 1 2 -2 z",
        "M 3 9 h 18",
    )

    /** lucide: check */
    val Check: ImageVector = build(
        "Check",
        "M 20 6 9 17 l -5 -5",
    )

    /** lucide: chevron-down */
    val ChevronDown: ImageVector = build(
        "ChevronDown",
        "m 6 9 6 6 6 -6",
    )

    /** lucide: chevron-right */
    val ChevronRight: ImageVector = build(
        "ChevronRight",
        "m 9 18 6 -6 -6 -6",
    )

    /** lucide: chevron-up */
    val ChevronUp: ImageVector = build(
        "ChevronUp",
        "m 18 15 -6 -6 -6 6",
    )

    /** lucide: circle */
    val Circle: ImageVector = build(
        "Circle",
        "M 2 12 a 10 10 0 1 0 20 0 a 10 10 0 1 0 -20 0",
    )

    /** lucide: circle-check */
    val CircleCheck: ImageVector = build(
        "CircleCheck",
        "M 2 12 a 10 10 0 1 0 20 0 a 10 10 0 1 0 -20 0",
        "m 16 9 -5.5 5.5 L 8 12",
    )

    /** lucide: copy */
    val Copy: ImageVector = build(
        "Copy",
        "M 10 8 h 10 a 2 2 0 0 1 2 2 v 10 a 2 2 0 0 1 -2 2 h -10 a 2 2 0 0 1 -2 -2 v -10  " +
        "a 2 2 0 0 1 2 -2 z",
        "M 4 16 c -1.1 0 -2 -.9 -2 -2 V 4 c 0 -1.1 .9 -2 2 -2 h 10 c 1.1 0 2 .9 2 2",
    )

    /** lucide: ellipsis */
    val Ellipsis: ImageVector = build(
        "Ellipsis",
        "M 11 12 a 1 1 0 1 0 2 0 a 1 1 0 1 0 -2 0",
        "M 18 12 a 1 1 0 1 0 2 0 a 1 1 0 1 0 -2 0",
        "M 4 12 a 1 1 0 1 0 2 0 a 1 1 0 1 0 -2 0",
    )

    /** lucide: file-code */
    val FileCode: ImageVector = build(
        "FileCode",
        "M 6 22 a 2 2 0 0 1 -2 -2 V 4 a 2 2 0 0 1 2 -2 h 8  " +
        "a 2.4 2.4 0 0 1 1.704 .706 l 3.588 3.588 A 2.4 2.4 0 0 1 20 8 v 12 a 2 2 0 0 1 -2 2 z",
        "M 14 2 v 5 a 1 1 0 0 0 1 1 h 5",
        "M 10 12.5 8 15 l 2 2.5",
        "m 14 12.5 2 2.5 -2 2.5",
    )

    /** lucide: file-text */
    val FileText: ImageVector = build(
        "FileText",
        "M 6 22 a 2 2 0 0 1 -2 -2 V 4 a 2 2 0 0 1 2 -2 h 8  " +
        "a 2.4 2.4 0 0 1 1.704 .706 l 3.588 3.588 A 2.4 2.4 0 0 1 20 8 v 12 a 2 2 0 0 1 -2 2 z",
        "M 14 2 v 5 a 1 1 0 0 0 1 1 h 5",
        "M 10 9 H 8",
        "M 16 13 H 8",
        "M 16 17 H 8",
    )

    /** lucide: grip-vertical */
    val GripVertical: ImageVector = build(
        "GripVertical",
        "M 8 12 a 1 1 0 1 0 2 0 a 1 1 0 1 0 -2 0",
        "M 8 5 a 1 1 0 1 0 2 0 a 1 1 0 1 0 -2 0",
        "M 8 19 a 1 1 0 1 0 2 0 a 1 1 0 1 0 -2 0",
        "M 14 12 a 1 1 0 1 0 2 0 a 1 1 0 1 0 -2 0",
        "M 14 5 a 1 1 0 1 0 2 0 a 1 1 0 1 0 -2 0",
        "M 14 19 a 1 1 0 1 0 2 0 a 1 1 0 1 0 -2 0",
    )

    /** lucide: house */
    val House: ImageVector = build(
        "House",
        "M 15 21 v -8 a 1 1 0 0 0 -1 -1 h -4 a 1 1 0 0 0 -1 1 v 8",
        "M 3 10 a 2 2 0 0 1 .709 -1.528 l 7 -6 a 2 2 0 0 1 2.582 0 l 7 6 A 2 2 0 0 1 21 10 v 9  " +
        "a 2 2 0 0 1 -2 2 H 5 a 2 2 0 0 1 -2 -2 z",
    )

    /** lucide: image */
    val Image: ImageVector = build(
        "Image",
        "M 5 3 h 14 a 2 2 0 0 1 2 2 v 14 a 2 2 0 0 1 -2 2 h -14 a 2 2 0 0 1 -2 -2 v -14  " +
        "a 2 2 0 0 1 2 -2 z",
        "M 7 9 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0",
        "m 21 15 -3.086 -3.086 a 2 2 0 0 0 -2.828 0 L 6 21",
    )

    /** lucide: inbox */
    val Inbox: ImageVector = build(
        "Inbox",
        "M 22 12 L 16 12 L 14 15 L 10 15 L 8 12 L 2 12",
        "M 5.45 5.11 2 12 v 6 a 2 2 0 0 0 2 2 h 16 a 2 2 0 0 0 2 -2 v -6 l -3.45 -6.89  " +
        "A 2 2 0 0 0 16.76 4 H 7.24 a 2 2 0 0 0 -1.79 1.11 z",
    )

    /** lucide: info */
    val Info: ImageVector = build(
        "Info",
        "M 2 12 a 10 10 0 1 0 20 0 a 10 10 0 1 0 -20 0",
        "M 12 16 v -4",
        "M 12 8 h .01",
    )

    /** lucide: italic */
    val Italic: ImageVector = build(
        "Italic",
        "M 19 4 L 10 4",
        "M 14 20 L 5 20",
        "M 15 4 L 9 20",
    )

    /** lucide: layers */
    val Layers: ImageVector = build(
        "Layers",
        "M 12.83 2.18 a 2 2 0 0 0 -1.66 0 L 2.6 6.08 a 1 1 0 0 0 0 1.83 l 8.58 3.91  " +
        "a 2 2 0 0 0 1.66 0 l 8.58 -3.9 a 1 1 0 0 0 0 -1.83 z",
        "M 2 12 a 1 1 0 0 0 .58 .91 l 8.6 3.91 a 2 2 0 0 0 1.65 0 l 8.58 -3.9 A 1 1 0 0 0 22 12",
        "M 2 17 a 1 1 0 0 0 .58 .91 l 8.6 3.91 a 2 2 0 0 0 1.65 0 l 8.58 -3.9 A 1 1 0 0 0 22 17",
    )

    /** lucide: loader-circle */
    val LoaderCircle: ImageVector = build(
        "LoaderCircle",
        "M 21 12 a 9 9 0 1 1 -6.219 -8.56",
    )

    /** lucide: mail */
    val Mail: ImageVector = build(
        "Mail",
        "m 22 7 -8.991 5.727 a 2 2 0 0 1 -2.009 0 L 2 7",
        "M 4 4 h 16 a 2 2 0 0 1 2 2 v 12 a 2 2 0 0 1 -2 2 h -16 a 2 2 0 0 1 -2 -2 v -12  " +
        "a 2 2 0 0 1 2 -2 z",
    )

    /** lucide: minus */
    val Minus: ImageVector = build(
        "Minus",
        "M 5 12 h 14",
    )

    /** lucide: moon */
    val Moon: ImageVector = build(
        "Moon",
        "M 20.985 12.486 a 9 9 0 1 1 -9.473 -9.472 c .405 -.022 .617 .46 .402 .803  " +
        "a 6 6 0 0 0 8.268 8.268 c .344 -.215 .825 -.004 .803 .401",
    )

    /** lucide: palette */
    val Palette: ImageVector = build(
        "Palette",
        "M 12 22 a 1 1 0 0 1 0 -20 10 9 0 0 1 10 9 5 5 0 0 1 -5 5 h -2.25  " +
        "a 1.75 1.75 0 0 0 -1.4 2.8 l .3 .4 a 1.75 1.75 0 0 1 -1.4 2.8 z",
        "M 13 6.5 a 0.5 0.5 0 1 0 1 0 a 0.5 0.5 0 1 0 -1 0",
        "M 17 10.5 a 0.5 0.5 0 1 0 1 0 a 0.5 0.5 0 1 0 -1 0",
        "M 6 12.5 a 0.5 0.5 0 1 0 1 0 a 0.5 0.5 0 1 0 -1 0",
        "M 8 7.5 a 0.5 0.5 0 1 0 1 0 a 0.5 0.5 0 1 0 -1 0",
    )

    /** lucide: panel-left */
    val PanelLeft: ImageVector = build(
        "PanelLeft",
        "M 5 3 h 14 a 2 2 0 0 1 2 2 v 14 a 2 2 0 0 1 -2 2 h -14 a 2 2 0 0 1 -2 -2 v -14  " +
        "a 2 2 0 0 1 2 -2 z",
        "M 9 3 v 18",
    )

    /** lucide: plus */
    val Plus: ImageVector = build(
        "Plus",
        "M 5 12 h 14",
        "M 12 5 v 14",
    )

    /** lucide: search */
    val Search: ImageVector = build(
        "Search",
        "m 21 21 -4.34 -4.34",
        "M 3 11 a 8 8 0 1 0 16 0 a 8 8 0 1 0 -16 0",
    )

    /** lucide: settings */
    val Settings: ImageVector = build(
        "Settings",
        "M 9.671 4.136  " +
        "a 2.34 2.34 0 0 1 4.659 0 2.34 2.34 0 0 0 3.319 1.915 2.34 2.34 0 0 1 2.33 4.033 2.34 2.34 0 0 0 0 3.831 2.34 2.34 0 0 1 -2.33 4.033 2.34 2.34 0 0 0 -3.319 1.915 2.34 2.34 0 0 1 -4.659 0 2.34 2.34 0 0 0 -3.32 -1.915 2.34 2.34 0 0 1 -2.33 -4.033 2.34 2.34 0 0 0 0 -3.831  " +
        "A 2.34 2.34 0 0 1 6.35 6.051 a 2.34 2.34 0 0 0 3.319 -1.915",
        "M 9 12 a 3 3 0 1 0 6 0 a 3 3 0 1 0 -6 0",
    )

    /** lucide: sparkles */
    val Sparkles: ImageVector = build(
        "Sparkles",
        "M 11.017 2.814 a 1 1 0 0 1 1.966 0 l 1.051 5.558 a 2 2 0 0 0 1.594 1.594 l 5.558 1.051  " +
        "a 1 1 0 0 1 0 1.966 l -5.558 1.051 a 2 2 0 0 0 -1.594 1.594 l -1.051 5.558  " +
        "a 1 1 0 0 1 -1.966 0 l -1.051 -5.558 a 2 2 0 0 0 -1.594 -1.594 l -5.558 -1.051  " +
        "a 1 1 0 0 1 0 -1.966 l 5.558 -1.051 a 2 2 0 0 0 1.594 -1.594 z",
        "M 20 2 v 4",
        "M 22 4 h -4",
        "M 2 20 a 2 2 0 1 0 4 0 a 2 2 0 1 0 -4 0",
    )

    /** lucide: star */
    val Star: ImageVector = build(
        "Star",
        "M 11.525 2.295 a .53 .53 0 0 1 .95 0 l 2.31 4.679  " +
        "a 2.123 2.123 0 0 0 1.595 1.16 l 5.166 .756 a .53 .53 0 0 1 .294 .904 l -3.736 3.638  " +
        "a 2.123 2.123 0 0 0 -.611 1.878 l .882 5.14 a .53 .53 0 0 1 -.771 .56 l -4.618 -2.428  " +
        "a 2.122 2.122 0 0 0 -1.973 0 L 6.396 21.01 a .53 .53 0 0 1 -.77 -.56 l .881 -5.139  " +
        "a 2.122 2.122 0 0 0 -.611 -1.879 L 2.16 9.795 a .53 .53 0 0 1 .294 -.906 l 5.165 -.755  " +
        "a 2.122 2.122 0 0 0 1.597 -1.16 z",
    )

    /** lucide: sun */
    val Sun: ImageVector = build(
        "Sun",
        "M 8 12 a 4 4 0 1 0 8 0 a 4 4 0 1 0 -8 0",
        "M 12 2 v 2",
        "M 12 20 v 2",
        "m 4.93 4.93 1.41 1.41",
        "m 17.66 17.66 1.41 1.41",
        "M 2 12 h 2",
        "M 20 12 h 2",
        "m 6.34 17.66 -1.41 1.41",
        "m 19.07 4.93 -1.41 1.41",
    )

    /** lucide: underline */
    val Underline: ImageVector = build(
        "Underline",
        "M 6 4 v 6 a 6 6 0 0 0 12 0 V 4",
        "M 4 20 L 20 20",
    )

    /** lucide: x */
    val X: ImageVector = build(
        "X",
        "M 18 6 6 18",
        "m 6 6 12 12",
    )

    /** lucide: audio-waveform */
    val AudioWaveform: ImageVector = build(
        "AudioWaveform",
        "M 2 13 a 2 2 0 0 0 2 -2 V 7 a 2 2 0 0 1 4 0 v 13 a 2 2 0 0 0 4 0 V 4 a 2 2 0 0 1 4 0  " +
        "v 13 a 2 2 0 0 0 4 0 v -4 a 2 2 0 0 1 2 -2",
    )

    /** lucide: book-open */
    val BookOpen: ImageVector = build(
        "BookOpen",
        "M 12 5 v 16",
        "M 20.001 19 A 2 2 0 0 0 22 17 V 5 a 2 2 0 0 0 -1.999 -2 L 16 3.002 A 5 5 0 0 0 12 5  " +
        "a 5 5 0 0 0 -4 -2 H 4 a 2 2 0 0 0 -2 2 v 12 a 2 2 0 0 0 1.999 2 H 8  " +
        "a 5 5 0 0 1 4 2 5 5 0 0 1 4 -2 z",
    )

    /** lucide: camera */
    val Camera: ImageVector = build(
        "Camera",
        "M 13.997 4 a 2 2 0 0 1 1.76 1.05 l .486 .9 A 2 2 0 0 0 18.003 7 H 20 a 2 2 0 0 1 2 2  " +
        "v 9 a 2 2 0 0 1 -2 2 H 4 a 2 2 0 0 1 -2 -2 V 9 a 2 2 0 0 1 2 -2 h 1.997  " +
        "a 2 2 0 0 0 1.759 -1.048 l .489 -.904 A 2 2 0 0 1 10.004 4 z",
        "M 9 13 a 3 3 0 1 0 6 0 a 3 3 0 1 0 -6 0",
    )

    /** lucide: chevron-left */
    val ChevronLeft: ImageVector = build(
        "ChevronLeft",
        "m 15 18 -6 -6 6 -6",
    )

    /** lucide: clock */
    val Clock: ImageVector = build(
        "Clock",
        "M 2 12 a 10 10 0 1 0 20 0 a 10 10 0 1 0 -20 0",
        "M 12 6 v 6 l 4 2",
    )

    /** lucide: coins */
    val Coins: ImageVector = build(
        "Coins",
        "M 13.744 17.736 a 6 6 0 1 1 -7.48 -7.48",
        "M 15 6 h 1 v 4",
        "m 6.134 14.768 .866 -.5 2 3.464",
        "M 10 8 a 6 6 0 1 0 12 0 a 6 6 0 1 0 -12 0",
    )

    /** lucide: crop */
    val Crop: ImageVector = build(
        "Crop",
        "M 6 2 v 14 a 2 2 0 0 0 2 2 h 14",
        "M 18 22 V 8 a 2 2 0 0 0 -2 -2 H 2",
    )

    /** lucide: droplet */
    val Droplet: ImageVector = build(
        "Droplet",
        "M 12 22 a 7 7 0 0 0 7 -7 c 0 -2 -1 -3.9 -3 -5.5 s -3.5 -4 -4 -6.5  " +
        "c -.5 2.5 -2 4.9 -4 6.5 C 6 11.1 5 13 5 15 a 7 7 0 0 0 7 7 z",
    )

    /** lucide: folder */
    val Folder: ImageVector = build(
        "Folder",
        "M 20 20 a 2 2 0 0 0 2 -2 V 8 a 2 2 0 0 0 -2 -2 h -7.9 a 2 2 0 0 1 -1.69 -.9 L 9.6 3.9  " +
        "A 2 2 0 0 0 7.93 3 H 4 a 2 2 0 0 0 -2 2 v 13 a 2 2 0 0 0 2 2 Z",
    )

    /** lucide: key */
    val Key: ImageVector = build(
        "Key",
        "m 2 21 9.6 -9.6",
        "m 7.5 15.5 2.3 2.3 a 1 1 0 0 1 0 1.4 l -2.1 2.1 a 1 1 0 0 1 -1.4 0 L 4 19",
        "M 10 7.5 a 5.5 5.5 0 1 0 11 0 a 5.5 5.5 0 1 0 -11 0",
    )

    /** lucide: layout-grid */
    val LayoutGrid: ImageVector = build(
        "LayoutGrid",
        "M 4 3 h 5 a 1 1 0 0 1 1 1 v 5 a 1 1 0 0 1 -1 1 h -5 a 1 1 0 0 1 -1 -1 v -5 a 1 1 0 0 1 1 -1 z",
        "M 15 3 h 5 a 1 1 0 0 1 1 1 v 5 a 1 1 0 0 1 -1 1 h -5 a 1 1 0 0 1 -1 -1 v -5 a 1 1 0 0 1 1 -1 z",
        "M 15 14 h 5 a 1 1 0 0 1 1 1 v 5 a 1 1 0 0 1 -1 1 h -5 a 1 1 0 0 1 -1 -1 v -5 a 1 1 0 0 1 1 -1 z",
        "M 4 14 h 5 a 1 1 0 0 1 1 1 v 5 a 1 1 0 0 1 -1 1 h -5 a 1 1 0 0 1 -1 -1 v -5 a 1 1 0 0 1 1 -1 z",
    )

    /** lucide: link */
    val Link: ImageVector = build(
        "Link",
        "M 10 13 a 5 5 0 0 0 7.54 .54 l 3 -3 a 5 5 0 0 0 -7.07 -7.07 l -1.72 1.71",
        "M 14 11 a 5 5 0 0 0 -7.54 -.54 l -3 3 a 5 5 0 0 0 7.07 7.07 l 1.71 -1.71",
    )

    /** lucide: list-video */
    val ListVideo: ImageVector = build(
        "ListVideo",
        "M 21 5 H 3",
        "M 10 12 H 3",
        "M 10 19 H 3",
        "M 15 12.003 a 1 1 0 0 1 1.517 -.859 l 4.997 2.997 a 1 1 0 0 1 0 1.718 l -4.997 2.997  " +
        "a 1 1 0 0 1 -1.517 -.86 z",
    )

    /** lucide: maximize */
    val Maximize: ImageVector = build(
        "Maximize",
        "M 8 3 H 5 a 2 2 0 0 0 -2 2 v 3",
        "M 21 8 V 5 a 2 2 0 0 0 -2 -2 h -3",
        "M 3 16 v 3 a 2 2 0 0 0 2 2 h 3",
        "M 16 21 h 3 a 2 2 0 0 0 2 -2 v -3",
    )

    /** lucide: minimize */
    val Minimize: ImageVector = build(
        "Minimize",
        "M 8 3 v 3 a 2 2 0 0 1 -2 2 H 3",
        "M 21 8 h -3 a 2 2 0 0 1 -2 -2 V 3",
        "M 3 16 h 3 a 2 2 0 0 1 2 2 v 3",
        "M 16 21 v -3 a 2 2 0 0 1 2 -2 h 3",
    )

    /** lucide: package-open */
    val PackageOpen: ImageVector = build(
        "PackageOpen",
        "M 12 22 v -9",
        "M 15.17 2.21 a 1.67 1.67 0 0 1 1.63 0 L 21 4.57 a 1.93 1.93 0 0 1 0 3.36 L 8.82 14.79  " +
        "a 1.655 1.655 0 0 1 -1.64 0 L 3 12.43 a 1.93 1.93 0 0 1 0 -3.36 z",
        "M 20 13 v 3.87 a 2.06 2.06 0 0 1 -1.11 1.83 l -6 3.08  " +
        "a 1.93 1.93 0 0 1 -1.78 0 l -6 -3.08 A 2.06 2.06 0 0 1 4 16.87 V 13",
        "M 21 12.43 a 1.93 1.93 0 0 0 0 -3.36 L 8.83 2.2 a 1.64 1.64 0 0 0 -1.63 0 L 3 4.57  " +
        "a 1.93 1.93 0 0 0 0 3.36 l 12.18 6.86 a 1.636 1.636 0 0 0 1.63 0 z",
    )

    /** lucide: pause */
    val Pause: ImageVector = build(
        "Pause",
        "M 15 3 h 3 a 1 1 0 0 1 1 1 v 16 a 1 1 0 0 1 -1 1 h -3 a 1 1 0 0 1 -1 -1 v -16 a 1 1 0 0 1 1 -1 z",
        "M 6 3 h 3 a 1 1 0 0 1 1 1 v 16 a 1 1 0 0 1 -1 1 h -3 a 1 1 0 0 1 -1 -1 v -16 a 1 1 0 0 1 1 -1 z",
    )

    /** lucide: pencil */
    val Pencil: ImageVector = build(
        "Pencil",
        "M 21.174 6.812 a 1 1 0 0 0 -3.986 -3.987 L 3.842 16.174  " +
        "a 2 2 0 0 0 -.5 .83 l -1.321 4.352 a .5 .5 0 0 0 .623 .622 l 4.353 -1.32  " +
        "a 2 2 0 0 0 .83 -.497 z",
        "m 15 5 4 4",
    )

    /** lucide: picture-in-picture */
    val PictureInPicture: ImageVector = build(
        "PictureInPicture",
        "M 2 10 h 6 V 4",
        "m 2 4 6 6",
        "M 21 10 V 7 a 2 2 0 0 0 -2 -2 h -7",
        "M 3 14 v 2 a 2 2 0 0 0 2 2 h 3",
        "M 13 14 h 8 a 1 1 0 0 1 1 1 v 5 a 1 1 0 0 1 -1 1 h -8 a 1 1 0 0 1 -1 -1 v -5 a 1 1 0 0 1 1 -1 z",
    )

    /** lucide: play */
    val Play: ImageVector = build(
        "Play",
        "M 5 5 a 2 2 0 0 1 3.008 -1.728 l 11.997 6.998 a 2 2 0 0 1 .003 3.458 l -12 7 A 2 2 0 0 1 5 19 z",
    )

    /** lucide: refresh-cw */
    val RefreshCw: ImageVector = build(
        "RefreshCw",
        "M 3 12 a 9 9 0 0 1 9 -9 9.75 9.75 0 0 1 6.74 2.74 L 21 8",
        "M 21 3 v 5 h -5",
        "M 21 12 a 9 9 0 0 1 -9 9 9.75 9.75 0 0 1 -6.74 -2.74 L 3 16",
        "M 8 16 H 3 v 5",
    )

    /** lucide: share-2 */
    val Share2: ImageVector = build(
        "Share2",
        "M 15 5 a 3 3 0 1 0 6 0 a 3 3 0 1 0 -6 0",
        "M 3 12 a 3 3 0 1 0 6 0 a 3 3 0 1 0 -6 0",
        "M 15 19 a 3 3 0 1 0 6 0 a 3 3 0 1 0 -6 0",
        "M 8.59 13.51 L 15.42 17.49",
        "M 15.41 6.51 L 8.59 10.49",
    )

    /** lucide: shield */
    val Shield: ImageVector = build(
        "Shield",
        "M 20 13 c 0 5 -3.5 7.5 -7.66 8.95 a 1 1 0 0 1 -.67 -.01 C 7.5 20.5 4 18 4 13 V 6  " +
        "a 1 1 0 0 1 1 -1 c 2 0 4.5 -1.2 6.24 -2.72 a 1.17 1.17 0 0 1 1.52 0  " +
        "C 14.51 3.81 17 5 19 5 a 1 1 0 0 1 1 1 z",
    )

    /** lucide: signal */
    val Signal: ImageVector = build(
        "Signal",
        "M 2 20 h .01",
        "M 7 20 v -4",
        "M 12 20 v -8",
        "M 17 20 V 8",
        "M 22 4 v 16",
    )

    /** lucide: square */
    val Square: ImageVector = build(
        "Square",
        "M 5 3 h 14 a 2 2 0 0 1 2 2 v 14 a 2 2 0 0 1 -2 2 h -14 a 2 2 0 0 1 -2 -2 v -14  " +
        "a 2 2 0 0 1 2 -2 z",
    )

    /** lucide: step-back */
    val StepBack: ImageVector = build(
        "StepBack",
        "M 13.971 4.285 A 2 2 0 0 1 17 6 v 12 a 2 2 0 0 1 -3.029 1.715 l -9.997 -5.998  " +
        "a 2 2 0 0 1 -.003 -3.432 z",
        "M 21 20 V 4",
    )

    /** lucide: step-forward */
    val StepForward: ImageVector = build(
        "StepForward",
        "M 10.029 4.285 A 2 2 0 0 0 7 6 v 12 a 2 2 0 0 0 3.029 1.715 l 9.997 -5.998  " +
        "a 2 2 0 0 0 .003 -3.432 z",
        "M 3 4 v 16",
    )

    /** lucide: tag */
    val Tag: ImageVector = build(
        "Tag",
        "M 12.586 2.586 A 2 2 0 0 0 11.172 2 H 4 a 2 2 0 0 0 -2 2 v 7.172  " +
        "a 2 2 0 0 0 .586 1.414 l 8.704 8.704 a 2.426 2.426 0 0 0 3.42 0 l 6.58 -6.58  " +
        "a 2.426 2.426 0 0 0 0 -3.42 z",
        "M 7 7.5 a 0.5 0.5 0 1 0 1 0 a 0.5 0.5 0 1 0 -1 0",
    )

    /** lucide: trash */
    val Trash: ImageVector = build(
        "Trash",
        "M 10 11 v 6",
        "M 14 11 v 6",
        "M 19 6 v 14 a 2 2 0 0 1 -2 2 H 7 a 2 2 0 0 1 -2 -2 V 6",
        "M 3 6 h 18",
        "M 8 6 V 4 a 2 2 0 0 1 2 -2 h 4 a 2 2 0 0 1 2 2 v 2",
    )

    /** lucide: trophy */
    val Trophy: ImageVector = build(
        "Trophy",
        "M 10 14.66 V 17 a 1 1 0 0 1 -1 1 2 2 0 0 0 -2 2 v 2",
        "M 14 14.66 V 17 a 1 1 0 0 0 1 1 2 2 0 0 1 2 2 v 2",
        "M 17.916 10 H 19.5 A 2.5 2.5 0 0 0 22 7.5 V 5 a 1 1 0 0 0 -1 -1 h -3",
        "M 4 22 h 16",
        "M 6 9 a 6 6 0 0 0 12 0 V 3 a 1 1 0 0 0 -1 -1 H 7 a 1 1 0 0 0 -1 1 z",
        "M 6.084 10 H 4.5 A 2.5 2.5 0 0 1 2 7.5 V 5 a 1 1 0 0 1 1 -1 h 3",
    )

    /** lucide: upload */
    val Upload: ImageVector = build(
        "Upload",
        "M 12 3 v 12",
        "m 17 8 -5 -5 -5 5",
        "M 21 15 v 4 a 2 2 0 0 1 -2 2 H 5 a 2 2 0 0 1 -2 -2 v -4",
    )

    /** lucide: user */
    val User: ImageVector = build(
        "User",
        "M 19 21 v -2 a 4 4 0 0 0 -4 -4 H 9 a 4 4 0 0 0 -4 4 v 2",
        "M 8 7 a 4 4 0 1 0 8 0 a 4 4 0 1 0 -8 0",
    )

    /** lucide: volume-2 */
    val Volume2: ImageVector = build(
        "Volume2",
        "M 11 4.702 a .705 .705 0 0 0 -1.203 -.498 L 6.413 7.587 A 1.4 1.4 0 0 1 5.416 8 H 3  " +
        "a 1 1 0 0 0 -1 1 v 6 a 1 1 0 0 0 1 1 h 2.416 a 1.4 1.4 0 0 1 .997 .413 l 3.383 3.384  " +
        "A .705 .705 0 0 0 11 19.298 z",
        "M 16 9 a 5 5 0 0 1 0 6",
        "M 19.364 18.364 a 9 9 0 0 0 0 -12.728",
    )

    /**
     * 实心圆（lucide 没有对应物，手写）。
     *
     * 用处：MView 的通透度端点键需要在「空心圆环 / 实心圆点」之间切换，
     * 同一个外径只差填充，所以这个必须与 [Circle] 共用 12/3.4-20.6 的半径。
     */
    val CircleSolid: ImageVector = run {
        val builder = ImageVector.Builder(
            name = "CircleSolid",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        builder.addPath(
            pathData = addPathNodes(
                "M12 3.4a8.6 8.6 0 1 0 0 17.2 8.6 8.6 0 0 0 0-17.2z",
            ),
            fill = SolidColor(Color.Black),
        )
        builder.build()
    }
}
