package com.example.data.model

import androidx.room.Entity

@Entity(
    tableName = "saved_products",
    primaryKeys = ["barcode", "userEmail"]
)
data class SavedProduct(
    val barcode: String,
    val userEmail: String = "",
    val name: String,
    val brand: String,
    val category: String,
    val imageUrl: String,
    val ingredients: String, // Comma separated or text
    val nutritionFacts: String, // Formatted text like "Sugar: 10g..."
    val healthScore: Int, // 0 to 100
    val healthExplanation: String, // Short explanation for the score
    val sugarScore: Int = 0, // Nutri-Score: şeker riski puanı 0-100 (yüksek=az şeker)
    val additiveScore: Int = 0, // Nutri-Score: katkı yoğunluk puanı 0-100 (yüksek=az katkı)
    val nutritionScore: Int = 0, // Nutri-Score: besin kalitesi puanı 0-100
    val scoreTags: String = "", // Değer etiketleri, "|" ile ayrılmış
    val eCodes: String, // Comma separated list of E-codes present
    val aiVerdict: String, // JSON/text of AI evaluation
    val scannedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val grammage: String = "", // e.g. "250g" or "330ml"
    val nutrition: String = "", // Yapısal besin (JSON: {energy,sugar,fat,satFat,salt,protein,carb,fiber})
    val allergens: String = "", // Alerjenler, "|" ile ayrılmış (Gluten|Süt|...)
    val nutriGrade: String = "", // Nutri-Score harfi (A-E) veya boş
    val isHalal: Boolean = true, // Halal suitability (halalStatus'tan türetilir, geriye uyumluluk)
    val halalStatus: String = "supheli", // muhtemel_helal | supheli | helal_degil | sertifikali
    val halalExplanation: String = "", // Detailed analysis of halal status
    val userNotes: String = "",
    val userFeedback: String = "",
    val price: Double? = null,
    val status: String = "pending",
    val scanCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
