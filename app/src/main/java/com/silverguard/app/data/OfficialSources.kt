package com.silverguard.app.data

import com.silverguard.app.model.OfficialSource

object OfficialSources {
    val nmpa = OfficialSource(
        name = "国家药监局",
        description = "查询药品、医疗器械等监管信息",
        url = "https://www.nmpa.gov.cn/zwfwqjd/index.html?type=pc"
    )

    val udi = OfficialSource(
        name = "医疗器械 UDI",
        description = "核对医疗器械唯一标识与产品信息",
        url = "https://udi.nmpa.gov.cn/"
    )

    val samr = OfficialSource(
        name = "市场监管总局",
        description = "查询特殊食品、抽检、召回等公开信息",
        url = "https://zwfw.samr.gov.cn/"
    )

    val standards = OfficialSource(
        name = "全国标准信息公共服务平台",
        description = "查询国家标准、行业标准等",
        url = "https://std.samr.gov.cn/"
    )

    fun forCategory(category: String): List<OfficialSource> = when {
        category.contains("健康") || category.contains("医疗") ->
            listOf(nmpa, udi, samr, standards)
        category.contains("节能") ->
            listOf(samr, standards)
        else ->
            listOf(samr, standards)
    }
}
