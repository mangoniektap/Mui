package com.mui.showcase

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mui.components.MibuCat
import com.mui.components.MibuCatAct
import com.mui.components.MuiAlertDialog
import com.mui.components.MuiAlert
import com.mui.components.MuiAspectRatio
import com.mui.components.MuiAvatar
import com.mui.components.MuiAvatarGroup
import com.mui.components.MuiAvatarGroupCount
import com.mui.components.MuiAvatarSize
import com.mui.components.MuiBadge
import com.mui.components.MuiBadgeVariant
import com.mui.components.MuiButton
import com.mui.components.MuiButtonGroup
import com.mui.components.MuiButtonSize
import com.mui.components.MuiButtonVariant
import com.mui.components.MuiCard
import com.mui.components.MuiCardContent
import com.mui.components.MuiCardDescription
import com.mui.components.MuiCardFooter
import com.mui.components.MuiCardHeader
import com.mui.components.MuiCardTitle
import com.mui.components.MuiCheckbox
import com.mui.components.MuiCombobox
import com.mui.components.MuiInput
import com.mui.components.MuiInputOtp
import com.mui.components.MuiKbd
import com.mui.components.MuiLabel
import com.mui.components.MuiPicker
import com.mui.components.MuiProgress
import com.mui.components.MuiRadioGroup
import com.mui.components.MuiSelect
import com.mui.components.MuiSelectOption
import com.mui.components.MuiSelectTriggerSize
import com.mui.components.MuiSeparator
import com.mui.components.MuiSkeleton
import com.mui.components.MuiSlider
import com.mui.components.MuiSpinner
import com.mui.components.MuiSwitch
import com.mui.components.MuiTabs
import com.mui.components.MuiTabItem
import com.mui.components.MuiTabsVariant
import com.mui.components.MuiText
import com.mui.components.MuiTextarea
import com.mui.components.MuiToggle
import com.mui.components.MuiToggleGroup
import com.mui.components.MuiToggleVariant
import com.mui.components.MuiTrafficLights
import com.mui.components.SegmentedItem
import com.mui.components.ToggleGroupType
import com.mui.components.TrafficLightItem
import com.mui.components.TrafficLightSize
import com.mui.theme.MuiTheme
import com.mui.theme.LocalMuiPalette
import com.mui.theme.MuiRadius
import com.mui.theme.muiElevation
import kotlin.math.roundToInt

/** 演示入口：MuiTheme 包全站，内容按 registry 的分区顺序铺 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MuiTheme {
                ShowcaseApp()
            }
        }
    }
}

/** showcase 主体：一屏纵向滚动，分区标题 + 各组件演示（顺序对齐 web 版 registry） */
@Composable
fun ShowcaseApp() {
    val palette = LocalMuiPalette.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp, 40.dp, 20.dp, 60.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { Header() }
        item { Section("Palette 调色板") { PaletteDemo() } }
        item { Section("Tokens 令牌") { TokensDemo() } }
        item { Section("Avatar 头像") { AvatarDemo() } }
        item { Section("Badge 徽标") { BadgeDemo() } }
        item { Section("Button 按钮") { ButtonDemo() } }
        item { Section("Card 卡片") { CardDemo() } }
        item { Section("Separator 分隔线") { MuiSeparator(Modifier.fillMaxWidth()) } }
        item { Section("Skeleton 骨架") { SkeletonDemo() } }
        item { Section("Spinner 加载") { MuiSpinner() } }
        item { Section("Switch 开关") { SwitchDemo() } }
        item { Section("Toggle / ToggleGroup") { ToggleDemo() } }
        item { Section("Slider 滑杆") { SliderDemo() } }
        item { Section("Picker 转盘") { PickerDemo() } }
        item { Section("Input / Textarea / Label") { FormDemo() } }
        item { Section("Checkbox / RadioGroup") { ChoiceDemo() } }
        item { Section("Select / Combobox") { SelectDemo() } }
        item { Section("Tabs 标签页") { TabsDemo() } }
        item { Section("Progress / Kbd") { MiscDemo() } }
        item { Section("Dialog / AlertDialog") { DialogDemo() } }
        item { Section("Traffic Lights 红绿灯") { TrafficLightsDemo() } }
        item { Section("Mibu Cat 像素猫") { MibuCatDemo() } }
    }
}

@Composable
private fun Header() {
    val palette = LocalMuiPalette.current
    Column {
        MuiText("Mui Showcase", fontSize = 28.sp, color = palette.foreground)
        MuiText("Kotlin · Jetpack Compose 版（与 web/ 同一设计语言）", fontSize = 14.sp, color = palette.mutedForeground)
    }
}

/** 分区容器：标题 + 内容 */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val palette = LocalMuiPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        MuiText(title, fontSize = 18.sp, color = palette.foreground)
        content()
    }
}

@Composable
private fun PaletteDemo() {
    val p = LocalMuiPalette.current
    val swatches = listOf(
        "primary" to p.primary, "secondary" to p.secondary, "muted" to p.muted,
        "accent" to p.accent, "destructive" to p.destructive, "success" to p.success,
        "warning" to p.warning, "info" to p.info, "border" to p.border,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            swatches.take(5).forEach { (name, color) -> Swatch(name, color) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            swatches.drop(5).forEach { (name, color) -> Swatch(name, color) }
        }
    }
}

@Composable
private fun Swatch(name: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(color))
        MuiText(name, fontSize = 10.sp, color = LocalMuiPalette.current.mutedForeground)
    }
}

@Composable
private fun TokensDemo() {
    val palette = LocalMuiPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // 圆角：classical 一排
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("sm" to MuiRadius.sm, "md" to MuiRadius.md, "lg" to MuiRadius.lg, "xl" to MuiRadius.xl).forEach { (name, r) ->
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(r))
                        .background(palette.muted),
                    contentAlignment = Alignment.Center,
                ) {
                    MuiText(name, fontSize = 10.sp, color = palette.mutedForeground)
                }
            }
        }
        // 阴影双排（亮/暗由主题给）
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf("xs", "sm", "md", "lg").forEach { level ->
                val shape = RoundedCornerShape(MuiRadius.md)
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(shape)
                        .background(palette.card, shape)
                        .muiElevation(level, shape),
                    contentAlignment = Alignment.Center,
                ) {
                    MuiText(level, fontSize = 10.sp, color = palette.mutedForeground)
                }
            }
        }
    }
}

@Composable
private fun AvatarDemo() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        MuiAvatar(fallbackText = "M")
        MuiAvatar(size = MuiAvatarSize.Lg, fallbackText = "K")
        MuiAvatar(size = MuiAvatarSize.Sm, fallbackText = "S", badge = { Box(Modifier.size(6.dp).clip(CircleShape).background(LocalMuiPalette.current.primary)) })
        MuiAvatarGroup {
            MuiAvatar(fallbackText = "A")
            MuiAvatar(fallbackText = "B")
            MuiAvatarGroupCount(5)
        }
    }
}

@Composable
private fun BadgeDemo() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MuiBadge { MuiText("Default") }
        MuiBadge(variant = MuiBadgeVariant.Secondary) { MuiText("Secondary") }
        MuiBadge(variant = MuiBadgeVariant.Destructive) { MuiText("Destructive") }
        MuiBadge(variant = MuiBadgeVariant.Outline) { MuiText("Outline") }
    }
}

@Composable
private fun ButtonDemo() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MuiButton(onClick = {}) { MuiText("Default") }
            MuiButton(onClick = {}, variant = MuiButtonVariant.Secondary) { MuiText("Secondary") }
            MuiButton(onClick = {}, variant = MuiButtonVariant.Outline) { MuiText("Outline") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MuiButton(onClick = {}, variant = MuiButtonVariant.Ghost) { MuiText("Ghost") }
            MuiButton(onClick = {}, variant = MuiButtonVariant.Link) { MuiText("Link") }
            MuiButton(onClick = {}, variant = MuiButtonVariant.Destructive) { MuiText("Destructive") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            MuiButton(onClick = {}, size = MuiButtonSize.Xs) { MuiText("XS") }
            MuiButton(onClick = {}, size = MuiButtonSize.Sm) { MuiText("SM") }
            MuiButton(onClick = {}, size = MuiButtonSize.Lg) { MuiText("LG") }
            MuiButton(onClick = {}, enabled = false) { MuiText("Disabled") }
        }
        MuiButtonGroup {
            MuiButton(onClick = {}, variant = MuiButtonVariant.Outline, size = MuiButtonSize.Sm) { MuiText("Day") }
            MuiButton(onClick = {}, variant = MuiButtonVariant.Outline, size = MuiButtonSize.Sm) { MuiText("Week") }
            MuiButton(onClick = {}, variant = MuiButtonVariant.Outline, size = MuiButtonSize.Sm) { MuiText("Month") }
        }
    }
}

@Composable
private fun CardDemo() {
    MuiCard(Modifier.fillMaxWidth()) {
        MuiCardHeader {
            Column {
                MuiCardTitle("玻璃引擎已随库移植")
                MuiCardDescription("Switch / Slider / Picker / ToggleGroup 四处走 AGSL")
            }
        }
        MuiCardContent {
            MuiText("kotlin/mui-ui/glass：SDF 折射、色散、三档本体、按压外鼓与运动惯性。")
        }
        MuiCardFooter {
            MuiButton(onClick = {}, size = MuiButtonSize.Sm) { MuiText("知道了") }
        }
    }
}

@Composable
private fun SkeletonDemo() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        MuiSkeleton(Modifier.size(40.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MuiSkeleton(Modifier.size(200.dp, 14.dp))
            MuiSkeleton(Modifier.size(140.dp, 14.dp))
        }
    }
}

@Composable
private fun SwitchDemo() {
    var checked by rememberSaveable { mutableStateOf(true) }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        com.mui.components.MuiSwitch(checked = checked, onCheckedChange = { checked = it })
        com.mui.components.MuiSwitch(checked = !checked, onCheckedChange = { checked = !it })
        MuiText(if (checked) "开" else "关")
    }
}

@Composable
private fun ToggleDemo() {
    var pressed by remember { mutableStateOf(false) }
    var seg by rememberSaveable { mutableStateOf("day") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MuiToggle(pressed = pressed, onPressedChange = { pressed = it }) { MuiText("Toggle") }
            MuiToggle(pressed = true, variant = MuiToggleVariant.Outline) { MuiText("On") }
        }
        MuiToggleGroup(
            type = ToggleGroupType.Single,
            items = listOf(
                SegmentedItem("day", "日"),
                SegmentedItem("week", "周"),
                SegmentedItem("month", "月"),
            ),
            value = seg,
            onValueChange = { seg = it },
        )
        MuiToggleGroup(
            type = ToggleGroupType.Multiple,
            variant = MuiToggleVariant.Outline,
            items = listOf(
                SegmentedItem("a", "粗体"),
                SegmentedItem("b", "斜体"),
                SegmentedItem("c", "下划线"),
            ),
        )
    }
}

@Composable
private fun SliderDemo() {
    var value by remember { mutableFloatStateOf(60f) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MuiSlider(value = value, onValueChange = { value = it }, min = 0f, max = 100f)
        MuiText("值：${value.roundToIntCompat()}", fontSize = 12.sp, color = LocalMuiPalette.current.mutedForeground)
    }
}

@Composable
private fun PickerDemo() {
    var v by remember { mutableStateOf(12f) }
    MuiPicker(
        min = 0f,
        max = 24f,
        value = v,
        onValueChange = { v = it },
        unit = "时",
    )
}

@Composable
private fun FormDemo() {
    var text by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MuiLabel("邮箱")
        MuiInput(value = text, onValueChange = { text = it }, placeholder = "you@example.com")
        MuiLabel("备注")
        MuiTextarea(value = text, onValueChange = { text = it }, placeholder = "写点什么…")
    }
}

@Composable
private fun ChoiceDemo() {
    var check by remember { mutableStateOf(false) }
    var radio by remember { mutableStateOf("a") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            MuiCheckbox(checked = check, onCheckedChange = { check = it })
            MuiText("勾选我")
        }
        MuiRadioGroup {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                com.mui.components.MuiRadioGroupItem(selected = radio == "a", onSelect = { radio = "a" })
                MuiText("苹果")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                com.mui.components.MuiRadioGroupItem(selected = radio == "b", onSelect = { radio = "b" })
                MuiText("香蕉")
            }
        }
    }
}

@Composable
private fun SelectDemo() {
    var sel by remember { mutableStateOf("apple") }
    var otp by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MuiSelect(
            options = listOf(
                SelectOption("apple", "苹果", group = "水果"),
                SelectOption("banana", "香蕉", group = "水果"),
                SelectOption("carrot", "胡萝卜", group = "蔬菜"),
            ),
            value = sel,
            onValueChange = { sel = it },
            triggerSize = SelectTriggerSize.Default,
        )
        MuiCombobox(
            options = listOf("React", "Kotlin", "Compose", "TypeScript"),
            onSelect = {},
            placeholder = "搜索框架…",
        )
        MuiInputOtp(value = otp, onValueChange = { otp = it }, slotCount = 6)
    }
}

@Composable
private fun TabsDemo() {
    MuiTabs(
        items = listOf(TabItem("one", "第一页"), TabItem("two", "第二页"), TabItem("three", "第三页")),
        variant = MuiTabsVariant.Default,
    ) { selected ->
        MuiText("当前：$selected", color = LocalMuiPalette.current.mutedForeground)
    }
}

@Composable
private fun MiscDemo() {
    var progress by remember { mutableFloatStateOf(60f) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MuiProgress(value = progress)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MuiKbd(text = "Ctrl")
            MuiKbd(text = "K")
        }
        MuiButton(onClick = { progress = if (progress > 90f) 10f else progress + 20f }, size = MuiButtonSize.Sm) {
            MuiText("进度 +20")
        }
    }
}

@Composable
private fun DialogDemo() {
    var openDialog by remember { mutableStateOf(false) }
    var openAlert by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MuiButton(onClick = { openDialog = true }, variant = MuiButtonVariant.Outline) { MuiText("Dialog") }
        MuiButton(onClick = { openAlert = true }) { MuiText("AlertDialog") }
    }
    if (openDialog) {
        MuiDialog(open = true, onOpenChange = { openDialog = it }) {
            MuiText("这是一个模态对话框。")
        }
    }
    if (openAlert) {
        MuiAlertDialog(
            open = true,
            onOpenChange = { openAlert = it },
            title = "确认操作",
            description = "该操作不可撤销，确定继续吗？",
            onConfirm = { openAlert = false },
        )
    }
}

@Composable
private fun TrafficLightsDemo() {
    val palette = LocalMuiPalette.current
    var active by rememberSaveable { mutableStateOf("red") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MuiTrafficLights(
            items = listOf(
                TrafficLightItem("red", "红", Color(0xFFFF5F57)),
                TrafficLightItem("yellow", "黄", Color(0xFFFEBC2E)),
                TrafficLightItem("green", "绿", Color(0xFF28C840)),
            ),
            value = active,
            onValueChange = { v, _ -> active = v },
            size = TrafficLightSize.Lg,
        )
        MuiText("当前：$active", color = palette.mutedForeground)
    }
}

@Composable
private fun MibuCatDemo() {
    var act by remember { mutableStateOf<MibuCatAct?>(null) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        MibuCat(act = act, onClick = { act = MibuCatAct.Alert })
        MuiText("点猫会警觉一下；平时随机做小动作")
    }
}

private fun Float.roundToIntCompat(): Int = roundToInt()
