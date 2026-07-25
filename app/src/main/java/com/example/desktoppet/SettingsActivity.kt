package com.example.desktoppet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 宠物设置界面 - Jetpack Compose 版本
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                SettingsScreen(onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var currentSizeDp by remember { mutableIntStateOf(PetSettings.getPetSizeDp(context)) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("宠物设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 标题
            Text(
                text = "宠物大小设置",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "选择预设大小或拖动滑块自定义",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // 预设按钮
            Text(
                text = "预设大小",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            // 预设按钮网格
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PetSettings.SIZE_OPTIONS.forEach { (sizeDp, label) ->
                    val isSelected = currentSizeDp == sizeDp
                    FilterChip(
                        selected = isSelected,
                        onClick = { currentSizeDp = sizeDp },
                        label = { Text("$label\n${sizeDp}dp", textAlign = TextAlign.Center) },
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 滑块
            Text(
                text = "精细调整: ${currentSizeDp}dp",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Slider(
                value = currentSizeDp.toFloat(),
                onValueChange = { currentSizeDp = it.toInt() },
                valueRange = 60f..250f,
                steps = 189, // (250-60)/1 - 1
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("60dp", style = MaterialTheme.typography.bodySmall)
                Text("250dp", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 预览区域
            Text(
                text = "预览",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .padding(vertical = 16.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                val previewSizeSp = (currentSizeDp / 6f).coerceIn(12f, 48f)
                val description = when (currentSizeDp) {
                    in 0..90 -> "超小 - 像个小图标"
                    in 91..110 -> "小 - 比较精致"
                    in 111..135 -> "较小 - 推荐尺寸"
                    in 136..165 -> "中 - 适中大小"
                    in 166..190 -> "较大 - 比较显眼"
                    in 191..230 -> "大 - 非常醒目"
                    else -> "超大 - 占据屏幕"
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🐱",
                        fontSize = previewSizeSp.sp
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 提示
            Text(
                text = "提示：设置后需要重启宠物才能生效",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 保存按钮
            Button(
                onClick = {
                    PetSettings.setPetSizeDp(context, currentSizeDp)
                    android.widget.Toast.makeText(
                        context,
                        "设置已保存！重启宠物后生效",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("保存设置", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

// 简单的 FlowRow 实现
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val hGapPx = 8.dp.roundToPx()
        val vGapPx = 8.dp.roundToPx()
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()

        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)

            if (currentRow.isNotEmpty() &&
                currentRowWidth + hGapPx + placeable.width > constraints.maxWidth
            ) {
                rows.add(currentRow)
                rowWidths.add(currentRowWidth)
                rowHeights.add(currentRowHeight)
                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }

            currentRow.add(placeable)
            currentRowWidth += if (currentRow.size == 1) placeable.width else hGapPx + placeable.width
            currentRowHeight = maxOf(currentRowHeight, placeable.height)
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowWidths.add(currentRowWidth)
            rowHeights.add(currentRowHeight)
        }

        val totalHeight = rowHeights.sum() + (rows.size - 1).coerceAtLeast(0) * vGapPx

        layout(
            width = constraints.maxWidth,
            height = totalHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        ) {
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                var x = when (horizontalArrangement) {
                    Arrangement.Center -> (constraints.maxWidth - rowWidths[rowIndex]) / 2
                    Arrangement.End -> constraints.maxWidth - rowWidths[rowIndex]
                    else -> 0
                }

                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + hGapPx
                }

                y += rowHeights[rowIndex] + vGapPx
            }
        }
    }
}
