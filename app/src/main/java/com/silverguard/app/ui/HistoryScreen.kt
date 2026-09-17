package com.silverguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverguard.app.model.AnalysisHistoryEntry
import com.silverguard.app.model.RiskLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun historyTime(time: Long): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(time))

@Composable
internal fun HistoryScreen(
    entries: List<AnalysisHistoryEntry>,
    storageMessage: String?,
    onOpen: (AnalysisHistoryEntry) -> Unit,
    onDelete: (String) -> Unit,
    onClear: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var deleteId by remember { mutableStateOf<String?>(null) }
    var clearConfirmation by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("history_screen")) {
        Text("历史记录", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("只存本机，最多保留最近 50 条。保存文字、分析和核对记录，不保存原照片；卸载应用后记录会被删除。", color = Muted, lineHeight = 23.sp)
        storageMessage?.let { Text(it, color = Ink, fontWeight = FontWeight.Bold) }
        OutlinedTextField(
            value = search, onValueChange = { search = it }, label = { Text("找商品名或输入过的文字") },
            modifier = Modifier.fillMaxWidth().testTag("history_search"), singleLine = true
        )
        if (entries.isNotEmpty() || storageMessage != null) {
            OutlinedButton(onClick = { clearConfirmation = true }, modifier = Modifier.heightIn(min = 52.dp).testTag("history_clear")) {
                Text("清空历史记录")
            }
        }
        val filtered = entries.filter { it.displayTitle.contains(search, true) || it.analysis.rawText.contains(search, true) }
        if (filtered.isEmpty()) {
            Text(if (entries.isEmpty()) "还没有记录，完成一次分析后会自动保存在这里。" else "没有找到相关记录，换个词试试。", color = Ink, fontSize = 18.sp)
        }
        filtered.forEach { entry ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink), border = CardBorder, shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(entry.displayTitle, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("保存于 ${historyTime(entry.savedAt)}", color = Muted)
                    val level = when (entry.analysis.level) { RiskLevel.HIGH -> "高"; RiskLevel.MEDIUM -> "中"; RiskLevel.LOW -> "低" }
                    Text("当时宣传风险：$level · ${entry.inputMethods.joinToString("、") { it.displayName }}", color = Ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onOpen(entry) }, modifier = Modifier.weight(1f).heightIn(min = 56.dp).testTag("history_open_${entry.id}")) { Text("打开记录", fontSize = 17.sp) }
                        OutlinedButton(onClick = { deleteId = entry.id }, modifier = Modifier.heightIn(min = 56.dp).testTag("history_delete_${entry.id}")) { Text("删除") }
                    }
                }
            }
        }
    }
    if (clearConfirmation || deleteId != null) {
        AlertDialog(
            onDismissRequest = { clearConfirmation = false; deleteId = null },
            title = { Text(if (clearConfirmation) "清空全部历史记录？" else "删除这条记录？") },
            text = { Text("删除后无法恢复，不会删除手机相册中的照片。") },
            confirmButton = {
                Button(onClick = {
                    if (clearConfirmation) onClear() else deleteId?.let(onDelete)
                    clearConfirmation = false
                    deleteId = null
                }, modifier = Modifier.heightIn(min = 52.dp).testTag("history_confirm_delete")) { Text("确认删除") }
            },
            dismissButton = { TextButton(onClick = { clearConfirmation = false; deleteId = null }, modifier = Modifier.heightIn(min = 52.dp)) { Text("保留记录") } }
        )
    }
}
