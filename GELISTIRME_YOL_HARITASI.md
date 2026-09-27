# Gıda Barkod — Geliştirme Yol Haritası

> Bu belge, kullanıcı notlarından çıkarılan tüm hata/geliştirme/iyileştirme
> maddelerini bağımlılık sırasına göre fazlara böler. Her faz; **Veritabanı →
> Backend (CI4) → Android** sırasıyla uygulanır.

## Mimari Özet

| Katman | Konum | Açıklama |
|--------|-------|----------|
| Android | `Desktop/gıda-barkod-analiz-uygulaması` | Room cache + `Ci4ApiService` + Gemini AI |
| Backend (CI4) | `C:\xampp\htdocs\ci4` | Modül: `modules/Gidabarkod` |
| API | `app/Controllers/Api/V1/Modules/GidaBarkodApiController.php` | `/api/v1/gida-barkod/*` |
| Web Panel | `modules/Gidabarkod/Controllers/Gidabarkod.php` + `Views/` | `/yonetim/gidabarkod` |
| DB | local `ci4` (XAMPP MySQL) → canlı `erp.cilginyazilim.com` | Migrations + `canli_alter_*.sql` |

Her şema değişikliği için **hem** CI4 migration **hem** `canli_alter_*.sql` (live'a
elle uygulanacak güvenli script) yazılır — mevcut konvansiyon.

---

## FAZ 1 — Nutri-Score Sağlık Puanlama Sistemi
**Neden ilk:** Mevcut `health_score`'u genişletir, her üründe görünür, başka
özelliğe bağımlı değildir.

- A: 90-100 · B: 75-89 · C: 50-74 · D: 25-49 · E: 0-24 harf notları
- 4 ayrı puan: **Genel Sağlık** (mevcut `health_score`), **Şeker Riski**,
  **Katkı Yoğunluğu**, **Besin Kalitesi** (her biri 0-100 → A-E)
- Değer etiketleri: "Protein yüksek", "Tuz düşük", "Palm yağı var", "İlave şeker var"

**DB:** `gida_barkod_products`'a `sugar_score`, `additive_score`, `nutrition_score`
(INT, null), `score_tags` (JSON/TEXT, null) eklenir.
**Backend:** Model `allowedFields` + validation; API otomatik döner (tam satır).
**Android:** `GeminiProductAnalysis` + prompt 3 yeni puan + etiket üretir;
`Ci4Product` + mapper + `SavedProduct` + Room v6; `ProductDetailsScreen` A-E rozet UI.

## FAZ 2 — Ürün Yorumları + Onay + Beğeni / Puanlama
- Kullanıcı yorum yazar → `pending`. Sahibi kendi yorumunu görür; onaylanınca herkese açık.
- Yorumlara beğeni + 1-5 yıldız puanlama.

**DB:** `gida_barkod_comments` (id, product_id, user_id, body, rating, status,
like_count, created_at...), `gida_barkod_comment_likes` (comment_id, user_id uniq).
**Backend:** Mobil API uçları (list/create/like) + web moderasyon paneli
(`pending`/approve/reject), RBAC scope.
**Android:** Ürün detayında yorum listesi + yazma + beğeni/puan UI.

## FAZ 3 — Hata Bildirimi / Düzeltme Talebi
- Yanlış/eksik bilgi için kullanıcı düzeltme talebi açar; panelde incelenir.

**DB:** `gida_barkod_corrections` (product_id, user_id, field, old_value,
suggested_value, note, status).
**Backend:** API create + web panel list/uygula. **Android:** detayda "Hata bildir" formu.

## FAZ 4 — AI Varsayılan Sorular (SSS) + Kullanıcı Sorgusu + CMS
- Web'de yönetilen varsayılan AI soruları (SSS) — kısa yanıt üretilir.
- Android'de kullanıcı kendi sorusunu/prompt eklemesini girer (kişiye özel yanıt
  genel yoruma dahil EDİLMEZ).
- `gida_pages` CMS tablosu (makale, kullanım şartları, gizlilik, iletişim).

**DB:** `gida_barkod_ai_questions` (soru, sıra, aktif), `gida_pages` (slug, title, body, status).
**Backend:** API (default questions + pages list/show) + web CRUD panelleri.
**Android:** AI sohbetinde varsayılan soru çipleri + kendi sorusu alanı; CMS sayfa görüntüleyici.

## FAZ 5 — Android Fiyat İyileştirmeleri
- Fiyat girişinde sadece sayısal klavye (`KeyboardType.Decimal`), küsürat (5,5) desteği.
- Ürün detayında güncel fiyat + fiyat geçmişi grafiği/listesi.

**Backend:** Yeni mobil uç `GET gida-barkod/barcode/{barcode}/price-history`
(mevcut `GidaBarkodPriceHistory::getHistory` kullanır).
**Android:** Fiyat girişi `OutlinedTextField` keyboard + detayda fiyat geçmişi bölümü.

## FAZ 6 — Android Görsel İyileştirmeler (EN SON)
- Kullanıcı isteği gereği en sona bırakıldı. Detay/liste/tarama ekranları
  görsel tutarlılık, boşluk, tipografi, rozet/renk iyileştirmeleri.

---

## İlerleme
- [x] Faz 1 — Nutri-Score ✅ (DB + backend + Android, BUILD SUCCESSFUL)
- [x] Faz 2 — Yorumlar + onay + puanlama ✅ (DB + backend + web panel + Android, BUILD SUCCESSFUL)
- [x] Faz 3 — Hata bildirimi ✅ (DB + backend + web panel + Android, BUILD SUCCESSFUL)
- [x] Faz 4 — AI sorular + CMS ✅ (DB + backend + web panel + Android, BUILD SUCCESSFUL)
- [x] Faz 5 — Fiyat iyileştirmeleri ✅ (mobil price-history ucu + sayısal klavye + detayda fiyat/geçmiş, BUILD SUCCESSFUL)
- [x] Faz 6 — Görsel iyileştirmeler (1. tur: global tipografi ölçeği) ✅ — cihaz testinden sonra ekran bazlı ince ayar yapılabilir

---

# 🟢 BÜYÜK GÜNCELLEME — 2026-06-26 / 27 (Tasarım + Temiz Veri Hattı + Faz-2 Yenilikler)

> Bu bölüm, rakip uygulama incelemesi sonrası yapılan **tam tasarım yenilemesi**,
> **veri standardizasyonu (temiz veri hattı)** ve birçok hata/özellik çalışmasını belgeler.
> ⚠️ Backend değişiklikleri **canlıya deploy edilmeden** mobilde etkili olmaz (app canlı backend'e bağlı).

## A) Mobil Tasarım Yenilemesi (tamamlandı, cihazda kurulu)
- **Canlı yeşil kimlik** (`#16A34A` primary / `#22C55E` vurgu / `#DCFCE7` çip), açık gri zemin, beyaz "yüzen" kartlar.
  Renk isimleri korunup değerleri değiştirildi → tüm ekranlar tek hamlede yenilendi.
- **Ortada yüzen tarama FAB'lı alt menü** (Geçmiş · Favoriler · ⬤TARA · Kara Liste · Profil).
- **Tam ekran karanlık tarayıcı** (köşe ayraçlı vizör + yeşil tarama çizgisi, "Barkod gir", "Analiz Et"/"Galeriden Seç").
- **Liste ekranları** modern ürün kartı (skor rozeti, koyu ad/gri marka) → paylaşılan `CommonComponents.kt`.
- **Geri tuşu çıkış onayı** (önce ürün ekranı → ana sekme → çıkış sorusu).

## B) Temiz Veri Hattı (OFF + AI → DB standardizasyonu)
- **Sıkı AI politikası:** uydurma YOK; doğrulanmayan alan "Bilinmiyor"/null. Türkçe çıktı, ad'dan marka/gramaj ayrıştırma.
- **Sabit kategori taksonomisi (38)** — AI yalnızca listeden seçer; backend whitelist'e zorlar; OFF `categories_tags_en` → kategori eşlemesi.
- **Resmi Nutri-Score** (deterministik, `NutriScoreCalculator.kt`) — yapısal besinden A–E + alt skorlar.
- **4 durumlu helal** (`muhtemel_helal|supheli|helal_degil|sertifikali`) — AI asla "sertifikalı"/kesin "helal" demez; ucu açık dil.
- **Yapısal besin (JSON)** + **alerjenler** + **E-kodları** (OFF additives_tags öncelikli).
- **ConfirmWeb UX:** kategori dropdown (alfabetik), alana dokununca metin seçili gelir, prefill kategori kanonikleştirilir, gramaj zorunlu.
- Yeni dosyalar: `data/FoodCategories.kt`, `data/OffCategoryMap.kt`, `data/NutriScoreCalculator.kt`.

## C) YENİ VERİTABANI TABLOLARI (şema)
**`gida_barkod_kategori`** — sabit kategori taksonomisi (~38 satır seed)
`id, name(TR), slug(uniq), off_tags(CSV — OFF İngilizce etiketleri), parent_id, sort, status, created_at, updated_at`

**`gida_barkod_ekod`** — zengin E-Kod sözlüğü (ilk parti 29 kod, ~400'e genişletilecek)
`id, code(uniq), name_tr, name_en, aliases, class_tr, what_is_tr(Nedir), function_tr(Fonksiyonu), source_tr(Kaynağı/Nasıl elde edilir), origin_type(bitkisel|hayvansal|mikrobiyal|sentetik|mineral), usage_areas_tr, risk(düşük|orta|yüksek|şüpheli), health_notes_tr, who_should_avoid_tr, halal_status(helal|şüpheli|haram-olası), halal_note_tr, vegan_suitable(evet|hayır|belirsiz), adi, banned_note, ref, status`

**`gida_barkod_user_notes`** — kullanıcıya özel **private** notlar
`id, user_id, barcode, notes, created_at, updated_at` · UNIQUE(user_id, barcode)

**`gida_barkod_products` yeni kolonlar:** `halal_status` VARCHAR(20), `nutri_grade` CHAR(1), `allergens` TEXT
(ayrıca mevcut `nutrition` JSON kolonu artık doldurulmaktadır)

**Tablo adı tutarlılığı:** `gida_pages` → **`gida_barkod_pages`** (tüm tablolar artık `gida_barkod_` ön ekli).

## D) YENİ / DEĞİŞEN API UÇLARI
- `GET  gida-barkod/categories` — sabit kategori listesi (ConfirmWeb dropdown + filtre)
- `GET  gida-barkod/ecodes` (arama `?q=`) · `GET gida-barkod/ecodes/{code}` — E-Kod sözlüğü (public)
- `GET  gida-barkod/user/notes/{barcode}` · `POST gida-barkod/user/notes` — kişisel notlar (upsert)
- `DELETE gida-barkod/ai-questions/{id}` — kullanıcı yalnızca KENDİ sorusunu siler
- `aiQuestions` artık varsayılanlar **+ isteyen kullanıcının kendi soruları**nı döndürür
- `sync.normalizePayload` artık: **marka kanonikleştirme** + **kategori whitelist** + **halal_status** uygular
- **RBAC kaldırıldı** (self-servis, kimlik yeterli): `reportPrice, addComment, toggleCommentLike, addCorrection, suggestAiQuestion, aiQuestions`

## E) DÜZELTİLEN HATALAR
- ✅ 403 "yetkiniz yok" → yorum gönderme / yıldız / fiyat girişi / düzeltme talebi artık çalışır
- ✅ `gida_barkod_ai_questions` Türkçe karakter (mojibake) düzeltildi
- ✅ Kara liste **silme/düzenleme** artık sunucuya işliyor (`addItem` upsert + Android value/type düzeltmeleri)
- ✅ Kişisel notlar + geri bildirim artık **sunucuya kalıcı** (user_notes tablosu + corrections)
- ✅ "Hızlı Engelle" alanı kaldırıldı; "Yeni Öğe Engelle" yeniden tasarlandı (otomatik tür algılama)

## F) ÜRÜN DETAYLARI (Değerlendirme) — yenilenen kısımlar
- Beyaz başlık kartı + skor rozeti, genel değerlendirme kartı
- **4 durumlu helal kartı** (renkli rozet), **alerjen rozetleri**, **Nutri-Score harfi (A–E)**
- **E-Kod detay alt sayfası:** katkı maddesine dokun → `gida_barkod_ekod`'tan Nedir/Fonksiyonu/Kaynağı/Kullanım/Helal
- _(Kalan: içindekiler/yorumlar bölümlerinin kart-restyling'i — sonraki tur)_

## G) 🚀 CANLI DEPLOY KONTROL LİSTESİ
**PHP dosyaları (yükle):**
`app/Controllers/Api/V1/Modules/GidaBarkodApiController.php`, `app/Config/Routes.php`,
`modules/Gidabarkod/Models/GidaBarkodProduct.php`, `GidaBarkodCategory.php` (YENİ), `GidaBarkodEcode.php` (YENİ),
`GidaBarkodUserNote.php` (YENİ), `GidaBarkodUserBlacklist.php`, `GidaBarkodAiQuestion.php`(değişmedi), `GidaPage.php`,
`modules/Gidabarkod/Controllers/Gidabarkod.php`

**SQL (social_erp'de mysql `SOURCE` ile — pipe Türkçeyi bozar):**
`canli_alter_2026-06-26_pipeline.sql` · `canli_alter_2026-06-26_ekod.sql` ·
`canli_alter_2026-06-26_user_notes.sql` · `fix_ai_questions_utf8.sql`
⚠️ `gida_pages`→`gida_barkod_pages` rename canlıda da olacak (veri korunur).

---

# 🌐 WEB PANELİ YOL HARİTASI (yeni — web tasarım/geliştirme için)

> Panel: `modules/Gidabarkod/Controllers/Gidabarkod.php` + `Views/` (`/yonetim/gidabarkod`).
> Yeni tablolar ve ürün alanları için panel yönetim ekranları gerekiyor.

## W1 — Kategori Yönetimi (`gida_barkod_kategori`) — YÜKSEK ÖNCELİK
- CRUD ekranı: ad, slug, **off_tags** (OFF eşleme etiketleri), sıra, durum.
- Ürün düzenlemede kategori artık **dropdown** (bu tablodan) olmalı — serbest metin kaldırılmalı.
- Mevcut serbest-metin kategorileri toplu **kanonikleştir** (eşleşmeyen → "Diğer") aracı.

## W2 — E-Kod Sözlüğü Yönetimi (`gida_barkod_ekod`) — YÜKSEK ÖNCELİK
- CRUD: tüm zengin alanlar (Nedir/Fonksiyonu/Kaynağı/origin_type/risk/halal_status/vegan...).
- **Sözlüğü ~400 koda genişlet** (şu an 29 kritik kod var). Toplu içe aktarma (CSV) faydalı olur.
- "E-Kod Kontrolcüsü" tarzı önizleme; ürün analizinde bu tablodan beslenecek (deterministik halal/vegan).

## W3 — Ürün Düzenleme Ekranı Güncellemesi — YÜKSEK ÖNCELİK
- Yeni alanlar: **`halal_status`** (4 durum dropdown — admin "Sertifikalı Helal" verebilir), **`nutri_grade`**, **`allergens`**, **`nutrition`** (JSON editör).
- Kategori dropdown (W1), marka kanonik (mevcut).
- "Sertifikalı Helal" yalnızca panelden verilebilir (AI veremez) — sertifika görseli/no alanı eklenebilir (gelecek).

## W4 — Geri Bildirim & Düzeltme İnceleme (`gida_barkod_corrections`)
- Mobil "Hata/Öneri/İstek" geri bildirimleri artık `field='geri_bildirim'` ile bu tabloya düşüyor.
- Panelde liste + durum (pending/incelendi) + ürüne uygula akışı. Geri bildirim tiplerini ayırt et (`[ŞİKAYET]/[ÖNERİ]/[İSTEK]`).

## W5 — AI Soru Yönetimi (`gida_barkod_ai_questions`)
- Kullanıcı önerileri (`source=user, is_active=0`) panelde **onay** akışı (onaylanınca herkese görünür).
- Varsayılan soruların (SSS) sıra/aktiflik yönetimi. Türkçe karakter düzeltmesi uygulanmış olmalı.

## W6 — CMS Sayfaları (`gida_barkod_pages`)
- Tablo adı `gida_pages`→`gida_barkod_pages` değişti — panel sorguları/Model güncellendi, **canlıda rename uygulanmalı**.
- Mevcut CRUD korunur.

## W7 — Kişisel Notlar (`gida_barkod_user_notes`) — GİZLİLİK
- Bu tablo **kullanıcıya özel/private**. Panelde **gösterilmemeli** (veya yalnızca anonim sayı/istatistik).

## W8 — Yorum Moderasyonu (`gida_barkod_comments`) — mevcut
- Pending/approve/reject akışı korunur; yeni RBAC değişikliği yalnızca mobil self-servisi etkiler.

## W9 — Marka & Nutri-Score Panelleri — mevcut
- `gida_barkod_brands` kanonikleştirme paneli (mevcut). Mobil sync artık marka kanonikleştirme yaptığından panelle tutarlı.

## Web Öncelik Sırası (öneri)
1. **W3** (ürün düzenleme yeni alanlar) → 2. **W1** (kategori) → 3. **W2** (E-Kod sözlüğü) →
4. **W4** (geri bildirim) → 5. **W5** (AI soru onay) → 6. **W6/W7/W8/W9** (bakım/gizlilik).
</content>
</invoke>
