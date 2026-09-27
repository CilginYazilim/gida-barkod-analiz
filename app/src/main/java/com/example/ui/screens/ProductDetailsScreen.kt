package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SavedProduct
import com.example.data.network.Ci4Comment
import com.example.data.network.Ci4Ecode
import com.example.data.network.GeminiApiHelper
import com.example.data.network.GeminiProductAnalysis
import com.example.ui.ProductViewModel
import com.example.ui.theme.*

// Parsed Additive structure
data class ParsedAdditive(
    val code: String,
    val name: String,
    val category: String,
    val risk: String,
    val description: String,
    val whoShouldAvoid: String
)

fun parseAdditives(raw: String): List<ParsedAdditive> {
    if (raw.trim().isEmpty()) return emptyList()
    return raw.split(",").mapNotNull { part ->
        val subParts = part.split(":")
        if (subParts.size >= 6) {
            ParsedAdditive(
                code = subParts[0],
                name = subParts[1],
                category = subParts[2],
                risk = subParts[3],
                description = subParts[4].replace(";", ","),
                whoShouldAvoid = subParts[5].replace(";", ",")
            )
        } else null
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailsScreen(
    product: SavedProduct,
    warnings: List<String>,
    viewModel: ProductViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val ForestGreen = MaterialTheme.colorScheme.primary
    var chatInput by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val chatMessages by viewModel.chatMessages.collectAsState()
    val chatLoading by viewModel.chatLoading.collectAsState()

    // Yorumlar
    val comments by viewModel.comments.collectAsState()
    val commentsLoading by viewModel.commentsLoading.collectAsState()
    val commentSubmitState by viewModel.commentSubmitState.collectAsState()
    val accessToken by viewModel.accessToken.collectAsState()
    var newCommentText by remember(product.barcode) { mutableStateOf("") }
    var newCommentRating by remember(product.barcode) { mutableIntStateOf(0) }

    // AI varsayılan sorular (SSS)
    val aiQuestions by viewModel.aiQuestions.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadAiQuestions() }

    // E-Kod detay alt sayfası
    val ecodeDetail by viewModel.ecodeDetail.collectAsState()
    val ecodeLoading by viewModel.ecodeLoading.collectAsState()

    // Fiyat geçmişi
    val priceHistory by viewModel.priceHistory.collectAsState()
    val priceReportMessage by viewModel.priceReportMessage.collectAsState()
    var priceReportInput by remember(product.barcode) { mutableStateOf("") }

    // Düzeltme talebi
    val correctionSubmitState by viewModel.correctionSubmitState.collectAsState()
    var correctionField by remember(product.barcode) { mutableStateOf("genel") }
    var correctionSuggested by remember(product.barcode) { mutableStateOf("") }
    var correctionNote by remember(product.barcode) { mutableStateOf("") }

    LaunchedEffect(product.barcode) {
        viewModel.clearCommentSubmitState()
        viewModel.clearCorrectionSubmitState()
        viewModel.clearPriceReportMessage()
        viewModel.loadComments(product.barcode)
        viewModel.loadPriceHistory(product.barcode)
    }
    LaunchedEffect(correctionSubmitState) {
        if (correctionSubmitState is com.example.ui.CorrectionSubmitState.Success) {
            correctionSuggested = ""
            correctionNote = ""
            correctionField = "genel"
        }
    }
    // Başarılı gönderimden sonra formu temizle
    LaunchedEffect(commentSubmitState) {
        if (commentSubmitState is com.example.ui.CommentSubmitState.Success) {
            newCommentText = ""
            newCommentRating = 0
        }
    }

    var userNotesState by remember(product.barcode) { mutableStateOf(product.userNotes) }
    var feedbackTypeState by remember { mutableStateOf("ŞİKAYET") } // "ŞİKAYET" | "ÖNERİ" | "İSTEK"
    var feedbackTextState by remember(product.barcode) { mutableStateOf(product.userFeedback) }
    var notesSavedSuccessfully by remember { mutableStateOf(false) }
    var feedbackSavedSuccessfully by remember { mutableStateOf(false) }

    // Deserialize rich analysis state
    val analysis = remember(product) {
        try {
            val adapter = com.example.data.network.GeminiRetrofitClient.moshi.adapter(com.example.data.network.GeminiProductAnalysis::class.java)
            adapter.fromJson(product.aiVerdict)
        } catch (e: Exception) {
            null
        }
    }

    val additivesList = remember(product) {
        parseAdditives(product.eCodes)
    }

    // Color code based on score
    val scoreColor = when {
        product.healthScore >= 75 -> RiskLow
        product.healthScore >= 50 -> RiskMedium
        else -> RiskHigh
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ürün Değerlendirmesi", fontWeight = FontWeight.Bold, color = ForestGreen) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("details_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = ForestGreen)
                    }
                },
                actions = {
                    // Paylaş butonu
                    val shareContext = LocalContext.current
                    IconButton(
                        onClick = { shareProductSummary(shareContext, product) },
                        modifier = Modifier.testTag("details_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Paylaş", tint = ForestGreen)
                    }
                    // Favorite Heart Toggle button
                    IconButton(
                        onClick = { viewModel.toggleFavorite(product) },
                        modifier = Modifier.testTag("details_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (product.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favori",
                            tint = if (product.isFavorite) Color.Red else ForestGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Blacklist Warning alerts
            if (warnings.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(2.dp, RiskHigh, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEEBEE))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Uyarı", tint = RiskHigh)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "KARA LİSTE TEHLİKESİ!",
                                fontWeight = FontWeight.Bold,
                                color = RiskHigh,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        warnings.forEach { warning ->
                            Text(
                                text = warning,
                                color = RiskHigh,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // 1.5 Kısıtlı / Yasaklı katkı uyarısı (E-Kod sözlüğüyle çapraz kontrol)
            val restrictedAdditives by produceState(initialValue = emptyList<Ci4Ecode>(), additivesList) {
                value = viewModel.restrictedAdditives(additivesList.map { it.code })
            }
            if (restrictedAdditives.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .border(2.dp, RiskHigh, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFEEBEE))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = RiskHigh)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "MEVZUATTA KISITLI KATKI İÇERİYOR",
                                fontWeight = FontWeight.Bold,
                                color = RiskHigh,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        restrictedAdditives.forEach { e ->
                            Text(
                                text = "• ${e.code}${if (!e.nameTr.isNullOrBlank()) " ${e.nameTr}" else ""}: ${e.bannedNote}",
                                color = RiskHigh,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // 2. Ürün başlığı (beyaz kart: görsel + marka/ad/gramaj + skor rozeti)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SoftMint),
                        contentAlignment = Alignment.Center
                    ) {
                        if (product.imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = product.imageUrl,
                                contentDescription = product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = when (product.category.lowercase()) {
                                    "içecek" -> Icons.Default.LocalDrink
                                    else -> Icons.Default.Restaurant
                                },
                                contentDescription = null,
                                tint = ForestGreen,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = product.brand,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                        Text(
                            text = product.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (product.grammage.isNotEmpty() && product.grammage != "Bilinmiyor") {
                            Text(
                                text = product.grammage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "Barkod: ${product.barcode}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    // Skor rozeti
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Skor", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(scoreColor)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("${product.healthScore}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                Text("/100", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            // 3. Genel değerlendirme açıklaması
            if (product.healthExplanation.isNotBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(scoreColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Insights, contentDescription = null, tint = scoreColor, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Genel Değerlendirme",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            Text(
                                text = product.healthExplanation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3.4 Nutri-Score sub-scores (A-E) + value tags
            val scoreTagList = remember(product.scoreTags) {
                product.scoreTags.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            }
            val hasSubScores = product.sugarScore > 0 || product.additiveScore > 0 || product.nutritionScore > 0
            if (hasSubScores || scoreTagList.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nutri-Score Değerlendirmesi",
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                        fontSize = 16.sp
                    )
                    if (product.nutriGrade.isNotBlank()) {
                        val gradeColor = when (product.nutriGrade.uppercase()) {
                            "A" -> Color(0xFF2E7D32); "B" -> Color(0xFF7CB342)
                            "C" -> Color(0xFFF9A825); "D" -> Color(0xFFEF6C00); else -> Color(0xFFD32F2F)
                        }
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(gradeColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(product.nutriGrade.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        }
                    }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            NutriScoreBadge("Genel Sağlık", product.healthScore, Modifier.weight(1f))
                            NutriScoreBadge("Şeker Riski", product.sugarScore, Modifier.weight(1f))
                            NutriScoreBadge("Katkı Yoğunluğu", product.additiveScore, Modifier.weight(1f))
                            NutriScoreBadge("Besin Kalitesi", product.nutritionScore, Modifier.weight(1f))
                        }

                        if (scoreTagList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                scoreTagList.forEach { tag -> ScoreTagChip(tag) }
                            }
                        }
                    }
                }
            }

            // 3.45 Fiyat Durumu & Geçmişi
            if (!product.barcode.startsWith("RESIM_") && !product.barcode.startsWith("IMAGE_")) {
                val currentPrice = priceHistory?.currentPrice ?: product.price
                val history = priceHistory?.history ?: emptyList()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PriceChange, contentDescription = "Fiyat", tint = ForestGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Fiyat Durumu & Geçmişi",
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (currentPrice != null && currentPrice > 0) "₺${"%.2f".format(currentPrice)}" else "Fiyat yok",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = if (currentPrice != null && currentPrice > 0) EcoGreen else Color.Gray
                            )
                        }

                        // Fiyat bildir
                        Spacer(modifier = Modifier.height(10.dp))
                        if (accessToken.isNullOrBlank()) {
                            Text(
                                text = "Gördüğünüz fiyatı bildirmek için Profil sekmesinden giriş yapın.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = priceReportInput,
                                    onValueChange = { v ->
                                        val normalized = v.replace(',', '.')
                                        if (normalized.isEmpty() || normalized.matches(Regex("^\\d{0,6}(\\.\\d{0,2})?$"))) priceReportInput = normalized
                                    },
                                    placeholder = { Text("Gördüğünüz fiyat", fontSize = 12.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    leadingIcon = { Text("₺", fontWeight = FontWeight.Bold, color = ForestGreen) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        val p = priceReportInput.trim().toDoubleOrNull()
                                        if (p != null && p > 0) {
                                            viewModel.reportPrice(product.barcode, p)
                                            priceReportInput = ""
                                        }
                                    },
                                    enabled = priceReportInput.trim().toDoubleOrNull()?.let { it > 0 } == true,
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Bildir", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            priceReportMessage?.let { msg ->
                                Text(
                                    text = msg,
                                    color = if (msg.contains("kaydedildi")) RiskLow else RiskHigh,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        // Geçmiş listesi
                        if (history.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Geçmiş Fiyatlar",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            history.take(10).forEach { entry ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = entry.recordedAt?.take(10) ?: "-",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    if (!entry.notes.isNullOrBlank()) {
                                        Text(
                                            text = entry.notes,
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            modifier = Modifier.weight(1f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Text(
                                        text = "₺${"%.2f".format(entry.price)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = ForestGreen
                                    )
                                }
                                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
                            }
                        }
                    }
                }
            }

            // 3.5 Helallik Durumu — 4 durumlu (sertifikali | muhtemel_helal | supheli | helal_degil)
            val halalStatus = product.halalStatus.ifBlank { if (product.isHalal) "muhtemel_helal" else "supheli" }
            val (halalLabel, halalColor, halalIcon) = when (halalStatus) {
                "sertifikali" -> Triple("Helal Sertifikalı", RiskLow, Icons.Default.VerifiedUser)
                "muhtemel_helal" -> Triple("Muhtemel Helal", RiskLow, Icons.Default.CheckCircle)
                "helal_degil" -> Triple("Helal Değil", RiskHigh, Icons.Default.Cancel)
                else -> Triple("Şüpheli", RiskMedium, Icons.Default.HelpOutline)
            }
            val halalDefaultText = when (halalStatus) {
                "sertifikali" -> "Doğrulanmış resmi bir helal sertifikası bulunmaktadır."
                "muhtemel_helal" -> "İçeriğinde haram veya şüpheli bir bileşen tespit edilmedi; ancak doğrulanmış sertifika olmadığından kesin değildir."
                "helal_degil" -> "İçeriğinde helal olmayan (alkol/domuz türevi vb.) bir bileşen olabileceği değerlendirildi."
                else -> "İçeriğinde kaynağı belirsiz (jelatin, gliserin, E471 vb.) bileşenler olabilir; kesinlik için kendi araştırmanızı yapınız."
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = halalColor.copy(alpha = 0.08f)),
                border = BorderStroke(1.dp, halalColor.copy(alpha = 0.40f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Helallik Durumu",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(halalColor)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(halalIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(halalLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = product.halalExplanation.ifBlank { halalDefaultText },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bu değerlendirme bilgilendirme amaçlıdır; resmi/dini kesin hüküm değildir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            // 4. Diet Grid (Vegan, Gluten-free compatibility)
            val activeAnalysis = analysis
            if (activeAnalysis != null) {
                Text(
                    text = "Gıda ve Diyet Uygunluğu",
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            DietBadge(label = "Vegan", isEligible = activeAnalysis.isVegan, modifier = Modifier.weight(1f))
                            DietBadge(label = "Vejetaryen", isEligible = activeAnalysis.isVegetarian, modifier = Modifier.weight(1f))
                            DietBadge(label = "Glutensiz", isEligible = activeAnalysis.isGlutenFree, modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            DietBadge(label = "Laktozsuz", isEligible = activeAnalysis.isLactoseFree, modifier = Modifier.weight(1f))
                            DietBadge(label = "Çocuklara Uygun", isEligible = activeAnalysis.isSuitableForChildren, modifier = Modifier.weight(1f))
                            DietBadge(label = "Diyabete Uygun", isEligible = activeAnalysis.isSuitableForDiabetics, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // 4.5 Alerjenler
            val allergenList = remember(product.allergens) {
                product.allergens.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            }
            if (allergenList.isNotEmpty()) {
                Text(
                    text = "Alerjen Uyarıları",
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allergenList.forEach { a ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(RiskMedium.copy(alpha = 0.12f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = RiskMedium, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(a, color = RiskMedium, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 5. Additives E-Codes list
            if (additivesList.isNotEmpty()) {
                Text(
                    text = "Tespit Edilen Katkı Maddeleri (E-Kodları)",
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(bottom = 8.dp)
                )

                additivesList.forEach { additive ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .clickable { viewModel.loadEcodeDetail(additive.code) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SoftMint.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(ForestGreen, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = additive.code,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = additive.name,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestGreen,
                                        fontSize = 14.sp
                                    )
                                }

                                // Risk badge chip
                                val (badgeBg, badgeText) = when (additive.risk.lowercase().trim()) {
                                    "yüksek" -> Pair(RiskHigh.copy(alpha = 0.15f), RiskHigh)
                                    "orta" -> Pair(RiskMedium.copy(alpha = 0.15f), RiskMedium)
                                    else -> Pair(RiskLow.copy(alpha = 0.15f), RiskLow)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(badgeBg, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${additive.risk} Risk",
                                        color = badgeText,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Kategori: ${additive.category}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = EcoGreen
                            )
                            Text(
                                text = additive.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            if (additive.whoShouldAvoid.isNotEmpty() && additive.whoShouldAvoid != "Bilinmiyor" && additive.whoShouldAvoid != "Yok") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Dikkat Etmesi Gerekenler: ${additive.whoShouldAvoid}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RiskHigh,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 6. Ingredients & Nutrition facts
            Text(
                text = "İçindekiler ve Besin Değerleri",
                fontWeight = FontWeight.Bold,
                color = ForestGreen,
                fontSize = 16.sp,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(vertical = 8.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "İçindekiler Listesi",
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = product.ingredients,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "100g Besin Değerleri",
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    NutritionFactsSection(product.nutritionFacts)
                }
            }

            // 7. General AI Verdict commentary
            if (analysis != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ForestGreen),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Stars, contentDescription = "AI", tint = WarmGold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Yapay Zekâ Sağlık Özeti",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = analysis.aiVerdict,
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // 7.2 Personal Notes & Suggestions Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Kullanıcı Notları & İstekleri",
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // 1. Text Field for Private Notes
                    Text(
                        text = "Kişisel Notlarım",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    OutlinedTextField(
                        value = userNotesState,
                        onValueChange = { userNotesState = it },
                        placeholder = { Text("Bu ürün hakkındaki kişisel notlarınızı buraya yazın (Örn: Alerjen içeriyor, evde 2 paket var vb.)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForestGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.updateProductNotes(product.barcode, userNotesState)
                            notesSavedSuccessfully = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Notu Kaydet", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (notesSavedSuccessfully) {
                        Text(
                            text = "✔️ Not başarıyla kaydedildi!",
                            color = RiskLow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp).align(Alignment.End)
                        )
                        LaunchedEffect(product.barcode) {
                            kotlinx.coroutines.delay(2000)
                            notesSavedSuccessfully = false
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // 2. Complaint & Request Section
                    Text(
                        text = "Şikayet, İstek veya Öneri Bildir",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ŞİKAYET", "ÖNERİ", "İSTEK").forEach { type ->
                            ElevatedFilterChip(
                                selected = feedbackTypeState == type,
                                onClick = { feedbackTypeState = type },
                                label = { Text(type, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.elevatedFilterChipColors(
                                    selectedContainerColor = EcoGreen.copy(alpha = 0.2f),
                                    selectedLabelColor = ForestGreen
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = feedbackTextState,
                        onValueChange = { feedbackTextState = it },
                        placeholder = { Text("Örn: Gramaj eksik yazılmış, içerikler güncellenmeli veya ürün şikayeti/görüşleriniz...") },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForestGreen,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val formattedFeedback = "[$feedbackTypeState] $feedbackTextState"
                            viewModel.updateProductFeedback(product.barcode, formattedFeedback)
                            feedbackSavedSuccessfully = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EcoGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Geri Bildirimi Kaydet", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    if (feedbackSavedSuccessfully) {
                        Text(
                            text = "✔️ Geri bildiriminiz kaydedildi!",
                            color = RiskLow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp).align(Alignment.End)
                        )
                        LaunchedEffect(product.barcode) {
                            kotlinx.coroutines.delay(2000)
                            feedbackSavedSuccessfully = false
                        }
                    }
                }
            }

            // 7.25 Hata Bildirimi / Düzeltme Talebi (sunucuya gider)
            if (!product.barcode.startsWith("RESIM_") && !product.barcode.startsWith("IMAGE_")) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ReportProblem, contentDescription = "Hata Bildir", tint = RiskMedium)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Hata Bildirimi / Düzeltme Talebi",
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "Üründe yanlış veya eksik bilgi varsa bize bildir; incelenip düzeltilecek.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        if (accessToken.isNullOrBlank()) {
                            Text(
                                text = "Düzeltme talebi göndermek için Profil sekmesinden giriş yapın.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        } else {
                            val fieldOptions = listOf(
                                "name" to "Ürün Adı",
                                "brand" to "Marka",
                                "category" to "Kategori",
                                "grammage" to "Gramaj",
                                "ingredients" to "İçindekiler",
                                "genel" to "Diğer"
                            )
                            Text("Hangi alan?", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                fieldOptions.forEach { (key, label) ->
                                    ElevatedFilterChip(
                                        selected = correctionField == key,
                                        onClick = { correctionField = key },
                                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        colors = FilterChipDefaults.elevatedFilterChipColors(
                                            selectedContainerColor = EcoGreen.copy(alpha = 0.2f),
                                            selectedLabelColor = ForestGreen
                                        )
                                    )
                                }
                            }

                            val currentValue = when (correctionField) {
                                "name" -> product.name
                                "brand" -> product.brand
                                "category" -> product.category
                                "grammage" -> product.grammage
                                "ingredients" -> product.ingredients
                                else -> ""
                            }
                            if (correctionField != "genel" && currentValue.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Mevcut: $currentValue",
                                    fontSize = 12.sp,
                                    color = RiskHigh,
                                    maxLines = 2
                                )
                            }

                            if (correctionField != "genel") {
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = correctionSuggested,
                                    onValueChange = { correctionSuggested = it },
                                    placeholder = { Text("Doğru olması gereken değer", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = ForestGreen,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = correctionNote,
                                onValueChange = { correctionNote = it },
                                placeholder = { Text("Açıklama (isteğe bağlı)", fontSize = 12.sp) },
                                modifier = Modifier.fillMaxWidth().height(72.dp),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ForestGreen,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val sendingCorrection = correctionSubmitState is com.example.ui.CorrectionSubmitState.Loading
                            Button(
                                onClick = {
                                    viewModel.submitCorrection(
                                        barcode = product.barcode,
                                        field = correctionField,
                                        currentValue = currentValue,
                                        suggestedValue = correctionSuggested,
                                        note = correctionNote
                                    )
                                },
                                enabled = !sendingCorrection &&
                                    (correctionSuggested.trim().isNotEmpty() || correctionNote.trim().isNotEmpty()),
                                colors = ButtonDefaults.buttonColors(containerColor = RiskMedium),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                if (sendingCorrection) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("Talebi Gönder", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            when (val st = correctionSubmitState) {
                                is com.example.ui.CorrectionSubmitState.Success -> Text(
                                    text = "✔️ ${st.message}",
                                    color = RiskLow, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                is com.example.ui.CorrectionSubmitState.Error -> Text(
                                    text = "⚠️ ${st.message}",
                                    color = RiskHigh, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                else -> {}
                            }
                        }
                    }
                }
            }

            // 7.3 Instant Blacklist Addition Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Hızlı Kara Liste Ekleme",
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "Aşağıdaki ögeleri tek tıkla kara listenize ekleyebilir veya çıkarabilirsiniz. Kara listedeki maddeler taramalarda anında kırmızı uyarı verir.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val blackListFlowItems by viewModel.blacklistItems.collectAsState()

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Product Name chip
                        val isProductBlacklisted = blackListFlowItems.any { it.value.lowercase().trim() == product.name.lowercase().trim() }
                        FilterChip(
                            selected = isProductBlacklisted,
                            onClick = {
                                if (isProductBlacklisted) {
                                    viewModel.removeFromBlacklistByValue(product.name)
                                } else {
                                    viewModel.addToBlacklist(product.name, "PRODUCT")
                                }
                            },
                            label = { Text("Ürün: " + product.name, fontSize = 11.sp) }
                        )

                        // Brand Name chip
                        val isBrandBlacklisted = blackListFlowItems.any { it.value.lowercase().trim() == product.brand.lowercase().trim() }
                        FilterChip(
                            selected = isBrandBlacklisted,
                            onClick = {
                                if (isBrandBlacklisted) {
                                    viewModel.removeFromBlacklistByValue(product.brand)
                                } else {
                                    viewModel.addToBlacklist(product.brand, "BRAND")
                                }
                            },
                            label = { Text("Marka: " + product.brand, fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Özel İçerik Voya Katkı Maddesi Ekle",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    var customBlacklistItem by remember { mutableStateOf("") }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customBlacklistItem,
                            onValueChange = { customBlacklistItem = it },
                            placeholder = { Text("Örn: E120, Jelatin, Palm Yağı vb.", fontSize = 12.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (customBlacklistItem.trim().isNotEmpty()) {
                                    viewModel.addToBlacklist(customBlacklistItem.trim(), "INGREDIENT")
                                    customBlacklistItem = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = RiskHigh),
                            shape = RoundedCornerShape(10.dp),
                            enabled = customBlacklistItem.trim().isNotEmpty()
                        ) {
                            Text("Ekle", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 7.5 Kullanıcı Yorumları + Puanlama + Beğeni
            if (!product.barcode.startsWith("RESIM_") && !product.barcode.startsWith("IMAGE_")) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RateReview, contentDescription = "Yorumlar", tint = EcoGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kullanıcı Yorumları",
                                fontWeight = FontWeight.Bold,
                                color = ForestGreen,
                                fontSize = 16.sp
                            )
                            if (comments.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("(${comments.size})", color = Color.Gray, fontSize = 13.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        // --- Yorum yazma formu ---
                        if (accessToken.isNullOrBlank()) {
                            Text(
                                text = "Yorum yapmak ve puanlamak için Profil sekmesinden giriş yapın.",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        } else {
                            Text(
                                text = "Puanın:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            StarRatingInput(
                                rating = newCommentRating,
                                onRatingChange = { newCommentRating = it }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = newCommentText,
                                onValueChange = { newCommentText = it },
                                placeholder = { Text("Bu ürün hakkındaki görüşünü paylaş...") },
                                modifier = Modifier.fillMaxWidth().height(90.dp),
                                shape = RoundedCornerShape(12.dp),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ForestGreen,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val submitting = commentSubmitState is com.example.ui.CommentSubmitState.Loading
                            Button(
                                onClick = {
                                    viewModel.submitComment(
                                        product.barcode,
                                        newCommentText,
                                        newCommentRating.takeIf { it > 0 }
                                    )
                                },
                                enabled = newCommentText.trim().isNotEmpty() && !submitting,
                                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                if (submitting) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("Yorumu Gönder", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            when (val st = commentSubmitState) {
                                is com.example.ui.CommentSubmitState.Success -> Text(
                                    text = "✔️ ${st.message}",
                                    color = RiskLow, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                is com.example.ui.CommentSubmitState.Error -> Text(
                                    text = "⚠️ ${st.message}",
                                    color = RiskHigh, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                else -> {}
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(12.dp))

                        // --- Yorum listesi ---
                        if (commentsLoading) {
                            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = EcoGreen, strokeWidth = 2.dp)
                            }
                        } else if (comments.isEmpty()) {
                            Text(
                                text = "Henüz yorum yok. İlk yorumu sen yap!",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            comments.forEach { c ->
                                CommentItem(
                                    comment = c,
                                    onLike = { viewModel.toggleCommentLike(c.id) }
                                )
                            }
                        }
                    }
                }
            }

            // 8. Interactive AI Food Chat
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Sohbet", tint = EcoGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Yapay Zekâya Sorun",
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen,
                            fontSize = 16.sp
                        )
                    }

                    Text(
                        text = "Bu ürünün sağlığa etkileri veya çocuk tüketimi hakkında sormak istediklerinizi aşağıya yazın.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Varsayılan hızlı sorular (SSS) — tıklayınca AI'ya sorulur
                    if (aiQuestions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hızlı Sorular",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            aiQuestions.forEach { q ->
                                AssistChip(
                                    onClick = {
                                        if (!chatLoading) viewModel.sendChatMessage(q.question)
                                    },
                                    label = { Text(q.question, fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(14.dp), tint = EcoGreen)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Chat messages list
                    if (chatMessages.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SoftMint.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            chatMessages.forEach { (text, isUser) ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    val bubbleBg = if (isUser) ForestGreen else Color.White
                                    val bubbleText = if (isUser) Color.White else CharcoalText
                                    val bubbleBorder = if (isUser) Modifier else Modifier.border(1.dp, ForestGreen.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(
                                                RoundedCornerShape(
                                                    topStart = 12.dp,
                                                    topEnd = 12.dp,
                                                    bottomStart = if (isUser) 12.dp else 0.dp,
                                                    bottomEnd = if (isUser) 0.dp else 12.dp
                                                )
                                            )
                                            .background(bubbleBg)
                                            .then(bubbleBorder)
                                            .padding(10.dp)
                                            .widthIn(max = 240.dp)
                                    ) {
                                        Text(
                                            text = text,
                                            color = bubbleText,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            if (chatLoading) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = EcoGreen, strokeWidth = 2.dp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Message input field row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = { chatInput = it },
                            placeholder = { Text("Örn: Bu ürün kanserojen mi?", fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (chatInput.trim().isNotEmpty() && !chatLoading) {
                                    viewModel.sendChatMessage(chatInput)
                                    chatInput = ""
                                    keyboardController?.hide()
                                }
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_text_field"),
                            shape = RoundedCornerShape(12.dp),
                            textStyle = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (chatInput.trim().isNotEmpty() && !chatLoading) {
                                    viewModel.sendChatMessage(chatInput)
                                    chatInput = ""
                                    keyboardController?.hide()
                                }
                            },
                            enabled = chatInput.trim().isNotEmpty() && !chatLoading,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (chatInput.trim().isNotEmpty() && !chatLoading) ForestGreen else Color.Gray)
                                .testTag("chat_send_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gönder", tint = Color.White)
                        }
                    }
                }
            }
        }
    }

    // E-Kod detay alt sayfası (sözlükten zengin bilgi)
    if (ecodeDetail != null || ecodeLoading) {
        EcodeDetailSheet(
            ecode = ecodeDetail,
            loading = ecodeLoading,
            onDismiss = { viewModel.clearEcodeDetail() }
        )
    }
}

/** Nutri-Score harf notu eşlemesi (A:90-100, B:75-89, C:50-74, D:25-49, E:0-24). */
fun nutriGrade(score: Int): Pair<String, Color> = when {
    score >= 90 -> "A" to Color(0xFF2E7D32)
    score >= 75 -> "B" to Color(0xFF66BB6A)
    score >= 50 -> "C" to Color(0xFFF9A825)
    score >= 25 -> "D" to Color(0xFFEF6C00)
    else        -> "E" to Color(0xFFC62828)
}

/* -------------------- E-Kod detay alt sayfası -------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcodeDetailSheet(ecode: Ci4Ecode?, loading: Boolean, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(40.dp), Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                ecode == null -> Text(
                    "Bu E-kodu için sözlükte kayıt bulunamadı.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
                else -> {
                    // Başlık
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(ecode.code, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                ecode.nameTr ?: ecode.code,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (!ecode.classTr.isNullOrBlank()) {
                                Text(ecode.classTr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Durum çipleri
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ecode.risk?.let { EcodeStatusChip(it.replaceFirstChar { c -> c.uppercase() } + " risk", riskColorOf(it)) }
                        ecode.halalStatus?.let { EcodeStatusChip(halalLabelOf(it), halalColorOf(it)) }
                        ecode.veganSuitable?.let {
                            val vg = when (it.lowercase()) { "evet" -> RiskLow; "hayır" -> RiskHigh; else -> RiskMedium }
                            EcodeStatusChip("Vegan: ${it.replaceFirstChar { c -> c.uppercase() }}", vg)
                        }
                    }

                    // Yasal kısıtlama / yasak uyarısı (resmi)
                    if (!ecode.bannedNote.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(RiskHigh.copy(alpha = 0.12f))
                                .padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = RiskHigh, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text("Yasal Kısıtlama", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = RiskHigh)
                                Text(ecode.bannedNote, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), lineHeight = 18.sp)
                            }
                        }
                    }

                    EcodeSection("Nedir?", ecode.whatIsTr)
                    EcodeSection("Fonksiyonu", ecode.functionTr)
                    EcodeSection("Kaynağı / Nasıl elde edilir", ecode.sourceTr)
                    EcodeSection("Kullanım Alanları", ecode.usageAreasTr)
                    EcodeSection("Sağlık Notları", ecode.healthNotesTr)
                    EcodeSection("Dikkat Etmesi Gerekenler", ecode.whoShouldAvoidTr)
                    EcodeSection("Helal Değerlendirmesi", ecode.halalNoteTr)
                    EcodeSection("Kabul Edilebilir Günlük Alım (ADI)", ecode.adi)

                    Spacer(Modifier.height(14.dp))
                    if (!ecode.ref.isNullOrBlank()) {
                        Text("Resmi Kaynak / Gerekçe", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        Text(ecode.ref, fontSize = 11.sp, color = Color.Gray, lineHeight = 15.sp)
                        Spacer(Modifier.height(6.dp))
                    }
                    Text(
                        "Bilgiler resmi mevzuat ve EFSA/JECFA değerlendirmelerine dayanır; kesin tıbbi/dini hüküm değildir.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun EcodeSection(title: String, body: String?) {
    if (body.isNullOrBlank()) return
    Spacer(Modifier.height(14.dp))
    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
    Spacer(Modifier.height(4.dp))
    Text(body, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f), lineHeight = 20.sp)
}

@Composable
private fun EcodeStatusChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

private fun riskColorOf(risk: String): Color = when (risk.lowercase().trim()) {
    "yüksek" -> RiskHigh; "orta", "şüpheli" -> RiskMedium; else -> RiskLow
}

private fun halalLabelOf(s: String): String = when (s.lowercase().trim()) {
    "helal" -> "Helal"; "haram-olası" -> "Haram olası"; else -> "Şüpheli"
}

private fun halalColorOf(s: String): Color = when (s.lowercase().trim()) {
    "helal" -> RiskLow; "haram-olası" -> RiskHigh; else -> RiskMedium
}

/** Ürün özetini metin olarak paylaşım sayfasına (WhatsApp, vb.) gönderir. */
private fun shareProductSummary(context: android.content.Context, product: com.example.data.model.SavedProduct) {
    val halal = when (product.halalStatus.lowercase().trim()) {
        "sertifikali" -> "Sertifikalı Helal"
        "muhtemel_helal" -> "Muhtemelen Helal"
        "helal_degil" -> "Helal Değil"
        else -> "Şüpheli"
    }
    val text = buildString {
        append(product.name)
        if (product.brand.isNotBlank()) append(" — ${product.brand}")
        append("\n")
        append("Sağlık Skoru: ${product.healthScore}/100")
        if (product.nutriGrade.isNotBlank()) append("  (Nutri-Score ${product.nutriGrade.uppercase()})")
        append("\n")
        append("Helal Durumu: $halal\n")
        if (product.barcode.isNotBlank()) append("Barkod: ${product.barcode}\n")
        append("\n— Akıllı Gıda uygulamasıyla analiz edildi")
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_SUBJECT, product.name)
        putExtra(android.content.Intent.EXTRA_TEXT, text)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Ürünü paylaş"))
}

@Composable
fun NutriScoreBadge(label: String, score: Int, modifier: Modifier = Modifier) {
    val (grade, color) = nutriGrade(score)
    Column(
        modifier = modifier.padding(horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Text(grade, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = "$score", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color)
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun ScoreTagChip(tag: String) {
    val lower = tag.lowercase()
    val negative = lower.contains("palm") ||
        lower.contains("ilave şeker var") ||
        (lower.contains("katkı") && lower.contains("yüksek")) ||
        lower.contains("yüksek katkı") ||
        (lower.contains("şeker") && lower.contains("var")) ||
        (lower.contains("tuz") && lower.contains("yüksek"))
    val color = if (negative) RiskHigh else EcoGreen
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(text = tag, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StarRatingInput(rating: Int, onRatingChange: (Int) -> Unit) {
    Row {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "$i yıldız",
                tint = if (i <= rating) WarmGold else Color.Gray,
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onRatingChange(i) }
                    .padding(2.dp)
            )
        }
    }
}

@Composable
fun StarRatingDisplay(rating: Int) {
    Row {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = null,
                tint = if (i <= rating) WarmGold else Color.Gray.copy(alpha = 0.4f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun CommentItem(comment: Ci4Comment, onLike: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AccountCircle, contentDescription = null, tint = EcoGreen, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comment.userName ?: "Kullanıcı",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = ForestGreen
                )
                if (comment.rating != null && comment.rating > 0) {
                    StarRatingDisplay(comment.rating)
                }
            }
            if (comment.isMine && comment.status == "pending") {
                Box(
                    modifier = Modifier
                        .background(WarmGold.copy(alpha = 0.18f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("Onay bekliyor", color = WarmGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = comment.body,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (comment.isLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                contentDescription = "Beğen",
                tint = if (comment.isLiked) ForestGreen else Color.Gray,
                modifier = Modifier
                    .size(18.dp)
                    .clickable { onLike() }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = "${comment.likeCount}", fontSize = 12.sp, color = Color.Gray)
            if (!comment.createdAt.isNullOrBlank()) {
                Spacer(modifier = Modifier.weight(1f))
                Text(text = comment.createdAt.take(10), fontSize = 10.sp, color = Color.Gray)
            }
        }
        Divider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
fun DietBadge(
    label: String,
    isEligible: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(4.dp)
            .background(if (isEligible) EcoGreen.copy(alpha = 0.1f) else RiskHigh.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
            .border(1.dp, if (isEligible) EcoGreen.copy(alpha = 0.2f) else RiskHigh.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isEligible) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = label,
            tint = if (isEligible) EcoGreen else RiskHigh,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isEligible) ForestGreen else Color.Gray
        )
    }
}

@Composable
fun NutritionFactsSection(nutritionString: String) {
    val items = remember(nutritionString) {
        val parsedList = mutableListOf<Pair<String, String>>()
        val parts = nutritionString.split(",")
        for (part in parts) {
            val kv = part.split(":")
            if (kv.size >= 2) {
                val rawKey = kv[0].trim().lowercase()
                val rawVal = kv[1].trim()
                val trKey = when {
                    rawKey.contains("sugar") || rawKey.contains("şeker") -> "Şeker"
                    rawKey.contains("saturated") || rawKey.contains("doymuş") -> "Doymuş Yağ"
                    rawKey.contains("fat") || rawKey.contains("yağ") -> "Toplam Yağ"
                    rawKey.contains("protein") -> "Protein"
                    rawKey.contains("carbohydrate") || rawKey.contains("karbonhidrat") -> "Karbonhidrat"
                    rawKey.contains("energy") || rawKey.contains("enerji") -> "Enerji"
                    rawKey.contains("salt") || rawKey.contains("tuz") -> "Tuz"
                    rawKey.contains("sodium") || rawKey.contains("sodyum") -> "Sodyum"
                    rawKey.contains("fiber") || rawKey.contains("lif") -> "Lif"
                    else -> kv[0].trim().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
                parsedList.add(trKey to rawVal)
            } else if (part.trim().isNotBlank()) {
                val trimmed = part.trim()
                val translated = trimmed
                    .replace("Sugar", "Şeker", ignoreCase = true)
                    .replace("Fat", "Yağ", ignoreCase = true)
                    .replace("Protein", "Protein", ignoreCase = true)
                    .replace("Carbohydrates", "Karbonhidrat", ignoreCase = true)
                    .replace("Energy", "Enerji", ignoreCase = true)
                parsedList.add("" to translated)
            }
        }
        parsedList
    }

    if (items.isEmpty()) {
        Text(
            text = nutritionString.ifBlank { "Besin değerleri bilgisi mevcut değil." },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 6.dp)
        ) {
            items.forEach { (key, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (key.isNotEmpty()) {
                        Text(
                            text = key,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                        Text(
                            text = value,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = ForestGreen
                        )
                    } else {
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            }
        }
    }
}

