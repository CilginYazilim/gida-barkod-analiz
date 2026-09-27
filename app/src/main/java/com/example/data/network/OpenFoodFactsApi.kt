package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OFFNutriments(
    @Json(name = "sugars_100g") val sugars100g: Double? = null,
    @Json(name = "fat_100g") val fat100g: Double? = null,
    @Json(name = "saturated-fat_100g") val saturatedFat100g: Double? = null,
    @Json(name = "salt_100g") val salt100g: Double? = null,
    @Json(name = "proteins_100g") val proteins100g: Double? = null,
    @Json(name = "carbohydrates_100g") val carbohydrates100g: Double? = null,
    @Json(name = "fiber_100g") val fiber100g: Double? = null,
    @Json(name = "energy-kcal_100g") val energyKcal100g: Double? = null
)

@JsonClass(generateAdapter = true)
data class OFFProduct(
    @Json(name = "product_name") val productName: String? = null,
    @Json(name = "product_name_tr") val productNameTr: String? = null,
    @Json(name = "brands") val brands: String? = null,
    @Json(name = "categories") val categories: String? = null,
    @Json(name = "categories_tags_en") val categoriesTagsEn: List<String>? = null,
    @Json(name = "additives_tags") val additivesTags: List<String>? = null,
    @Json(name = "allergens_tags") val allergensTags: List<String>? = null,
    @Json(name = "image_front_url") val imageFrontUrl: String? = null,
    @Json(name = "ingredients_text") val ingredientsText: String? = null,
    @Json(name = "ingredients_text_tr") val ingredientsTextTr: String? = null,
    @Json(name = "nutriments") val nutriments: OFFNutriments? = null,
    @Json(name = "quantity") val quantity: String? = null,
    @Json(name = "product_quantity") val productQuantity: String? = null,
    @Json(name = "serving_size") val servingSize: String? = null
)

@JsonClass(generateAdapter = true)
data class OFFResponse(
    @Json(name = "status") val status: Int,
    @Json(name = "code") val code: String? = null,
    @Json(name = "product") val product: OFFProduct? = null
)

interface OpenFoodFactsService {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductInfo(@Path("barcode") barcode: String): OFFResponse
}

object OFFRetrofitClient {
    private const val BASE_URL = "https://world.openfoodfacts.org/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val service: OpenFoodFactsService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(OpenFoodFactsService::class.java)
    }
}
