package com.example.data.local

import androidx.room.*
import com.example.data.model.BlacklistItem
import com.example.data.model.SavedProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedProductDao {
    @Query("SELECT * FROM saved_products WHERE userEmail = :userEmail ORDER BY scannedAt DESC")
    fun getHistoryProducts(userEmail: String): Flow<List<SavedProduct>>

    @Query("SELECT * FROM saved_products WHERE userEmail = :userEmail AND isFavorite = 1 ORDER BY scannedAt DESC")
    fun getFavoriteProducts(userEmail: String): Flow<List<SavedProduct>>

    @Query("SELECT * FROM saved_products WHERE barcode = :barcode AND userEmail = :userEmail LIMIT 1")
    suspend fun getProductByBarcode(barcode: String, userEmail: String): SavedProduct?

    @Query("SELECT * FROM saved_products WHERE barcode = :barcode LIMIT 1")
    suspend fun getProductByBarcodeAny(barcode: String): SavedProduct?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: SavedProduct)

    @Query("UPDATE saved_products SET isFavorite = :isFavorite WHERE barcode = :barcode AND userEmail = :userEmail")
    suspend fun updateFavoriteStatus(barcode: String, userEmail: String, isFavorite: Boolean)

    @Query("UPDATE saved_products SET userNotes = :userNotes WHERE barcode = :barcode AND userEmail = :userEmail")
    suspend fun updateProductNotes(barcode: String, userEmail: String, userNotes: String)

    @Query("UPDATE saved_products SET userFeedback = :userFeedback WHERE barcode = :barcode AND userEmail = :userEmail")
    suspend fun updateProductFeedback(barcode: String, userEmail: String, userFeedback: String)

    @Query("DELETE FROM saved_products WHERE barcode = :barcode AND userEmail = :userEmail")
    suspend fun deleteProductByBarcode(barcode: String, userEmail: String)

    @Query("DELETE FROM saved_products WHERE userEmail = :userEmail")
    suspend fun clearAllProducts(userEmail: String)

    /** Misafir (oturumsuz) kayıtları hesap e-postasına taşır. Çakışan barkodlar atlanır. */
    @Query("UPDATE OR IGNORE saved_products SET userEmail = :toEmail WHERE userEmail = :fromEmail")
    suspend fun reassignEmail(fromEmail: String, toEmail: String)
}

@Dao
interface BlacklistDao {
    @Query("SELECT * FROM blacklist WHERE userEmail = :userEmail ORDER BY addedAt DESC")
    fun getAllItems(userEmail: String): Flow<List<BlacklistItem>>

    /** Sunucu senkronunda çift kayıt önlemek için mevcut değerler (küçük harf). */
    @Query("SELECT LOWER(TRIM(value)) FROM blacklist WHERE userEmail = :userEmail")
    suspend fun getBlacklistValuesLower(userEmail: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: BlacklistItem)

    @Query("UPDATE blacklist SET reason = :reason WHERE id = :id AND userEmail = :userEmail")
    suspend fun updateItemReason(id: Int, userEmail: String, reason: String)

    @Query("DELETE FROM blacklist WHERE id = :id AND userEmail = :userEmail")
    suspend fun deleteItemById(id: Int, userEmail: String)
    
    @Query("DELETE FROM blacklist WHERE TRIM(LOWER(value)) = TRIM(LOWER(:value)) AND userEmail = :userEmail")
    suspend fun deleteItemByValue(value: String, userEmail: String)

    @Query("DELETE FROM blacklist WHERE userEmail = :userEmail")
    suspend fun clearAllBlacklist(userEmail: String)

    /** Misafir (oturumsuz) engelleri hesap e-postasına taşır. Çakışanlar atlanır. */
    @Query("UPDATE OR IGNORE blacklist SET userEmail = :toEmail WHERE userEmail = :fromEmail")
    suspend fun reassignEmail(fromEmail: String, toEmail: String)
}

@Database(entities = [SavedProduct::class, BlacklistItem::class], version = 7, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun savedProductDao(): SavedProductDao
    abstract fun blacklistDao(): BlacklistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "barcode_analyzer_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
