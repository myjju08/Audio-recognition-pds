package com.earbrief.app.domain.model

data class CustomKeyword(
    val id: String,
    val keyword: String,
    val pattern: String? = null,
    val actionType: KeywordActionType = KeywordActionType.NOTIFY,
    val responseTemplate: String? = null,
    val isActive: Boolean = true,
    val triggerCount: Int = 0,
    val lastTriggeredMs: Long? = null,
    val createdAtMs: Long = System.currentTimeMillis()
)

enum class KeywordActionType {
    SHOPPING_LIST, BOOKMARK, PAUSE, NOTIFY, CUSTOM
}
