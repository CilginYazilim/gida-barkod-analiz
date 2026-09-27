package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.ProductViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlacklistScreen(
    viewModel: ProductViewModel,
    modifier: Modifier = Modifier
) {
    val blacklist by viewModel.blacklistItems.collectAsState()
    var inputValue by remember { mutableStateOf("") }
    var inputReason by remember { mutableStateOf("") }
    var inputType by remember { mutableStateOf("INGREDIENT") } // INGREDIENT, ADDITIVE, BOYCOTT
    val keyboardController = LocalSoftwareKeyboardController.current

    // Complete Flattened single LazyColumn layout strictly fixes all nested scrolling issues.
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Header Portion
        item {
            Text(
                text = "Kişisel Kara Listeniz",
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Tüketmek istemediğiniz katkı maddelerini, alerjenleri veya boykot ettiğiniz markaları ekleyin. Tarama yapıldığında sistem sizi uyaracaktır.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
            )
        }

        // 2. Add New Blacklisted Item Form Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Başlık + ikon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SoftMint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Block, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Yeni Öğe Engelle",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Taramada uyarılmak istediğin içerik, katkı veya markayı ekle",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = {
                            inputValue = it
                            // Otomatik tür: "E951" gibi başlıyorsa Katkı seç.
                            if (Regex("^[Ee]\\d").containsMatchIn(it.trim())) inputType = "ADDITIVE"
                        },
                        label = { Text("Değer") },
                        placeholder = { Text("E951, Palm Yağı, Coca-Cola...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = ForestGreen) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("blacklist_input_field"),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreen)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = inputReason,
                        onValueChange = { inputReason = it },
                        label = { Text("Gerekçe (isteğe bağlı)") },
                        placeholder = { Text("Alerjim var, boykot ediyorum...") },
                        leadingIcon = { Icon(Icons.Default.EditNote, contentDescription = null, tint = ForestGreen) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (inputValue.trim().isNotEmpty()) {
                                viewModel.addToBlacklist(inputValue, inputType, inputReason)
                                inputValue = ""; inputReason = ""; keyboardController?.hide()
                            }
                        }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreen)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "TÜR",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val types = listOf(
                        Triple("INGREDIENT", "İçerik / Alerjen", Icons.Default.Grass),
                        Triple("ADDITIVE", "E-Kod / Katkı", Icons.Default.Science),
                        Triple("BOYCOTT", "Marka / Boykot", Icons.Default.Storefront)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        types.forEach { (value, label, icon) ->
                            val selected = inputType == value
                            FilterChip(
                                selected = selected,
                                onClick = { inputType = value },
                                label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                                leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                modifier = Modifier.weight(1f).testTag("chip_type_${value.lowercase()}"),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SoftMint,
                                    selectedLabelColor = ForestGreen,
                                    selectedLeadingIconColor = ForestGreen
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (inputValue.trim().isNotEmpty()) {
                                viewModel.addToBlacklist(inputValue, inputType, inputReason)
                                inputValue = ""; inputReason = ""; keyboardController?.hide()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("blacklist_add_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                        enabled = inputValue.trim().isNotEmpty()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Listeye Ekle", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4. Section Label "Aktif Kara Listeniz"
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Aktif Kara & Boykot Listeniz (${blacklist.size} Öğe)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen
                )
            }
        }

        // 5. Active list render
        if (blacklist.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Boş",
                            tint = ForestGreen.copy(alpha = 0.2f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Kara listeniz şu anda boş.",
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(blacklist, key = { it.id }) { item ->
                var isEditingReason by remember { mutableStateOf(false) }
                var editedReasonText by remember(item.reason) { mutableStateOf(item.reason) }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("blacklist_item_${item.id}"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (item.type == "BOYCOTT") Color(0xFFE65100) else RiskHigh,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (item.type == "BOYCOTT") "BOYKOT" else "YASAKLI",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = item.value,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = when (item.type) {
                                            "ADDITIVE" -> "Katkı Maddesi (E-Kod)"
                                            "BOYCOTT" -> "Boykot Markası / Ürün"
                                            "BRAND" -> "Yasaklı Marka"
                                            "PRODUCT" -> "Yasaklı Ürün"
                                            else -> "Genel İçerik / Alerjen"
                                        },
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = { isEditingReason = !isEditingReason },
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Düzenle",
                                        tint = ForestGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.removeFromBlacklist(item.id, item.value) },
                                    modifier = Modifier.testTag("delete_blacklist_item_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Sil",
                                        tint = RiskHigh,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (!isEditingReason && item.reason.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                                ) {
                                Text(
                                    text = "📝 Gerekçe: ${item.reason}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }

                        if (isEditingReason) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = editedReasonText,
                                onValueChange = { editedReasonText = it },
                                placeholder = { Text("Yasaklama nedenini girin...", fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = ForestGreen
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = { isEditingReason = false }) {
                                    Text("İptal", color = Color.Gray, fontSize = 12.sp)
                                }
                                Button(
                                    onClick = {
                                        viewModel.updateBlacklistReason(item.id, editedReasonText, item.value, item.type)
                                        isEditingReason = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Güncelle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
