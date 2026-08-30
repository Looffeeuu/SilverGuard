package com.silverguard.app.engine

import com.silverguard.app.model.EcommerceProduct
import com.silverguard.app.model.ProductInfo

object EcommerceProductToProductInfoMapper {

    fun toProductInfo(product: EcommerceProduct): ProductInfo {
        val extractionText = buildString {
            product.title?.let { appendLine(it) }
            product.brand?.let { appendLine("品牌：$it") }
            product.model?.let { appendLine("型号：$it") }
            product.specification?.let { appendLine("规格：$it") }
            product.price?.let { appendLine("价格：$it") }
            product.description?.let { appendLine(it) }
        }.trim()
        val extracted = ProductInfoExtractor.extract(extractionText)

        return ProductInfo(
            name = product.title ?: extracted.name,
            brand = product.brand ?: extracted.brand,
            manufacturer = extracted.manufacturer,
            model = product.model ?: extracted.model,
            specification = product.specification ?: extracted.specification,
            registrationNumber = extracted.registrationNumber,
            price = product.price ?: extracted.price
        )
    }

    fun merge(preferred: ProductInfo, fallback: ProductInfo): ProductInfo = ProductInfo(
        name = preferred.name ?: fallback.name,
        brand = preferred.brand ?: fallback.brand,
        manufacturer = preferred.manufacturer ?: fallback.manufacturer,
        model = preferred.model ?: fallback.model,
        specification = preferred.specification ?: fallback.specification,
        registrationNumber = preferred.registrationNumber ?: fallback.registrationNumber,
        price = preferred.price ?: fallback.price
    )

    fun riskAnalysisText(product: EcommerceProduct): String = listOfNotNull(
        product.title,
        product.description
    ).joinToString("\n")
}
