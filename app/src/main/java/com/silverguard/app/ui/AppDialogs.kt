package com.silverguard.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** The confirmation and consent dialogs the ViewModel can ask for. */
@Composable
internal fun AppDialogs(
    vm: SilverGuardViewModel,
    onConfirmReset: () -> Unit,
    onPickOfficialScreenshot: () -> Unit
) {
    val context = LocalContext.current

    if (vm.showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { vm.showResetConfirmation = false },
            title = { Text("要重新查一个商品吗？", fontWeight = FontWeight.Bold) },
            text = { Text("当前分析会保留在本机历史记录中，然后回到首页。大字和高对比设置会保留。") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.showResetConfirmation = false
                        onConfirmReset()
                    },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) { Text("确认重新查") }
            },
            dismissButton = {
                TextButton(
                    onClick = { vm.showResetConfirmation = false },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) { Text("继续看当前结果") }
            }
        )
    }

    if (vm.showAiConsentDialog) {
        AlertDialog(
            onDismissRequest = { vm.onAiConsent(granted = false) },
            title = { Text("使用 AI 深入分析？", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("将发送已经整理的宣传文字和商品字段给智谱 AI，帮助理解话术。")
                    Text("照片不会发送；手机号、邮箱、证件号和联系方式会先隐藏。")
                    Text("AI 可能理解错误，也不会改变本地风险分数或代替官方核验。", color = Muted)
                }
            },
            confirmButton = {
                Button(
                    onClick = { vm.onAiConsent(granted = true) },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) { Text("同意并开始分析") }
            },
            dismissButton = {
                TextButton(
                    onClick = { vm.onAiConsent(granted = false) },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) { Text("暂不使用") }
            }
        )
    }

    if (vm.showOfficialReturnDialog) {
        AlertDialog(
            onDismissRequest = { vm.showOfficialReturnDialog = false },
            title = { Text("已经完成官方查询了吗？", fontWeight = FontWeight.Bold) },
            text = {
                Text("如果已经查到结果，建议先截图，再回到银龄安心查继续核对。")
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            vm.showOfficialReturnDialog = false
                            onPickOfficialScreenshot()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                    ) { Text("选择查询结果截图") }
                    OutlinedButton(
                        onClick = {
                            vm.showOfficialReturnDialog = false
                            Toast.makeText(context, "请在官方核验区逐项填写查询结果", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                    ) { Text("回核验区手动记录") }
                    TextButton(
                        onClick = { vm.showOfficialReturnDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                    ) { Text("还没有查完") }
                }
            }
        )
    }
}
