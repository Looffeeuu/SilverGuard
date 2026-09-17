package com.silverguard.app.data

import com.silverguard.app.model.RiskCase
import com.silverguard.app.model.RiskCaseKind

object RiskCaseRepository {
    const val REVIEWED_ON = "2026-09-06"
    private const val CASES_URL = "https://www.samr.gov.cn/xw/zj/art/2025/art_a1f4895b3b3648669587a87afb28c435.html"
    private const val CASES_TITLE = "市场监管总局公布七起老年人药品、保健品虚假宣传典型案例"
    val cases = listOf(
        RiskCase(
            "health-disease-claims", "保健食品被宣传成治病产品", RiskCaseKind.PUBLISHED_CASE,
            "总局公布的四川威远案例中，经营者以免费体验和宣传视频推销保健食品，声称能治疗多种疾病，受到市场监管部门查处。",
            "核对登记用途和功效依据，不要根据商品广告自行停药。",
            "国家市场监督管理总局", CASES_TITLE, CASES_URL, "2025-10-29",
            listOf(listOf("保健食品", "保健品", "胶囊"), listOf("治疗", "治愈", "根治", "降血糖", "不用吃药", "抗癌"))
        ),
        RiskCase(
            "gift-private-sales", "低价礼品引流到群内销售", RiskCaseKind.PUBLISHED_CASE,
            "总局公布的北京平谷案例中，经营者用低价鸡蛋吸引老人入群，随后开展直播销售，并出现夸大宣传及个人信息收集问题。",
            "先核对卖家信息，保留广告和付款凭证，不因领取礼品急着付款。",
            "国家市场监督管理总局", CASES_TITLE, CASES_URL, "2025-10-29",
            listOf(listOf("鸡蛋", "免费礼品", "免费领取", "送礼品"), listOf("加微信", "微信群", "进群", "直播"))
        ),
        RiskCase(
            "health-group-reviews", "养生课与群内好评诱导下单", RiskCaseKind.PUBLISHED_CASE,
            "总局公布的安徽滁州案例中，经营者通过健康讲座引流入群，夸大药品效果，客服还冒充买家发布好评来促成购买。",
            "群内好评不能替代证据，向商家索要完整包装资料和适用说明。",
            "国家市场监督管理总局", CASES_TITLE, CASES_URL, "2025-10-29",
            listOf(listOf("养生", "健康讲座", "保健", "药品"), listOf("延长寿命", "永葆青春", "群友推荐", "效果非常好"))
        ),
        RiskCase(
            "health-urgency", "健康直播间中的权威背书和催付款", RiskCaseKind.CONSUMER_NOTICE,
            "总局与中消协的联合提示提醒消费者，留意私域健康直播中的专家背书、功效承诺和限时优惠，核查资质并保留交易证据。",
            "先看清商品和卖家信息，给自己和家人留出核对时间。",
            "国家市场监督管理总局、中国消费者协会",
            "关于防范私域直播间老年人药品保健品消费风险提示",
            "https://www.samr.gov.cn/xw/zj/art/2025/art_096968970c684f76bcdf578a00233e5c.html", "2025-07-02",
            listOf(listOf("保健", "养生", "理疗", "药品"), listOf("专家推荐", "教授推荐", "仅限今天", "限时秒杀", "最后优惠"))
        ),
        RiskCase(
            "pension-returns", "面向老人的保本高收益话术", RiskCaseKind.CONSUMER_NOTICE,
            "湖北金融监管局提示，有推销者淡化投资风险，以保本或零风险高收益话术吸引老年人，可能造成产品与个人承受能力不匹配。",
            "先核对机构和产品信息，了解资金去向与可能损失，不要被收益承诺催着转账。",
            "国家金融监督管理总局湖北监管局", "关于老年群体投资理财适当性的风险提示",
            "https://www.nfra.gov.cn/branch/hubei/view/pages/common/ItemDetail.html?docId=1251457&itemId=1414", "2026-03-06",
            listOf(listOf("投资", "理财", "养老", "退休赚钱"), listOf("保本", "稳赚", "零风险", "高收益", "月入过万"))
        )
    )
}
