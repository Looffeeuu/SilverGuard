package com.silverguard.app.model

data class OfficialSource(
    val id: String,
    val name: String,
    val organization: String,
    val url: String,
    val supportedType: RegistrationType,
    val description: String
)
