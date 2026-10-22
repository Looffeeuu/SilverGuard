package com.silverguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverguard.app.engine.PhoneNumbers
import com.silverguard.app.model.RiskLevel

private data class HelpLine(val number: String, val label: String, val hint: String, val tag: String)

private val helpLines = listOf(
    HelpLine("12315", "拨打 12315 消费投诉", "商品宣传或售后有问题，向市场监管部门投诉举报", "help_call_12315"),
    HelpLine("96110", "拨打 96110 反诈咨询", "拿不准是不是骗局，可以先问反诈专线", "help_call_96110"),
    HelpLine("110", "已经付款或转账：拨打 110", "怀疑被骗并且已经付款，请尽快报警并保留聊天和付款记录", "help_call_110")
)

/**
 * Lets the user ring family or an official hotline from the result. Calls go through the system dialer,
 * so nothing is dialled until the user presses call there, and no phone permission is needed.
 */
@Composable
internal fun HelpContactsCard(
    level: RiskLevel,
    familyPhone: String?,
    onSaveFamilyPhone: (String) -> Boolean,
    onClearFamilyPhone: () -> Unit,
    onDial: (String) -> Unit
) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    var draftError by rememberSaveable { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        border = CardBorder,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag("help_contacts_card")
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "拿不准？先找人问问",
                modifier = Modifier.semantics { heading() },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Text(
                if (level == RiskLevel.HIGH) "先别付款。打个电话问问家人或官方热线，再做决定。" else "付款前可以先和家人商量一下。",
                lineHeight = 24.sp
            )

            if (familyPhone != null && !editing) {
                Button(
                    onClick = { onDial(familyPhone) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp)
                        .testTag("help_call_family"),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("给家人打电话（${PhoneNumbers.mask(familyPhone)}）", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            draft = familyPhone
                            draftError = false
                            editing = true
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) { Text("修改家人电话") }
                    TextButton(
                        onClick = onClearFamilyPhone,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("help_family_clear")
                    ) { Text("删除") }
                }
            } else {
                OutlinedTextField(
                    value = draft,
                    onValueChange = {
                        draft = it
                        draftError = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("help_family_input"),
                    label = { Text("家人电话（只保存在本机）") },
                    isError = draftError,
                    supportingText = if (draftError) {
                        { Text("请输入正确的电话号码，例如 13812345678") }
                    } else null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(14.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (onSaveFamilyPhone(draft)) {
                                editing = false
                                draft = ""
                            } else {
                                draftError = true
                            }
                        },
                        modifier = Modifier.heightIn(min = 52.dp).testTag("help_family_save"),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("保存家人电话") }
                    if (editing) {
                        TextButton(
                            onClick = { editing = false },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("取消") }
                    }
                }
            }

            helpLines.forEach { line ->
                OutlinedButton(
                    onClick = { onDial(line.number) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 58.dp)
                        .semantics { contentDescription = "${line.label}。${line.hint}" }
                        .testTag(line.tag),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(line.label, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(line.hint, fontSize = 13.sp, color = Muted, lineHeight = 18.sp)
                    }
                }
            }
            Text("点击后会打开手机拨号界面，需要你再按一次拨打。", fontSize = 13.sp, color = Muted)
        }
    }
}
