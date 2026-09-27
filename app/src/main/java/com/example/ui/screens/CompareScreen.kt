package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SavedProduct
import com.example.ui.ComparisonUiState
import com.example.ui.ProductViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    viewModel: ProductViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val ForestGreen = MaterialTheme.colorScheme.primary
    val historyProducts by viewModel.historyProducts.collectAsState()

    val product1 by viewModel.selectedCompareProduct1.collectAsState()
    val product2 by viewModel.selectedCompareProduct2.collectAsState()
    val comparisonState by viewModel.comparisonState.collectAsState()

    var dropdownSlot1Expanded by remember { mutableStateOf(false) }
    var dropdownSlot2Expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Screen Header
        Text(
            text = "Ürün Karşılaştırma",
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Geçmişinizdeki iki gıdayı seçerek besin değerleri, şeker oranları ve katkı maddeleri açısından yapay zekâ ile kıyaslayın.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp).padding(horizontal = 8.dp)
        )

        // Comparison Slots (Side-by-Side Cards)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Slot 1 Selection
            Box(modifier = Modifier.weight(1.5f)) {
                Column {
                    Text(
                        text = "1. Gıda Ürünü",
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownSlot1Expanded = true }
                            .testTag("compare_slot_1"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (product1 != null) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SoftMint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (product1!!.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = product1!!.imageUrl,
                                            contentDescription = product1!!.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Default.Restaurant, contentDescription = "Gıda", tint = ForestGreen)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = product1!!.name,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreen,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Skor: ${product1!!.healthScore}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (product1!!.healthScore >= 75) RiskLow else if (product1!!.healthScore >= 50) RiskMedium else RiskHigh,
                                    fontSize = 11.sp
                                )
                            } else {
                                Icon(Icons.Default.Restaurant, contentDescription = "Boş", tint = Color.Gray, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Ürün Seç", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Menu", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Dropdown menu Slot 1
                DropdownMenu(
                    expanded = dropdownSlot1Expanded,
                    onDismissRequest = { dropdownSlot1Expanded = false },
                    modifier = Modifier.width(160.dp)
                ) {
                    if (historyProducts.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Taranan ürün yok", fontSize = 12.sp) },
                            onClick = { dropdownSlot1Expanded = false }
                        )
                    } else {
                        historyProducts.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.brand} - ${item.name}", maxLines = 1, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.selectProductForCompare(item, 1)
                                    dropdownSlot1Expanded = false
                                },
                                modifier = Modifier.testTag("slot_1_item_${item.barcode}")
                            )
                        }
                    }
                }
            }

            // VS Middle Badge
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(0.4f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ForestGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text("VS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Slot 2 Selection
            Box(modifier = Modifier.weight(1.5f)) {
                Column {
                    Text(
                        text = "2. Gıda Ürünü",
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownSlot2Expanded = true }
                            .testTag("compare_slot_2"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (product2 != null) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SoftMint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (product2!!.imageUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = product2!!.imageUrl,
                                            contentDescription = product2!!.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Default.Restaurant, contentDescription = "Gıda", tint = ForestGreen)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = product2!!.name,
                                    fontWeight = FontWeight.Bold,
                                    color = ForestGreen,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Skor: ${product2!!.healthScore}",
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (product2!!.healthScore >= 75) RiskLow else if (product2!!.healthScore >= 50) RiskMedium else RiskHigh,
                                    fontSize = 11.sp
                                )
                            } else {
                                Icon(Icons.Default.Restaurant, contentDescription = "Boş", tint = Color.Gray, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Ürün Seç", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Menu", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Dropdown menu Slot 2
                DropdownMenu(
                    expanded = dropdownSlot2Expanded,
                    onDismissRequest = { dropdownSlot2Expanded = false },
                    modifier = Modifier.width(160.dp)
                ) {
                    if (historyProducts.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Taranan ürün yok", fontSize = 12.sp) },
                            onClick = { dropdownSlot2Expanded = false }
                        )
                    } else {
                        historyProducts.forEach { item ->
                            DropdownMenuItem(
                                text = { Text("${item.brand} - ${item.name}", maxLines = 1, fontSize = 12.sp) },
                                onClick = {
                                    viewModel.selectProductForCompare(item, 2)
                                    dropdownSlot2Expanded = false
                                },
                                modifier = Modifier.testTag("slot_2_item_${item.barcode}")
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Compare execution button
        Button(
            onClick = { viewModel.runComparison() },
            enabled = product1 != null && product2 != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("run_comparison_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
            Icon(Icons.Default.CompareArrows, contentDescription = "Kıyasla")
            Spacer(modifier = Modifier.width(6.dp))
            Text("Yapay Zekâ ile Karşılaştır", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Results Card state machine
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            contentAlignment = Alignment.TopCenter
        ) {
            when (val state = comparisonState) {
                is ComparisonUiState.Idle -> {
                    if (product1 == null || product2 == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Lütfen kıyaslama yapmak için geçmiş veya favorilerden iki ürün seçin.",
                                color = Color.Gray,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                }

                is ComparisonUiState.Loading -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    ) {
                        CircularProgressIndicator(color = EcoGreen)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Yapay zekâ gıda veritabanını, şeker, yağ ve katkı maddelerini karşılaştırıyor...",
                            fontWeight = FontWeight.Medium,
                            color = ForestGreen,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is ComparisonUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Karşılaştırma sırasında bir hata oluştu: ${state.message}",
                            color = RiskHigh,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                is ComparisonUiState.Success -> {
                    val comp = state.comparison
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Healthier banner callout
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = ForestGreen)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Stars, contentDescription = "Öneri", tint = WarmGold, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Yapay Zekâ Seçimi",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Daha Sağlıklı Seçenek: ${comp.healthierOption}",
                                    color = WarmGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = comp.directRecommendation,
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // Side-by-side Score breakdown cards
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Product 1 Score
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(product1!!.name, maxLines = 1, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CharcoalText)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${comp.product1Score}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (comp.product1Score >= 75) RiskLow else if (comp.product1Score >= 50) RiskMedium else RiskHigh
                                    )
                                    Text("Sağlık Puanı", fontSize = 10.sp, color = Color.Gray)
                                }
                            }

                            // Product 2 Score
                            Card(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(product2!!.name, maxLines = 1, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CharcoalText)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${comp.product2Score}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (comp.product2Score >= 75) RiskLow else if (comp.product2Score >= 50) RiskMedium else RiskHigh
                                    )
                                    Text("Sağlık Puanı", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }

                        // Detailed side-by-side comparative diagnostics text
                        Text(
                            text = "Detaylı Karşılaştırma Analizi",
                            fontWeight = FontWeight.Bold,
                            color = ForestGreen,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                text = comp.comparisonDetails,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = CharcoalText,
                                lineHeight = 21.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
