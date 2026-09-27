package com.example.data

/**
 * Sabit gıda kategori taksonomisi (çevrimdışı ayna).
 *
 * Birincil kaynak backend `gida_kategori` tablosudur; bu liste sunucuya erişilemediğinde
 * ConfirmWeb dropdown'u ve AI prompt'u için yedek olarak kullanılır. İkisi senkron tutulmalıdır.
 */
object FoodCategories {
    const val OTHER = "Diğer"

    val ALL: List<String> = listOf(
        "Atıştırmalık & Cips",
        "Bisküvi & Kraker",
        "Çikolata & Şekerleme",
        "Kek & Pasta",
        "Kahvaltılık (Reçel/Bal/Kaymak)",
        "Gevrek & Müsli",
        "Süt & Süt İçecekleri",
        "Peynir",
        "Yoğurt & Ayran",
        "Tereyağı & Margarin",
        "Yumurta",
        "Et & Şarküteri",
        "Tavuk & Beyaz Et",
        "Balık & Deniz Ürünleri",
        "Dondurulmuş Gıda",
        "Bakliyat",
        "Pirinç & Bulgur & Makarna",
        "Un & Pastane Malzemeleri",
        "Sıvı Yağ & Zeytinyağı",
        "Salça & Konserve",
        "Sos & Ketçap & Mayonez",
        "Baharat & Çeşni",
        "Turşu & Salamura",
        "Zeytin",
        "Kuruyemiş & Kuru Meyve",
        "Gazlı İçecek",
        "Gazsız İçecek (Meyve Suyu/Nektar)",
        "Su & Maden Suyu",
        "Çay",
        "Kahve",
        "Enerji & Sporcu İçecekleri",
        "Bebek & Çocuk Gıdası",
        "Dondurma",
        "Hazır Yemek & Çorba",
        "Ekmek & Unlu Mamul",
        "Tatlı & Helva & Pekmez",
        "Vegan & Bitkisel Ürünler",
        OTHER
    )

    /** Verilen değer taksonomide var mı (birebir). */
    fun isValid(name: String?): Boolean = name != null && ALL.any { it.equals(name.trim(), ignoreCase = true) }

    /** Geçerli değilse en yakın kanonik adı, yoksa "Diğer" döndürür. */
    fun canonical(name: String?): String {
        val v = name?.trim().orEmpty()
        if (v.isEmpty()) return OTHER
        return ALL.firstOrNull { it.equals(v, ignoreCase = true) } ?: OTHER
    }
}
