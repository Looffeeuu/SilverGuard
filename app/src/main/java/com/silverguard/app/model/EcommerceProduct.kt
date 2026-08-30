package com.silverguard.app.model

data class EcommerceProduct(
    val platform: EcommercePlatform,
    val productId: String?,
    val sourceUrl: String,
    val canonicalUrl: String? = null,
    val title: String? = null,
    val price: String? = null,
    val originalPrice: String? = null,
    val shopName: String? = null,
    val brand: String? = null,
    val sellerName: String? = null,
    val model: String? = null,
    val specification: String? = null,
    val images: List<String> = emptyList(),
    val description: String? = null,
    val parseStatus: ProductDetailParseStatus = ProductDetailParseStatus.NOT_STARTED,
    val fields: List<EcommerceProductField> = emptyList(),
    val fetchedAt: Long? = null,
    val message: String = "尚未读取商品页面"
)
