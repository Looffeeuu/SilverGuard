package com.silverguard.app.engine

import com.silverguard.app.model.OfficialScreenshotComparison
import com.silverguard.app.model.OfficialScreenshotReview
import com.silverguard.app.model.ScreenshotComparisonStatus
import com.silverguard.app.model.VerificationChecklistItem
import com.silverguard.app.model.VerificationEvidenceField
import com.silverguard.app.model.VerificationResult

/**
 * Compares locally recognized OCR text with the product clues already extracted by the app.
 * The result is only an OCR-assisted comparison and never an official database response.
 */
object OfficialScreenshotAnalyzer {

    fun analyze(
        ocrText: String,
        verification: VerificationResult,
        now: Long = System.currentTimeMillis()
    ): OfficialScreenshotReview {
        val cleanText = ocrText.trim()
        return OfficialScreenshotReview(
            processedAt = now,
            recognizedCharacterCount = cleanText.count { !it.isWhitespace() },
            comparisons = verification.checklist.map { item -> compare(item, cleanText) }
        )
    }

    private fun compare(
        item: VerificationChecklistItem,
        text: String
    ): OfficialScreenshotComparison = when (item.field) {
        VerificationEvidenceField.REGISTRATION_NUMBER -> compareRegistration(item, text)
        VerificationEvidenceField.PRODUCT_NAME -> compareExpectedValue(
            item,
            text,
            labels = listOf("产品名称", "商品名称", "产品名")
        )
        VerificationEvidenceField.REGISTRANT -> compareExpectedValue(
            item,
            text,
            labels = listOf("注册人", "备案人", "企业名称", "生产企业", "注册人名称")
        )
        VerificationEvidenceField.MODEL_OR_SPECIFICATION -> compareModel(item, text)
        VerificationEvidenceField.REGISTERED_SCOPE -> compareScope(item, text)
        VerificationEvidenceField.REGISTRATION_STATUS -> compareRegistrationStatus(item, text)
    }

    private fun compareRegistration(
        item: VerificationChecklistItem,
        text: String
    ): OfficialScreenshotComparison {
        val expected = item.expectedValue.orEmpty()
        if (expected.isNotBlank() && containsNormalized(text, expected)) {
            return comparison(
                item,
                expected,
                ScreenshotComparisonStatus.MATCHED,
                "截图中识别到了与包装相同的注册 / 备案号。"
            )
        }

        val candidate = RegistrationNumberClassifier.findCandidate(text)
        return if (!candidate.isNullOrBlank()) {
            comparison(
                item,
                candidate,
                ScreenshotComparisonStatus.POSSIBLE_MISMATCH,
                "截图中识别到了另一个编号，请人工逐字核对。"
            )
        } else {
            notRecognized(item, "截图中没有识别到可比较的注册 / 备案号。")
        }
    }

    private fun compareExpectedValue(
        item: VerificationChecklistItem,
        text: String,
        labels: List<String>
    ): OfficialScreenshotComparison {
        val expected = item.expectedValue
        if (expected.isNullOrBlank()) {
            return notRecognized(item, "包装信息中没有这一项，无法自动比较。")
        }
        if (containsNormalized(text, expected)) {
            return comparison(
                item,
                expected,
                ScreenshotComparisonStatus.MATCHED,
                "截图中识别到了与包装相同的文字。"
            )
        }

        val labeledLine = findLabeledLine(text, labels)
        return if (labeledLine != null) {
            comparison(
                item,
                labeledLine,
                ScreenshotComparisonStatus.POSSIBLE_MISMATCH,
                "截图中找到了对应字段，但没有识别到包装上的完整文字，请人工确认。"
            )
        } else {
            notRecognized(item, "截图中没有识别到这一字段。")
        }
    }

    private fun compareModel(
        item: VerificationChecklistItem,
        text: String
    ): OfficialScreenshotComparison {
        val expectedParts = item.expectedValue
            ?.split("/")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        if (expectedParts.isEmpty()) {
            return notRecognized(item, "包装信息中没有型号规格，无法自动比较。")
        }
        val matchedParts = expectedParts.filter { containsNormalized(text, it) }
        if (matchedParts.size == expectedParts.size) {
            return comparison(
                item,
                matchedParts.joinToString(" / "),
                ScreenshotComparisonStatus.MATCHED,
                "截图中的型号规格与包装文字一致。"
            )
        }
        if (matchedParts.isNotEmpty()) {
            return comparison(
                item,
                matchedParts.joinToString(" / "),
                ScreenshotComparisonStatus.NEEDS_CONFIRMATION,
                "只识别到部分型号规格，需要人工确认。"
            )
        }

        val labeledLine = findLabeledLine(text, listOf("型号规格", "规格型号", "型号", "规格"))
        return if (labeledLine != null) {
            comparison(
                item,
                labeledLine,
                ScreenshotComparisonStatus.POSSIBLE_MISMATCH,
                "截图中找到了型号规格字段，但没有识别到包装上的型号，请人工确认。"
            )
        } else {
            notRecognized(item, "截图中没有识别到型号规格。")
        }
    }

    private fun compareScope(
        item: VerificationChecklistItem,
        text: String
    ): OfficialScreenshotComparison {
        val line = findLabeledLine(
            text,
            listOf("适用范围", "预期用途", "主要功能", "保健功能", "功能主治", "适应症")
        )
        return if (line != null) {
            comparison(
                item,
                line,
                ScreenshotComparisonStatus.NEEDS_CONFIRMATION,
                "已识别到登记用途文字，但仍需人工判断是否与广告宣传相符。"
            )
        } else {
            notRecognized(item, "截图中没有识别到登记用途或适用范围。")
        }
    }

    private fun compareRegistrationStatus(
        item: VerificationChecklistItem,
        text: String
    ): OfficialScreenshotComparison {
        val negative = Regex("注销|撤销|失效|过期|暂停|已废止").find(text)?.value
        if (negative != null) {
            return comparison(
                item,
                negative,
                ScreenshotComparisonStatus.POSSIBLE_MISMATCH,
                "截图中出现“$negative”，需要人工核对登记状态和有效期。"
            )
        }

        val positiveLine = text.lineSequence()
            .map(String::trim)
            .firstOrNull { line ->
                Regex("登记状态|注册状态|备案状态|有效期").containsMatchIn(line) &&
                    Regex("有效|正常|在册").containsMatchIn(line)
            }
        if (positiveLine != null) {
            return comparison(
                item,
                positiveLine.take(MAX_DETECTED_TEXT_LENGTH),
                ScreenshotComparisonStatus.MATCHED,
                "截图中识别到正常或有效状态文字，仍应人工确认日期。"
            )
        }

        val statusLine = findLabeledLine(text, listOf("登记状态", "注册状态", "备案状态", "有效期"))
        return if (statusLine != null) {
            comparison(
                item,
                statusLine,
                ScreenshotComparisonStatus.NEEDS_CONFIRMATION,
                "已识别到状态相关文字，请人工确认是否仍在有效期内。"
            )
        } else {
            notRecognized(item, "截图中没有识别到登记状态或有效期。")
        }
    }

    private fun comparison(
        item: VerificationChecklistItem,
        detectedText: String?,
        status: ScreenshotComparisonStatus,
        explanation: String
    ) = OfficialScreenshotComparison(
        field = item.field,
        expectedValue = item.expectedValue,
        detectedText = detectedText?.trim()?.take(MAX_DETECTED_TEXT_LENGTH),
        status = status,
        explanation = explanation
    )

    private fun notRecognized(
        item: VerificationChecklistItem,
        explanation: String
    ) = comparison(
        item,
        detectedText = null,
        status = ScreenshotComparisonStatus.NOT_RECOGNIZED,
        explanation = explanation
    )

    private fun findLabeledLine(text: String, labels: List<String>): String? = text
        .lineSequence()
        .map(String::trim)
        .firstOrNull { line ->
            line.isNotBlank() && labels.any { label -> line.contains(label, ignoreCase = true) }
        }
        ?.take(MAX_DETECTED_TEXT_LENGTH)

    private fun containsNormalized(text: String, expected: String): Boolean {
        val normalizedExpected = normalize(expected)
        return normalizedExpected.length >= 2 && normalize(text).contains(normalizedExpected)
    }

    private fun normalize(value: String): String = value
        .uppercase()
        .replace(Regex("[\\s:：()（）\\-—_/·.,，。]"), "")

    private const val MAX_DETECTED_TEXT_LENGTH = 80
}
