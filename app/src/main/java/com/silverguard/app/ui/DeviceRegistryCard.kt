package com.silverguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverguard.app.model.DeviceRegistryCheck
import com.silverguard.app.model.FieldAgreement

/** Shows what the local copy of the NMPA UDI database says about the device number on the packaging. */
@Composable
internal fun DeviceRegistryCard(check: DeviceRegistryCheck) {
    val record = check.record
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (check.hasMismatch) SoftAmber else Color.White,
            contentColor = Ink
        ),
        border = CardBorder,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.testTag("device_registry_card")
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                when {
                    record == null -> "本机登记数据里没有找到这个编号"
                    check.hasMismatch -> "这个编号登记的是别的产品或企业"
                    else -> "本机登记数据找到了这个编号"
                },
                modifier = Modifier.semantics { heading() },
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text("编号：${check.queriedNumber}", color = Muted, fontSize = 14.sp)

            if (record == null) {
                Text(
                    "这不代表商品是假的：唯一标识数据库没有覆盖全部医疗器械，数据也有更新延迟。请以官方注册证查询结果为准。",
                    lineHeight = 24.sp
                )
            } else {
                RegistryRow("登记产品", record.productNames.joinToString("、").ifBlank { "未登记名称" }, check.nameAgreement)
                RegistryRow("注册人 / 备案人", record.holder.ifBlank { "未登记" }, check.holderAgreement)
                if (record.category.isNotBlank()) Text("类别：${record.category}", fontSize = 15.sp)
                Text(
                    if (check.hasMismatch) {
                        "包装上的信息和登记记录对不上。有些商品会借用别人的注册证编号，付款前请到官方入口再核对一次。"
                    } else {
                        "编号存在，只说明有这样一条登记记录，不代表宣传的功效已经获批。仍要核对登记用途。"
                    },
                    lineHeight = 24.sp,
                    fontWeight = if (check.hasMismatch) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            Text(
                "数据来自国家药监局医疗器械唯一标识数据库，保存在本机，截至 ${check.dataDate}；不是实时查询。",
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun RegistryRow(label: String, value: String, agreement: FieldAgreement) {
    Column {
        Text(label, color = Muted, fontSize = 14.sp)
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 23.sp)
        when (agreement) {
            FieldAgreement.MATCHES -> Text("与包装上的信息一致", fontSize = 14.sp)
            FieldAgreement.DIFFERS -> Text("与包装上的信息不一致", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            FieldAgreement.NOT_COMPARED -> Text("包装上没有识别到，无法比对", color = Muted, fontSize = 14.sp)
        }
    }
}
