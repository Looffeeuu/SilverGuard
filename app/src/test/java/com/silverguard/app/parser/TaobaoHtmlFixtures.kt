package com.silverguard.app.parser

object TaobaoHtmlFixtures {
    val ogOnly = """
        <html><head>
        <meta property="og:title" content="测试理疗仪">
        <meta property="og:image" content="https://img.alicdn.com/test.jpg">
        </head><body></body></html>
    """.trimIndent()

    val jsonLdOnly = """
        <html><head><script type="application/ld+json">
        {
          "@context": "https://schema.org",
          "@type": "Product",
          "name": "JSON商品名称",
          "brand": {"@type": "Brand", "name": "安心牌"},
          "offers": {"@type": "Offer", "price": "2980", "seller": {"name": "安心旗舰店"}}
        }
        </script></head><body></body></html>
    """.trimIndent()

    val jsonLdAndOpenGraph = """
        <html><head>
        <script type="application/ld+json">
        {"@type":"Product","name":"JSON优先标题"}
        </script>
        <meta property="og:title" content="Open Graph标题">
        <title>HTML标题 - 淘宝网</title>
        </head><body></body></html>
    """.trimIndent()

    val titleOnly = """
        <html><head><title>只有标题的商品 - 淘宝网</title></head><body></body></html>
    """.trimIndent()

    val titleAndPrice = """
        <html><head>
        <meta property="og:title" content="测试理疗仪">
        <meta property="product:price:amount" content="2980.00">
        </head><body></body></html>
    """.trimIndent()

    val riskyTitle = """
        <html><head><meta property="og:title" content="七天降血糖不用吃药保健品"></head></html>
    """.trimIndent()

    val loginPage = """
        <html><head><title>会员登录</title></head><body>亲，请登录后继续查看</body></html>
    """.trimIndent()

    val captchaPage = """
        <html><head><title>安全验证</title></head><body>请完成滑动验证码</body></html>
    """.trimIndent()

    val emptyPage = "<html><head></head><body>普通页面</body></html>"

    val shortPageWithStaticProductUrl = """
        <html><head><title>打开淘宝</title></head><body><script>
        var target = "https://item.taobao.com/item.htm?id=1044769261323&spm=abc&wxsign=secret";
        </script></body></html>
    """.trimIndent()
}
