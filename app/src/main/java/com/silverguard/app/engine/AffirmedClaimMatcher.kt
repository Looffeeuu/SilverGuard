package com.silverguard.app.engine

/**
 * Finds claim wording that the text actually asserts.
 * Compliance statements such as the mandatory health-food label "保健食品不是药物，不能代替药物治疗疾病"
 * mention treatment only to deny it, so a match preceded by a negation cue in the same clause is skipped.
 * Only negation before the claim counts: "不治愈不收费" is still a promise and must stay flagged.
 */
object AffirmedClaimMatcher {

    private const val LOOKBEHIND_CHARS = 12

    // A negation cue followed by at most six characters without crossing a clause boundary.
    // OCR line breaks are removed first because packaging text often wraps mid-sentence.
    private val negationBeforeClaim = Regex(
        "(?:不能|不可|不会|不得|不宜|无法|并非|不是|没有|并不|不具有|不具备|禁止|切勿|请勿)[^，,。；;！!？?]{0,6}$"
    )

    fun findAll(regex: Regex, text: String): Sequence<MatchResult> =
        regex.findAll(text).filterNot { isNegated(text, it.range.first) }

    fun find(regex: Regex, text: String): MatchResult? = findAll(regex, text).firstOrNull()

    fun containsTerm(text: String, term: String): Boolean =
        find(Regex(Regex.escape(term), RegexOption.IGNORE_CASE), text) != null

    private fun isNegated(text: String, claimStart: Int): Boolean {
        val prefix = text
            .substring(maxOf(0, claimStart - LOOKBEHIND_CHARS), claimStart)
            .filterNot(Char::isWhitespace)
        return negationBeforeClaim.containsMatchIn(prefix)
    }
}
