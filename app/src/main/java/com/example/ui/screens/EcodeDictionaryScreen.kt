package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.network.Ci4Ecode
import com.example.ui.ProductViewModel
import com.example.ui.theme.*

/**
 * E-Kod Sözlüğü — ürüne bağlı olmadan tüm katkı maddelerini arayıp inceleme ekranı.
 * Profil'den açılır (overlay). Satıra tıklayınca mevcut EcodeDetailSheet açılır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcodeDictionaryScreen(viewModel: ProductViewModel, onBack: () -> Unit) {
    val list by viewModel.ecodeList.collectAsState()
    val loading by viewModel.ecodeListLoading.collectAsState()
    val query by viewModel.ecodeQuery.collectAsState()
    val ecodeDetail by viewModel.ecodeDetail.collectAsState()
    val ecodeLoading by viewModel.ecodeLoading.collectAsState()

    BackHandler(enabled = true) { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("E-Kod Sözlüğü", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Arama kutusu
            OutlinedTextField(
                value = query,
                onValueChange = { viewModel.loadEcodeList(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Kod veya ad ara (örn. E330, Sitrik)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            // Bilgi satırı
            Text(
                text = if (loading) "Aranıyor…" else "${list.size} katkı maddesi",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp)
            )

            when {
                loading && list.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = ForestGreen)
                }
                list.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Science, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Sonuç bulunamadı.", color = Color.Gray)
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(list, key = { it.code }) { e ->
                        EcodeRow(e) { viewModel.loadEcodeDetail(e.code) }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    // Detay alt sayfası (ProductDetailsScreen'deki ortak bileşen)
    if (ecodeDetail != null || ecodeLoading) {
        EcodeDetailSheet(
            ecode = ecodeDetail,
            loading = ecodeLoading,
            onDismiss = { viewModel.clearEcodeDetail() }
        )
    }
}

@Composable
private fun EcodeRow(e: Ci4Ecode, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Kod rozeti
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ForestGreen)
                    .padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Text(e.code, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        e.nameTr ?: e.code,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!e.bannedNote.isNullOrBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Block, contentDescription = "Kısıtlı", tint = RiskHigh, modifier = Modifier.size(14.dp))
                    }
                }
                if (!e.classTr.isNullOrBlank()) {
                    Text(e.classTr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // Risk + helal mini çipler
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                e.risk?.let { MiniChip(it.replaceFirstChar { c -> c.uppercase() }, riskTone(it)) }
                e.halalStatus?.let { MiniChip(halalText(it), halalTone(it)) }
            }
        }
    }
}

@Composable
private fun MiniChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text, color = color, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

private fun riskTone(risk: String): Color = when (risk.lowercase().trim()) {
    "yüksek" -> RiskHigh; "orta", "şüpheli" -> RiskMedium; else -> RiskLow
}
private fun halalText(s: String): String = when (s.lowercase().trim()) {
    "helal" -> "Helal"; "haram-olası" -> "Haram olası"; else -> "Şüpheli"
}
private fun halalTone(s: String): Color = when (s.lowercase().trim()) {
    "helal" -> RiskLow; "haram-olası" -> RiskHigh; else -> RiskMedium
}
