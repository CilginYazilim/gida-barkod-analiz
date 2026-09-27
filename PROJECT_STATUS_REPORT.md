# GıdaBarkod Projesi — Durum Raporu

**Tarih:** 2026-06-25
**Kapsam:** Web yönetim paneli (CodeIgniter 4) + Android uygulaması (Kotlin/Compose)
**Hazırlayan:** Otomatik geliştirme oturumu

---

## 1. Yapılan Değişiklikler (Dosya Bazında)

### Web — CodeIgniter 4 (`C:\xampp\htdocs\ci4`)

| Dosya | Değişiklik |
|------|-----------|
| `modules/Gidabarkod/Controllers/Gidabarkod.php` | Marka/ad/kategori normalizasyonu, OFF barkod sorgu, marka yönetimi (CRUD + birleştirme), fiyat analitiği, AJAX autocomplete uçları, **kritik update/store validation bug fix**, detaylı hata mesajları |
| `modules/Gidabarkod/Models/GidaBarkodProduct.php` | `approve()`, `reject()`, `upsertByBarcode()` içinde `skipValidation(true)` — partial update'lerde `is_unique[{id}]` çökmesini giderdi |
| `modules/Gidabarkod/Models/GidaBarkodPriceHistory.php` | Ürün fiyatı güncellemesinde `skipValidation(true)` |
| `modules/Gidabarkod/Models/GidaBarkodBrand.php` | **YENİ** — Kanonik marka modeli: `canonicalize()` (alias eşleme + otomatik öğrenme), `suggest()`, `syncFromProducts()`, `recountProducts()` |
| `modules/Gidabarkod/Libraries/TextNormalizer.php` | **YENİ** — Türkçe duyarlı Title Case, marka sadeleştirme, slug üretimi |
| `modules/Gidabarkod/Commands/GidaBarkodSyncBrands.php` | **YENİ** — `php spark gidabarkod:sync-brands` komutu |
| `modules/Gidabarkod/Views/index.php` | DataTable, marka filtresi, ayrı **Tarama** sütunu (sıralanabilir), ürün **görsel** thumbnail'ı, durum/kategori filtreleri; section bug fix |
| `modules/Gidabarkod/Views/detail.php` | Yeniden tasarım: fiyat geçmişi özeti, besin değerleri **Türkçe çeviri**, görsel, AI yorumu aç/kapa |
| `modules/Gidabarkod/Views/_form.php` | Barkoddan **otomatik doldur** butonu (OFF), marka & kategori **autocomplete**, double-escape bug fix, Türkçe placeholder'lar |
| `modules/Gidabarkod/Views/price_history.php` | **Chart.js grafiği**, min/max/ortalama/değişim kartları, satır bazlı değişim göstergesi |
| `modules/Gidabarkod/Views/brands.php` | **YENİ** — Marka yönetim ekranı (düzenle, sil, birleştir, ara) |
| `modules/Gidabarkod/Views/menu.php` | Sidebar'a "Marka Yönetimi" eklendi |
| `modules/Gidabarkod/Routes.php` | Marka yönetimi + AJAX (brand-suggest, category-suggest, lookup-barcode) route'ları |
| `app/Controllers/Api/V1/Auth/AuthController.php` | `register()` ve `googleToken()` artık `app_source` kaydeder |
| `app/Models/UserModel.php` | `app_source` allowedFields'e eklendi |
| `app/Database/Migrations/2026-06-25-000001_add_app_source_to_users.php` | **YENİ** — `users.app_source` sütunu (mobil kullanıcı kaynak takibi) |
| `app/Database/Migrations/2026-06-25-000002_create_gida_barkod_brands.php` | **YENİ** — `gida_barkod_brands` tablosu |

### Android — Kotlin/Compose (`app/src/main/java/com/example`)

| Dosya | Değişiklik |
|------|-----------|
| `data/network/Ci4Api.kt` | Yeni endpoint'ler (favori/blacklist/fiyat/profil toggle), JWT bearer header, `app_source` alanları, `Ci4GoogleTokenRequest` |
| `data/repository/ProductRepository.kt` | Favori/blacklist/fiyat/profil **sunucu senkronizasyonu**, Google giriş, çapraz hesap barkod arama, katalog merge |
| `ui/ProductViewModel.kt` | JWT token yönetimi, **barkod duplicate akışı** (yerel → diğer hesap → CI4 katalog → AI), Google giriş, profil sunucu sync |
| `ui/screens/ProfileScreen.kt` | **Google ile Giriş/Kayıt** butonu, profil düzenleme sunucu sync göstergesi |
| `data/local/AppDatabase.kt` | `getProductByBarcodeAny()` DAO metodu (hesaplar arası barkod arama) |
| `build.gradle.kts` | `play-services-auth:21.3.0` bağımlılığı |
| `.env` / `.env.example` | `GOOGLE_WEB_CLIENT_ID` eklendi |

---

## 2. Yeni Özellikler

### 2.1 Marka Standartlaştırma (Veri Kalitesi)
- **Kanonik marka tablosu** (`gida_barkod_brands`): ad, slug, alias, durum, ürün sayısı.
- **Otomatik öğrenme**: Yeni bir marka ilk kez girildiğinde tabloya eklenir.
- **Alias eşleme**: "BİM Marketleri", "bim mağazaları", "Bim Market" → hepsi tek kanonik **"Bim"** markasına eşlenir.
- **Marka yönetim ekranı**: Düzenleme, alias tanımlama, **iki markayı birleştirme** (ürünleri taşır, kaynağı alias yapar), silme, arama.
- **Autocomplete**: Ürün formunda marka/kategori için canlı öneri (datalist).

### 2.2 Türkçe Yazım Normalizasyonu
- `"TARIM kredi NAR ekşisi"` → `"Tarım Kredi Nar Ekşisi"` (otomatik).
- Fazla boşluk temizliği, Türkçe i/İ ve ı/I kuralları korunur.
- Bağlaçlar (ve, ile, de, da) küçük kalır; bilinen kısaltmalar (TR, UHT, AB) büyük kalır.
- Marka/ürün adı/kategori her kayıtta normalize edilir.

### 2.3 Barkoddan Otomatik Ürün Oluşturma (Open Food Facts)
- Formdaki **"Otomatik Doldur"** butonu Open Food Facts API'sini sorgular.
- Strateji: **1) Yerel DB** (zaten kayıtlıysa uyarı + düzenleme linki) → **2) Open Food Facts API**.
- Otomatik dolan alanlar: ürün adı, marka (kanonikleştirilmiş), kategori, gramaj, görsel, içindekiler, besin değerleri (100g, Türkçe etiketli).
- Kullanıcı kontrol edip kaydeder (en az manuel giriş).

### 2.4 Fiyat Geçmişi Analitikleri
- **Chart.js çizgi grafiği** (tarih bazlı fiyat seyri).
- En düşük / en yüksek / ortalama / toplam değişim (%) kartları.
- Tabloda satır bazlı artış/azalış göstergesi.
- AJAX ile anlık güncelleme (yeni fiyat eklendiğinde grafik+kartlar yenilenir).

### 2.5 Liste Sayfası İyileştirmeleri
- Ürün **görsel thumbnail'ı** tabloda.
- **Tarama sayısı ayrı sütun** — en çok/az sorgulanana göre sıralanabilir (varsayılan: en çok taranan).
- Marka, kategori, durum filtreleri + hızlı temizleme.

### 2.6 Mobil Google Sign-In + Kaynak Takibi
- Android'de "Google ile Giriş/Kayıt" butonu (Play Services Auth).
- Mobil kayıtlar `app_source = "gida_barkod"` ile işaretlenir — bu uygulamadan gelen kullanıcılar panelden ayırt edilebilir (admin'e bağlı değil).

---

## 3. Veritabanı Değişiklikleri

| Migration | Tablo | Açıklama |
|-----------|-------|----------|
| `2026-06-25-000001_add_app_source_to_users` | `users` | `app_source VARCHAR(50) NULL` + index. Mobil kullanıcı kaynak takibi. |
| `2026-06-25-000002_create_gida_barkod_brands` | `gida_barkod_brands` | Kanonik marka tablosu: `id, name, slug(unique), aliases, logo_url, status, product_count, timestamps` |

> Her iki migration **yerel ortamda çalıştırıldı** (`php spark migrate`). Canlıda da çalıştırılmalıdır.

---

## 4. API İyileştirmeleri

- `POST /api/v1/auth/register` → `app_source` parametresi kabul eder (varsayılan `gida_barkod`).
- `POST /api/v1/auth/google-token` → yeni kullanıcı oluştururken `app_source` kaydeder.
- Mobil kullanıcıya özel uçlar (önceki oturumda eklenmişti, doğrulandı): favori toggle, blacklist ekle/sil, fiyat bildir, profil güncelle — tümü JWT bearer ile korumalı.
- **Web AJAX uçları** (auth korumalı): `brand-suggest`, `category-suggest`, `lookup-barcode`.

---

## 5. Android İyileştirmeleri

- **Barkod duplicate sorunu çözüldü**: Aynı barkod tekrar tarandığında AI'ya gitmeden önce sırasıyla yerel DB → diğer hesaplardaki kayıt → CI4 merkezi katalog kontrol edilir.
- **Favori/Blacklist/Fiyat sunucu senkronizasyonu**: Giriş yapan kullanıcının işlemleri artık sunucuya yazılır (offline-first; hata akışı bozmaz).
- **Profil düzenleme**: Hem yerel hem sunucu (`users` tablosu) güncellenir.
- **Google Sign-In**: ID token alınıp `auth/google-token` ile JWT'ye çevrilir.
- ConfirmWeb ekranında fiyat girişi → ürünle birlikte kaydedilir.

---

## 6. Tespit Edilen / Açık Konular

1. **Android tam derleme yapılmadı**: Bu ortamda Gradle build/emülatör çalıştırılmadı (proje hafızası: fiziksel cihaza `gradlew installDebug` ile kurulmalı). Kod statik olarak gözden geçirildi ve mevcut ViewModel public API'si korundu; yine de cihazda derleme+test gerekir.
2. **Google Sign-In SHA-1**: `94:A4:6D:1A:6C:A2:DE:1D:3F:BA:5D:F9:33:2E:6F:5F:60:72:55:25` Firebase'e eklendi mi teyit edilmeli; `google-services.json` güncel sürümü `app/` altında olmalı. `GOOGLE_WEB_CLIENT_ID` `.env`'e yazıldı.
3. **OFF kategori/marka kalitesi**: Open Food Facts verisi her barkod için tam olmayabilir; manuel kontrol akışı korundu.
4. **Besin değerleri**: OFF'tan 100g bazında çekiliyor; ürünün porsiyon bazlı değerleri farklı olabilir.
5. **Marka birleştirme geri alınamaz**: Ürünler hedef markaya taşınır; dikkatli kullanılmalı.

---

## 7. Canlıya Çıkış Notları

### Sunucuya gönderilecek dosyalar (CI4)
```
modules/Gidabarkod/Controllers/Gidabarkod.php
modules/Gidabarkod/Models/GidaBarkodProduct.php
modules/Gidabarkod/Models/GidaBarkodPriceHistory.php
modules/Gidabarkod/Models/GidaBarkodBrand.php          (YENİ)
modules/Gidabarkod/Libraries/TextNormalizer.php        (YENİ)
modules/Gidabarkod/Commands/GidaBarkodSyncBrands.php   (YENİ)
modules/Gidabarkod/Routes.php
modules/Gidabarkod/Views/index.php
modules/Gidabarkod/Views/detail.php
modules/Gidabarkod/Views/_form.php
modules/Gidabarkod/Views/price_history.php
modules/Gidabarkod/Views/brands.php                    (YENİ)
modules/Gidabarkod/Views/menu.php
app/Controllers/Api/V1/Auth/AuthController.php
app/Models/UserModel.php
app/Database/Migrations/2026-06-25-000001_add_app_source_to_users.php   (YENİ)
app/Database/Migrations/2026-06-25-000002_create_gida_barkod_brands.php (YENİ)
```

### Migration (canlı sunucuda sırayla)
```bash
php spark migrate
php spark gidabarkod:sync-brands   # mevcut ürün markalarını kanonik tabloya doldurur
```

### Config / ENV değişiklikleri
- **CI4 `.env`**: `GOOGLE_CLIENT_ID` Google ID token doğrulaması için tanımlı olmalı (google-token endpoint kullanıyor).
- **Android `.env`**:
  - `GOOGLE_WEB_CLIENT_ID=524588146714-prnr3usb7e3d6bqecma3du23i2dtpe6d.apps.googleusercontent.com`
  - `CI4_API_BASE_URL`, `CI4_API_KEY`, `GEMINI_API_KEY` (mevcut).
- **Firebase**: Android paket adı `com.aistudio.barcodeanalyzer.trgmdz` + SHA-1 parmak izi eklenmeli; güncel `google-services.json` indirilip `app/` altına konmalı.

### Android derleme
```bash
./gradlew assembleDebug      # veya
./gradlew installDebug       # fiziksel cihaz bağlıyken
```

---

## 8. Test Kontrol Listesi (Canlı Öncesi)

### Web
- [ ] `yonetim/gidabarkod` listesi açılıyor (RuntimeException yok), filtreler/sıralama çalışıyor.
- [ ] Yeni ürün ekleme + **barkoddan otomatik doldur** (geçerli bir barkod ile).
- [ ] Ürün **düzenleme/güncelleme** hatasız (önceki "güncellenirken hata" giderildi).
- [ ] Marka autocomplete öneri getiriyor; yeni marka otomatik öğreniliyor.
- [ ] `yonetim/gidabarkod/brands` — düzenle/birleştir/sil çalışıyor.
- [ ] Fiyat geçmişi grafiği + analitik kartları doğru; yeni fiyat ekleyince güncelleniyor.
- [ ] Ürün silme, onay/red akışı.
- [ ] Detay sayfası besin değerleri Türkçe, fiyat geçmişi özeti görünüyor.

### Android (fiziksel cihaz)
- [ ] Uygulama derleniyor (`installDebug`).
- [ ] E-posta/şifre giriş + kayıt.
- [ ] **Google ile giriş** (Firebase SHA-1 + Web Client ID ayarlı ise).
- [ ] Barkod okuma; aynı barkodu ikinci kez okuyunca AI'ya gitmeden mevcut veri gösteriliyor.
- [ ] Favori ekle → sunucuda `gida_barkod_user_favorites` kaydı oluşuyor.
- [ ] Blacklist ekle/sil → sunucuda `gida_barkod_user_blacklist`.
- [ ] Fiyat bildir → `gida_barkod_price_history`.
- [ ] Profil düzenleme → `users` tablosu güncelleniyor.
- [ ] Mobil kayıt sonrası `users.app_source = 'gida_barkod'`.

---

*Not: Bu rapor otomatik geliştirme oturumu sonunda üretilmiştir. Canlıya almadan önce yedek alınması önerilir.*
