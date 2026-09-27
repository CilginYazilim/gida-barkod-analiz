package com.example.data.repository

import com.example.data.local.BlacklistDao
import com.example.data.local.SavedProductDao
import com.example.data.model.BlacklistItem
import com.example.data.model.SavedProduct
import com.example.data.network.GeminiApiHelper
import com.example.data.network.OFFRetrofitClient
import com.example.data.network.OFFProduct
import com.example.data.network.Ci4ApiService
import com.example.data.network.Ci4RetrofitClient
import com.example.data.network.Ci4LoginRequest
import com.example.data.network.Ci4RegisterRequest
import com.example.data.network.Ci4GoogleTokenRequest
import com.example.data.network.Ci4AuthResponse
import com.example.data.network.Ci4Product
import com.example.data.network.Ci4BlacklistAddRequest
import com.example.data.network.Ci4FavoriteToggleRequest
import com.example.data.network.Ci4PriceReportRequest
import com.example.data.network.Ci4ProfileUpdateRequest
import com.example.data.network.Ci4ChangePasswordRequest
import com.example.data.network.toCi4Product
import com.example.data.network.toSavedProduct
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProductRepository(
    private val savedProductDao: SavedProductDao,
    private val blacklistDao: BlacklistDao,
    private val ci4Api: Ci4ApiService = Ci4RetrofitClient.service
) {
    fun getHistoryProducts(userEmail: String): Flow<List<SavedProduct>> =
        savedProductDao.getHistoryProducts(userEmail)

    fun getFavoriteProducts(userEmail: String): Flow<List<SavedProduct>> =
        savedProductDao.getFavoriteProducts(userEmail)

    fun getBlacklistItems(userEmail: String): Flow<List<BlacklistItem>> =
        blacklistDao.getAllItems(userEmail)

    suspend fun getProductByBarcode(barcode: String, userEmail: String): SavedProduct? =
        savedProductDao.getProductByBarcode(barcode, userEmail)

    // =====================================================================
    //  CI4 Paylaşılan Katalog Senkronizasyonu
    // =====================================================================

    /**
     * Merkezi katalogda barkodu arar. Bulunursa Room DB'ye yazar.
     * Offline-first: herhangi bir hata akışı bozmaz.
     */
    suspend fun fetchFromCatalog(barcode: String, userEmail: String): SavedProduct? = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isConfigured()) return@withContext null
        try {
            val resp = ci4Api.getByBarcode(barcode.trim())
            val product = resp.body()?.data
            if (resp.isSuccessful && product != null) {
                val fromServer = product.toSavedProduct(userEmail)
                val existing = savedProductDao.getProductByBarcode(barcode.trim(), userEmail)
                val merged = if (existing != null) {
                    fromServer.copy(
                        isFavorite   = existing.isFavorite,
                        userNotes    = existing.userNotes,
                        userFeedback = existing.userFeedback,
                        createdAt    = existing.createdAt,
                        imageUrl     = fromServer.imageUrl.ifEmpty { existing.imageUrl }
                    )
                } else fromServer

                savedProductDao.insertProduct(merged)
                merged
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Yerel ürünü merkezi kataloga gönderir (upsert).
     * Kullanıcı giriş yapmışsa JWT gönderilir → sunucu created_by'a kullanıcı id'sini yazar.
     */
    suspend fun pushToCatalog(product: SavedProduct, accessToken: String? = null) = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isConfigured()) return@withContext
        val bc = product.barcode.trim()
        if (bc.isEmpty() || bc.startsWith("RESIM_") || bc.startsWith("IMAGE_")) return@withContext
        try {
            ci4Api.sync(
                userToken = accessToken?.takeIf { it.isNotBlank() },
                body      = product.toCi4Product()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // =====================================================================
    //  Barcode Analiz
    // =====================================================================

    suspend fun analyzeAndSaveBarcode(
        barcode: String,
        userEmail: String,
        providedOffProduct: OFFProduct? = null,
        accessToken: String? = null
    ): SavedProduct? = withContext(Dispatchers.IO) {
        val sanitizedBarcode = barcode.trim()
        if (sanitizedBarcode.isEmpty()) return@withContext null

        val cachedProduct = savedProductDao.getProductByBarcode(sanitizedBarcode, userEmail)
        if (cachedProduct != null) {
            val updated = cachedProduct.copy(scannedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
            savedProductDao.insertProduct(updated)
            return@withContext updated
        }

        val fromCatalog = fetchFromCatalog(sanitizedBarcode, userEmail)
        if (fromCatalog != null) return@withContext fromCatalog

        val offProduct = providedOffProduct ?: try {
            val response = OFFRetrofitClient.service.getProductInfo(sanitizedBarcode)
            if (response.status == 1) response.product else null
        } catch (e: Exception) { null }

        val analysis = GeminiApiHelper.analyzeBarcode(sanitizedBarcode, offProduct) ?: return@withContext null

        val finalProduct = analysis.toSavedProduct(
            barcode = sanitizedBarcode,
            userEmail = userEmail,
            imageUrl = offProduct?.imageFrontUrl ?: ""
        )

        savedProductDao.insertProduct(finalProduct)
        pushToCatalog(finalProduct, accessToken)
        finalProduct
    }

    suspend fun analyzeAndSaveImage(base64Image: String, mimeType: String, userEmail: String): SavedProduct? = withContext(Dispatchers.IO) {
        val analysis = GeminiApiHelper.analyzeImage(base64Image, mimeType) ?: return@withContext null
        val generatedBarcode = "RESIM_" + (100000..999999).random().toString()

        val finalProduct = analysis.toSavedProduct(barcode = generatedBarcode, userEmail = userEmail)

        savedProductDao.insertProduct(finalProduct)
        finalProduct
    }

    // =====================================================================
    //  Favori Senkronizasyonu
    // =====================================================================

    /**
     * Favoriye ekle/çıkar. Local DB güncellenir; kullanıcı giriş yapmışsa server sync yapılır.
     */
    suspend fun toggleFavorite(barcode: String, userEmail: String, isFavorite: Boolean, accessToken: String? = null) {
        savedProductDao.updateFavoriteStatus(barcode, userEmail, isFavorite)

        if (!accessToken.isNullOrBlank() && Ci4RetrofitClient.isConfigured()) {
            try {
                ci4Api.toggleFavorite(
                    bearer = "Bearer $accessToken",
                    body   = Ci4FavoriteToggleRequest(barcode = barcode)
                )
            } catch (e: Exception) {
                e.printStackTrace() // yutulur; local güncelleme zaten yapıldı
            }
        }
    }

    // =====================================================================
    //  Kara Liste Senkronizasyonu
    // =====================================================================

    suspend fun addToBlacklist(value: String, type: String, reason: String = "", userEmail: String, accessToken: String? = null) {
        blacklistDao.insertItem(BlacklistItem(value = value.trim(), type = type, reason = reason, userEmail = userEmail))

        if (!accessToken.isNullOrBlank() && Ci4RetrofitClient.isConfigured()) {
            try {
                ci4Api.addBlacklist(
                    bearer = "Bearer $accessToken",
                    body   = Ci4BlacklistAddRequest(value = value.trim(), type = blacklistTypeToServer(type), reason = reason.ifBlank { null })
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun removeFromBlacklist(id: Int, userEmail: String, value: String = "", accessToken: String? = null) {
        blacklistDao.deleteItemById(id, userEmail)

        if (value.isNotBlank() && !accessToken.isNullOrBlank() && Ci4RetrofitClient.isConfigured()) {
            try {
                ci4Api.removeBlacklist(bearer = "Bearer $accessToken", value = value.trim())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun removeFromBlacklistByValue(value: String, userEmail: String, accessToken: String? = null) {
        blacklistDao.deleteItemByValue(value, userEmail)

        if (value.isNotBlank() && !accessToken.isNullOrBlank() && Ci4RetrofitClient.isConfigured()) {
            try {
                ci4Api.removeBlacklist(bearer = "Bearer $accessToken", value = value.trim())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // =====================================================================
    //  Fiyat Bildirimi
    // =====================================================================

    suspend fun getPriceHistory(barcode: String, accessToken: String?): com.example.data.network.Ci4PriceHistory? = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isConfigured()) return@withContext null
        try {
            val resp = ci4Api.getPriceHistory(
                barcode = barcode.trim(),
                bearer  = accessToken?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
            )
            if (resp.isSuccessful) resp.body()?.data else null
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }

    suspend fun reportPrice(barcode: String, price: Double, notes: String? = null, accessToken: String? = null): Boolean {
        if (!accessToken.isNullOrBlank() && Ci4RetrofitClient.isConfigured()) {
            try {
                val resp = ci4Api.reportPrice(
                    bearer = "Bearer $accessToken",
                    body   = Ci4PriceReportRequest(barcode = barcode, price = price, notes = notes)
                )
                return resp.isSuccessful
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return false
    }

    // =====================================================================
    //  Ürün Yorumları + Beğeni
    // =====================================================================

    suspend fun getComments(barcode: String, accessToken: String?): List<com.example.data.network.Ci4Comment> = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isConfigured()) return@withContext emptyList()
        try {
            val resp = ci4Api.listComments(
                barcode = barcode.trim(),
                bearer  = accessToken?.takeIf { it.isNotBlank() }?.let { "Bearer $it" }
            )
            if (resp.isSuccessful) resp.body()?.data ?: emptyList() else emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addComment(barcode: String, body: String, rating: Int?, accessToken: String): com.example.data.network.Ci4Comment? = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isConfigured()) return@withContext null
        try {
            val resp = ci4Api.addComment(
                bearer  = "Bearer $accessToken",
                barcode = barcode.trim(),
                body    = com.example.data.network.Ci4CommentCreateRequest(body = body, rating = rating)
            )
            if (resp.isSuccessful || resp.code() == 201) resp.body()?.data else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun toggleCommentLike(commentId: Int, accessToken: String): com.example.data.network.Ci4CommentLikeResult? = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isConfigured()) return@withContext null
        try {
            val resp = ci4Api.likeComment(bearer = "Bearer $accessToken", id = commentId)
            if (resp.isSuccessful) resp.body()?.data else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // =====================================================================
    //  Hata Bildirimi / Düzeltme Talebi
    // =====================================================================

    suspend fun submitCorrection(
        barcode: String,
        field: String,
        currentValue: String?,
        suggestedValue: String?,
        note: String?,
        accessToken: String
    ): Boolean = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isConfigured()) return@withContext false
        try {
            val resp = ci4Api.addCorrection(
                bearer  = "Bearer $accessToken",
                barcode = barcode.trim(),
                body    = com.example.data.network.Ci4CorrectionRequest(
                    field          = field,
                    currentValue   = currentValue?.takeIf { it.isNotBlank() },
                    suggestedValue = suggestedValue?.takeIf { it.isNotBlank() },
                    note           = note?.takeIf { it.isNotBlank() }
                )
            )
            resp.isSuccessful || resp.code() == 201
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // =====================================================================
    //  AI Varsayılan Sorular + CMS Sayfaları
    // =====================================================================

    suspend fun getAiQuestions(accessToken: String?): List<com.example.data.network.Ci4AiQuestion> = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isConfigured()) return@withContext emptyList()
        try {
            val resp = ci4Api.listAiQuestions(accessToken?.takeIf { it.isNotBlank() }?.let { "Bearer $it" })
            if (resp.isSuccessful) resp.body()?.data ?: emptyList() else emptyList()
        } catch (e: Exception) {
            e.printStackTrace(); emptyList()
        }
    }

    suspend fun suggestAiQuestion(question: String, accessToken: String): Boolean = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isConfigured() || accessToken.isBlank()) return@withContext false
        try {
            val resp = ci4Api.suggestAiQuestion("Bearer $accessToken", com.example.data.network.Ci4AiQuestionRequest(question.trim()))
            resp.isSuccessful || resp.code() == 201
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    suspend fun deleteAiQuestion(id: Int, accessToken: String): Boolean = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isConfigured() || accessToken.isBlank()) return@withContext false
        try {
            ci4Api.deleteAiQuestion("Bearer $accessToken", id).isSuccessful
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    suspend fun getPages(accessToken: String?): List<com.example.data.network.Ci4Page> = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isConfigured()) return@withContext emptyList()
        try {
            val resp = ci4Api.listPages(accessToken?.takeIf { it.isNotBlank() }?.let { "Bearer $it" })
            if (resp.isSuccessful) resp.body()?.data ?: emptyList() else emptyList()
        } catch (e: Exception) {
            e.printStackTrace(); emptyList()
        }
    }

    suspend fun getPage(slug: String, accessToken: String?): com.example.data.network.Ci4Page? = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isConfigured()) return@withContext null
        try {
            val resp = ci4Api.getPage(slug.trim(), accessToken?.takeIf { it.isNotBlank() }?.let { "Bearer $it" })
            if (resp.isSuccessful) resp.body()?.data else null
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }

    suspend fun getEcode(code: String): com.example.data.network.Ci4Ecode? = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isConfigured()) return@withContext null
        try {
            val resp = ci4Api.getEcode(code.trim())
            if (resp.isSuccessful) resp.body()?.data else null
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }

    /** E-Kod sözlüğü listesi (opsiyonel arama). Sözlük ekranı için. */
    suspend fun listEcodes(query: String? = null): List<com.example.data.network.Ci4Ecode> = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isConfigured()) return@withContext emptyList()
        try {
            val resp = ci4Api.getEcodes(query?.trim().takeIf { !it.isNullOrEmpty() })
            if (resp.isSuccessful) resp.body()?.data ?: emptyList() else emptyList()
        } catch (e: Exception) {
            e.printStackTrace(); emptyList()
        }
    }

    // =====================================================================
    //  Profil Güncelleme
    // =====================================================================

    suspend fun updateProfileOnServer(firstName: String, lastName: String, accessToken: String?): Boolean {
        if (accessToken.isNullOrBlank() || !Ci4RetrofitClient.isConfigured()) return false
        return try {
            val resp = ci4Api.updateProfile(
                bearer = "Bearer $accessToken",
                body   = Ci4ProfileUpdateRequest(firstName = firstName, lastName = lastName)
            )
            resp.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Şifre değiştirir. Başarılıysa (null), değilse hata mesajı döner. */
    suspend fun changePasswordOnServer(
        currentPassword: String, newPassword: String, accessToken: String?
    ): String? = withContext(Dispatchers.IO) {
        if (accessToken.isNullOrBlank() || !Ci4RetrofitClient.isConfigured()) return@withContext "Oturum bulunamadı"
        try {
            val resp = ci4Api.changePassword(
                bearer = "Bearer $accessToken",
                body   = Ci4ChangePasswordRequest(
                    currentPassword = currentPassword,
                    password = newPassword,
                    passwordConfirmation = newPassword
                )
            )
            if (resp.isSuccessful) null
            else {
                val errorBody = resp.errorBody()?.string() ?: ""
                try { org.json.JSONObject(errorBody).optString("message", "Şifre değiştirilemedi") }
                catch (_: Exception) { "Şifre değiştirilemedi (${resp.code()})" }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Bağlantı hatası: ${e.localizedMessage}"
        }
    }

    // =====================================================================
    //  CI4 Auth
    // =====================================================================

    suspend fun loginWithServer(email: String, password: String): Pair<Ci4AuthResponse?, String?> = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isBaseUrlConfigured()) return@withContext Pair(null, "Sunucu adresi yapılandırılmamış")
        try {
            val resp = ci4Api.login(Ci4LoginRequest(email = email, password = password))
            if (resp.isSuccessful) {
                Pair(resp.body()?.data, null)
            } else {
                val errorBody = resp.errorBody()?.string() ?: ""
                val msg = try {
                    val json = org.json.JSONObject(errorBody)
                    json.optString("message", "Giriş başarısız (${resp.code()})")
                } catch (_: Exception) { "Giriş başarısız (${resp.code()})" }
                Pair(null, msg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, "Bağlantı hatası: ${e.localizedMessage}")
        }
    }

    suspend fun registerWithServer(email: String, password: String, firstName: String, lastName: String): Pair<Ci4AuthResponse?, String?> = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isBaseUrlConfigured()) return@withContext Pair(null, "Sunucu adresi yapılandırılmamış")
        try {
            val resp = ci4Api.register(Ci4RegisterRequest(email = email, password = password, firstName = firstName.ifBlank { null }, lastName = lastName.ifBlank { null }))
            if (resp.isSuccessful || resp.code() == 201) {
                Pair(resp.body()?.data, null)
            } else {
                val errorBody = resp.errorBody()?.string() ?: ""
                val msg = try {
                    val json = org.json.JSONObject(errorBody)
                    json.optString("message", "Kayıt başarısız (${resp.code()})")
                } catch (_: Exception) { "Kayıt başarısız (${resp.code()})" }
                Pair(null, msg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, "Bağlantı hatası: ${e.localizedMessage}")
        }
    }

    /** Google ID token ile sunucuda giriş yap / kaydol. */
    suspend fun loginWithGoogle(idToken: String): Pair<Ci4AuthResponse?, String?> = withContext(Dispatchers.IO) {
        if (!Ci4RetrofitClient.isBaseUrlConfigured()) return@withContext Pair(null, "Sunucu adresi yapılandırılmamış")
        try {
            val resp = ci4Api.googleToken(Ci4GoogleTokenRequest(idToken = idToken))
            if (resp.isSuccessful || resp.code() == 201) {
                Pair(resp.body()?.data, null)
            } else {
                val errorBody = resp.errorBody()?.string() ?: ""
                val msg = try {
                    org.json.JSONObject(errorBody).optString("message", "Google girişi başarısız")
                } catch (_: Exception) { "Google girişi başarısız (${resp.code()})" }
                Pair(null, msg)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, "Bağlantı hatası: ${e.localizedMessage}")
        }
    }

    // =====================================================================
    //  Yerel DB İşlemleri
    // =====================================================================

    suspend fun updateProductNotes(barcode: String, userEmail: String, notes: String, accessToken: String? = null) {
        savedProductDao.updateProductNotes(barcode, userEmail, notes)
        // Sunucuya kalıcı (oturum varsa): kullanıcıya özel notlar tablosuna upsert.
        if (!accessToken.isNullOrBlank() && Ci4RetrofitClient.isConfigured() &&
            !barcode.startsWith("RESIM_") && !barcode.startsWith("IMAGE_")) {
            try {
                ci4Api.saveUserNote(
                    bearer = "Bearer $accessToken",
                    body   = com.example.data.network.Ci4UserNoteRequest(barcode = barcode.trim(), notes = notes)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun updateProductFeedback(barcode: String, userEmail: String, feedback: String) =
        savedProductDao.updateProductFeedback(barcode, userEmail, feedback)

    suspend fun deleteProduct(barcode: String, userEmail: String) =
        savedProductDao.deleteProductByBarcode(barcode, userEmail)

    suspend fun clearHistory(userEmail: String) =
        savedProductDao.clearAllProducts(userEmail)

    suspend fun clearAllUserData(userEmail: String) {
        savedProductDao.clearAllProducts(userEmail)
        blacklistDao.clearAllBlacklist(userEmail)
    }

    /**
     * Oturum açıldığında misafir (oturumsuz) yerel verileri hesap e-postasına taşır;
     * böylece giriş öncesi taramalar/favoriler/engeller hesapta görünür. Çakışan kayıtlar
     * atlanır ve artık misafir e-postasında kalan kalıntılar temizlenir.
     */
    suspend fun migrateGuestData(fromEmail: String, toEmail: String) = withContext(Dispatchers.IO) {
        if (fromEmail == toEmail) return@withContext
        savedProductDao.reassignEmail(fromEmail, toEmail)
        savedProductDao.clearAllProducts(fromEmail)   // çakışıp taşınamayan kalıntıları sil
        blacklistDao.reassignEmail(fromEmail, toEmail)
        blacklistDao.clearAllBlacklist(fromEmail)
    }

    /**
     * Oturum açan kullanıcının sunucudaki verilerini (taramalar=created_by, favoriler,
     * kara liste) yerel Room'a çeker. ADDITIVE: yerel veriyi silmez, üstüne ekler/birleştirir.
     * Böylece taramalarım/favorilerim/engellerim sunucu DB'sini yansıtır.
     */
    suspend fun syncUserDataFromServer(userEmail: String, accessToken: String?) = withContext(Dispatchers.IO) {
        if (accessToken.isNullOrBlank() || !Ci4RetrofitClient.isConfigured() || userEmail.isBlank()) return@withContext
        val bearer = "Bearer $accessToken"

        // 1) Kullanıcının eklediği ürünler (taramalar) → created_by
        try {
            val resp = ci4Api.getUserProducts(bearer)
            if (resp.isSuccessful) resp.body()?.data?.forEach { upsertServerProduct(it, userEmail, forceFavorite = false) }
        } catch (e: Exception) { e.printStackTrace() }

        // 2) Favoriler → isFavorite=true
        try {
            val resp = ci4Api.getUserFavorites(bearer)
            if (resp.isSuccessful) resp.body()?.data?.forEach { upsertServerProduct(it, userEmail, forceFavorite = true) }
        } catch (e: Exception) { e.printStackTrace() }

        // 3) Kara liste (çift kayıt önlenir)
        try {
            val resp = ci4Api.getUserBlacklist(bearer)
            if (resp.isSuccessful) {
                val existing = blacklistDao.getBlacklistValuesLower(userEmail).toSet()
                resp.body()?.data?.forEach { item ->
                    val v = item.value.trim()
                    if (v.isNotEmpty() && v.lowercase() !in existing) {
                        blacklistDao.insertItem(
                            BlacklistItem(
                                value = v,
                                type = (item.type ?: "INGREDIENT").uppercase(),
                                reason = item.reason ?: "",
                                userEmail = userEmail
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    /** Sunucudan gelen ürünü yerel kayıtla birleştirerek upsert eder. */
    private suspend fun upsertServerProduct(p: Ci4Product, userEmail: String, forceFavorite: Boolean) {
        val bc = p.barcode.trim()
        if (bc.isEmpty()) return
        val existing = savedProductDao.getProductByBarcode(bc, userEmail)
        val base = p.toSavedProduct(userEmail)
        val merged = if (existing != null) {
            base.copy(
                isFavorite   = forceFavorite || existing.isFavorite,
                userNotes    = existing.userNotes,
                userFeedback = existing.userFeedback,
                createdAt    = existing.createdAt,
                imageUrl     = base.imageUrl.ifEmpty { existing.imageUrl }
            )
        } else {
            base.copy(isFavorite = forceFavorite)
        }
        savedProductDao.insertProduct(merged)
    }

    suspend fun updateBlacklistReason(
        id: Int,
        userEmail: String,
        value: String = "",
        type: String = "INGREDIENT",
        reason: String,
        accessToken: String? = null
    ) {
        blacklistDao.updateItemReason(id, userEmail, reason)

        // Sunucuda upsert: addBlacklist mevcut (user,value) için reason/type günceller.
        if (value.isNotBlank() && !accessToken.isNullOrBlank() && Ci4RetrofitClient.isConfigured()) {
            try {
                ci4Api.addBlacklist(
                    bearer = "Bearer $accessToken",
                    body   = Ci4BlacklistAddRequest(value = value.trim(), type = blacklistTypeToServer(type), reason = reason.ifBlank { null })
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

/** Android kara liste tiplerini backend enum'una eşler (ingredient|brand|e_code|category). */
private fun blacklistTypeToServer(type: String): String = when (type.uppercase()) {
    "ADDITIVE" -> "e_code"
    "BOYCOTT", "BRAND" -> "brand"
    "CATEGORY" -> "category"
    else -> "ingredient"
}

/**
 * Gemini AI analiz sonucunu yerel [SavedProduct]'a dönüştürür.
 * Barkod ve görsel tarama akışlarının ortak eşleme mantığı (tekrar önlenir).
 */
internal fun com.example.data.network.GeminiProductAnalysis.toSavedProduct(
    barcode: String,
    userEmail: String,
    imageUrl: String = "",
    nameOverride: String? = null,
    brandOverride: String? = null,
    categoryOverride: String? = null,
    grammageOverride: String? = null,
    price: Double? = null
): SavedProduct {
    val now = System.currentTimeMillis()

    // Kullanıcı (ConfirmWeb) değerleri varsa AI önerisini geçersiz kılar.
    val effName = nameOverride?.takeIf { it.isNotBlank() } ?: name
    val effBrand = brandOverride?.takeIf { it.isNotBlank() } ?: brand
    val effGrammage = grammageOverride?.takeIf { it.isNotBlank() } ?: grammage

    // Kategori sabit taksonomiye kanonikleştirilir.
    val canonicalCategory = com.example.data.FoodCategories.canonical(
        categoryOverride?.takeIf { it.isNotBlank() } ?: category
    )

    // Yapısal besin → resmi Nutri-Score ile DETERMİNİSTİK skorlar (AI yedek).
    val nv = nutrition
    val calc = nv?.let {
        com.example.data.NutriScoreCalculator.compute(
            com.example.data.NutriScoreCalculator.Input(
                energyKcal = it.energy, sugar = it.sugar, satFat = it.satFat,
                salt = it.salt, protein = it.protein, fiber = it.fiber
            )
        )
    }
    val finalHealth = calc?.healthScore ?: healthScore
    val finalSugar = calc?.sugarScore ?: sugarScore
    val finalNutritionScore = calc?.nutritionScore ?: nutritionScore
    val finalAdditive = if (eCodesList.isNotEmpty()) {
        var s = 100 - eCodesList.size * 8
        eCodesList.forEach {
            when {
                it.risk.contains("Yüksek", true) -> s -= 12
                it.risk.contains("Orta", true) -> s -= 6
            }
        }
        s.coerceIn(15, 100)
    } else additiveScore
    val gradeStr = calc?.grade?.toString() ?: ""

    val nutritionJson = nv?.let {
        com.example.data.network.GeminiRetrofitClient.moshi
            .adapter(com.example.data.network.NutritionValues::class.java).toJson(it)
    } ?: ""

    // 4 durumlu helal'den geriye uyumlu isHalal türetilir.
    val derivedIsHalal = halalStatus == "muhtemel_helal" || halalStatus == "sertifikali"

    return SavedProduct(
        barcode = barcode,
        userEmail = userEmail,
        name = effName,
        brand = effBrand,
        category = canonicalCategory,
        imageUrl = imageUrl,
        ingredients = ingredients,
        nutritionFacts = nutritionFacts,
        nutrition = nutritionJson,
        allergens = allergens.joinToString("|"),
        nutriGrade = gradeStr,
        healthScore = finalHealth,
        healthExplanation = healthExplanation,
        sugarScore = finalSugar,
        additiveScore = finalAdditive,
        nutritionScore = finalNutritionScore,
        scoreTags = scoreTags.joinToString("|"),
        eCodes = eCodesList.joinToString(",") {
            "${it.code}:${it.name}:${it.category}:${it.risk}:${it.description.replace(",", ";")}:${it.whoShouldAvoid.replace(",", ";")}"
        },
        aiVerdict = com.example.data.network.GeminiRetrofitClient.moshi
            .adapter(com.example.data.network.GeminiProductAnalysis::class.java).toJson(this),
        scannedAt = now,
        isFavorite = false,
        grammage = effGrammage,
        isHalal = derivedIsHalal,
        halalStatus = halalStatus,
        halalExplanation = halalStatusExplanation,
        userNotes = "",
        userFeedback = "",
        price = price,
        createdAt = now,
        updatedAt = now
    )
}
