package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blacklist")
data class BlacklistItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val value: String, // e.g., "E951" or "Aspartam" or "Gluten" or "E621"
    val type: String, // "ADDITIVE" or "INGREDIENT" or "BRAND" or "PRODUCT" or "BOYCOTT"
    val reason: String = "", // Custom explanation why this item is blacklisted
    val addedAt: Long = System.currentTimeMillis(),
    val userEmail: String = "" // Under unique user
)
