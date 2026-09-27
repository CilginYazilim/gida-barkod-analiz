package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.BlacklistItem
import com.example.data.model.SavedProduct
import com.example.data.network.GeminiApiHelper
import com.example.data.network.Ci4Comment
import com.example.data.network.Ci4AiQuestion
import com.example.data.network.Ci4Page
import com.example.data.network.Ci4PriceHistory
import com.example.data.network.GeminiComparison
import com.example.data.network.GeminiProductAnalysis
import com.example.data.repository.ProductRepository
import com.example.data.repository.toSavedProduct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    object Idle : ScanUiState
    object Loading : ScanUiState
    data class ConfirmLocal(val product: SavedProduct) : ScanUiState
    data class ConfirmWeb(
        val brand: String,
        val name: String,
        val category: String,
        val grammage: String,
        val imageUrl: String,
        val barcode: String,
        val rawProduct: com.example.data.network.OFFProduct?
    ) : ScanUiState
    data class Success(val product: SavedProduct, val blacklistWarnings: List<String>) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

sealed interface AuthUiState {
    object Idle : AuthUiState
    object Loading : AuthUiState
    data class Success(val isNew: Boolean = false) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

sealed interface ComparisonUiState {
    object Idle : ComparisonUiState
    object Loading : ComparisonUiState
    data class Success(val comparison: GeminiComparison) : ComparisonUiState
    data class Error(val message: String) : ComparisonUiState
}

sealed interface CommentSubmitState {
    object Idle : CommentSubmitState
    object Loading : CommentSubmitState
    data class Success(val message: String) : CommentSubmitState
    data class Error(val message: String) : CommentSubmitState
}

sealed interface CorrectionSubmitState {
    object Idle : CorrectionSubmitState
    object Loading : CorrectionSubmitState
    data class Success(val message: String) : CorrectionSubmitState
    data class Error(val message: String) : CorrectionSubmitState
}

enum class NavigationTab { SCAN, FAVORITES, BLACKLIST, COMPARE, HISTORY, PROFILE }

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val db         = AppDatabase.getDatabase(application)
    private val repository = ProductRepository(db.savedProductDao(), db.blacklistDao())
    private val prefs      = application.getSharedPreferences("barcode_app_prefs", Context.MODE_PRIVATE)

    // Kullanıcı durumu
    private val _currentUser    = MutableStateFlow<String?>(null)
    val currentUser: StateFlow<String?> = _currentUser.asStateFlow()

    private val _currentUserSurname = MutableStateFlow<String?>(null)
    val currentUserSurname: StateFlow<String?> = _currentUserSurname.asStateFlow()

    private val _userEmail = MutableStateFlow<String>("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    // Token (JWT) — API çağrılarında kullanılır
    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    // DB Flow'lar
    val historyProducts: StateFlow<List<SavedProduct>> = _userEmail
        .flatMapLatest { email -> repository.getHistoryProducts(email) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteProducts: StateFlow<List<SavedProduct>> = _userEmail
        .flatMapLatest { email -> repository.getFavoriteProducts(email) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blacklistItems: StateFlow<List<BlacklistItem>> = _userEmail
        .flatMapLatest { email -> repository.getBlacklistItems(email) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTab = MutableStateFlow(NavigationTab.SCAN)
    val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

    private val _themePreference = MutableStateFlow<String>("light")
    val themePreference: StateFlow<String> = _themePreference.asStateFlow()

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val chatMessages: StateFlow<List<Pair<String, Boolean>>> = _chatMessages.asStateFlow()

    private val _chatLoading = MutableStateFlow(false)
    val chatLoading: StateFlow<Boolean> = _chatLoading.asStateFlow()

    private val _selectedCompareProduct1 = MutableStateFlow<SavedProduct?>(null)
    val selectedCompareProduct1: StateFlow<SavedProduct?> = _selectedCompareProduct1.asStateFlow()

    private val _selectedCompareProduct2 = MutableStateFlow<SavedProduct?>(null)
    val selectedCompareProduct2: StateFlow<SavedProduct?> = _selectedCompareProduct2.asStateFlow()

    private val _comparisonState = MutableStateFlow<ComparisonUiState>(ComparisonUiState.Idle)
    val comparisonState: StateFlow<ComparisonUiState> = _comparisonState.asStateFlow()

    // Yorumlar
    private val _comments = MutableStateFlow<List<Ci4Comment>>(emptyList())
    val comments: StateFlow<List<Ci4Comment>> = _comments.asStateFlow()

    private val _commentsLoading = MutableStateFlow(false)
    val commentsLoading: StateFlow<Boolean> = _commentsLoading.asStateFlow()

    private val _commentSubmitState = MutableStateFlow<CommentSubmitState>(CommentSubmitState.Idle)
    val commentSubmitState: StateFlow<CommentSubmitState> = _commentSubmitState.asStateFlow()

    private val _correctionSubmitState = MutableStateFlow<CorrectionSubmitState>(CorrectionSubmitState.Idle)
    val correctionSubmitState: StateFlow<CorrectionSubmitState> = _correctionSubmitState.asStateFlow()

    // AI varsayılan sorular (SSS)
    private val _aiQuestions = MutableStateFlow<List<Ci4AiQuestion>>(emptyList())
    val aiQuestions: StateFlow<List<Ci4AiQuestion>> = _aiQuestions.asStateFlow()

    // E-Kod detayı (sözlükten)
    private val _ecodeDetail = MutableStateFlow<com.example.data.network.Ci4Ecode?>(null)
    val ecodeDetail: StateFlow<com.example.data.network.Ci4Ecode?> = _ecodeDetail.asStateFlow()
    private val _ecodeLoading = MutableStateFlow(false)
    val ecodeLoading: StateFlow<Boolean> = _ecodeLoading.asStateFlow()

    fun loadEcodeDetail(code: String) {
        viewModelScope.launch {
            _ecodeLoading.value = true
            _ecodeDetail.value = repository.getEcode(code)
            _ecodeLoading.value = false
        }
    }
    fun clearEcodeDetail() { _ecodeDetail.value = null }

    // E-Kod sözlüğü önbelleği (kısıtlı katkı kontrolü için).
    private var ecodeMapCache: Map<String, com.example.data.network.Ci4Ecode>? = null
    private fun normCode(code: String) = code.uppercase().replace(" ", "").replace("-", "")

    /** Verilen E-kod listesinden mevzuatta kısıtlı/yasaklı olanları döndürür (banned_note dolu). */
    suspend fun restrictedAdditives(codes: List<String>): List<com.example.data.network.Ci4Ecode> {
        if (codes.isEmpty()) return emptyList()
        val map = ecodeMapCache ?: repository.listEcodes("")
            .associateBy { normCode(it.code) }
            .also { ecodeMapCache = it }
        return codes.map { normCode(it) }.distinct()
            .mapNotNull { map[it] }
            .filter { !it.bannedNote.isNullOrBlank() }
    }

    // E-Kod Sözlüğü ekranı (arama/listeleme)
    private val _ecodeList = MutableStateFlow<List<com.example.data.network.Ci4Ecode>>(emptyList())
    val ecodeList: StateFlow<List<com.example.data.network.Ci4Ecode>> = _ecodeList.asStateFlow()
    private val _ecodeListLoading = MutableStateFlow(false)
    val ecodeListLoading: StateFlow<Boolean> = _ecodeListLoading.asStateFlow()
    private val _ecodeQuery = MutableStateFlow("")
    val ecodeQuery: StateFlow<String> = _ecodeQuery.asStateFlow()
    private var ecodeSearchJob: kotlinx.coroutines.Job? = null

    // E-Kod Sözlüğü ekranı görünürlüğü (Profil'den açılan overlay)
    private val _showEcodeDictionary = MutableStateFlow(false)
    val showEcodeDictionary: StateFlow<Boolean> = _showEcodeDictionary.asStateFlow()
    fun openEcodeDictionary() {
        _showEcodeDictionary.value = true
        if (_ecodeList.value.isEmpty()) loadEcodeList("")
    }
    fun closeEcodeDictionary() { _showEcodeDictionary.value = false }

    /** Sözlük listesini yükler (arama metni değişince 300ms debounce ile). */
    fun loadEcodeList(query: String = _ecodeQuery.value) {
        _ecodeQuery.value = query
        ecodeSearchJob?.cancel()
        ecodeSearchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300)
            _ecodeListLoading.value = true
            _ecodeList.value = repository.listEcodes(query)
            _ecodeListLoading.value = false
        }
    }

    // CMS sayfaları
    private val _pages = MutableStateFlow<List<Ci4Page>>(emptyList())
    val pages: StateFlow<List<Ci4Page>> = _pages.asStateFlow()

    private val _selectedPage = MutableStateFlow<Ci4Page?>(null)
    val selectedPage: StateFlow<Ci4Page?> = _selectedPage.asStateFlow()

    private val _pageLoading = MutableStateFlow(false)
    val pageLoading: StateFlow<Boolean> = _pageLoading.asStateFlow()

    // Fiyat geçmişi
    private val _priceHistory = MutableStateFlow<Ci4PriceHistory?>(null)
    val priceHistory: StateFlow<Ci4PriceHistory?> = _priceHistory.asStateFlow()

    private val _priceReportMessage = MutableStateFlow<String?>(null)
    val priceReportMessage: StateFlow<String?> = _priceReportMessage.asStateFlow()

    init {
        // Tema tercihini geri yükle (light / dark / system)
        prefs.getString("theme_pref", null)?.let { _themePreference.value = it }

        val savedName    = prefs.getString("user_name", null)
        val savedSurname = prefs.getString("user_surname", null)
        val savedEmail   = prefs.getString("user_email", null)
        val savedToken   = prefs.getString("access_token", null)
        if (savedName != null && savedEmail != null) {
            _currentUser.value    = savedName
            _currentUserSurname.value = savedSurname ?: ""
            _userEmail.value      = savedEmail
            _accessToken.value    = savedToken
            // Uygulama açılışında sunucu verisini (taramalar/favori/engel) tazele
            if (!savedToken.isNullOrBlank()) {
                viewModelScope.launch { repository.syncUserDataFromServer(savedEmail, savedToken) }
            }
        }
    }

    /**
     * Oturumu uygular. rememberMe=true ise kalıcı saklanır (uygulama yeniden açılınca
     * giriş korunur); false ise yalnızca bellekte tutulur, prefs'e yazılmaz.
     */
    private fun applySession(
        firstName: String,
        lastName: String,
        email: String,
        accessToken: String,
        refreshToken: String,
        rememberMe: Boolean = true
    ) {
        _currentUser.value        = firstName
        _currentUserSurname.value = lastName
        _userEmail.value          = email
        _accessToken.value        = accessToken.ifBlank { null }
        if (rememberMe) {
            prefs.edit().apply {
                putString("user_name", firstName)
                putString("user_surname", lastName)
                putString("user_email", email)
                putString("access_token", accessToken)
                putString("refresh_token", refreshToken)
                putBoolean("remember_me", true)
                apply()
            }
        } else {
            // Kalıcı saklama yok — varsa eski kalıntıları temizle
            prefs.edit().apply {
                remove("user_name"); remove("user_surname"); remove("user_email")
                remove("access_token"); remove("refresh_token")
                putBoolean("remember_me", false)
                apply()
            }
        }
    }

    /** Oturum açıldığında misafir yerel verisini hesaba taşır, sonra oturumu uygular. */
    private suspend fun migrateAndApplySession(
        firstName: String, lastName: String, email: String,
        accessToken: String, refreshToken: String, rememberMe: Boolean
    ) {
        val guestEmail = _userEmail.value
        if (guestEmail != email) {
            repository.migrateGuestData(guestEmail, email)
        }
        applySession(firstName, lastName, email, accessToken, refreshToken, rememberMe)
        // Sunucudaki taramalar/favoriler/kara listeyi yerel Room'a çek
        repository.syncUserDataFromServer(email, accessToken.ifBlank { null })
    }

    /** Oturum açık kullanıcının sunucu verisini (taramalar/favori/engel) yeniden çeker. */
    fun refreshUserData() {
        val token = getAccessToken()
        val email = _userEmail.value
        if (token.isNullOrBlank() || email.isBlank()) return
        viewModelScope.launch { repository.syncUserDataFromServer(email, token) }
    }

    /** Token okumak için helper. */
    fun getAccessToken(): String? = _accessToken.value ?: prefs.getString("access_token", null)

    // =====================================================================
    //  Auth
    // =====================================================================

    fun loginUserWithApi(email: String, password: String, rememberMe: Boolean = true) {
        if (email.isBlank() || password.isBlank()) return
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            val (auth, error) = repository.loginWithServer(email.trim(), password.trim())
            if (auth != null) {
                val user = auth.user
                val fn   = user?.firstName ?: user?.username ?: email.substringBefore("@")
                val ln   = user?.lastName ?: ""
                migrateAndApplySession(fn, ln, email.trim(), auth.accessToken ?: "", auth.refreshToken ?: "", rememberMe)
                _authState.value = AuthUiState.Success(isNew = false)
            } else {
                _authState.value = AuthUiState.Error(error ?: "Giriş başarısız")
            }
        }
    }

    fun registerUserWithApi(firstName: String, lastName: String, email: String, password: String, rememberMe: Boolean = true) {
        if (email.isBlank() || password.isBlank()) return
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            val (auth, error) = repository.registerWithServer(email.trim(), password.trim(), firstName.trim(), lastName.trim())
            if (auth != null) {
                val user = auth.user
                val fn   = user?.firstName?.takeIf { it.isNotBlank() } ?: firstName.trim().ifBlank { email.substringBefore("@") }
                val ln   = user?.lastName?.takeIf { it.isNotBlank() } ?: lastName.trim()
                migrateAndApplySession(fn, ln, email.trim(), auth.accessToken ?: "", auth.refreshToken ?: "", rememberMe)
                _authState.value = AuthUiState.Success(isNew = true)
            } else {
                _authState.value = AuthUiState.Error(error ?: "Kayıt başarısız")
            }
        }
    }

    /** Google ID token ile giriş/kayıt. */
    fun loginWithGoogle(idToken: String, rememberMe: Boolean = true) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            val (auth, error) = repository.loginWithGoogle(idToken)
            if (auth != null) {
                val user = auth.user
                val fn   = user?.firstName ?: user?.username ?: "Kullanıcı"
                val ln   = user?.lastName ?: ""
                val em   = user?.email ?: ""
                migrateAndApplySession(fn, ln, em, auth.accessToken ?: "", auth.refreshToken ?: "", rememberMe)
                _authState.value = AuthUiState.Success(isNew = auth.isNew ?: false)
            } else {
                _authState.value = AuthUiState.Error(error ?: "Google girişi başarısız")
            }
        }
    }

    fun clearAuthState() { _authState.value = AuthUiState.Idle }

    fun logoutUser() {
        _currentUser.value        = null
        _currentUserSurname.value = null
        _userEmail.value          = ""
        _accessToken.value        = null
        prefs.edit().apply {
            remove("user_name"); remove("user_surname"); remove("user_email")
            remove("access_token"); remove("refresh_token")
            apply()
        }
    }

    fun clearAllUserData() {
        val email = _userEmail.value
        viewModelScope.launch {
            if (email.isNotEmpty()) repository.clearAllUserData(email)
            logoutUser()
        }
    }

    fun updateUserProfile(name: String, surname: String) {
        val nameClean    = name.trim()
        val surnameClean = surname.trim()
        _currentUser.value        = nameClean
        _currentUserSurname.value = surnameClean
        prefs.edit().apply {
            putString("user_name", nameClean)
            putString("user_surname", surnameClean)
            apply()
        }
        // Server sync
        val token = getAccessToken()
        if (!token.isNullOrBlank()) {
            viewModelScope.launch {
                repository.updateProfileOnServer(nameClean, surnameClean, token)
            }
        }
    }

    fun setThemePreference(pref: String) {
        _themePreference.value = pref
        prefs.edit().putString("theme_pref", pref).apply()
    }

    // Şifre değiştirme durumu: null=boşta, "success"=başarılı, diğer=hata mesajı
    private val _passwordChangeState = MutableStateFlow<String?>(null)
    val passwordChangeState: StateFlow<String?> = _passwordChangeState.asStateFlow()
    private val _passwordChangeLoading = MutableStateFlow(false)
    val passwordChangeLoading: StateFlow<Boolean> = _passwordChangeLoading.asStateFlow()

    fun changePassword(currentPassword: String, newPassword: String) {
        val token = getAccessToken()
        if (token.isNullOrBlank()) { _passwordChangeState.value = "Oturum bulunamadı"; return }
        viewModelScope.launch {
            _passwordChangeLoading.value = true
            val error = repository.changePasswordOnServer(currentPassword, newPassword, token)
            _passwordChangeState.value = error ?: "success"
            _passwordChangeLoading.value = false
        }
    }

    fun clearPasswordChangeState() { _passwordChangeState.value = null }
    fun setTab(tab: NavigationTab) {
        _currentTab.value = tab
        // Alt menüden bir sekmeye geçilince aktif ürün/onay ekranından çık (sekme içeriği görünsün)
        if (_scanState.value !is ScanUiState.Idle) {
            _scanState.value    = ScanUiState.Idle
            _chatMessages.value = emptyList()
        }
    }

    // =====================================================================
    //  Barkod Tarama Akışı
    // =====================================================================

    fun analyzeProduct(barcode: String) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isEmpty()) return

        viewModelScope.launch {
            _scanState.value = ScanUiState.Loading
            _chatMessages.value = emptyList()

            try {
                val email = _userEmail.value

                // 1. Yerel Room DB'de ara (aynı kullanıcı için)
                val localProduct = repository.getProductByBarcode(cleanBarcode, email)
                if (localProduct != null) {
                    _scanState.value = ScanUiState.ConfirmLocal(localProduct)
                    return@launch
                }

                // 2. Diğer kullanıcı emailiyle kayıtlı mı? (cihaz paylaşımı / hesap geçişi)
                val localAny = db.savedProductDao().getProductByBarcodeAny(cleanBarcode)
                if (localAny != null) {
                    // Mevcut kullanıcıya ata ve göster (AI analizi olmadan)
                    val adopted = localAny.copy(userEmail = email, scannedAt = System.currentTimeMillis())
                    db.savedProductDao().insertProduct(adopted)
                    _scanState.value = ScanUiState.ConfirmLocal(adopted)
                    return@launch
                }

                // 3. Merkezi katalogda (CI4 server) ara
                val catalogProduct = repository.fetchFromCatalog(cleanBarcode, email)
                if (catalogProduct != null) {
                    _scanState.value = ScanUiState.ConfirmLocal(catalogProduct)
                    return@launch
                }

                // 4. Katalogda da yok → Open Food Facts + Gemini AI
                searchWebProduct(cleanBarcode)

            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error("Arama başlatılırken hata oluştu: ${e.localizedMessage}")
            }
        }
    }

    private fun translateCategory(cat: String?): String {
        if (cat.isNullOrBlank()) return "Gıda"
        val clean = cat.trim().lowercase()
        return when {
            clean.contains("beverage") || clean.contains("drink") || clean.contains("gazoz") || clean.contains("cola") || clean.contains("soda") -> "İçecek"
            clean.contains("snack") || clean.contains("chips") || clean.contains("cips") -> "Atıştırmalık"
            clean.contains("chocolate") || clean.contains("cacao") || clean.contains("kakao") -> "Çikolata / Kakaolu Ürün"
            clean.contains("biscuit") || clean.contains("gofret") || clean.contains("wafer") -> "Bisküvi / Gofret / Kek"
            clean.contains("dairy") || clean.contains("cheese") || clean.contains("peynir") || clean.contains("yoğurt") -> "Süt ve Süt Ürünü"
            clean.contains("milk") || clean.contains("süt") -> "Süt"
            clean.contains("sauce") || clean.contains("ketçap") || clean.contains("mayonez") -> "Sos"
            clean.contains("cereal") || clean.contains("tahıl") -> "Tahıl / Hububat"
            clean.contains("bread") || clean.contains("ekmek") -> "Ekmek / Unlu Mamül"
            clean.contains("meat") || clean.contains("sucuk") || clean.contains("salam") -> "Et / Şarküteri"
            clean.contains("fish") || clean.contains("balık") -> "Balık / Deniz Ürünü"
            clean.contains("frozen") || clean.contains("dondurul") -> "Dondurulmuş Gıda"
            clean.contains("soup") || clean.contains("çorba") -> "Çorba"
            else -> cat.split(":").lastOrNull()?.trim()?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() } ?: cat.trim()
        }
    }

    private fun searchWebProduct(barcode: String) {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Loading
            try {
                val response = com.example.data.network.OFFRetrofitClient.service.getProductInfo(barcode)
                if (response.status == 1 && response.product != null) {
                    val p = response.product
                    _scanState.value = ScanUiState.ConfirmWeb(
                        brand    = p.brands ?: "Bilinmeyen Marka",
                        name     = if (!p.productNameTr.isNullOrBlank()) p.productNameTr else p.productName ?: "Bilinmeyen Ürün",
                        category = translateCategory(p.categories?.split(",")?.firstOrNull()),
                        grammage = p.quantity ?: "Bilinmiyor",
                        imageUrl = p.imageFrontUrl ?: "",
                        barcode  = barcode,
                        rawProduct = p
                    )
                } else {
                    _scanState.value = ScanUiState.ConfirmWeb(
                        brand = "Bilinmeyen Marka", name = "Bilinmeyen Ürün",
                        category = "Gıda", grammage = "Bilinmiyor",
                        imageUrl = "", barcode = barcode, rawProduct = null
                    )
                }
            } catch (e: Exception) {
                _scanState.value = ScanUiState.ConfirmWeb(
                    brand = "Bilinmeyen Marka", name = "Bilinmeyen Ürün",
                    category = "Gıda", grammage = "Bilinmiyor",
                    imageUrl = "", barcode = barcode, rawProduct = null
                )
            }
        }
    }

    fun confirmUsageOfLocalProduct(product: SavedProduct) {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Loading
            val updated = product.copy(scannedAt = System.currentTimeMillis(), updatedAt = System.currentTimeMillis())
            db.savedProductDao().insertProduct(updated)
            val warnings = getBlacklistWarningsForProduct(updated)
            _scanState.value = ScanUiState.Success(updated, warnings)
        }
    }

    fun forceSearchWebProduct(barcode: String) {
        val cleanBarcode = barcode.trim()
        if (cleanBarcode.isEmpty()) return
        _chatMessages.value = emptyList()
        searchWebProduct(cleanBarcode)
    }

    fun initiateWebProductAnalysis(
        barcode: String,
        brand: String,
        name: String,
        category: String,
        grammage: String,
        imageUrl: String,
        rawProduct: com.example.data.network.OFFProduct?,
        price: Double? = null
    ) {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Loading
            try {
                val email = _userEmail.value
                val passedProduct = com.example.data.network.OFFProduct(
                    productName      = name,
                    productNameTr    = name,
                    brands           = brand,
                    categories       = category,
                    imageFrontUrl    = imageUrl.ifEmpty { rawProduct?.imageFrontUrl ?: "" },
                    ingredientsText  = rawProduct?.ingredientsText ?: rawProduct?.ingredientsTextTr,
                    ingredientsTextTr = rawProduct?.ingredientsTextTr ?: rawProduct?.ingredientsText,
                    nutriments       = rawProduct?.nutriments,
                    quantity         = grammage
                )

                val analysis = GeminiApiHelper.analyzeBarcode(barcode, passedProduct)
                if (analysis != null) {
                    // Ortak eşleme: yapısal besin, resmi Nutri-Score skoru, kategori kanonikleştirme,
                    // 4 durumlu helal vb. tek yerden gelir. Kullanıcı (ConfirmWeb) değerleri AI'yı geçersiz kılar.
                    val finalProduct = analysis.toSavedProduct(
                        barcode = barcode,
                        userEmail = email,
                        imageUrl = imageUrl.ifEmpty { passedProduct.imageFrontUrl ?: "" },
                        nameOverride = name,
                        brandOverride = brand,
                        categoryOverride = category,
                        grammageOverride = grammage,
                        price = price
                    )

                    db.savedProductDao().insertProduct(finalProduct)
                    repository.pushToCatalog(finalProduct, getAccessToken())

                    val warnings = getBlacklistWarningsForProduct(finalProduct)
                    _scanState.value = ScanUiState.Success(finalProduct, warnings)
                } else {
                    _scanState.value = ScanUiState.Error("Ürün yapay zekâ değerlendirmesinden geçirilemedi.")
                }
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error("Analiz hatası: ${e.localizedMessage}")
            }
        }
    }

    fun clearActiveProductState() {
        _scanState.value    = ScanUiState.Idle
        _chatMessages.value = emptyList()
    }

    fun scanRandomProduct() {
        val barcodes = listOf("8691505001222","8690504021206","8690624301547","8690766110298","8690632039233","8690504018657","8690526081448","8690533038312")
        analyzeProduct(barcodes.random())
    }

    fun analyzeProductImageUri(uri: Uri) {
        viewModelScope.launch {
            _scanState.value = ScanUiState.Loading
            _chatMessages.value = emptyList()
            try {
                val context = getApplication<Application>()
                val (base64, mimeType) = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    val b64  = bytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: ""
                    val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                    Pair(b64, mime)
                }
                if (base64.isNotEmpty()) {
                    val product = repository.analyzeAndSaveImage(base64, mimeType, _userEmail.value)
                    if (product != null) {
                        _scanState.value = ScanUiState.Success(product, getBlacklistWarningsForProduct(product))
                    } else {
                        _scanState.value = ScanUiState.Error("Seçilen görsel analiz edilemedi.")
                    }
                } else {
                    _scanState.value = ScanUiState.Error("Görsel dosyası okunamadı.")
                }
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error("Görsel işlenirken hata: ${e.localizedMessage}")
            }
        }
    }

    fun analyzeProductImage(base64Image: String, mimeType: String) {
        if (base64Image.trim().isEmpty()) return
        viewModelScope.launch {
            _scanState.value = ScanUiState.Loading
            _chatMessages.value = emptyList()
            try {
                val product = repository.analyzeAndSaveImage(base64Image, mimeType, _userEmail.value)
                if (product != null) {
                    _scanState.value = ScanUiState.Success(product, getBlacklistWarningsForProduct(product))
                } else {
                    _scanState.value = ScanUiState.Error("Görsel analiz edilemedi.")
                }
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error("Görsel analiz edilirken hata: ${e.localizedMessage}")
            }
        }
    }

    // =====================================================================
    //  Ürün İşlemleri
    // =====================================================================

    fun updateProductNotes(barcode: String, notes: String) {
        viewModelScope.launch {
            repository.updateProductNotes(barcode, _userEmail.value, notes, getAccessToken())
            val current = _scanState.value
            if (current is ScanUiState.Success && current.product.barcode == barcode) {
                _scanState.value = current.copy(product = current.product.copy(userNotes = notes, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun updateProductFeedback(barcode: String, feedback: String) {
        viewModelScope.launch {
            // Yerel kayıt (anında görünür)
            repository.updateProductFeedback(barcode, _userEmail.value, feedback)
            // Sunucuya kalıcı: geri bildirim corrections tablosuna yazılır (oturum varsa).
            val token = getAccessToken()
            if (!token.isNullOrBlank() && feedback.isNotBlank()) {
                repository.submitCorrection(barcode, "geri_bildirim", null, null, feedback, token)
            }
            val current = _scanState.value
            if (current is ScanUiState.Success && current.product.barcode == barcode) {
                _scanState.value = current.copy(product = current.product.copy(userFeedback = feedback, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun toggleFavorite(product: SavedProduct) {
        viewModelScope.launch {
            val newFav = !product.isFavorite
            repository.toggleFavorite(
                barcode     = product.barcode,
                userEmail   = _userEmail.value,
                isFavorite  = newFav,
                accessToken = getAccessToken()
            )
            val current = _scanState.value
            if (current is ScanUiState.Success && current.product.barcode == product.barcode) {
                _scanState.value = current.copy(product = current.product.copy(isFavorite = newFav, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    fun deleteProductFromHistory(barcode: String) {
        viewModelScope.launch { repository.deleteProduct(barcode, _userEmail.value) }
    }

    fun clearHistory() {
        viewModelScope.launch { repository.clearHistory(_userEmail.value) }
    }

    // =====================================================================
    //  Kara Liste
    // =====================================================================

    fun addToBlacklist(value: String, type: String, reason: String = "") {
        viewModelScope.launch {
            repository.addToBlacklist(value, type, reason, _userEmail.value, getAccessToken())
        }
    }

    fun updateBlacklistReason(id: Int, reason: String, value: String = "", type: String = "INGREDIENT") {
        viewModelScope.launch {
            repository.updateBlacklistReason(id, _userEmail.value, value, type, reason, getAccessToken())
        }
    }

    fun removeFromBlacklist(id: Int, value: String = "") {
        viewModelScope.launch { repository.removeFromBlacklist(id, _userEmail.value, value, getAccessToken()) }
    }

    fun removeFromBlacklistByValue(value: String) {
        viewModelScope.launch { repository.removeFromBlacklistByValue(value, _userEmail.value, getAccessToken()) }
    }

    // =====================================================================
    //  Karşılaştırma
    // =====================================================================

    fun selectProductForCompare(product: SavedProduct, slot: Int) {
        if (slot == 1) _selectedCompareProduct1.value = product else _selectedCompareProduct2.value = product
        _comparisonState.value = ComparisonUiState.Idle
    }

    fun clearCompareSlots() {
        _selectedCompareProduct1.value = null
        _selectedCompareProduct2.value = null
        _comparisonState.value = ComparisonUiState.Idle
    }

    fun runComparison() {
        val p1 = _selectedCompareProduct1.value ?: return
        val p2 = _selectedCompareProduct2.value ?: return
        viewModelScope.launch {
            _comparisonState.value = ComparisonUiState.Loading
            try {
                val result = GeminiApiHelper.compareProducts(p1, p2)
                _comparisonState.value = if (result != null) ComparisonUiState.Success(result)
                else ComparisonUiState.Error("Karşılaştırma yapılamadı.")
            } catch (e: Exception) {
                _comparisonState.value = ComparisonUiState.Error("Analiz hatası: ${e.localizedMessage}")
            }
        }
    }

    // =====================================================================
    //  AI Chat
    // =====================================================================

    fun sendChatMessage(question: String) {
        val current = _scanState.value
        if (current !is ScanUiState.Success || question.trim().isEmpty()) return
        viewModelScope.launch {
            val updated = _chatMessages.value.toMutableList()
            updated.add(Pair(question, true))
            _chatMessages.value = updated
            _chatLoading.value = true
            try {
                val reply = GeminiApiHelper.chatAboutProduct(
                    productName = current.product.name,
                    ingredients = current.product.ingredients,
                    question    = question,
                    chatHistory = updated
                )
                val final = _chatMessages.value.toMutableList()
                final.add(Pair(reply, false))
                _chatMessages.value = final
            } catch (e: Exception) {
                val err = _chatMessages.value.toMutableList()
                err.add(Pair("Bir hata oluştu: ${e.localizedMessage}", false))
                _chatMessages.value = err
            } finally {
                _chatLoading.value = false
            }
        }
    }

    // =====================================================================
    //  Ürün Yorumları + Beğeni
    // =====================================================================

    /** Bir ürünün yorumlarını yükler (onaylı + kullanıcının kendi yorumları). */
    fun loadComments(barcode: String) {
        if (barcode.isBlank() || barcode.startsWith("RESIM_") || barcode.startsWith("IMAGE_")) {
            _comments.value = emptyList()
            return
        }
        viewModelScope.launch {
            _commentsLoading.value = true
            _comments.value = repository.getComments(barcode, getAccessToken())
            _commentsLoading.value = false
        }
    }

    /** Yeni yorum gönderir (pending). */
    fun submitComment(barcode: String, body: String, rating: Int?) {
        val token = getAccessToken()
        if (token.isNullOrBlank()) {
            _commentSubmitState.value = CommentSubmitState.Error("Yorum yapmak için giriş yapmalısınız.")
            return
        }
        if (body.trim().isEmpty()) return
        viewModelScope.launch {
            _commentSubmitState.value = CommentSubmitState.Loading
            val created = repository.addComment(barcode, body.trim(), rating, token)
            if (created != null) {
                _commentSubmitState.value = CommentSubmitState.Success("Yorumunuz alındı, onaylandıktan sonra herkese görünür olacak.")
                loadComments(barcode)
            } else {
                _commentSubmitState.value = CommentSubmitState.Error("Yorum gönderilemedi. Lütfen tekrar deneyin.")
            }
        }
    }

    /** Bir yorumu beğen/geri al. */
    fun toggleCommentLike(commentId: Int) {
        val token = getAccessToken() ?: return
        viewModelScope.launch {
            val result = repository.toggleCommentLike(commentId, token)
            if (result != null) {
                _comments.value = _comments.value.map {
                    if (it.id == commentId) it.copy(isLiked = result.isLiked, likeCount = result.likeCount) else it
                }
            }
        }
    }

    fun clearCommentSubmitState() { _commentSubmitState.value = CommentSubmitState.Idle }

    // =====================================================================
    //  Hata Bildirimi / Düzeltme Talebi
    // =====================================================================

    fun submitCorrection(barcode: String, field: String, currentValue: String?, suggestedValue: String?, note: String?) {
        val token = getAccessToken()
        if (token.isNullOrBlank()) {
            _correctionSubmitState.value = CorrectionSubmitState.Error("Düzeltme talebi için giriş yapmalısınız.")
            return
        }
        if (suggestedValue.isNullOrBlank() && note.isNullOrBlank()) {
            _correctionSubmitState.value = CorrectionSubmitState.Error("Önerilen değer veya açıklama girin.")
            return
        }
        viewModelScope.launch {
            _correctionSubmitState.value = CorrectionSubmitState.Loading
            val ok = repository.submitCorrection(barcode, field, currentValue, suggestedValue, note, token)
            _correctionSubmitState.value = if (ok) {
                CorrectionSubmitState.Success("Düzeltme talebiniz alındı, en kısa sürede incelenecek. Teşekkürler!")
            } else {
                CorrectionSubmitState.Error("Talep gönderilemedi. Lütfen tekrar deneyin.")
            }
        }
    }

    fun clearCorrectionSubmitState() { _correctionSubmitState.value = CorrectionSubmitState.Idle }

    // =====================================================================
    //  AI Varsayılan Sorular + CMS Sayfaları
    // =====================================================================

    fun loadAiQuestions(force: Boolean = false) {
        if (!force && _aiQuestions.value.isNotEmpty()) return
        viewModelScope.launch {
            _aiQuestions.value = repository.getAiQuestions(getAccessToken())
        }
    }

    /** Kullanıcının kendi hızlı sorusunu ekler ve listeyi tazeler. */
    fun addUserAiQuestion(question: String) {
        val token = getAccessToken() ?: return
        if (question.trim().length < 3) return
        viewModelScope.launch {
            repository.suggestAiQuestion(question, token)
            _aiQuestions.value = repository.getAiQuestions(token)
        }
    }

    /** Kullanıcının kendi sorusunu siler ve listeyi tazeler. */
    fun deleteUserAiQuestion(id: Int) {
        val token = getAccessToken() ?: return
        viewModelScope.launch {
            repository.deleteAiQuestion(id, token)
            _aiQuestions.value = repository.getAiQuestions(token)
        }
    }

    fun loadPages() {
        viewModelScope.launch {
            _pages.value = repository.getPages(getAccessToken())
        }
    }

    fun openPage(slug: String) {
        viewModelScope.launch {
            _pageLoading.value = true
            _selectedPage.value = repository.getPage(slug, getAccessToken())
            _pageLoading.value = false
        }
    }

    fun clearSelectedPage() { _selectedPage.value = null }

    // =====================================================================
    //  Fiyat Geçmişi + Bildirimi
    // =====================================================================

    fun loadPriceHistory(barcode: String) {
        if (barcode.isBlank() || barcode.startsWith("RESIM_") || barcode.startsWith("IMAGE_")) {
            _priceHistory.value = null
            return
        }
        viewModelScope.launch {
            _priceHistory.value = repository.getPriceHistory(barcode, getAccessToken())
        }
    }

    fun reportPrice(barcode: String, price: Double, notes: String? = null) {
        val token = getAccessToken()
        if (token.isNullOrBlank()) {
            _priceReportMessage.value = "Fiyat bildirmek için giriş yapmalısınız."
            return
        }
        if (price <= 0) {
            _priceReportMessage.value = "Geçerli bir fiyat girin."
            return
        }
        viewModelScope.launch {
            val ok = repository.reportPrice(barcode, price, notes, token)
            if (ok) {
                _priceReportMessage.value = "Fiyat bildiriminiz kaydedildi. Teşekkürler!"
                loadPriceHistory(barcode)
                // scanState'teki ürünün güncel fiyatını da güncelle
                val current = _scanState.value
                if (current is ScanUiState.Success && current.product.barcode == barcode) {
                    _scanState.value = current.copy(product = current.product.copy(price = price, updatedAt = System.currentTimeMillis()))
                }
            } else {
                _priceReportMessage.value = "Fiyat kaydedilemedi. Lütfen tekrar deneyin."
            }
        }
    }

    fun clearPriceReportMessage() { _priceReportMessage.value = null }

    // =====================================================================
    //  Private
    // =====================================================================

    private fun getBlacklistWarningsForProduct(product: SavedProduct): List<String> {
        val warnings = mutableListOf<String>()
        for (item in blacklistItems.value) {
            val keyword = item.value.lowercase().trim()
            if (keyword.isEmpty()) continue
            val hit = product.ingredients.lowercase().contains(keyword) ||
                      product.eCodes.lowercase().contains(keyword) ||
                      product.name.lowercase().contains(keyword) ||
                      product.brand.lowercase().contains(keyword)
            if (hit) {
                val typeLabel = when (item.type) {
                    "BOYCOTT"  -> "⚠️ Bu ürün boykot listesinde bulunmaktadır. Gerekçe: ${item.reason}"
                    "ADDITIVE" -> "⚠️ Kara Listenizde olan Katkı Maddesi '$keyword' tespit edildi! Gerekçe: ${item.reason}"
                    else       -> "⚠️ Kara Listenizde olan İçerik '$keyword' tespit edildi! Gerekçe: ${item.reason}"
                }
                warnings.add(typeLabel)
            }
        }
        return warnings
    }
}
