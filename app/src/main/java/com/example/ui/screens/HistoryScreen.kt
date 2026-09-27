package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.ProductViewModel
import com.example.ui.theme.RiskHigh

@Composable
fun HistoryScreen(
    viewModel: ProductViewModel,
    modifier: Modifier = Modifier
) {
    val history by viewModel.historyProducts.collectAsState()
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(ListSort.RECENT) }
    val shown = remember(history, query, sort) { applyListControls(history, query, sort) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = "Tarama Geçmişi",
            action = {
                if (history.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearHistory() },
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Geçmişi Temizle",
                            tint = RiskHigh
                        )
                    }
                }
            }
        )

        if (history.isEmpty()) {
            EmptyState(
                icon = Icons.Default.History,
                title = "Tarama Geçmişiniz Temiz",
                subtitle = "Daha önce analiz ettiğiniz tüm ürünler burada listelenir."
            )
        } else {
            ListSearchSortBar(query = query, onQuery = { query = it }, sort = sort, onSort = { sort = it })
            if (shown.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.History,
                    title = "Sonuç Yok",
                    subtitle = "Aramanıza uygun ürün bulunamadı."
                )
            } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(shown) { product ->
                    ProductRowCard(
                        name = product.name,
                        brand = product.brand,
                        imageUrl = product.imageUrl,
                        score = product.healthScore,
                        onClick = { viewModel.analyzeProduct(product.barcode) },
                        cardTestTag = "history_item_${product.barcode}",
                        grade = product.nutriGrade
                    ) {
                        IconButton(
                            onClick = { viewModel.deleteProductFromHistory(product.barcode) },
                            modifier = Modifier.testTag("delete_history_item_${product.barcode}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = "Sil",
                                tint = RiskHigh.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
            }
        }
    }
}
