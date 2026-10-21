package com.silverguard.app.engine

/**
 * Finds claim wording that the text actually asserts.
 * Compliance statements such as the mandatory health-food label "保健食品不是药物，不能代替药物治疗疾病"
 * mention treatment only to deny it, so a match preceded by a negation cue in the same clause is skipped.
 * Only negation before the claim counts: "不治愈不收费" is still a promise and must stay flagged.
 * Warnings that users paste, such as "承诺保本高收益的都要警惕", quote scam wording without asserting it,
 * so a match in a clause that also carries a warning cue is skipped as well.
 */
object AffirmedClaimMatcher {

    private const val LOOKBEHIND_CHARS = 12
    private const val CLAUSE_BREAKS = "，,。；;！!？?\n"

    // A negation cue followed by at most six characters without crossing a clause boundary.
    // OCR line breaks are removed first because packaging text often wraps mid-sentence.
    private val negationBeforeClaim = Regex(
        "(?:不能|不可|不会|不得|不宜|不要|千万别|无法|并非|不是|没有|并不|不具有|不具备|禁止|切勿|请勿)[^，,。；;！!？?]{0,6}$"
    )

    // Ambiguous words such as "小心" and "当心" ("当心血管…") are left out on purpose.
    private val warningCue = Regex("警惕|谨防|提防|留心|防范|别信|不要相信|骗局|诈骗|陷阱|套路")

    fun findAll(regex: Regex, text: String): Sequence<MatchResult> =
        regex.findAll(text).filterNot { isNegated(text, it.range.first) || isInWarningClause(text, it.range) }

    fun find(regex: Regex, text: String): MatchResult? = findAll(regex, text).firstOrNull()

    fun containsTerm(text: String, term: String): Boolean =
        find(Regex(Regex.escape(term), RegexOption.IGNORE_CASE), text) != null

    private fun isNegated(text: String, claimStart: Int): Boolean {
        val prefix = text
            .substring(maxOf(0, claimStart - LOOKBEHIND_CHARS), claimStart)
            .filterNot(Char::isWhitespace)
        return negationBeforeClaim.containsMatchIn(prefix)
    }

    private fun isInWarningClause(text: String, claim: IntRange): Boolean {
        val start = text.lastIndexOfAny(CLAUSE_BREAKS.toCharArray(), claim.first - 1) + 1
        val end = text.indexOfAny(CLAUSE_BREAKS.toCharArray(), claim.last + 1).let { if (it < 0) text.length else it }
        return warningCue.containsMatchIn(text.substring(start, end))
    }
}
