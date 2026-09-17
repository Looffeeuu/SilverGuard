package com.silverguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverguard.app.engine.PriceReferenceEvaluator
import com.silverguard.app.model.PriceQuoteInput
import com.silverguard.app.model.PriceReferenceInput
import com.silverguard.app.model.PriceReferenceResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun PriceReferenceCard(result: PriceReferenceResult, onChange: (PriceReferenceInput) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    val input = result.input
    Card(
        modifier = Modifier.fillMaxWidth().testTag("price_reference_card"),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White, contentColor = Ink),
        border = CardBorder,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("④ 比一比价格", modifier = Modifier.semantics { heading() }, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(result.status.displayName, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            if (result.hasRange) {
                Text(
                    "已录报价：${PriceReferenceEvaluator.formatPrice(requireNotNull(result.minCents))} ～ ${PriceReferenceEvaluator.formatPrice(requireNotNull(result.maxCents))}",
                    fontSize = 22.sp, lineHeight = 30.sp, color = Brand, fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("price_reference_range")
                )
                Text("当前到手价：${PriceReferenceEvaluator.formatPrice(requireNotNull(result.askingCents))}", fontSize = 17.sp)
            }
            Text(result.message, lineHeight = 24.sp)
            Text("依据：你录入的报价，不是实时市场价或官方指导价；不改变宣传风险分数。", color = Muted, fontSize = 14.sp, lineHeight = 21.sp)
            result.recordedAt?.let { time ->
                Text("录入时间：${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(time))}", color = Muted, fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = { editing = !editing },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("price_reference_edit")
            ) { Text(if (editing) "收起报价填写" else "录入 / 修改对比报价", fontSize = 17.sp) }
            if (editing) {
                Text("只比较同品牌、同型号、同规格、同数量的人民币到手价。运费、优惠和售后条件也要核对。", lineHeight = 23.sp)
                OutlinedTextField(
                    value = input.askingPrice,
                    onValueChange = { onChange(input.copy(askingPrice = it.take(40), sameProductConfirmed = false)) },
                    label = { Text("当前商品到手价（元）") },
                    modifier = Modifier.fillMaxWidth().testTag("price_current"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                input.quotes.forEachIndexed { index, quote ->
                    Spacer(Modifier.height(2.dp))
                    Text("参考报价 ${index + 1}", fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = quote.source,
                        onValueChange = { source ->
                            onChange(input.copy(quotes = input.quotes.mapIndexed { i, old ->
                                if (i == index) old.copy(source = source.take(80)) else old
                            }, sameProductConfirmed = false))
                        },
                        label = { Text("平台 + 店铺，或其他来源") },
                        modifier = Modifier.fillMaxWidth().testTag("price_source_$index"), singleLine = true
                    )
                    OutlinedTextField(
                        value = quote.price,
                        onValueChange = { price ->
                            onChange(input.copy(quotes = input.quotes.mapIndexed { i, old ->
                                if (i == index) old.copy(price = price.take(40)) else old
                            }, sameProductConfirmed = false))
                        },
                        label = { Text("这家店的到手价（元）") },
                        modifier = Modifier.fillMaxWidth().testTag("price_quote_$index"), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    if (input.quotes.size > 3) {
                        OutlinedButton(
                            onClick = { onChange(input.copy(quotes = input.quotes.filterIndexed { i, _ -> i != index }, sameProductConfirmed = false)) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("移除报价 ${index + 1}") }
                    }
                }
                if (input.quotes.size < PriceReferenceEvaluator.MAX_QUOTES) {
                    OutlinedButton(
                        onClick = { onChange(input.copy(quotes = input.quotes + PriceQuoteInput(), sameProductConfirmed = false)) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                    ) { Text("再添一家报价") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("price_same_product")
                        .toggleable(value = input.sameProductConfirmed, role = Role.Checkbox) {
                            onChange(input.copy(sameProductConfirmed = it))
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = input.sameProductConfirmed, onCheckedChange = null)
                    Text("我已核对：这些报价的商品和购买条件相同", modifier = Modifier.weight(1f), lineHeight = 24.sp)
                }
                Text("仅保留在本次结果中；补充材料并重新分析或重新查一个后，需要重新录入并核对报价。", color = Muted, fontSize = 13.sp, lineHeight = 21.sp)
            }
        }
    }
}
