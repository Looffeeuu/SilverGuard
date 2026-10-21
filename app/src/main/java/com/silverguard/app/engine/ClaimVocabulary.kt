package com.silverguard.app.engine

/**
 * Health-claim wording shared by RiskAnalyzer and ClaimRegistrationConflictAnalyzer,
 * so the risk score and the registration-conflict card always agree on what counts as a claim.
 * Every new phrase needs a positive and a hard-negative sample in the evaluation set (docs/RISK_EVALUATION.md).
 */
object ClaimVocabulary {

    /** Claims that a product treats or cures a disease. "治疗仪" is a device name, not a claim. */
    const val DISEASE_CLAIMS =
        "根治|治愈|治疗(?![仪器])|治好|断根|抗癌|消炎|修复疾病|逆转糖尿病|" +
            "降血糖|降血压|降三高|治(?:疗)?高血压|治鼻炎|改善鼻炎|疏通血管|清血管|清除血栓|" +
            "(?:彻底|完全|永久)(?:好了|康复|断根)"

    /** Claims that a product replaces medicine or medical care. "暂停药品销售" is not a claim. */
    const val REPLACEMENT_CLAIMS =
        "不用吃药|无需吃药|不用看医生|替代药物|不打针|无需手术|" +
            "停(?:了|掉)?(?:降压|降糖|降脂|西)?药(?![品店房])|" +
            "胰岛素\\S{0,2}不用(?:再)?打|不用(?:再)?打胰岛素|告别药罐子|比吃药(?:还)?(?:管用|好使|有效)"

    /** Wording that is a normal indication once a drug or medical device registration number is found. */
    const val REGISTERED_INDICATIONS = "治疗|消炎|降血糖|降血压|改善鼻炎|疏通血管"
}
