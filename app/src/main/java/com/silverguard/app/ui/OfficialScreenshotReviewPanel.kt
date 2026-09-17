package com.silverguard.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.silverguard.app.model.OfficialScreenshotComparison
import com.silverguard.app.model.OfficialScreenshotReview
import com.silverguard.app.model.ScreenshotComparisonStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun OfficialScreenshotReviewPanel(review: OfficialScreenshotReview) {
    val containerColor = when {
        review.hasPossibleMismatch -> SoftRed
        review.matchedCount > 0 -> SoftGreen
        else -> SoftAmber
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = Ink),
        border = CardBorder,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(15.dp)) {
            Text("官方查询截图辅助比对", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(5.dp))
            Text(review.summary, color = Ink, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
            Text(
                "本地识别 ${review.recognizedCharacterCount} 个文字 · ${formatScreenshotTime(review.processedAt)}",
                color = Muted,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(10.dp))

            review.comparisons.forEach { comparison ->
                ScreenshotComparisonRow(comparison)
            }

            Text(
                "截图 OCR 可能看错字。请在下方人工核对区确认，人工选择会作为最终记录。",
                color = Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ScreenshotComparisonRow(comparison: OfficialScreenshotComparison) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White, contentColor = Ink),
        border = CardBorder,
        shape = RoundedCornerShape(13.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    comparison.field.displayName,
                    modifier = Modifier.weight(1f),
                    color = Ink,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = comparisonStatusColor(comparison.status),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        comparison.status.displayName,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = Ink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            comparison.expectedValue?.let { expected ->
                Spacer(Modifier.height(5.dp))
                Text("包装识别：$expected", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
            comparison.detectedText?.let { detected ->
                Text("截图识别：$detected", color = Muted, fontSize = 12.sp, lineHeight = 18.sp)
            }
            Spacer(Modifier.height(3.dp))
            Text(comparison.explanation, color = Ink, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun comparisonStatusColor(status: ScreenshotComparisonStatus): Color = when (status) {
    ScreenshotComparisonStatus.MATCHED -> SoftGreen
    ScreenshotComparisonStatus.POSSIBLE_MISMATCH -> SoftRed
    ScreenshotComparisonStatus.NOT_RECOGNIZED -> Background
    ScreenshotComparisonStatus.NEEDS_CONFIRMATION -> SoftAmber
}

private fun formatScreenshotTime(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(Date(timestamp))
