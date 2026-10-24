package com.silverguard.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.silverguard.app.engine.ShareReportBuilder
import com.silverguard.app.model.RiskAnalysis

/** Hand-offs to other apps: browser, dialer, clipboard and the share sheet. */

internal fun openUrl(context: Context, url: String): Boolean = try {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    true
} catch (_: Exception) {
    Toast.makeText(context, "没有找到可以打开该页面的应用", Toast.LENGTH_LONG).show()
    false
}

/** Opens the dialer with the number filled in; the user still presses call, so no phone permission is needed. */
internal fun openDialer(context: Context, number: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
    } catch (_: Exception) {
        Toast.makeText(context, "这台设备不能拨打电话，请用手机拨打 $number", Toast.LENGTH_LONG).show()
    }
}

internal fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "编号已复制，可粘贴到官方查询框", Toast.LENGTH_SHORT).show()
}

internal fun shareAnalysis(context: Context, analysis: RiskAnalysis) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, ShareReportBuilder.build(analysis))
    }
    context.startActivity(Intent.createChooser(intent, "发给家人"))
}
