package com.silverguard.app.data

import com.silverguard.app.model.OfficialSource
import com.silverguard.app.model.OfficialSourceRole
import com.silverguard.app.model.RegistrationType

/**
 * Stable official landing pages only. The app does not scrape these pages or claim
 * that opening them is equivalent to completing an official database match.
 */
object OfficialSources {
    val nmpaMedicalDevice = OfficialSource(
        id = "nmpa-medical-device",
        name = "国家药监局医疗器械查询",
        organization = "国家药品监督管理局",
        url = "https://www.nmpa.gov.cn/zwfwqjd/index.html?type=pc",
        supportedType = RegistrationType.MEDICAL_DEVICE,
        description = "查询境内、进口医疗器械注册与备案信息",
        role = OfficialSourceRole.PRIMARY,
        queryHint = "复制编号后，选择境内或进口医疗器械查询，再用注册 / 备案号搜索。"
    )

    val medicalDeviceUdi = OfficialSource(
        id = "nmpa-medical-device-udi",
        name = "国家医疗器械 UDI 查询",
        organization = "国家药品监督管理局",
        url = "https://udi.nmpa.gov.cn/",
        supportedType = RegistrationType.MEDICAL_DEVICE,
        description = "辅助核对医疗器械唯一标识、产品名称、企业和型号",
        role = OfficialSourceRole.SUPPLEMENTARY,
        queryHint = "适合核对包装上的 UDI、产品名称、企业和型号，不替代注册 / 备案信息查询。"
    )

    val nmpaDrug = OfficialSource(
        id = "nmpa-drug",
        name = "国家药监局药品查询",
        organization = "国家药品监督管理局",
        url = "https://www.nmpa.gov.cn/zwfwqjd/index.html?type=pc",
        supportedType = RegistrationType.DRUG_APPROVAL,
        description = "核对国产、进口药品及批准文号信息",
        role = OfficialSourceRole.PRIMARY,
        queryHint = "复制批准文号后，选择国产或进口药品查询，再用批准文号搜索。"
    )

    val healthFood = OfficialSource(
        id = "samr-special-food",
        name = "特殊食品信息查询平台",
        organization = "国家市场监督管理总局",
        url = "https://ypzsx.gsxt.gov.cn/specialfood/",
        supportedType = RegistrationType.HEALTH_FOOD,
        description = "查询保健食品注册、备案及产品信息",
        role = OfficialSourceRole.PRIMARY,
        queryHint = "复制注册 / 备案号后，在特殊食品产品查询中核对产品和企业信息。"
    )

    val samrService = OfficialSource(
        id = "samr-service",
        name = "市场监管总局政务服务平台",
        organization = "国家市场监督管理总局",
        url = "https://zwfw.samr.gov.cn/",
        supportedType = RegistrationType.UNKNOWN,
        description = "查询特殊食品、广告、召回和市场监管公开信息",
        role = OfficialSourceRole.SUPPLEMENTARY,
        queryHint = "当前编号类型不明确，请先核对商品类别，再选择相应的官方查询服务。"
    )

    val nmpaCosmetic = OfficialSource(
        id = "nmpa-cosmetic",
        name = "国家药监局化妆品查询",
        organization = "国家药品监督管理局",
        url = "https://www.nmpa.gov.cn/zwfwqjd/index.html?type=pc",
        supportedType = RegistrationType.COSMETIC,
        description = "查询特殊化妆品注册及相关备案信息",
        role = OfficialSourceRole.PRIMARY,
        queryHint = "复制编号后，选择特殊化妆品注册信息查询，再核对产品和注册人。"
    )

    val all: List<OfficialSource> = listOf(
        nmpaMedicalDevice,
        medicalDeviceUdi,
        nmpaDrug,
        healthFood,
        samrService,
        nmpaCosmetic
    )

    fun forType(type: RegistrationType): List<OfficialSource> = when (type) {
        RegistrationType.MEDICAL_DEVICE -> listOf(nmpaMedicalDevice, medicalDeviceUdi)
        RegistrationType.DRUG_APPROVAL -> listOf(nmpaDrug)
        RegistrationType.HEALTH_FOOD -> listOf(healthFood)
        RegistrationType.COSMETIC -> listOf(nmpaCosmetic)
        RegistrationType.UNKNOWN -> listOf(samrService)
    }
}
