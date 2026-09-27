package com.example.data.network

import com.example.BuildConfig
import com.example.data.model.SavedProduct
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// =====================================================================
//  CI4 (erp.cilginyazilim.com) — Paylaşılan Ürün Kataloğu API'si
// =====================================================================

/** Sunucunun tüm yanıtlarını saran zarf: { status, data, meta }. */
@JsonClass(generateAdapter = true)
data class Ci4Envelope<T>(
    val status: String? = null,
    val data: T? = null
)

/** CI4 `gida_barkod_products` satırı (snake_case). Tüm alanlar nullable. */
@JsonClass(generateAdapter = true)
data class Ci4Product(
    val barcode: String,
    val name: String,
    val brand: String? = null,
    val category: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    val grammage: String? = null,
    val ingredients: String? = null,
    @Json(name = "nutrition_facts") val nutritionFacts: String? = null,
    val nutrition: String? = null, // Yapısal besin JSON
    val allergens: String? = null, // Alerjenler (virgül/pipe ile)
    @Json(name = "nutri_grade") val nutriGrade: String? = null,
    @Json(name = "e_codes") val eCodes: String? = null,
    @Json(name = "health_score") val healthScore: Int? = null,
    @Json(name = "health_explanation") val healthExplanation: String? = null,
    @Json(name = "sugar_score") val sugarScore: Int? = null,
    @Json(name = "additive_score") val additiveScore: Int? = null,
    @Json(name = "nutrition_score") val nutritionScore: Int? = null,
    @Json(name = "score_tags") val scoreTags: String? = null,
    @Json(name = "is_vegan") val isVegan: Boolean? = null,
    @Json(name = "is_vegetarian") val isVegetarian: Boolean? = null,
    @Json(name = "is_gluten_free") val isGlutenFree: Boolean? = null,
    @Json(name = "is_lactose_free") val isLactoseFree: Boolean? = null,
    @Json(name = "is_suitable_for_children") val isSuitableForChildren: Boolean? = null,
    @Json(name = "is_suitable_for_diabetics") val isSuitableForDiabetics: Boolean? = null,
    @Json(name = "ai_verdict") val aiVerdict: String? = null,
    @Json(name = "is_halal") val isHalal: Boolean? = null,
    @Json(name = "halal_status") val halalStatus: String? = null,
    @Json(name = "halal_status_explanation") val halalStatusExplanation: String? = null,
    val price: Double? = null,
    val status: String? = null,
    @Json(name = "scan_count") val scanCount: Int? = null
)

// =====================================================================
//  Auth modelleri
// =====================================================================

@JsonClass(generateAdapter = true)
data class Ci4LoginRequest(val email: String, val password: String)

@JsonClass(generateAdapter = true)
data class Ci4RegisterRequest(
    val email: String,
    val password: String,
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "last_name") val lastName: String? = null,
    @Json(name = "app_source") val appSource: String = "gida_barkod"
)

@JsonClass(generateAdapter = true)
data class Ci4GoogleTokenRequest(
    @Json(name = "id_token") val idToken: String? = null,
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "app_source") val appSource: String = "gida_barkod"
)

@JsonClass(generateAdapter = true)
data class Ci4AuthUser(
    val id: Int? = null,
    val username: String? = null,
    val email: String? = null,
    @Json(name = "first_name") val firstName: String? = null,
    @Json(name = "last_name") val lastName: String? = null,
    val role: String? = null,
    val avatar: String? = null
)

@JsonClass(generateAdapter = true)
data class Ci4AuthResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "expires_in") val expiresIn: Int? = null,
    @Json(name = "is_new") val isNew: Boolean? = null,
    val user: Ci4AuthUser? = null
)

// =====================================================================
//  Kullanıcı işlem modelleri
// =====================================================================

@JsonClass(generateAdapter = true)
data class Ci4FavoriteToggleRequest(@Json(name = "barcode") val barcode: String)

@JsonClass(generateAdapter = true)
data class Ci4BlacklistAddRequest(
    val value: String,
    val type: String = "ingredient",
    val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class Ci4UserNoteRequest(
    val barcode: String,
    val notes: String
)

/** Zengin E-Kod sözlüğü kaydı (gida_barkod_ekod). */
@JsonClass(generateAdapter = true)
data class Ci4Ecode(
    val code: String,
    @Json(name = "name_tr") val nameTr: String? = null,
    @Json(name = "name_en") val nameEn: String? = null,
    @Json(name = "class_tr") val classTr: String? = null,
    @Json(name = "what_is_tr") val whatIsTr: String? = null,
    @Json(name = "function_tr") val functionTr: String? = null,
    @Json(name = "source_tr") val sourceTr: String? = null,
    @Json(name = "origin_type") val originType: String? = null,
    @Json(name = "usage_areas_tr") val usageAreasTr: String? = null,
    val risk: String? = null,
    @Json(name = "health_notes_tr") val healthNotesTr: String? = null,
    @Json(name = "who_should_avoid_tr") val whoShouldAvoidTr: String? = null,
    @Json(name = "halal_status") val halalStatus: String? = null,
    @Json(name = "halal_note_tr") val halalNoteTr: String? = null,
    @Json(name = "vegan_suitable") val veganSuitable: String? = null,
    val adi: String? = null,
    @Json(name = "banned_note") val bannedNote: String? = null,
    val ref: String? = null
)

@JsonClass(generateAdapter = true)
data class Ci4PriceReportRequest(
    val barcode: String,
    val price: Double,
    val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class Ci4ProfileUpdateRequest(
    @Json(name = "first_name") val firstName: String,
    @Json(name = "last_name") val lastName: String
)

@JsonClass(generateAdapter = true)
data class Ci4ChangePasswordRequest(
    @Json(name = "current_password") val currentPassword: String,
    @Json(name = "password") val password: String,
    @Json(name = "password_confirmation") val passwordConfirmation: String
)

/** Sunucu kara liste kaydı (gida_barkod_user_blacklist). */
@JsonClass(generateAdapter = true)
data class Ci4BlacklistItem(
    val value: String,
    val type: String? = null,
    val reason: String? = null
)

// =====================================================================
//  Yorum modelleri
// =====================================================================

@JsonClass(generateAdapter = true)
data class Ci4Comment(
    val id: Int,
    @Json(name = "user_name") val userName: String? = null,
    val rating: Int? = null,
    val body: String,
    val status: String? = null,
    @Json(name = "like_count") val likeCount: Int = 0,
    @Json(name = "is_liked") val isLiked: Boolean = false,
    @Json(name = "is_mine") val isMine: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Ci4CommentCreateRequest(
    val body: String,
    val rating: Int? = null
)

@JsonClass(generateAdapter = true)
data class Ci4CommentLikeResult(
    @Json(name = "comment_id") val commentId: Int? = null,
    @Json(name = "is_liked") val isLiked: Boolean = false,
    @Json(name = "like_count") val likeCount: Int = 0
)

// =====================================================================
//  Düzeltme talebi modeli
// =====================================================================

@JsonClass(generateAdapter = true)
data class Ci4CorrectionRequest(
    val field: String,
    @Json(name = "current_value") val currentValue: String? = null,
    @Json(name = "suggested_value") val suggestedValue: String? = null,
    val note: String? = null
)

// =====================================================================
//  AI Varsayılan Sorular + CMS Sayfa modelleri
// =====================================================================

@JsonClass(generateAdapter = true)
data class Ci4AiQuestion(
    val id: Int,
    val question: String,
    val source: String? = null,
    @Json(name = "user_id") val userId: Int? = null
)

@JsonClass(generateAdapter = true)
data class Ci4AiQuestionRequest(
    val question: String
)

@JsonClass(generateAdapter = true)
data class Ci4Page(
    val id: Int? = null,
    val slug: String,
    val title: String,
    val body: String? = null,
    val status: String? = null
)

// =====================================================================
//  Fiyat geçmişi modelleri
// =====================================================================

@JsonClass(generateAdapter = true)
data class Ci4PriceHistoryEntry(
    val price: Double,
    val notes: String? = null,
    @Json(name = "recorded_at") val recordedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Ci4PriceHistory(
    @Json(name = "current_price") val currentPrice: Double? = null,
    val history: List<Ci4PriceHistoryEntry> = emptyList()
)

// =====================================================================
//  API Service Interface
// =====================================================================

interface Ci4ApiService {
    // --- Ürün Kataloğu ---
    @GET("gida-barkod/barcode/{barcode}")
    suspend fun getByBarcode(@Path("barcode") barcode: String): Response<Ci4Envelope<Ci4Product>>

    // sync API Key ile yetkilenir (write:gida-barkod scope). Kullanıcı kimliği,
    // filtreyi JWT/RBAC yoluna sokmadan yalnızca created_by için X-User-Token ile taşınır.
    @POST("gida-barkod/sync")
    suspend fun sync(
        @Header("X-User-Token") userToken: String? = null,
        @Body body: Ci4Product
    ): Response<Ci4Envelope<Ci4Product>>

    // --- Auth ---
    @POST("auth/login")
    suspend fun login(@Body body: Ci4LoginRequest): Response<Ci4Envelope<Ci4AuthResponse>>

    @POST("auth/register")
    suspend fun register(@Body body: Ci4RegisterRequest): Response<Ci4Envelope<Ci4AuthResponse>>

    @POST("auth/google-token")
    suspend fun googleToken(@Body body: Ci4GoogleTokenRequest): Response<Ci4Envelope<Ci4AuthResponse>>

    @GET("auth/me")
    suspend fun me(@Header("Authorization") bearer: String): Response<Ci4Envelope<Ci4AuthUser>>

    @POST("auth/change-password")
    suspend fun changePassword(
        @Header("Authorization") bearer: String,
        @Body body: Ci4ChangePasswordRequest
    ): Response<Ci4Envelope<Map<String, Any>>>

    // --- Kullanıcı Favorileri ---
    @GET("gida-barkod/user/favorites")
    suspend fun getUserFavorites(
        @Header("Authorization") bearer: String
    ): Response<Ci4Envelope<List<Ci4Product>>>

    @POST("gida-barkod/user/favorites/toggle")
    suspend fun toggleFavorite(
        @Header("Authorization") bearer: String,
        @Body body: Ci4FavoriteToggleRequest
    ): Response<Ci4Envelope<Map<String, Any>>>

    // --- Kullanıcının eklediği ürünler (taramalar) ---
    @GET("gida-barkod/user/products")
    suspend fun getUserProducts(
        @Header("Authorization") bearer: String
    ): Response<Ci4Envelope<List<Ci4Product>>>

    // --- Kullanıcı Kara Listesi ---
    @GET("gida-barkod/user/blacklist")
    suspend fun getUserBlacklist(
        @Header("Authorization") bearer: String
    ): Response<Ci4Envelope<List<Ci4BlacklistItem>>>

    @POST("gida-barkod/user/blacklist")
    suspend fun addBlacklist(
        @Header("Authorization") bearer: String,
        @Body body: Ci4BlacklistAddRequest
    ): Response<Ci4Envelope<Map<String, Any>>>

    // --- Kullanıcıya özel kişisel notlar ---
    @POST("gida-barkod/user/notes")
    suspend fun saveUserNote(
        @Header("Authorization") bearer: String,
        @Body body: Ci4UserNoteRequest
    ): Response<Ci4Envelope<Map<String, Any>>>

    // --- E-Kod sözlüğü (public) ---
    @GET("gida-barkod/ecodes/{code}")
    suspend fun getEcode(
        @Path("code") code: String
    ): Response<Ci4Envelope<Ci4Ecode>>

    @GET("gida-barkod/ecodes")
    suspend fun getEcodes(
        @Query("q") query: String? = null
    ): Response<Ci4Envelope<List<Ci4Ecode>>>

    @DELETE("gida-barkod/user/blacklist/{value}")
    suspend fun removeBlacklist(
        @Header("Authorization") bearer: String,
        @Path("value") value: String
    ): Response<Ci4Envelope<Map<String, Any>>>

    // --- Fiyat Bildirimi ---
    @POST("gida-barkod/user/price")
    suspend fun reportPrice(
        @Header("Authorization") bearer: String,
        @Body body: Ci4PriceReportRequest
    ): Response<Ci4Envelope<Map<String, Any>>>

    // --- Profil Güncelleme ---
    @POST("gida-barkod/user/profile")
    suspend fun updateProfile(
        @Header("Authorization") bearer: String,
        @Body body: Ci4ProfileUpdateRequest
    ): Response<Ci4Envelope<Map<String, Any>>>

    // --- Ürün Yorumları + Beğeni ---
    @GET("gida-barkod/product/{barcode}/comments")
    suspend fun listComments(
        @Path("barcode") barcode: String,
        @Header("Authorization") bearer: String? = null
    ): Response<Ci4Envelope<List<Ci4Comment>>>

    @POST("gida-barkod/product/{barcode}/comments")
    suspend fun addComment(
        @Header("Authorization") bearer: String,
        @Path("barcode") barcode: String,
        @Body body: Ci4CommentCreateRequest
    ): Response<Ci4Envelope<Ci4Comment>>

    @POST("gida-barkod/comments/{id}/like")
    suspend fun likeComment(
        @Header("Authorization") bearer: String,
        @Path("id") id: Int
    ): Response<Ci4Envelope<Ci4CommentLikeResult>>

    // --- Hata Bildirimi / Düzeltme Talebi ---
    @POST("gida-barkod/product/{barcode}/corrections")
    suspend fun addCorrection(
        @Header("Authorization") bearer: String,
        @Path("barcode") barcode: String,
        @Body body: Ci4CorrectionRequest
    ): Response<Ci4Envelope<Map<String, Any>>>

    // --- Fiyat Geçmişi ---
    @GET("gida-barkod/product/{barcode}/price-history")
    suspend fun getPriceHistory(
        @Path("barcode") barcode: String,
        @Header("Authorization") bearer: String? = null
    ): Response<Ci4Envelope<Ci4PriceHistory>>

    // --- AI Varsayılan Sorular ---
    @GET("gida-barkod/ai-questions")
    suspend fun listAiQuestions(
        @Header("Authorization") bearer: String? = null
    ): Response<Ci4Envelope<List<Ci4AiQuestion>>>

    @POST("gida-barkod/ai-questions/suggest")
    suspend fun suggestAiQuestion(
        @Header("Authorization") bearer: String,
        @Body body: Ci4AiQuestionRequest
    ): Response<Ci4Envelope<Map<String, Any>>>

    @DELETE("gida-barkod/ai-questions/{id}")
    suspend fun deleteAiQuestion(
        @Header("Authorization") bearer: String,
        @Path("id") id: Int
    ): Response<Ci4Envelope<Map<String, Any>>>

    // --- CMS Sayfaları ---
    @GET("gida-barkod/pages")
    suspend fun listPages(
        @Header("Authorization") bearer: String? = null
    ): Response<Ci4Envelope<List<Ci4Page>>>

    @GET("gida-barkod/pages/{slug}")
    suspend fun getPage(
        @Path("slug") slug: String,
        @Header("Authorization") bearer: String? = null
    ): Response<Ci4Envelope<Ci4Page>>
}

// =====================================================================
//  Retrofit Client
// =====================================================================

object Ci4RetrofitClient {
    private val baseUrl: String =
        BuildConfig.CI4_API_BASE_URL.let { if (it.endsWith("/")) it else "$it/" }

    private val headerInterceptor = Interceptor { chain ->
        val req = chain.request().newBuilder()
            .header("X-API-Key", BuildConfig.CI4_API_KEY)
            .header("X-Client-App", "GidaBarkod-APK")
            .header("Accept", "application/json")
            .build()
        chain.proceed(req)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(headerInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder().build()

    val service: Ci4ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(Ci4ApiService::class.java)
    }

    fun isConfigured(): Boolean =
        BuildConfig.CI4_API_KEY.isNotBlank() &&
        BuildConfig.CI4_API_KEY != "BURAYA_PANELDEN_URETILEN_TOKEN" &&
        BuildConfig.CI4_API_BASE_URL.isNotBlank()

    fun isBaseUrlConfigured(): Boolean =
        BuildConfig.CI4_API_BASE_URL.isNotBlank()
}

// =====================================================================
//  Dönüştürücüler: SavedProduct  <->  Ci4Product
// =====================================================================

fun SavedProduct.toCi4Product(): Ci4Product {
    val analysis: GeminiProductAnalysis? = runCatching {
        GeminiRetrofitClient.moshi.adapter(GeminiProductAnalysis::class.java).fromJson(aiVerdict)
    }.getOrNull()

    return Ci4Product(
        barcode = barcode,
        name = name,
        brand = brand,
        category = category,
        imageUrl = imageUrl,
        grammage = grammage,
        ingredients = ingredients,
        nutritionFacts = nutritionFacts,
        nutrition = nutrition.ifBlank { null },
        allergens = allergens.ifBlank { null },
        nutriGrade = nutriGrade.ifBlank { null },
        eCodes = eCodes,
        healthScore = healthScore,
        healthExplanation = healthExplanation,
        sugarScore = sugarScore.takeIf { it > 0 },
        additiveScore = additiveScore.takeIf { it > 0 },
        nutritionScore = nutritionScore.takeIf { it > 0 },
        scoreTags = scoreTags.ifBlank { null },
        isVegan = analysis?.isVegan,
        isVegetarian = analysis?.isVegetarian,
        isGlutenFree = analysis?.isGlutenFree,
        isLactoseFree = analysis?.isLactoseFree,
        isSuitableForChildren = analysis?.isSuitableForChildren,
        isSuitableForDiabetics = analysis?.isSuitableForDiabetics,
        aiVerdict = aiVerdict,
        isHalal = isHalal,
        halalStatus = halalStatus.ifBlank { null },
        halalStatusExplanation = halalExplanation,
        price = price,
        status = status,
        scanCount = scanCount
    )
}

fun Ci4Product.toSavedProduct(userEmail: String): SavedProduct {
    val now = System.currentTimeMillis()
    return SavedProduct(
        barcode = barcode,
        userEmail = userEmail,
        name = name,
        brand = brand ?: "",
        category = category ?: "",
        imageUrl = imageUrl ?: "",
        ingredients = ingredients ?: "",
        nutritionFacts = nutritionFacts ?: "",
        healthScore = healthScore ?: 0,
        healthExplanation = healthExplanation ?: "",
        sugarScore = sugarScore ?: 0,
        additiveScore = additiveScore ?: 0,
        nutritionScore = nutritionScore ?: 0,
        scoreTags = scoreTags ?: "",
        eCodes = eCodes ?: "",
        aiVerdict = aiVerdict ?: "",
        scannedAt = now,
        isFavorite = false,
        grammage = grammage ?: "",
        nutrition = nutrition ?: "",
        allergens = allergens ?: "",
        nutriGrade = nutriGrade ?: "",
        isHalal = isHalal ?: true,
        halalStatus = halalStatus ?: "supheli",
        halalExplanation = halalStatusExplanation ?: "",
        userNotes = "",
        userFeedback = "",
        price = price,
        status = status ?: "pending",
        scanCount = scanCount ?: 1,
        createdAt = now,
        updatedAt = now
    )
}
