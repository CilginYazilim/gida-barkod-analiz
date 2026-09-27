package com.example.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.AuthUiState
import com.example.ui.NavigationTab
import com.example.ui.ProductViewModel
import com.example.ui.theme.EcoGreen
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.RiskHigh
import com.example.ui.theme.SoftMint
import com.example.ui.theme.WarmGold
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProductViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser        by viewModel.currentUser.collectAsState()
    val currentUserSurname by viewModel.currentUserSurname.collectAsState()
    val userEmail          by viewModel.userEmail.collectAsState()
    val authState          by viewModel.authState.collectAsState()

    val history   by viewModel.historyProducts.collectAsState()
    val favorites by viewModel.favoriteProducts.collectAsState()
    val blacklist by viewModel.blacklistItems.collectAsState()

    val themePref            by viewModel.themePreference.collectAsState()
    val passwordChangeState  by viewModel.passwordChangeState.collectAsState()
    val passwordChangeLoading by viewModel.passwordChangeLoading.collectAsState()

    // CMS sayfaları (Kullanım Şartları, Gizlilik, vb.)
    val pages        by viewModel.pages.collectAsState()
    val selectedPage by viewModel.selectedPage.collectAsState()
    val pageLoading  by viewModel.pageLoading.collectAsState()
    val aiQuestionsList by viewModel.aiQuestions.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadPages(); viewModel.refreshUserData(); viewModel.loadAiQuestions() }

    val scrollState = rememberScrollState()

    LaunchedEffect(authState) {
        if (authState is AuthUiState.Success) {
            viewModel.clearAuthState()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (currentUser == null) {
            AuthPanel(
                authState    = authState,
                onLogin      = { email, password, rememberMe -> viewModel.loginUserWithApi(email, password, rememberMe) },
                onRegister   = { firstName, lastName, email, password, rememberMe ->
                    viewModel.registerUserWithApi(firstName, lastName, email, password, rememberMe)
                },
                onGoogleLogin = { idToken, rememberMe -> viewModel.loginWithGoogle(idToken, rememberMe) },
                onClearAuthState = { viewModel.clearAuthState() }
            )
        } else {
            ProfileDashboard(
                name           = currentUser ?: "",
                surname        = currentUserSurname ?: "",
                email          = userEmail,
                scannedCount   = history.size,
                favoritesCount = favorites.size,
                blacklistCount = blacklist.size,
                themePref      = themePref,
                passwordChangeState = passwordChangeState,
                passwordChangeLoading = passwordChangeLoading,
                onLogout       = { viewModel.logoutUser() },
                onUpdateProfile = { newName, newSurname -> viewModel.updateUserProfile(newName, newSurname) },
                onClearAllData = { viewModel.clearAllUserData() },
                onOpenTab      = { tab -> viewModel.setTab(tab) },
                onRefresh      = { viewModel.refreshUserData() },
                onSetTheme     = { pref -> viewModel.setThemePreference(pref) },
                onChangePassword = { cur, new -> viewModel.changePassword(cur, new) },
                onClearPasswordState = { viewModel.clearPasswordChangeState() },
                aiQuestions = aiQuestionsList,
                onAddAiQuestion = { q -> viewModel.addUserAiQuestion(q) },
                onDeleteAiQuestion = { id -> viewModel.deleteUserAiQuestion(id) },
                onOpenEcodeDictionary = { viewModel.openEcodeDictionary() }
            )
        }

        // ─── Bilgi & Politikalar (CMS sayfaları) ───────────────────────
        if (pages.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Bilgi & Politikalar",
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    pages.forEachIndexed { index, page ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.openPage(page.slug) }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = EcoGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = page.title,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                        }
                        if (index < pages.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // CMS sayfa içerik dialog'u
    if (selectedPage != null || pageLoading) {
        AlertDialog(
            onDismissRequest = { viewModel.clearSelectedPage() },
            title = {
                Text(
                    text = selectedPage?.title ?: "Yükleniyor...",
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen
                )
            },
            text = {
                if (pageLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = ForestGreen)
                        Spacer(Modifier.width(10.dp))
                        Text("İçerik yükleniyor...", fontSize = 13.sp, color = Color.Gray)
                    }
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = selectedPage?.body?.ifBlank { "İçerik bulunamadı." } ?: "İçerik bulunamadı.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.clearSelectedPage() }) {
                    Text("Kapat", color = ForestGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun AuthPanel(
    authState: AuthUiState,
    onLogin: (email: String, password: String, rememberMe: Boolean) -> Unit,
    onRegister: (firstName: String, lastName: String, email: String, password: String, rememberMe: Boolean) -> Unit,
    onGoogleLogin: (idToken: String, rememberMe: Boolean) -> Unit,
    onClearAuthState: () -> Unit
) {
    var isSignUpTab by remember { mutableStateOf(false) }

    var firstNameInput by remember { mutableStateOf("") }
    var lastNameInput  by remember { mutableStateOf("") }
    var emailInput     by remember { mutableStateOf("") }
    var passwordInput  by remember { mutableStateOf("") }
    var localError     by remember { mutableStateOf("") }
    var rememberMe     by remember { mutableStateOf(true) }

    val isLoading = authState is AuthUiState.Loading
    val apiError  = if (authState is AuthUiState.Error) authState.message else ""

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // Modern Google Sign-In: Credential Manager + Google Identity Services.
    // GetSignInWithGoogleOption her seferinde hesap seçtirir ve SEÇİLEN hesap için
    // TAZE idToken döndürür — legacy API'deki "bayat token / yanlış hesap" bug'ı yoktur.
    fun launchGoogleSignIn() {
        val webClientId = try { BuildConfig.GOOGLE_WEB_CLIENT_ID } catch (_: Exception) { "" }
        if (webClientId.isBlank() || webClientId == "BURAYA_GOOGLE_WEB_ISTEMCI_ID_YAZIN") {
            localError = "Google Sign-In yapılandırılmamış. GOOGLE_WEB_CLIENT_ID (.env) eksik."
            return
        }
        scope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdCredential.idToken
                    Log.e("GoogleSignInDebug", "CredMgr ok email=${googleIdCredential.id}")
                    onGoogleLogin(idToken, rememberMe)
                } else {
                    localError = "Beklenmeyen kimlik türü döndü."
                }
            } catch (e: GetCredentialCancellationException) {
                // Kullanıcı iptal etti — sessiz geç.
            } catch (e: NoCredentialException) {
                localError = "Cihazda uygun Google hesabı bulunamadı. Ayarlar → Hesaplar'dan bir Google hesabı ekleyin."
            } catch (e: GetCredentialException) {
                Log.e("GoogleSignInDebug", "GetCredential error: ${e.javaClass.simpleName} ${e.message}", e)
                localError = "Google girişi başarısız: ${e.message ?: e.javaClass.simpleName}"
            }
        }
    }

    LaunchedEffect(isSignUpTab) { onClearAuthState() }

    Text(
        text = "Profil Yönetimi",
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 24.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )

    Text(
        text = "Kişisel kara listenizi, gıda tarama geçmişinizi ve favorilerinizi yedeklemek ve cihazlar arası senkronize etmek için hesabınıza giriş yapın.",
        color = Color.Gray,
        fontSize = 13.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(bottom = 20.dp, start = 8.dp, end = 8.dp)
    )

    // Giriş Yap / Kayıt Ol seçici
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(26.dp))
                .background(if (!isSignUpTab) ForestGreen else Color.Transparent)
                .clickable(enabled = !isLoading) { isSignUpTab = false; localError = "" }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Giriş Yap", fontWeight = FontWeight.Bold,
                color = if (!isSignUpTab) Color.White else Color.Gray, fontSize = 14.sp)
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(26.dp))
                .background(if (isSignUpTab) ForestGreen else Color.Transparent)
                .clickable(enabled = !isLoading) { isSignUpTab = true; localError = "" }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Kayıt Ol", fontWeight = FontWeight.Bold,
                color = if (isSignUpTab) Color.White else Color.Gray, fontSize = 14.sp)
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = if (isSignUpTab) "Yeni Hesap Oluştur" else "Hoş Geldiniz",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = ForestGreen,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (isSignUpTab) {
                OutlinedTextField(
                    value = firstNameInput,
                    onValueChange = { firstNameInput = it; localError = "" },
                    label = { Text("Ad") },
                    leadingIcon = { Icon(Icons.Outlined.Person, null, tint = ForestGreen) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = lastNameInput,
                    onValueChange = { lastNameInput = it; localError = "" },
                    label = { Text("Soyad") },
                    leadingIcon = { Icon(Icons.Outlined.Person, null, tint = ForestGreen) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it; localError = "" },
                label = { Text("E-Posta Adresi") },
                leadingIcon = { Icon(Icons.Outlined.Email, null, tint = ForestGreen) },
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it; localError = "" },
                label = { Text("Şifre") },
                leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = ForestGreen) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                shape = RoundedCornerShape(12.dp)
            )

            val shownError = localError.ifEmpty { apiError }
            AnimatedVisibility(visible = shownError.isNotEmpty()) {
                Text(
                    text = shownError,
                    color = RiskHigh,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // ─── Beni Hatırla ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoading) { rememberMe = !rememberMe }
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = rememberMe,
                    onCheckedChange = { rememberMe = it },
                    enabled = !isLoading,
                    colors = CheckboxDefaults.colors(checkedColor = ForestGreen)
                )
                Text(
                    text = "Beni Hatırla",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (rememberMe) "Oturum açık kalır" else "Çıkışta unutulur",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            // E-posta/şifre ile giriş/kayıt butonu
            Button(
                onClick = {
                    localError = ""
                    val email = emailInput.trim()
                    val pw    = passwordInput.trim()
                    when {
                        email.isEmpty() || pw.isEmpty() -> localError = "E-posta ve şifre boş bırakılamaz."
                        !email.contains("@")            -> localError = "Geçerli bir e-posta adresi girin."
                        pw.length < 6                   -> localError = "Şifre en az 6 karakter olmalıdır."
                        isSignUpTab -> onRegister(firstNameInput.trim(), lastNameInput.trim(), email, pw, rememberMe)
                        else        -> onLogin(email, pw, rememberMe)
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("auth_action_button"),
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Lütfen bekleyin...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                } else {
                    Text(
                        text = if (isSignUpTab) "Kaydol ve Giriş Yap" else "Güvenli Giriş Yap",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // ─── Ayırıcı ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                Text(
                    text = "  veya  ",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            }

            // ─── Google ile Giriş Yap ───────────────────────────────────
            OutlinedButton(
                onClick = { localError = ""; launchGoogleSignIn() },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                // Google "G" logo rengi — basit Text ile
                Text(
                    text = "G",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF4285F4) // Google mavi
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isSignUpTab) "Google ile Kaydol" else "Google ile Giriş Yap",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun ProfileDashboard(
    name: String,
    surname: String,
    email: String,
    scannedCount: Int,
    favoritesCount: Int,
    blacklistCount: Int,
    themePref: String,
    passwordChangeState: String?,
    passwordChangeLoading: Boolean,
    onLogout: () -> Unit,
    onUpdateProfile: (String, String) -> Unit,
    onClearAllData: () -> Unit,
    onOpenTab: (NavigationTab) -> Unit = {},
    onRefresh: () -> Unit = {},
    onSetTheme: (String) -> Unit = {},
    onChangePassword: (String, String) -> Unit = { _, _ -> },
    onClearPasswordState: () -> Unit = {},
    aiQuestions: List<com.example.data.network.Ci4AiQuestion> = emptyList(),
    onAddAiQuestion: (String) -> Unit = {},
    onDeleteAiQuestion: (Int) -> Unit = {},
    onOpenEcodeDictionary: () -> Unit = {}
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var editName    by remember(name)    { mutableStateOf(name) }
    var editSurname by remember(surname) { mutableStateOf(surname) }
    var isSavingProfile by remember { mutableStateOf(false) }

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    val initials = buildString {
        if (name.isNotEmpty())    append(name.first().uppercaseChar())
        if (surname.isNotEmpty()) append(surname.first().uppercaseChar())
        if (isEmpty()) append("PR")
    }

    // Şifre değiştir dialog'u
    if (showPasswordDialog) {
        ChangePasswordDialog(
            state = passwordChangeState,
            loading = passwordChangeLoading,
            onSubmit = onChangePassword,
            onDismiss = { showPasswordDialog = false; onClearPasswordState() },
            onClearState = onClearPasswordState
        )
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSavingProfile) showEditDialog = false },
            title = { Text("Profili Düzenle", fontWeight = FontWeight.Bold, color = ForestGreen) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Adınızı ve soyadınızı güncelleyin:", fontSize = 13.sp, color = Color.Gray)
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Adınız") },
                        singleLine = true,
                        enabled = !isSavingProfile,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editSurname,
                        onValueChange = { editSurname = it },
                        label = { Text("Soyadınız") },
                        singleLine = true,
                        enabled = !isSavingProfile,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isSavingProfile) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ForestGreen)
                            Spacer(Modifier.width(8.dp))
                            Text("Sunucuya kaydediliyor...", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.trim().isNotEmpty()) {
                            isSavingProfile = true
                            onUpdateProfile(editName.trim(), editSurname.trim())
                            // Kısa gecikme sonra dialog kapat
                            isSavingProfile = false
                            showEditDialog = false
                        }
                    },
                    enabled = !isSavingProfile && editName.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }, enabled = !isSavingProfile) {
                    Text("İptal", color = Color.Gray)
                }
            }
        )
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Tüm Verileri Sıfırla?", fontWeight = FontWeight.Bold, color = RiskHigh) },
            text = {
                Text("Tüm tarama geçmişinizi, favori gıdalarınızı ve kara listenizi kalıcı olarak silip hesaptan çıkış yapmak istediğinizden emin misiniz? Bu işlem geri alınamaz.")
            },
            confirmButton = {
                Button(
                    onClick = { onClearAllData(); showClearConfirmDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RiskHigh),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Evet, Her Şeyi Sil ve Sıfırla")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Vazgeç", color = Color.Gray)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .size(90.dp)
            .clip(CircleShape)
            .background(SoftMint)
            .border(2.dp, ForestGreen, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = ForestGreen
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Merhaba, $name${if (surname.isNotEmpty()) " $surname" else ""} 👋",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = ForestGreen,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(6.dp))
        IconButton(
            onClick = { showEditDialog = true },
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Profili Düzenle",
                tint = ForestGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    Text(
        text = email,
        fontSize = 13.sp,
        color = Color.Gray,
        modifier = Modifier.padding(bottom = 24.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            title = "Taramalarım",
            count = scannedCount.toString(),
            icon = Icons.Default.QrCodeScanner,
            color = ForestGreen,
            modifier = Modifier.weight(1.0f),
            onClick = { onOpenTab(NavigationTab.HISTORY) }
        )
        StatCard(
            title = "Favorilerim",
            count = favoritesCount.toString(),
            icon = Icons.Default.Favorite,
            color = EcoGreen,
            modifier = Modifier.weight(1.0f),
            onClick = { onOpenTab(NavigationTab.FAVORITES) }
        )
        StatCard(
            title = "Engellerim",
            count = blacklistCount.toString(),
            icon = Icons.Default.Block,
            color = RiskHigh,
            modifier = Modifier.weight(1.0f),
            onClick = { onOpenTab(NavigationTab.BLACKLIST) }
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SoftMint),
        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(ForestGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Verified, contentDescription = null, tint = Color.White)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Gıda Güvenliği Seviyeniz",
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen,
                    fontSize = 14.sp
                )
                Text(
                    text = if (scannedCount == 0) "Tarama yapmaya başlayın!" else "Yüksek Bilinçli Tüketici 🌟",
                    fontSize = 12.sp,
                    color = ForestGreen.copy(alpha = 0.8f)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // ─── Hesap & Güvenlik ──────────────────────────────────────────
    ProfileSectionCard(title = "Hesap & Güvenlik") {
        ProfileActionRow(
            icon = Icons.Default.Edit,
            iconTint = ForestGreen,
            title = "Profili Düzenle",
            subtitle = "Ad ve soyadınızı güncelleyin",
            onClick = { showEditDialog = true }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ProfileActionRow(
            icon = Icons.Default.Lock,
            iconTint = EcoGreen,
            title = "Şifre Değiştir",
            subtitle = "Hesap şifrenizi güncelleyin",
            onClick = { onClearPasswordState(); showPasswordDialog = true }
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ProfileActionRow(
            icon = Icons.Default.CloudSync,
            iconTint = WarmGold,
            title = "Verileri Yenile",
            subtitle = "Taramalar, favoriler ve engelleri sunucudan çek",
            onClick = onRefresh
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // ─── Bilgi & Araçlar ───────────────────────────────────────────
    ProfileSectionCard(title = "Bilgi & Araçlar") {
        ProfileActionRow(
            icon = Icons.Default.Science,
            iconTint = ForestGreen,
            title = "E-Kod Sözlüğü",
            subtitle = "Tüm katkı maddelerini ara, risk/helal/resmi bilgilerini incele",
            onClick = onOpenEcodeDictionary
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // ─── Görünüm / Tema ────────────────────────────────────────────
    ProfileSectionCard(title = "Görünüm") {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeOption("Açık", Icons.Default.LightMode, themePref == "light", Modifier.weight(1f)) { onSetTheme("light") }
            ThemeOption("Koyu", Icons.Default.DarkMode, themePref == "dark", Modifier.weight(1f)) { onSetTheme("dark") }
            ThemeOption("Sistem", Icons.Default.SettingsBrightness, themePref != "light" && themePref != "dark", Modifier.weight(1f)) { onSetTheme("system") }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // ─── AI Hızlı Sorularım ────────────────────────────────────────
    ProfileSectionCard(title = "AI Hızlı Sorularım") {
        var newQuestion by remember { mutableStateOf("") }
        Text(
            text = "Ürün ekranındaki \"Yapay Zekâya Sor\" alanında çıkacak kendi hızlı sorularınızı ekleyin.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newQuestion,
                onValueChange = { newQuestion = it },
                placeholder = { Text("Örn: Spor yapan biri için uygun mu?", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreen)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { if (newQuestion.trim().length >= 3) { onAddAiQuestion(newQuestion.trim()); newQuestion = "" } },
                enabled = newQuestion.trim().length >= 3,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Ekle")
            }
        }

        val myQuestions = aiQuestions.filter { it.source == "user" }
        if (myQuestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            myQuestions.forEach { q ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(q.question, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                    IconButton(onClick = { onDeleteAiQuestion(q.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Sil", tint = RiskHigh, modifier = Modifier.size(20.dp))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    OutlinedButton(
        onClick = onLogout,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("logout_button"),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ForestGreen),
        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.ExitToApp, contentDescription = null, tint = ForestGreen)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Hesaptan Güvenli Çıkış Yap", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = ForestGreen)
    }

    Spacer(modifier = Modifier.height(10.dp))

    Button(
        onClick = { showClearConfirmDialog = true },
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("clear_all_data_button"),
        colors = ButtonDefaults.buttonColors(containerColor = RiskHigh.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, RiskHigh.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp),
        elevation = null
    ) {
        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RiskHigh)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Tarihçeyi & Verileri Sıfırla", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RiskHigh)
    }
}

@Composable
fun StatCard(
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = count,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = color
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Başlıklı bölüm kartı (profil ayar grupları için). */
@Composable
fun ProfileSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = ForestGreen,
                fontSize = 15.sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            content()
        }
    }
}

/** Tıklanabilir ayar satırı (ikon + başlık + alt metin + ok). */
@Composable
fun ProfileActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}

/** Tema seçim kutusu (Açık/Koyu/Sistem). */
@Composable
fun ThemeOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val border = if (selected) ForestGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val bg     = if (selected) SoftMint else Color.Transparent
    val tint   = if (selected) ForestGreen else Color.Gray
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(if (selected) 2.dp else 1.dp, border, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = tint)
    }
}

/** Şifre değiştirme dialog'u. state: null=boşta, "success"=başarılı, diğer=hata mesajı. */
@Composable
fun ChangePasswordDialog(
    state: String?,
    loading: Boolean,
    onSubmit: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onClearState: () -> Unit
) {
    var currentPw by remember { mutableStateOf("") }
    var newPw     by remember { mutableStateOf("") }
    var confirmPw by remember { mutableStateOf("") }
    var localErr  by remember { mutableStateOf("") }

    // Başarılı olduğunda dialog'u kapat
    LaunchedEffect(state) {
        if (state == "success") {
            onDismiss()
        }
    }
    val serverErr = if (state != null && state != "success") state else ""

    AlertDialog(
        onDismissRequest = { if (!loading) onDismiss() },
        title = { Text("Şifre Değiştir", fontWeight = FontWeight.Bold, color = ForestGreen) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = currentPw,
                    onValueChange = { currentPw = it; localErr = ""; if (state != null) onClearState() },
                    label = { Text("Mevcut Şifre") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, enabled = !loading,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = newPw,
                    onValueChange = { newPw = it; localErr = ""; if (state != null) onClearState() },
                    label = { Text("Yeni Şifre (min 8)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, enabled = !loading,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPw,
                    onValueChange = { confirmPw = it; localErr = ""; if (state != null) onClearState() },
                    label = { Text("Yeni Şifre (Tekrar)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true, enabled = !loading,
                    shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
                )
                val shown = localErr.ifEmpty { serverErr }
                AnimatedVisibility(visible = shown.isNotEmpty()) {
                    Text(shown, color = RiskHigh, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                if (loading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = ForestGreen)
                        Spacer(Modifier.width(8.dp))
                        Text("Güncelleniyor...", fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    localErr = when {
                        currentPw.isBlank() || newPw.isBlank() || confirmPw.isBlank() -> "Tüm alanlar zorunlu."
                        newPw.length < 8 -> "Yeni şifre en az 8 karakter olmalı."
                        newPw != confirmPw -> "Yeni şifreler eşleşmiyor."
                        else -> ""
                    }
                    if (localErr.isEmpty()) onSubmit(currentPw.trim(), newPw.trim())
                },
                enabled = !loading,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Güncelle") }
        },
        dismissButton = {
            TextButton(onClick = { if (!loading) onDismiss() }) { Text("İptal", color = Color.Gray) }
        }
    )
}
