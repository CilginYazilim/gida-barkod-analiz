package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Canlı "Fresh Green" tasarım dili (v2 — 2026-06-26 yeniden tasarım).
 *
 * NOT: Eski val isimleri (ForestGreen, EcoGreen, SoftMint, WarmGold ...) bilerek
 * KORUNDU; yalnızca renk DEĞERLERİ canlı yeşile çevrildi. Böylece tüm ekranlar
 * tek hamlede yeni kimliğe geçer, hiçbir import kırılmaz.
 */

// --- Ana marka yeşilleri ---
val ForestGreen = Color(0xFF16A34A)   // Primary — buton/FAB/aksan, beyaz üstünde okunur (green-600)
val EcoGreen = Color(0xFF22C55E)       // Parlak vurgu / seçili durum / degrade (green-500)
val SoftMint = Color(0xFFDCFCE7)       // Açık yeşil çip zemini & seçili gösterge (green-100)
val WarmGold = Color(0xFFF59E0B)       // İkincil uyarı / vurgu (amber-500)

val BrightLeaf = Color(0xFF4ADE80)     // En parlak yeşil (degrade üst ucu, green-400)
val DeepGreen = Color(0xFF15803D)      // Koyu yeşil (başlık metni / degrade alt ucu, green-700)

// --- Koyu tema yeşilleri ---
val LightForestGreen = Color(0xFF4ADE80) // Koyu temada primary (parlak, koyu zeminde okunur)
val MediumEcoGreen = Color(0xFF22C55E)
val DarkMint = Color(0xFF14271B)

// --- Durum renkleri (risk / helal / haram) ---
val RiskHigh = Color(0xFFEF4444)       // Kırmızı — Haram / yüksek risk (red-500)
val RiskMedium = Color(0xFFF59E0B)     // Turuncu — şüpheli / orta risk (amber-500)
val RiskLow = Color(0xFF22C55E)        // Yeşil — helal / düşük risk (green-500)

// --- Zemin & nötr yüzeyler ---
val LightBackground = Color(0xFFF1F3F5) // Açık gri zemin (kartlar beyaz "yüzer")
val LightSurface = Color(0xFFFFFFFF)    // Beyaz kart yüzeyi
val DarkBackground = Color(0xFF0B0F0C)  // Koyu zemin
val DarkSurface = Color(0xFF151916)     // Koyu kart yüzeyi
val CharcoalText = Color(0xFF111827)    // Yüksek kontrastlı koyu metin (gray-900)
val MutedText = Color(0xFF6B7280)       // İkincil/gri metin (gray-500)
val HairlineLight = Color(0xFFE5E7EB)   // İnce ayraç / kart kenarı
