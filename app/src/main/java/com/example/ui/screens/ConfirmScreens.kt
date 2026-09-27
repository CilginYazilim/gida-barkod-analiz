package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.example.data.FoodCategories
import com.example.data.OffCategoryMap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SavedProduct
import com.example.data.network.OFFProduct
import com.example.ui.ProductViewModel
import com.example.ui.theme.EcoGreen
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.RiskHigh
import com.example.ui.theme.SoftMint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmLocalScreen(
    product: SavedProduct,
    viewModel: ProductViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SoftMint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CloudQueue,
                contentDescription = null,
                tint = ForestGreen,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Yerel Veritabanında Bulundu!",
            fontWeight = FontWeight.Bold,
            color = ForestGreen,
            fontSize = 22.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Bu gıda ürünü daha önce analiz edilmiş ve güvenli yerel hafızada bulunuyor.",
            color = Color.Gray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Product Summary verification card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
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
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = ForestGreen,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = product.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = product.brand,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Kategori: ${product.category}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        if (product.grammage.isNotEmpty() && product.grammage != "Bilinmiyor") {
                            Text(
                                text = "Gramaj: ${product.grammage}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Aradığınız ürün tam olarak bu mu?",
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions to accept or trigger a web lookup manually
        Button(
            onClick = { viewModel.confirmUsageOfLocalProduct(product) },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Check, contentDescription = "Onayla")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Evet, Doğru Ürün - Detayları Göster", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { viewModel.forceSearchWebProduct(product.barcode) }, // Bypasses DB check to force network lookup
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RiskHigh),
            border = BorderStroke(1.dp, RiskHigh.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Yenile")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Hayır, İnternetten Yeniden Sorgula", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = { viewModel.clearActiveProductState() }
        ) {
            Text("Geri Dön", color = Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmWebScreen(
    barcode: String,
    brand: String,
    name: String,
    category: String,
    grammage: String,
    imageUrl: String,
    rawProduct: OFFProduct?,
    viewModel: ProductViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Düzenlenebilir alanlar (TextFieldValue → odaklanınca tüm metin seçilir, hemen yazılabilir).
    var brandInput by remember { mutableStateOf(TextFieldValue(brand)) }
    var nameInput by remember { mutableStateOf(TextFieldValue(name)) }
    // Kategori prefill'i sabit taksonomiye çevrilir (serbest metin "Nar Ekşisi" gelmesin).
    var categoryInput by remember {
        mutableStateOf(
            OffCategoryMap.resolve(rawProduct?.categoriesTagsEn, category) ?: FoodCategories.canonical(category)
        )
    }
    var grammageInput by remember { mutableStateOf(TextFieldValue(grammage)) }
    var priceInput by remember { mutableStateOf("") }
    var showValidationError by remember { mutableStateOf(false) }

    val unknownValues = setOf("bilinmiyor", "unknown", "n/a", "yok", "")

    fun isUnknown(s: String) = s.trim().lowercase() in unknownValues

    // Bir alana odaklanınca içeriği tümüyle seçer (yazınca anında değişir).
    fun selectAll(v: TextFieldValue) = v.copy(selection = TextRange(0, v.text.length))

    fun hasRequiredFieldErrors(): Boolean =
        isUnknown(nameInput.text) ||
        isUnknown(brandInput.text) ||
        isUnknown(categoryInput) ||
        isUnknown(grammageInput.text)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SoftMint),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = ForestGreen,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ürün Bulundu (Onaylanıyor)",
            fontWeight = FontWeight.Bold,
            color = ForestGreen,
            fontSize = 22.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Yapay zekâ analizi öncesinde gıda bilgilerini doğrulayın ya da gerekli düzeltmeleri yapın:",
            color = Color.Gray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Taranan barkod
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SoftMint,
            border = BorderStroke(1.dp, ForestGreen.copy(alpha = 0.25f))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode2,
                    contentDescription = "Barkod",
                    tint = ForestGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = barcode,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen,
                    fontSize = 16.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Image & Edit inputs card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                
                // Show detected image if any, otherwise show beautiful Material design placeholder
                if (imageUrl.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SoftMint)
                            .align(Alignment.CenterHorizontally)
                            .border(1.dp, ForestGreen.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Ürün Görseli",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .align(Alignment.CenterHorizontally)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Fastfood,
                                contentDescription = "Görsel Yok",
                                tint = ForestGreen,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Görsel Yok",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "Ürün Bilgilerini Doğrulayın:",
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Editable Fields — "Bilinmiyor"/boş alana ilk odakta otomatik temizlenir.
                OutlinedTextField(
                    value = brandInput,
                    onValueChange = { brandInput = it; showValidationError = false },
                    label = { Text("Marka *") },
                    singleLine = true,
                    isError = showValidationError && isUnknown(brandInput.text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .onFocusChanged { if (it.isFocused) brandInput = selectAll(brandInput) },
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it; showValidationError = false },
                    label = { Text("Ürün Adı *") },
                    singleLine = true,
                    isError = showValidationError && isUnknown(nameInput.text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .onFocusChanged { if (it.isFocused) nameInput = selectAll(nameInput) },
                    shape = RoundedCornerShape(10.dp)
                )

                // Kategori: sabit taksonomiden dropdown (serbest metin değil), Türkçe alfabetik ("Diğer" sonda).
                var categoryExpanded by remember { mutableStateOf(false) }
                val sortedCategories = remember {
                    val collator = java.text.Collator.getInstance(java.util.Locale("tr", "TR"))
                    FoodCategories.ALL.filter { it != FoodCategories.OTHER }
                        .sortedWith(compareBy(collator) { it }) + FoodCategories.OTHER
                }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    OutlinedTextField(
                        value = categoryInput,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Kategori *") },
                        placeholder = { Text("Kategori seçin") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        isError = showValidationError && isUnknown(categoryInput),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        sortedCategories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    categoryInput = cat
                                    categoryExpanded = false
                                    showValidationError = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = grammageInput,
                    onValueChange = { grammageInput = it; showValidationError = false },
                    label = { Text("Gramaj / Ağırlık *") },
                    singleLine = true,
                    isError = showValidationError && isUnknown(grammageInput.text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .onFocusChanged { if (it.isFocused) grammageInput = selectAll(grammageInput) },
                    shape = RoundedCornerShape(10.dp),
                    supportingText = if (showValidationError && isUnknown(grammageInput.text)) {
                        { Text("Gramaj belirtilmeli (örn. 250g, 330ml)", color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    } else null
                )

                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { v ->
                        // Virgülü noktaya çevir, yalnızca rakam + tek ondalık ayraç kabul et
                        val normalized = v.replace(',', '.')
                        if (normalized.isEmpty() || normalized.matches(Regex("^\\d{0,6}(\\.\\d{0,2})?$"))) priceInput = normalized
                    },
                    label = { Text("Fiyat ₺ (opsiyonel)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = { Text("₺", fontWeight = FontWeight.Bold, color = ForestGreen) },
                    placeholder = { Text("0.00") }
                )

                if (showValidationError && hasRequiredFieldErrors()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Ürün Adı, Marka, Kategori ve Gramaj alanları 'Bilinmiyor' olarak bırakılamaz. Lütfen doğru bilgileri girin.",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Proceed with AI Analyzer
        Button(
            onClick = {
                if (hasRequiredFieldErrors()) {
                    showValidationError = true
                } else {
                    viewModel.initiateWebProductAnalysis(
                        barcode = barcode,
                        brand = brandInput.text.trim(),
                        name = nameInput.text.trim(),
                        category = categoryInput.trim(),
                        grammage = grammageInput.text.trim(),
                        imageUrl = imageUrl,
                        rawProduct = rawProduct,
                        price = priceInput.trim().toDoubleOrNull()
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Send, contentDescription = "Analiz Et")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Doğrula ve Analizi Başlat", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = { viewModel.clearActiveProductState() }
        ) {
            Text("İptal Et ve Geri Dön", color = RiskHigh, fontWeight = FontWeight.Bold)
        }
    }
}
