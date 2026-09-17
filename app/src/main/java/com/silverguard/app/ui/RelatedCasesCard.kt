package com.silverguard.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverguard.app.data.RiskCaseRepository
import com.silverguard.app.model.RiskCaseMatch

@Composable
internal fun RelatedCasesCard(matches: List<RiskCaseMatch>, onOpenSource: (String) -> Unit) {
    var showLibrary by remember { mutableStateOf(false) }
    val visibleCases = if (showLibrary) RiskCaseRepository.cases else matches.map { it.case }
    Card(
        modifier = Modifier.fillMaxWidth().testTag("related_cases_card"),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White, contentColor = Ink),
        border = CardBorder,
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("⑤ 看看类似案例与提醒", modifier = Modifier.semantics { heading() }, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("只按话术匹配，不代表当前商品或商家就是案例中的对象，也不据此增加风险分数。", color = Muted, lineHeight = 22.sp)
            if (matches.isEmpty() && !showLibrary) {
                Text("暂未匹配到本地条目。案例库范围有限，没有匹配不代表商品安全。", lineHeight = 24.sp)
            }
            visibleCases.forEach { case ->
                HorizontalDivider()
                Text(case.kind.displayName, color = Brand, fontSize = 14.sp)
                Text(case.title, fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 25.sp)
                matches.firstOrNull { it.case.id == case.id }?.let { match ->
                    Text("相似话术：${match.matchedTerms.joinToString("、")}", color = Muted, fontSize = 14.sp)
                }
                Text(case.summary, lineHeight = 24.sp)
                Text(case.action, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp)
                Text("来源：${case.sourceName}\n发布：${case.publishedOn}", color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
                OutlinedButton(
                    onClick = { onOpenSource(case.sourceUrl) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
                ) { Text("打开该条公开来源", fontSize = 16.sp) }
            }
            OutlinedButton(
                onClick = { showLibrary = !showLibrary },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("case_library_toggle")
            ) { Text(if (showLibrary) "只看本次匹配" else "浏览本地案例库（${RiskCaseRepository.cases.size} 条）", fontSize = 17.sp) }
            Text("本地资料整理日期：${RiskCaseRepository.REVIEWED_ON}。摘要可离线阅读，打开来源需要联网。", color = Muted, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}
