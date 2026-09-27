package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.RiskHigh
import com.example.ui.theme.RiskLow
import com.example.ui.theme.RiskMedium
import com.example.ui.theme.SoftMint

/**
 * Yeniden tasarım (2026-06-26) için paylaşılan modern UI bileşenleri.
 * Liste ekranları (Favoriler / Geçmiş / Kara Liste) ve diğerleri buradan beslenir.
 */

fun scoreColor(score: Int): Color = when {
    score >= 75 -> RiskLow
    score >= 50 -> RiskMedium
    else -> RiskHigh
}

/** Nutri-Score harf rengi (A yeşil → E kırmızı). */
fun nutriGradeColor(grade: String): Color = when (grade.uppercase().trim()) {
    "A" -> Color(0xFF16A34A)
    "B" -> Color(0xFF65A30D)
    "C" -> Color(0xFFCA8A04)
    "D" -> Color(0xFFEA580C)
    "E" -> Color(0xFFDC2626)
    else -> Color(0xFF9CA3AF)
}

/** Küçük Nutri-Score harf rozeti (A-E). */
@Composable
fun NutriGradeMini(grade: String, modifier: Modifier = Modifier) {
    val g = grade.uppercase().trim()
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(nutriGradeColor(g)),
        contentAlignment = Alignment.Center
    ) {
        Text(g, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

/** Liste sıralama seçenekleri. */
enum class ListSort(val label: String) { RECENT("En Yeni"), HEALTH("Sağlık"), NAME("A-Z") }

/** Liste ekranları için arama kutusu + sıralama çipleri. */
@Composable
fun ListSearchSortBar(
    query: String,
    onQuery: (String) -> Unit,
    sort: ListSort,
    onSort: (ListSort) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = query,
            onValueChange = onQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Ara: ürün veya marka") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Temizle")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp)
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ListSort.values().forEach { opt ->
                FilterChip(
                    selected = sort == opt,
                    onClick = { onSort(opt) },
                    label = { Text(opt.label, fontSize = 13.sp) }
                )
            }
        }
        Spacer(Modifier.height(10.dp))
    }
}

/** Bir SavedProduct listesine arama + sıralama uygular. */
fun applyListControls(
    items: List<com.example.data.model.SavedProduct>,
    query: String,
    sort: ListSort
): List<com.example.data.model.SavedProduct> {
    val q = query.trim().lowercase()
    val filtered = if (q.isEmpty()) items else items.filter {
        it.name.lowercase().contains(q) || it.brand.lowercase().contains(q)
    }
    return when (sort) {
        ListSort.RECENT -> filtered.sortedByDescending { it.scannedAt }
        ListSort.HEALTH -> filtered.sortedByDescending { it.healthScore }
        ListSort.NAME -> filtered.sortedBy { it.name.lowercase() }
    }
}

/** Renk kodlu sağlık skoru rozeti (ör. 82/100). */
@Composable
fun ScorePill(score: Int, modifier: Modifier = Modifier) {
    val c = scoreColor(score)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(c)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$score", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text("/100", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp)
    }
}

/** Ekran başlığı + isteğe bağlı sağ aksiyon. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 24.sp
        )
        action?.invoke()
    }
}

/** Boş durum görseli (ikon + başlık + açıklama). */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(SoftMint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(46.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}

/**
 * Liste için modern ürün kartı: görsel + ad/marka + skor rozeti + sondaki aksiyon.
 * Ad koyu (onSurface), marka gri — yeşil yalnızca skorda aksan olarak kullanılır.
 */
@Composable
fun ProductRowCard(
    name: String,
    brand: String,
    imageUrl: String,
    score: Int,
    onClick: () -> Unit,
    cardTestTag: String,
    modifier: Modifier = Modifier,
    grade: String? = null,
    trailing: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(cardTestTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoftMint),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Text(
                    text = brand,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ScorePill(score)
                    if (!grade.isNullOrBlank()) NutriGradeMini(grade)
                }
            }

            trailing()
        }
    }
}
