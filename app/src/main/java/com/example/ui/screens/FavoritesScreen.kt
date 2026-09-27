package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.ProductViewModel
import com.example.ui.theme.RiskHigh

@Composable
fun FavoritesScreen(
    viewModel: ProductViewModel,
    modifier: Modifier = Modifier
) {
    val favorites by viewModel.favoriteProducts.collectAsState()
    var query by remember { mutableStateOf("") }
    var sort by remember { mutableStateOf(ListSort.RECENT) }
    val shown = remember(favorites, query, sort) { applyListControls(favorites, query, sort) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ScreenHeader(title = "Favori Ürünlerim")

        if (favorites.isEmpty()) {
            EmptyState(
                icon = Icons.Default.FavoriteBorder,
                title = "Henüz Favori Eklenmemiş",
                subtitle = "Analiz ettiğiniz ürünlerde sağ üstteki kalp simgesine dokunarak buraya ekleyebilirsiniz."
            )
        } else {
            ListSearchSortBar(query = query, onQuery = { query = it }, sort = sort, onSort = { sort = it })
            if (shown.isEmpty()) {
                EmptyState(
                    icon = Icons.Default.FavoriteBorder,
                    title = "Sonuç Yok",
                    subtitle = "Aramanıza uygun favori bulunamadı."
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
                        cardTestTag = "favorite_item_${product.barcode}",
                        grade = product.nutriGrade
                    ) {
                        IconButton(
                            onClick = { viewModel.toggleFavorite(product) },
                            modifier = Modifier.testTag("delete_favorite_${product.barcode}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteOutline,
                                contentDescription = "Kaldır",
                                tint = RiskHigh
                            )
                        }
                    }
                }
            }
            }
        }
    }
}
