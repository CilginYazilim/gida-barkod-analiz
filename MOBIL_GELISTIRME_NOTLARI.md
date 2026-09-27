# Gıda Barkod — Mobil & Backend Geliştirme Notları

> Bu belge, canlıya alma + mobil tarafın uçtan uca düzeltilmesi sürecindeki tüm
> değişiklikleri, kök nedenleri ve **canlı deploy kontrol listesini** içerir.
> Tarih: 2026-06-25 / 2026-06-26.

## Mimari Özet

| Katman | Konum | Not |
|--------|-------|-----|
| Android | `Desktop/gıda-barkod-analiz-uygulaması` | Kotlin/Compose, Room cache + Retrofit/Moshi + Gemini AI |
| Backend (CI4) | `C:\xampp\htdocs\ci4` | Modül: `modules/Gidabarkod`, API: `app/Controllers/Api/V1/Modules/GidaBarkodApiController.php` |
| DB | local `ci4` → canlı `social_erp` | Şema değişiklikleri `modules/Gidabarkod/Database/canli_alter_*.sql` |
| API tabanı | `https://erp.cilginyazilim.com/api/v1` | Android `.env` → `CI4_API_BASE_URL`, `CI4_API_KEY` |

---

## 1) Kritik Backend Hataları ve Çözümleri

### 1.1 Tüm `gida-barkod` API uçları 500 veriyordu (FATAL)
- **Kök neden:** `GidaBarkodApiController::getPage($slug)` metodu, parent
  `BaseApiController::getPage(): int` (sayfalama helper'ı) ile **imza çakışması**
  yapıyordu → PHP controller'ı hiç yükleyemiyordu → her uç boş gövdeyle 500.
- **Çözüm:** CMS metodu `showPage()` olarak yeniden adlandırıldı; route güncellendi.
- Dosyalar: `GidaBarkodApiController.php`, `app/Config/Routes.php`.

### 1.2 `Ci4Product` parse hatası → tarama "sıfırdan" yapıyordu
- **Kök neden:** MySQLi sayısal/bool kolonları JSON'da **string** döndürür
  (`"health_score":"65"`, `"is_vegan":"1"`). Android `Ci4Product` bunları
  `Int?`/`Boolean?` bekliyor → Moshi parse'ı patlıyor → `fetchFromCatalog` null
  → katalogda olsa bile yeniden AI taraması.
- **Çözüm:** `GidaBarkodProduct::castTypes()` eklendi; `barcode`, `sync`,
  `userFavorites`, `userProducts` yanıtlarında uygulanıyor (int/float/bool cast).

### 1.3 `created_by` NULL kaydediliyordu
- **Kök neden:** `sync()` ucu hiç kimlik göndermiyordu. Düz `Authorization: Bearer`
  eklemek tehlikeli: filtre JWT/RBAC yoluna geçip normal kullanıcıya **403** verir.
- **Çözüm:** `sync` API-Key ile yetkilenmeye devam ediyor; kullanıcı kimliği ayrı
  **`X-User-Token`** header'ı ile taşınıyor. `getJwtUserId()` hem `Authorization`
  hem `X-User-Token`'ı okuyor. Oturum yoksa `created_by = NULL` (normal).

### 1.4 Fiyat `products.price`'a yazılıyordu
- **İstenen:** Fiyat yalnızca `gida_barkod_price_history`'de tutulsun.
- **Çözüm:** `GidaBarkodPriceHistory::recordPrice()` artık `products.price`'ı
  GÜNCELLEMİYOR; `getCurrentPrice()` ile güncel fiyat geçmişin en yenisinden
  türetiliyor. `price-history` ucunun `current_price`'ı buna göre güncellendi.

### 1.5 Favori / Kara Liste / Taramalar sunucuya yazmıyordu (403)
- **Kök neden:** `toggleFavorite`, `addBlacklist`, `removeBlacklist` gereksiz
  `authorize('edit'/'delete')` (modül RBAC) istiyordu → JWT'li normal kullanıcı 403.
- **Çözüm:** Bu **kullanıcıya özel self-servis** uçlardan RBAC kontrolü kaldırıldı
  (kimlik = `getJwtUserId` yeterli). `userFavorites`/`userBlacklist` GET'lerinden de
  `canRead()` kaldırıldı.
- **Yeni uç:** `GET gida-barkod/user/products` → `created_by = kullanıcı` (taramalarım).
- `userFavorites` artık **tam ürün satırı** döndürüyor (Ci4Product parse edilebilsin).

### 1.6 `login()` ad/soyad döndürmüyordu → re-login'de soyad boş
- **Çözüm:** `AuthController::login()` yanıtına `first_name` + `last_name` eklendi
  (register/me zaten dönüyordu).

---

## 2) Google ile Giriş (en kritik)

İki ayrı OAuth client **bilerek** var:
- **Web** (panel girişi): `683276583498-ctqocm6t...` → backend `.env` `GOOGLE_CLIENT_ID`
- **Mobil** (Android): proje `barkodanalizuygulamasi` / `524588146714`

### Yaşanan sorunlar ve çözümleri
1. **aud uyuşmazlığı:** backend `GOOGLE_CLIENT_ID` ile mobil token'ın `aud`'u farklıydı.
   → Backend `googleToken()` artık `GOOGLE_MOBILE_CLIENT_ID` **ve** `GOOGLE_CLIENT_ID`'yi
   kabul ediyor (mobil öncelik). Web client'ı bozmadan ayrı değişken eklendi.
2. **Kod 10 (DEVELOPER_ERROR):** `requestIdToken()`'a **Android** client ID veriliyordu.
   → Aynı projede **Web application** client oluşturuldu:
   `524588146714-1ovri37b8k8rq55vnibu17mrl2vm990d`. Android `GOOGLE_WEB_CLIENT_ID`
   ve backend `GOOGLE_MOBILE_CLIENT_ID` bu Web client'a ayarlandı. Android client
   (SHA-1: `94:A4:6D:1A:6C:A2:DE:1D:3F:BA:5D:F9:33:2E:6F:5F:60:72:55:25`,
   paket: `com.aistudio.barcodeanalyzer.trgmdz`) sign-in izni için projede duruyor.
3. **Yanlış hesaba giriş (legacy bug):** Eski `GoogleSignIn` API çok hesaplı cihazda
   **bayat/cached idToken** döndürüyordu (seçilen hesap doğru ama token önceki hesabın).
   `revokeAccess()` de çözmedi.
   → **Credential Manager + Google Identity Services**'e geçildi
   (`GetSignInWithGoogleOption` + `GoogleIdTokenCredential`). Seçilen hesap için her
   zaman **taze idToken** üretir; bu bug kökten çözülür.
   - Eklenen bağımlılıklar: `androidx.credentials:credentials(-play-services-auth):1.3.0`,
     `com.google.android.libraries.identity.googleid:googleid:1.1.1`.

---

## 3) Profil Sayfası — Yeniden Tasarım + Özellikler

- **Soyad kalıcılığı:** login ad/soyad döndürüyor; oturum SharedPreferences'ta saklanıyor.
- **Misafir verisi taşıma:** Girişte `userEmail=""` altındaki yerel taramalar/engeller
  hesap e-postasına taşınıyor (`migrateGuestData`, çakışanlar atlanır).
- **Sunucu senkronu:** `syncUserDataFromServer()` — oturum açıkken taramalar
  (`created_by`), favoriler, kara liste sunucudan çekilip Room'a yazılıyor (ADDITIVE).
  Çağrı: girişte, uygulama açılışında, Profil ekranı açılınca.
- **Tıklanabilir kartlar:** Taramalarım→Geçmiş, Favorilerim→Favoriler, Engellerim→Kara Liste.
- **Beni Hatırla:** Giriş alanında checkbox (kapalıysa oturum kalıcı saklanmaz).
- **Yeni bölümler:** "Hesap & Güvenlik" (Profili Düzenle, **Şifre Değiştir**,
  Verileri Yenile) + "Görünüm" (Açık/Koyu/Sistem **tema** seçici, kalıcı).
- **Şifre değiştir:** `auth/change-password` (current/new/confirm).

---

## 4) Diğer Mobil İyileştirmeler

- **Alt menü:** Ürün değerlendirme (Success) ekranında da görünür; sekmeye basınca
  ürün ekranından çıkar (`setTab` scanState'i sıfırlıyor).
- **ConfirmWeb:** "Ürün Bulundu" ekranında taranan **barkod rozeti** gösteriliyor.

---

## 5) CANLI DEPLOY KONTROL LİSTESİ

### Kod (C:\xampp\htdocs\ci4 → erp.cilginyazilim.com)
```
app/Controllers/Api/V1/Modules/GidaBarkodApiController.php
app/Controllers/Api/V1/Auth/AuthController.php
app/Config/Routes.php
modules/Gidabarkod/Models/GidaBarkodProduct.php
modules/Gidabarkod/Models/GidaBarkodPriceHistory.php
modules/Gidabarkod/Models/GidaBarkodUserFavorite.php
modules/Gidabarkod/ (web panel: Controllers/Gidabarkod.php + Views/* — Nutri-Score paneli)
```

### Canlı `.env`
```
GOOGLE_CLIENT_ID=683276583498-ctqocm6tpqe1bdfdeq1gl5r6o2mvbv4e.apps.googleusercontent.com
GOOGLE_MOBILE_CLIENT_ID=524588146714-1ovri37b8k8rq55vnibu17mrl2vm990d.apps.googleusercontent.com
```

### Canlı DB (social_erp) — daha önce uygulanan SQL'ler
`canli_alter_2026-06-25.sql` (nutri) → `-v2` (yorumlar) → `-v3` (düzeltme) → `-v4` (AI+pages).
`users.app_source` kolonu mevcut olmalı (mobil kayıt işaretleme).

### Android
`gradlew installDebug` ile cihaza kuruldu. `.env`:
- `GOOGLE_WEB_CLIENT_ID=524588146714-1ovri37b8k8rq55vnibu17mrl2vm990d.apps.googleusercontent.com`

---

## 6) Bilinen / İzlenecekler
- Google Sign-In artık Credential Manager ile; cihazda çok hesap varsa doğru hesap seçtirir.
- Sunucu senkronu ADDITIVE'dir: "Verileri Sıfırla" yalnızca yereli temizler; tekrar
  giriş/senkron sunucudan geri çeker (sunucu kaynak doğrudur).





---

## 7) E-Kod Sözlüğü Genişletme (2026-06-29)

Sunucu E-Kod sözlüğü **158 yasal koda** çıkarıldı (AB 1333/2008 + Türk Gıda Kodeksi,
resmi gerekçe/ADI/yasak notlarıyla). Mobil bu sözlüğü API'den çekiyor — yerel seed yok,
yani 158 kod otomatik geçerli. Mobil tarafta yapılan değişiklikler:

- **`Ci4Ecode` modeli** (`data/network/Ci4Api.kt`): yeni alanlar `adi`, `banned_note` (bannedNote),
  `ref` eklendi. Liste ucu `GET gida-barkod/ecodes?q=` için `getEcodes(query)` retrofit metodu eklendi.
- **`ProductRepository.listEcodes(q)`** + **`ProductViewModel`**: sözlük listesi + 300ms debounce'lu arama
  state'i (`ecodeList/ecodeListLoading/ecodeQuery`) ve overlay görünürlüğü (`showEcodeDictionary`).
- **EcodeDetailSheet** (ProductDetailsScreen): artık **yasal kısıtlama uyarısı** (kırmızı banner; örn.
  E171 AB yasak), **ADI** (kabul edilebilir günlük alım) ve **resmi kaynak/gerekçe** (`ref`) gösteriyor.
- **Yeni ekran `EcodeDictionaryScreen.kt`**: ürüne bağlı olmadan tüm katkıları ara/listele; satıra tıkla →
  ortak EcodeDetailSheet açılır; kısıtlı kodlarda ⛔ rozeti. **Profil → "Bilgi & Araçlar → E-Kod Sözlüğü"**
  ile açılır (tam ekran overlay, `MainAppScreen`).
- Derleme: `compileDebugKotlin` BUILD SUCCESSFUL (yalnız mevcut deprecation uyarıları).

---

## 8) Kullanışlılık & Özellik Turu (2026-06-29)

Kullanıcı deneyimi + özellik eklemeleri (hepsi derlendi, BUILD SUCCESSFUL):

1. **Haptik geri bildirim** — tarama/analiz başarıyla tamamlanınca cihaz titreşimi (`MainAppScreen`,
   `LocalHapticFeedback` + `LaunchedEffect(scanState)`).
2. **Fener (torch) düğmesi** — tarama ekranı üst barına flaş aç/kapa; CameraX `cameraControl.enableTorch`
   ile (donanım `hasFlashUnit()` destekliyorsa). `ScanSearchScreen` + `CameraScannerView`/`CameraXPreviewSurface`
   `torchOn` parametresiyle.
3. **Nutri-Score harf rozeti** — liste kartlarında (Geçmiş/Favoriler) skor rozetinin yanında renkli A-E
   harfi (`CommonComponents.NutriGradeMini`, `ProductRowCard(grade=...)`).
4. **Arama + sıralama** — Geçmiş & Favoriler ekranlarında ürün/marka araması + En Yeni/Sağlık/A-Z sıralama
   çipleri (`CommonComponents.ListSearchSortBar` + `applyListControls`, `ListSort` enum).
5. **Kısıtlı/yasaklı katkı uyarısı** — ürün detayında, üründeki E-kodlar 158'lik sözlükle çapraz kontrol edilip
   mevzuatta kısıtlı olan (banned_note) katkılar kırmızı uyarı kartında listelenir (`ProductDetailsScreen`
   + `ProductViewModel.restrictedAdditives()`, sözlük önbellekli).
6. **Paylaş butonu** — ürün detay üst barında; ad/marka/sağlık skoru/Nutri-Score/helal/barkod özetini
   `ACTION_SEND` ile paylaşır (`shareProductSummary`).

APK: `app/build/outputs/apk/debug/app-debug.apk` (46 MB). Kurulum: cihazı bağlayıp
`adb install -r ...apk` ya da APK'yı telefona kopyalayıp elle kur.
