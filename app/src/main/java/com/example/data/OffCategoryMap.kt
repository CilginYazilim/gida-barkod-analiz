package com.example.data

/**
 * OpenFoodFacts'in İngilizce kategori hiyerarşisini (`categories_tags_en`) sabit
 * taksonomimize (FoodCategories) deterministik olarak eşler.
 *
 * Kurallar en SPESİFİK eşleşme önce gelecek şekilde sıralıdır; ilk eşleşen kazanır.
 * Eşleşme bulunamazsa null döner (çağıran taraf AI/kullanıcıya bırakır).
 */
object OffCategoryMap {

    // (anahtar kelimeler) -> taksonomi adı. Sıra önemlidir: spesifik üstte.
    private val RULES: List<Pair<List<String>, String>> = listOf(
        listOf("ketchup", "mayonnaise", "mustard", "pesto", "sauce", "condiment", "dressing") to "Sos & Ketçap & Mayonez",
        listOf("tomato paste", "tomato-paste", "tomato sauce", "canned", "preserve", "tinned") to "Salça & Konserve",
        listOf("pickle", "gherkin", "pickled") to "Turşu & Salamura",
        listOf("olive") to "Zeytin",
        listOf("olive oil", "vegetable oil", "sunflower oil", "oils") to "Sıvı Yağ & Zeytinyağı",
        listOf("margarine") to "Tereyağı & Margarin",
        listOf("butter") to "Tereyağı & Margarin",
        listOf("cheese", "cheeses") to "Peynir",
        listOf("yogurt", "yoghurt", "ayran") to "Yoğurt & Ayran",
        listOf("milk", "dairies", "dairy") to "Süt & Süt İçecekleri",
        listOf("egg") to "Yumurta",
        listOf("ice cream", "ice-cream", "frozen dessert") to "Dondurma",
        listOf("frozen") to "Dondurulmuş Gıda",
        listOf("chips", "crisps", "snack") to "Atıştırmalık & Cips",
        listOf("biscuit", "cracker", "cookie", "wafer", "gofret") to "Bisküvi & Kraker",
        listOf("chocolate", "candy", "confectioner", "sweets", "gum") to "Çikolata & Şekerleme",
        listOf("cake", "pastry", "pie") to "Kek & Pasta",
        listOf("cereal", "muesli", "granola", "breakfast cereal") to "Gevrek & Müsli",
        listOf("jam", "honey", "marmalade", "spread") to "Kahvaltılık (Reçel/Bal/Kaymak)",
        listOf("halva", "tahini", "molasses", "dessert", "baklava") to "Tatlı & Helva & Pekmez",
        listOf("legume", "lentil", "chickpea", "bean", "pulses") to "Bakliyat",
        listOf("rice", "bulgur", "pasta", "noodle", "macaroni") to "Pirinç & Bulgur & Makarna",
        listOf("flour", "baking", "yeast") to "Un & Pastane Malzemeleri",
        listOf("spice", "seasoning", "herb") to "Baharat & Çeşni",
        listOf("nut", "dried fruit", "dried-fruit", "raisin", "almond", "hazelnut") to "Kuruyemiş & Kuru Meyve",
        listOf("chicken", "poultry", "turkey meat") to "Tavuk & Beyaz Et",
        listOf("fish", "seafood", "tuna", "anchovy") to "Balık & Deniz Ürünleri",
        listOf("meat", "sausage", "salami", "charcuterie", "deli") to "Et & Şarküteri",
        listOf("soda", "cola", "carbonated", "soft drink", "fizzy") to "Gazlı İçecek",
        listOf("juice", "nectar", "fruit drink") to "Gazsız İçecek (Meyve Suyu/Nektar)",
        listOf("water", "mineral water", "spring water") to "Su & Maden Suyu",
        listOf("tea", "herbal tea") to "Çay",
        listOf("coffee") to "Kahve",
        listOf("energy drink", "sports drink") to "Enerji & Sporcu İçecekleri",
        listOf("baby", "infant", "toddler") to "Bebek & Çocuk Gıdası",
        listOf("soup", "ready meal", "ready-made", "instant meal") to "Hazır Yemek & Çorba",
        listOf("bread", "bakery", "loaf", "bun") to "Ekmek & Unlu Mamul",
        listOf("plant-based", "vegan", "vegetarian", "tofu", "soy drink") to "Vegan & Bitkisel Ürünler"
    )

    /**
     * OFF kategori etiketleri + serbest metinden taksonomi adını çözer.
     * @param tagsEn `categories_tags_en` (ör. ["Dairies","Milks","Semi-skimmed milks"])
     * @param categoriesText serbest `categories` alanı (yedek)
     */
    fun resolve(tagsEn: List<String>?, categoriesText: String?): String? {
        val haystack = buildList {
            tagsEn?.forEach { add(normalize(it)) }
            categoriesText?.let { add(normalize(it)) }
        }
        if (haystack.isEmpty()) return null
        // En spesifik eşleşmeyi yakalamak için kuralları sırayla; etiketlerde EN SON (en derin) öğe en spesifiktir.
        val joined = haystack.joinToString(" | ")
        for ((keywords, category) in RULES) {
            if (keywords.any { joined.contains(it) }) return category
        }
        return null
    }

    private fun normalize(s: String): String =
        s.removePrefix("en:").removePrefix("tr:").replace('-', ' ').lowercase().trim()
}
