package com.silverguard.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Shown before anything else until the user agrees to the current privacy policy. */
@Composable
internal fun ConsentScreen(
    onAccept: () -> Unit,
    onShowPolicy: () -> Unit,
    onExit: () -> Unit
) {
    var showRefusal by rememberSaveable { mutableStateOf(false) }

    if (showRefusal) {
        AlertDialog(
            onDismissRequest = { showRefusal = false },
            title = { Text("不同意就无法使用分析功能", fontWeight = FontWeight.Bold) },
            text = { Text("银龄安心查需要按隐私政策处理你输入的文字和图片，才能给出分析结果。你可以再看看政策内容，或退出应用。") },
            confirmButton = {
                Button(
                    onClick = { showRefusal = false },
                    modifier = Modifier.heightIn(min = 52.dp).testTag("consent_reconsider")
                ) { Text("再看看") }
            },
            dismissButton = {
                TextButton(
                    onClick = onExit,
                    modifier = Modifier.heightIn(min = 52.dp).testTag("consent_exit")
                ) { Text("退出应用") }
            }
        )
    }

    PrivacyFrame {
        Text(
            "欢迎使用银龄安心查",
            modifier = Modifier.semantics { heading() },
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )
        Spacer(Modifier.height(12.dp))
        Text("开始之前，请先了解我们怎样处理你的信息：", fontSize = 18.sp, lineHeight = 27.sp, color = Ink)
        Spacer(Modifier.height(12.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
            border = CardBorder,
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(
                    "不用注册账号，不申请相机、相册、通讯录、位置权限。",
                    "照片和截图在手机上识别文字，不上传，也不保存原图。",
                    "分析历史和家人电话只存在你的手机里，可以随时删除。",
                    "分析淘宝、天猫商品链接时，会读取这件商品的公开网页。",
                    "文字识别组件（Google ML Kit）会向 Google 发送使用统计，不含图片和文字。"
                ).forEach { point -> PolicyBullet(point) }
            }
        }
        Spacer(Modifier.height(16.dp))
        OutlinedButton(
            onClick = onShowPolicy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("consent_show_policy"),
            shape = RoundedCornerShape(18.dp)
        ) { Text("查看完整隐私政策", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onAccept,
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp).testTag("consent_accept"),
            colors = ButtonDefaults.buttonColors(containerColor = Brand),
            shape = RoundedCornerShape(18.dp)
        ) { Text("同意并继续", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick = { showRefusal = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("consent_refuse")
        ) { Text("不同意", fontSize = 16.sp) }
    }
}

/** The full policy. After consent it also offers withdrawing it. */
@Composable
internal fun PrivacyPolicyScreen(
    policy: String,
    canWithdraw: Boolean,
    onBack: () -> Unit,
    onWithdraw: () -> Unit
) {
    var confirmWithdraw by rememberSaveable { mutableStateOf(false) }
    val blocks = remember(policy) { PolicyMarkdown.parse(policy) }
    BackHandler(onBack = onBack)

    if (confirmWithdraw) {
        AlertDialog(
            onDismissRequest = { confirmWithdraw = false },
            title = { Text("要撤回同意吗？", fontWeight = FontWeight.Bold) },
            text = { Text("撤回后会回到同意页面，不能继续分析。已保存的历史不会自动删除，可以在“历史记录”里清空，或在手机设置中清除本应用数据。") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmWithdraw = false
                        onWithdraw()
                    },
                    modifier = Modifier.heightIn(min = 52.dp).testTag("policy_withdraw_confirm")
                ) { Text("确认撤回") }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmWithdraw = false },
                    modifier = Modifier.heightIn(min = 52.dp)
                ) { Text("取消") }
            }
        )
    }

    PrivacyFrame {
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("policy_back"),
            shape = RoundedCornerShape(18.dp)
        ) { Text("返回", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(16.dp))
        blocks.forEach { block -> PolicyBlockView(block) }
        if (canWithdraw) {
            Spacer(Modifier.height(20.dp))
            OutlinedButton(
                onClick = { confirmWithdraw = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("policy_withdraw"),
                shape = RoundedCornerShape(18.dp)
            ) { Text("撤回同意", fontSize = 17.sp) }
        }
    }
}

@Composable
private fun PrivacyFrame(content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = Background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) { content() }
    }
}

@Composable
private fun PolicyBullet(text: String) {
    Row {
        Text("•", fontSize = 17.sp, lineHeight = 26.sp, modifier = Modifier.width(18.dp))
        Text(text, fontSize = 17.sp, lineHeight = 26.sp)
    }
}

@Composable
private fun PolicyBlockView(block: PolicyMarkdown.Block) {
    when (block) {
        is PolicyMarkdown.Block.Heading -> {
            Spacer(Modifier.height(if (block.level == 1) 4.dp else 18.dp))
            Text(
                annotated(block.spans),
                modifier = Modifier.semantics { heading() },
                fontSize = if (block.level == 1) 26.sp else 21.sp,
                lineHeight = if (block.level == 1) 34.sp else 29.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )
            Spacer(Modifier.height(8.dp))
        }
        is PolicyMarkdown.Block.Paragraph -> Text(
            annotated(block.spans),
            modifier = Modifier.padding(start = (block.indent * 22).dp, bottom = 8.dp),
            fontSize = 17.sp,
            lineHeight = 27.sp,
            color = Ink
        )
        is PolicyMarkdown.Block.ListItem -> Row(Modifier.padding(start = (block.indent * 22).dp, bottom = 8.dp)) {
            Text(block.marker, fontSize = 17.sp, lineHeight = 27.sp, color = Ink, modifier = Modifier.width(26.dp))
            Text(annotated(block.spans), fontSize = 17.sp, lineHeight = 27.sp, color = Ink)
        }
        // A wide table is hard to read on a phone, so each row becomes a small card.
        is PolicyMarkdown.Block.Table -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            block.rows.forEach { row ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
                    border = CardBorder,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(annotated(row.first()), fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.Bold)
                        row.drop(1).forEachIndexed { column, cell ->
                            val label = block.header.getOrNull(column + 1)?.joinToString("") { it.text }.orEmpty()
                            Text(
                                annotated(listOf(PolicyMarkdown.Span("$label：")) + cell),
                                fontSize = 16.sp,
                                lineHeight = 25.sp
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun annotated(spans: List<PolicyMarkdown.Span>): AnnotatedString {
    // Brand follows the high-contrast palette, so links stay readable in both modes.
    val linkColor = Brand
    return buildAnnotatedString {
        spans.forEach { span ->
            when {
                span.url != null -> withLink(
                    LinkAnnotation.Url(
                        span.url,
                        TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                    )
                ) { append(span.text) }
                span.bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(span.text) }
                else -> append(span.text)
            }
        }
    }
}
