package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NavigationTab
import com.example.ui.ProductViewModel
import com.example.ui.ScanUiState
import com.example.ui.theme.BrightLeaf
import com.example.ui.theme.DeepGreen
import com.example.ui.theme.EcoGreen
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.RiskHigh
import com.example.ui.theme.SoftMint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: ProductViewModel,
    modifier: Modifier = Modifier
) {
    val ForestGreen = MaterialTheme.colorScheme.primary
    val currentTab by viewModel.currentTab.collectAsState()
    val scanState by viewModel.scanState.collectAsState()

    // Tarama/analiz başarıyla tamamlandığında haptik geri bildirim.
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(scanState) {
        if (scanState is ScanUiState.Success) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // E-Kod Sözlüğü tam ekran overlay (Profil'den açılır) — her şeyin üstünde.
    val showEcodeDict by viewModel.showEcodeDictionary.collectAsState()
    if (showEcodeDict) {
        EcodeDictionaryScreen(viewModel = viewModel, onBack = { viewModel.closeEcodeDictionary() })
        return
    }

    // Geri tuşu: önce ürün/onay ekranından çık → sonra ana sekmeye dön → en sonda çıkış onayı sor.
    val context = androidx.compose.ui.platform.LocalContext.current
    var showExitDialog by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = true) {
        when {
            scanState !is ScanUiState.Idle -> viewModel.clearActiveProductState()
            currentTab != NavigationTab.SCAN -> viewModel.setTab(NavigationTab.SCAN)
            else -> showExitDialog = true
        }
    }
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            icon = { Icon(Icons.Filled.Logout, contentDescription = null, tint = ForestGreen) },
            title = { Text("Çıkmak istiyor musunuz?", fontWeight = FontWeight.Bold) },
            text = { Text("Uygulamadan çıkmak üzeresiniz.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitDialog = false
                    (context as? android.app.Activity)?.finish()
                }) { Text("Evet, Çık", color = RiskHigh, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("Vazgeç") }
            }
        )
    }

    // Base Scaffold handles main layout and bottom navigation
    Scaffold(
        bottomBar = {
            // Alt menü: ana sekmelerde ve ürün değerlendirme (Success) ekranında görünür.
            // Onay/yükleme/hata ekranlarında gizli kalır (yanlışlıkla geçişi önlemek için).
            if (scanState is ScanUiState.Idle || scanState is ScanUiState.Success) {
                FreshBottomBar(
                    currentTab = currentTab,
                    onSelect = { viewModel.setTab(it) }
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = modifier
    ) { innerPadding ->
        // State Machine intercepts screen contents based on active scanState
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = scanState) {
                is ScanUiState.Idle -> {
                    // Render selected navigation tab content
                    when (currentTab) {
                        NavigationTab.SCAN -> ScanSearchScreen(viewModel = viewModel)
                        NavigationTab.FAVORITES -> FavoritesScreen(viewModel = viewModel)
                        NavigationTab.BLACKLIST -> BlacklistScreen(viewModel = viewModel)
                        NavigationTab.COMPARE -> CompareScreen(viewModel = viewModel)
                        NavigationTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                        NavigationTab.PROFILE -> ProfileScreen(viewModel = viewModel)
                    }
                }

                is ScanUiState.ConfirmLocal -> {
                    ConfirmLocalScreen(
                        product = state.product,
                        viewModel = viewModel
                    )
                }

                is ScanUiState.ConfirmWeb -> {
                    ConfirmWebScreen(
                        barcode = state.barcode,
                        brand = state.brand,
                        name = state.name,
                        category = state.category,
                        grammage = state.grammage,
                        imageUrl = state.imageUrl,
                        rawProduct = state.rawProduct,
                        viewModel = viewModel
                    )
                }

                is ScanUiState.Loading -> {
                    // High quality, full-screen shimmering loader visual
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = ForestGreen,
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Yapay Zekâ Analiz Ediyor...",
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Gıda veritabanı taranıyor, içindekiler listeleniyor ve katkı maddesi E-kodları sağlık açısından yorumlanıyor.",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }

                is ScanUiState.Success -> {
                    // Immersive product evaluation sheet
                    ProductDetailsScreen(
                        product = state.product,
                        warnings = state.blacklistWarnings,
                        viewModel = viewModel,
                        onBack = { viewModel.clearActiveProductState() }
                    )
                }

                is ScanUiState.Error -> {
                    // Full screen diagnostic card with recover buttons
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Hata",
                            tint = RiskHigh,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Analiz yapılamadı",
                            fontWeight = FontWeight.Bold,
                            color = RiskHigh,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.message,
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.clearActiveProductState() },
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Tekrar Deneyin", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/* ------------------------------------------------------------------ */
/* Modern alt menü: ortada yüzen tarama FAB'ı + iki yanda 2'şer sekme  */
/* ------------------------------------------------------------------ */

private data class BarTab(
    val tab: NavigationTab,
    val label: String,
    val filled: ImageVector,
    val outlined: ImageVector
)

@Composable
private fun FreshBottomBar(
    currentTab: NavigationTab,
    onSelect: (NavigationTab) -> Unit
) {
    val leftTabs = listOf(
        BarTab(NavigationTab.HISTORY, "Geçmiş", Icons.Filled.History, Icons.Outlined.History),
        BarTab(NavigationTab.FAVORITES, "Favoriler", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder)
    )
    val rightTabs = listOf(
        BarTab(NavigationTab.BLACKLIST, "Kara Liste", Icons.Filled.Block, Icons.Outlined.Block),
        BarTab(NavigationTab.PROFILE, "Profil", Icons.Filled.Person, Icons.Outlined.Person)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("bottom_nav_bar")
    ) {
        // Bar yüzeyi (slotu tam doldurur; FAB bunun ÜSTÜNE içeriğe taşar → gri şerit yok)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            shadowElevation = 16.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leftTabs.forEach { item ->
                    BarItem(item, currentTab == item.tab, Modifier.weight(1f)) { onSelect(item.tab) }
                }
                Spacer(Modifier.weight(1f)) // FAB için orta boşluk
                rightTabs.forEach { item ->
                    BarItem(item, currentTab == item.tab, Modifier.weight(1f)) { onSelect(item.tab) }
                }
            }
        }

        // Ortada yüzen tarama FAB'ı — barın üst kenarından içeriğe taşar
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-26).dp)
                .size(64.dp)
                .shadow(12.dp, CircleShape, spotColor = ForestGreen)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(listOf(BrightLeaf, ForestGreen))
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onSelect(NavigationTab.SCAN) }
                .testTag("nav_tab_scan"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.QrCodeScanner,
                contentDescription = "Barkod Tara",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

@Composable
private fun BarItem(
    item: BarTab,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .testTag("nav_tab_${item.tab.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) item.filled else item.outlined,
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = item.label,
            color = tint,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}
