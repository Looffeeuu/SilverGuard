package com.silverguard.app.model

enum class OfficialSourceRole(val displayName: String) {
    PRIMARY("主要核验入口"),
    SUPPLEMENTARY("辅助核对入口")
}

data class OfficialSource(
    val id: String,
    val name: String,
    val organization: String,
    val url: String,
    val supportedType: RegistrationType,
    val description: String,
    val role: OfficialSourceRole,
    val queryHint: String
)
