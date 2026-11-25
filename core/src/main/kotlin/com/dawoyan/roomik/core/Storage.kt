package com.dawoyan.roomik.core

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object PlanJson {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true; coerceInputValues = true }

    fun encode(plan: Plan): String = json.encodeToString(plan)
    fun decode(text: String): Plan = json.decodeFromString(Plan.serializer(), text)
}

/** A 3D model kept in the library of 3D assets (brought from Idea 3D or a file), to put on any plan and floor. */
@kotlinx.serialization.Serializable
data class Asset(val id: String, val name: String, val parts: List<ModelPart>, val updatedAt: Long = 0)

object AssetJson {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; coerceInputValues = true }
    fun encode(a: Asset): String = json.encodeToString(Asset.serializer(), a)
    fun decode(text: String): Asset = json.decodeFromString(Asset.serializer(), text)
}
