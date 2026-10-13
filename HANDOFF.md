# 🎯 HANDOFF — Imtiyaz Admin Android

**Update terakhir:** 13 Okt 2026 (v1.2.0, TAHAP 24 SELESAI)
**Repo:** https://github.com/kahfihidayat87/imtiyaz-admin-android
**Local:** `C:\Users\DELL\Documents\GitHub\imtiyaz-admin-android`

---

## 🎯 Status Saat Ini

| Item | Nilai |
|---|---|
| Versi aktif | **1.1.0** (versionCode 2) — dirilis via GitHub Release |
| Commit terakhir | `411ea5d` docs: catat fitur Cek Kesehatan di Saya Screen |
| Package | `com.imtiyaztour.admin` |
| Warna tema | Biru `#1E3A8A` (`AdminPrimary`) |
| Distribusi | APK Release signed via GitHub Release + share langsung |

---

## 🔑 Kredensial & Server

### Server Hostinger
| Item | Nilai |
|---|---|
| IP | `153.92.10.222` |
| SSH Port | `65002` |
| SSH User | `u120369480` |
| SSH Command | `ssh -p 65002 u120369480@153.92.10.222` |

### WordPress (Backend)
| Item | Nilai |
|---|---|
| URL | `https://pastiumrah.com` |
| WP Admin | `https://pastiumrah.com/wp-admin` |
| Plugin folder | `~/domains/pastiumrah.com/public_html/wp-content/plugins/connector-app/` |
| DB Name | `u120369480_ampih` |
| DB User | (lihat `wp-config.php`) |

### Node.js (API Proxy)
| Item | Nilai |
|---|---|
| URL | `https://api.pastiumrah.com` |
| Folder | `~/domains/api.pastiumrah.com/hbuilds/current/nodejs/` |
| CWD Aktual | `~/domains/api.pastiumrah.com/hbuilds/versions/01a0c969-934d-723f-9aba-f2cf0517d7a8/nodejs/` |
| `.env` | `~/domains/api.pastiumrah.com/hbuilds/config/.env` |
| Env aktif | `/opt/alt/alt-nodejs24/enable` |
| Node version | v24.6.0 |

### Admin Accounts (WP Table `wpfq_imtiyaz_admin`)
| ID | Username | Role | Password |
|---|---|---|---|
| 1 | `admin` | `super_admin` | **LIHAT CATATAN TERPISAH** |
| 4 | `keuangan_fatimah` | `keuangan` | (lihat admin app) |
| 5 | `superadmin` | `admin` | (lihat admin app) |

⚠️ **Password ID 1 terakhir direset ke:** `AdminImtiyaz2026!` (SARAN: rotate segera setelah testing)

### API Key (Node.js)
| Item | Nilai |
|---|---|
| Header | `x-api-key` |
| Nilai | (lihat hPanel → Websites → api.pastiumrah.com → Variabel environment) |
| Dipakai di | `ApiConfig.kt` → `INVOICE_API_KEY` |

---

## 📊 Fitur yang Sudah Selesai

### RBAC Role
| Role | Akses | Catatan |
|---|---|---|
| 🟣 `super_admin` | SEMUA + tab Admin + hapus jamaah | Level tertinggi |
| 🔵 `admin` | Dashboard, Jamaah, Bukti, Kanal, Info, Saya + tambah jamaah | Tidak bisa hapus |
| 🟢 `keuangan` | Dashboard, Jamaah, Bukti, Saya + tambah jamaah | Fokus finansial |
| 🟠 `tl` | Dashboard, Kanal, Info, Saya | Lapangan |

### Fitur Utama
| # | Fitur | Endpoint | File |
|---|---|---|---|
| 1 | Login admin | `/admin-login` | `LoginScreen.kt` |
| 2 | Kelola Admin (list/create/delete/update-role/reset-pass) | `/admin-list`, `/admin-create`, `/admin-delete`, `/admin-update-role`, `/admin-reset-password` | `AdminListScreen.kt` |
| 3 | Tambah Jamaah | `/admin-jamaah-create` | `AddJamaahScreen.kt` |
| 4 | Hapus Jamaah (super_admin only) | `/admin-jamaah-delete` | `EditJamaahScreen.kt` |
| 5 | Generate Invoice PDF | `/api/admin-invoice-generate` (Node.js) | `InvoiceScreen.kt` |
| 6 | Simpan detail paket | `/admin-invoice-data-save` | `InvoiceScreen.kt` |
| 7 | Ambil detail paket | `/admin-invoice-data-get` | `InvoiceScreen.kt` |
| 8 | Approve/reject bukti | `/admin-bukti-approve`, `/admin-bukti-reject` | `BuktiListScreen.kt` |
| 9 | Tab Skrining + list + buka PDF | `/wp-json/imtiyaz/v1/skrining-list`, `/api/skrining/pdf/:id` | `SkriningListScreen.kt` |
| 10 | Form Cek Kesehatan (EditJamaah) | `/admin-kesehatan-save`, `/admin-kesehatan-get` | `EditJamaahScreen.kt` |
| 11 | Config item kesehatan (WP) | `/kesehatan-items` (public) | `EditJamaahScreen.kt` |

---

## 🚧 Yang Sedang Dikerjakan / Belum Selesai

### 🔴 PRIORITAS TINGGI
**1. Fix compile error `InvoiceScreen.kt`**
- Error: `Unresolved reference: fasilitasText`, `excludedText`, `savedSnapshot`
- Penyebab: urutan state declaration di `InvoiceScreen.kt` tidak valid
- **Yang sudah dilakukan:** patch restructure (state dipindah ke atas sebelum `LaunchedEffect`)
- **Status:** menunggu verifikasi CI hijau setelah commit terakhir
- **Kalau masih error:** cek bahwa semua `var ... by remember` muncul SEBELUM `LaunchedEffect`

**2. Stage B belum dijalankan**
- File: `AdminModels.kt` — perlu tambah model `InvoiceDataSaveRequest` & `InvoiceDataGetResponse`
- File: `AdminApi.kt` — perlu tambah method `invoiceDataSave` & `invoiceDataGet`
- Cek: `grep -n "InvoiceDataSaveRequest\|invoiceDataSave\|invoiceDataGet" AdminModels.kt AdminApi.kt`
- Kalau kosong → jalankan patch Stage B (lihat bagian "Command Pattern")

### 🟡 PRIORITAS SEDANG
**3. Simpan metadata invoice ke WP postmeta**
- Saat ini metadata invoice ada di file JSON server Node.js (`uploads/invoice/_jamaah_X.json`)
- Idealnya juga di WP postmeta → backup otomatis cover
- Sudah ada `_invoice_data` postmeta untuk detail paket (Stage A)

**4. Test edit jamaah → `_invoice_data` TIDAK hilang**
- Alur test: buat jamaah → isi invoice → generate → edit jamaah → balik invoice → cek data masih ada

### 🟢 PRIORITAS RENDAH
- Bump versi admin app ke 1.1.0
- Splash screen admin
- Log aktivitas admin
- Fitur "Ubah Password Sendiri" untuk super admin (biar tidak perlu SSH)

---

## 🔧 Masalah Umum & Solusi

### A. Server Node.js tidak auto-restart
**Gejala:** patch `app.js` / `invoice-routes.js` tidak aktif meski file sudah berubah.

**Penyebab:** Hostinger pakai **LiteSpeed** (bukan Passenger), `touch tmp/restart.txt` tidak berfungsi.

**Solusi:**
```bash
# 1. Kill proses lama
PID=$(ps aux | grep "app.js" | grep -v grep | awk '{print $2}' | head -1)
kill $PID
sleep 3

# 2. Start manual dengan env
cd ~/domains/api.pastiumrah.com/hbuilds/versions/01a0c969-934d-723f-9aba-f2cf0517d7a8/nodejs/
source /opt/alt/alt-nodejs24/enable
set -a
source ~/domains/api.pastiumrah.com/hbuilds/config/.env
set +a
setsid nohup node app.js >> console.log 2>> stderr.log < /dev/null &
sleep 4

# 3. Cek
ps aux | grep "app.js" | grep -v grep
tail -3 console.log
---

## TAHAP 17 — Fix Invoice (29 Sep 2026)

### Fix 1: Tanggal per Pembayaran

**Commit:** `fix(invoice): tambah input tanggal per pembayaran...`
**File:** `android-app/app/src/main/java/com/imtiyaztour/admin/InvoiceScreen.kt` + `AdminModels.kt`

**Sebelum:** Semua pembayaran pakai `System.currentTimeMillis()` saat Generate → tanggal sama semua.
**Sesudah:** Setiap baris riwayat punya field Tanggal (dengan DatePicker) + Jumlah.

### Fix 2: Logo di Invoice PDF

**File server:** `~/domains/api.pastiumrah.com/hbuilds/versions/01a0c969-934d-723f-9aba-f2cf0517d7a8/nodejs/invoice-routes.js`
**Asset:** `uploads/logo-imtiyaz.png` (150×150 RGBA, 13917 bytes)

**Perubahan:**
- Logo 80×80 pt di kiri atas (x=40, y=40)
- Teks header digeser ke x=130
- Divider hijau digeser dari y=100 → y=128
- y0 (baseline meta) digeser dari 115 → 143

**Backup:** `/tmp/invoice-routes.js.bak-logo-*` di server

---

## TAHAP 18 — Invoice Polish (29 Sep 2026)

### Perubahan Server (`invoice-routes.js`)

| # | Perubahan | Lokasi |
|---|---|---|
| 1 | Logo perkecil 80→64 pt (y: 40→44) | line 45 |
| 2 | Tambah mobile kedua: `081999876546` | line 51 |
| 3 | "Amount Due (IDR)" geser kiri (x: 350→310, value 495→455, width 60→100) — anti-wrap | line 168-170 |
| 4 | Footer: "Apabila terdapat ketidaksesuaian dengan data Anda, mohon bisa melakukan konfirmasi ke Admin Keuangan di 0811176544" | line 191 |

**Backup server:** `/tmp/invoice-routes.js.bak-tahap18-*`

### Perubahan Android (`InvoiceScreen.kt`)

- State `paymentDue` + UI field "Payment Due" + DatePicker + mapping `payment_due = paymentDue`

### Commit TAHAP 18

- `372eabd` feat(invoice): tambah field Payment Due di app admin
- (server) perubahan invoice-routes.js langsung di server (backup di `/tmp/`)

---

## Restart Node.js — CARA WAJIB

**Gunakan script:** `bash scripts/restart-node-admin.sh`

Script ini:
1. Kill **SEMUA** proses via filter `-E "api\.pastiumrah|node app\.js"` (menangkap parent `lsnode` **dan** child `node app.js`)
2. Verifikasi proses bersih sebelum spawn
3. Arsip log lama (auto timestamp)
4. Spawn `node app.js` baru dengan env lengkap
5. Cek stderr — jika ada error, exit dengan kode 1

**Target startup sukses:**
- PID tunggal `node app.js`
- `stderr.log` **KOSONG** (tidak ada EADDRINUSE)
- Log: `Imtiyaz API v2.12.0 jalan di port 3000`

**Catatan:** Proses `lsnode:...api.pastiumrah...` (LiteSpeed wrapper) **bisa auto-respawn** setelah kill — ini normal. Selama port 3000 sudah dipakai spawn manual, wrapper akan idle dan tidak mengganggu.

---

## Kandidat Lanjutan (Sesi Berikutnya)

- **PDF cache buster** — tambah `&v=<timestamp>` di `pdf_url` response agar browser selalu ambil PDF terbaru
- **Sinkronisasi `_invoice_data`** ke WP postmeta (backup otomatis)
- **Supervisor / watchdog Node** auto-respawn (mitigasi crash tanpa restart manual)
- **Bump versi app admin** ke 1.1.0 + changelog

---

## TAHAP 19 — Export PDF Skrining (29 Sep 2026)

**Status:** Selesai & teruji — 1 halaman

### Server Endpoint
- File: `~/domains/api.pastiumrah.com/hbuilds/current/nodejs/skrining-routes.js` (~215 baris, server-only)
- Route: `GET /api/skrining/pdf/:postId`
- Registrasi: `app.js:542` setelah `require('./invoice-routes')...`
- Deps: reuse `axios`, `pdfkit`, `WP_URL`, `WP_AUTH`

### WP Endpoint
- File: `imtiyaz-connector.php` (plugin `connector-app`)
- Route: `GET /wp-json/imtiyaz/v1/skrining/:id`
- Ambil `_skrining_data` + `_jamaah_id`, strip `token`, return JSON

### Fitur PDF
- 8 section A-H, format mirip invoice (logo 46pt, margin 36)
- 1 halaman A4 untuk ~32 field
- Info box: nama / jamaah ID / usia (gap ke section A)
- Font: header 14pt, section 9.5pt, field 8.5pt, row 10pt
- Catatan otomatis medis (auto-detect kondisi kritis)
- Footer `lineBreak:false` + posisi dalam margin (fix spurious page)

### Cara Pakai
https://api.pastiumrah.com/api/skrining/pdf/1482

### Test
- Post 1482 (gjkk, jamaah 1454): 1 halaman, layout rapi
- Post 1481 (title kosong): 1 halaman

### Catatan
- Server code TIDAK di git — edit langsung via SSH
- Backup: `app.js.bak-skrining-*`, `skrining-routes.js.bak-*`
- Log diagnostic: `[skrining-pdf] pages: N` di console.log


---

## TAHAP 21 — Cek Kesehatan Jamaah (9 Okt 2026)

**Status:** ✅ Selesai & teruji

### Fitur
Admin dapat mengisi hasil pemeriksaan kesehatan jamaah:
- Tensi, Gula Darah, Asam Urat, Kolesterol (default)
- Item bisa ditambah/edit/hapus via WP Admin → Pengaturan Kesehatan
- Metadata: Tanggal, Petugas, Catatan

### Sumber Input (3 jalur)
1. **Admin App** — form di EditJamaahScreen, bawah Checklist Dokumen
2. **WP Admin** — metabox di Edit Post Jamaah
3. **API manual** — POST /admin-kesehatan-save

Ketiganya tulis ke postmeta `_kesehatan_data` (JSON).

### File Server
- `imtiyaz-admin-kesehatan.php` (page + metabox)
- `imtiyaz-kesehatan-endpoints.php` (REST endpoints)
- Terdaftar via `require_once` di `imtiyaz-connector.php`

### Endpoints Baru
- `GET /imtiyaz/v1/kesehatan-items` — list item config (public)
- `POST /imtiyaz/v1/admin-kesehatan-get` — admin ambil data
- `POST /imtiyaz/v1/admin-kesehatan-save` — admin simpan
- `POST /imtiyaz/v1/kesehatan-me` — jamaah lihat sendiri

### Data Structure
```json
{
  "items": {"tensi": {"value": "120/80"}, ...},
  "tanggal": 1791559497,
  "petugas": "Wulan",
  "catatan": "Sehat",
  "updated_at": 1791559534
}

---

## TAHAP 20 — Tab Skrining + Buka PDF (9 Okt 2026)

**Status:** Selesai & teruji

### Fitur App Admin
- Tab baru **Skrining** di bottom navigation
- List skrining dari WP (`skrining_kesehatan` post type)
- Tap item -> buka PDF: `https://api.pastiumrah.com/api/skrining/pdf/:postId`

### File Terkait
- `SkriningListScreen.kt` (baru)
- `MainActivity.kt` — tambah tab Skrining

### Endpoint
- `GET /wp-json/imtiyaz/v1/skrining-list` — list post skrining
- `GET /api/skrining/pdf/:id` (Node.js) — PDF 1 halaman

---

## TAHAP 21 — Cek Kesehatan di App Admin (9 Okt 2026)

**Status:** Selesai & teruji

### Fitur
Form di `EditJamaahScreen.kt` (bawah Checklist Dokumen):
- Item default: Tensi, Gula Darah, Asam Urat, Kolesterol
- Item bisa dikonfigurasi via WP Admin -> Pengaturan Kesehatan
- Metadata: Tanggal, Petugas, Catatan

### Alur
1. Admin buka jamaah -> scroll ke Cek Kesehatan
2. Isi nilai per item -> Simpan
3. POST ke `/admin-kesehatan-save` -> tulis postmeta `_kesehatan_data`

### File Terkait
- `EditJamaahScreen.kt` — form + submit
- `AdminModels.kt` — model `KesehatanData`, `KesehatanItemConfig`
- `AdminApi.kt` — endpoint kesehatan

### Endpoint
- `GET /imtiyaz/v1/kesehatan-items` — config item (public)
- `POST /imtiyaz/v1/admin-kesehatan-get` — admin ambil data jamaah
- `POST /imtiyaz/v1/admin-kesehatan-save` — admin simpan

### Verifikasi
Jamaah app baca via `POST /imtiyaz/v1/kesehatan-me` -> Card "Cek Kesehatan" di Saya Screen.

---

## TAHAP 22 — CI Workflow + Signing (10 Okt 2026)

**Status:** Selesai — rilis pertama via CI

### Perubahan
- `versionCode 1 -> 2`, `versionName "1.0.0" -> "1.1.0"`
- `signingConfigs` di `app/build.gradle` (env var: `RELEASE_KEYSTORE_PATH`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`)
- Workflow `build-apk.yml` dirombak:
  - `paths-ignore`: `.md`, `.gitignore`, `.gitattributes`, `scripts/`, `logs/`
  - Build debug APK (artifact)
  - Build release APK signed (env var, conditional)
  - Auto GitHub Release dengan tag `v1.1.0-build<run_number>`
- Reuse keystore dari repo jamaah (package name beda, aman)

### Secrets (4, sama dengan jamaah)
- `RELEASE_KEYSTORE_BASE64`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`

---

## TAHAP 23 — Fix Endpoint Proxy + DNS Mismatch (12 Okt 2026)

**Status:** ✅ Selesai

### Masalah
App admin error `failed to connect to pastiumrah.com/88.223.91.143` (port 443) — IP tidak ada di DNS record.

### Fix
- `ApiConfig.BASE_URL`: `pastiumrah.com` → `api.pastiumrah.com`
- `AdminApi.kt`: 28 endpoint `wp-json/imtiyaz/v1/admin-*` → `api/admin/admin-*`
- Node.js: proxy generik `/api/admin/:endpoint` + override DNS lookup di HTTPS agent (`pastiumrah.com` → `127.0.0.1`)

### Commit
- `370c859` fix(admin): alihkan endpoint admin ke Node.js proxy
- `2ff2e50` chore(gitignore): blokir console.log + stderr.log
- `dfe3567` style(icon): ganti ikon app — logo hitam + background putih

---

## TAHAP 24 — Firebase Custom Auth + Fitur D3 Auto-Assign Kamar (13 Okt 2026)

**Status:** ✅ Selesai, teruji end-to-end

### Ringkasan

Dua pilar utama:
1. **Firebase Custom Auth** — auth admin pakai custom claims (`platform: "admin"`), gate akses Firestore rules
2. **D3 Auto-Assign Kamar** — fitur agensi untuk kelola trip + hotel + pembagian kamar otomatis

### Fase Firebase (A1-A2)

**Server (Node.js):**
- Endpoint baru `POST /api/firebase-token-admin` — verifikasi via WP `admin-me`, return custom token
- Auto-create Firebase user (`createUser`) kalau belum ada
- Set custom claims: `{ role, adminId, platform: "admin" }`

**Android:**
- `FirebaseAuthHelper.kt` — login/logout Firebase
- Firebase SDK: BOM 33.7.0 + Firestore + Auth
- `google-services.json` dengan package `com.imtiyaztour.admin`
- Wire login (`LoginScreen.kt`) + logout (`SayaScreen.kt`)

### Firestore Rules (Additive)

```javascript
function isAdmin() {
  return isSignedIn() && request.auth.token.platform == 'admin';
}
function isAdminOrManager() {
  return isAdmin() && request.auth.token.role in ['admin', 'super_admin', 'keuangan'];
}
match /trips/{tripId}/{document=**} {
  allow read: if isAdmin();
  allow write: if isAdminOrManager();
}
Fitur D3 — Trip & Kamar
Models (TripModels.kt):

Trip, Hotel, Room, Penghuni, JamaahManifest, AssignResult, AssignWarning

roomCapacityFor(tipe): single(1), double(2), triple(3), quad(4), quint(5)

Repository (TripRepository.kt):

CRUD Trip / Hotel / Room / Manifest

updateRoomNumber, updateRoomAndPenghuni (sync nama ke manifest)

deleteRoom, deleteTrip (cascade sub-collections)

addRoomsBatch (batch write max 500)

Auto-Assign Engine (RoomAssignEngine.kt):

assign(jamaahList, rooms) — untuk manifest real (D2 nanti)

assignByCount(paxL, paxP, rooms) — untuk input pax saja (D3)

Sort kamar dari kapasitas terbesar

Setiap kamar dialokasikan ke gender yang proporsi keterisiannya paling tertinggal (fair)

1 kamar = 1 gender (tidak mixed)

UI Screens:

TripListScreen — list trip + FAB tambah

AddTripScreen — mode create/edit

TripDetailScreen — list hotel + edit/delete

AddHotelScreen — mode create/edit

HotelRoomScreen — list kamar + warna gender + sortir + export PDF

GenerateRoomsScreen — Flow 1 (Manual): input pax L/P + jumlah kamar per tipe

GenerateKamarAutoScreen — Flow 2 (Auto): input pax L/P saja

Fitur Utama
Flow 1 — Manual:

Input: pax L/P + jumlah kamar per tipe (quint/quad/triple/double/single)

Preview total kapasitas vs pax

Validasi kapasitas cukup

Auto-assign proporsional

Flow 2 — Auto (baru):

Input: pax L/P saja

Algoritma: quad (4) → triple (3) → double (2) → single (1)

Tanpa quint

Contoh: L=15 → 3 quad + 1 triple = 4 kamar

Preview: format "3 quad + 1 triple = 4 kamar"

Edit Kamar:

Tap kamar → dialog:

Edit nomor kamar

Edit nama setiap penghuni (per field)

Sync nama ke manifest/{jamaahId}.nama

Tombol: BATAL / SIMPAN / HAPUS

Warna Kamar:

Gender	Background	Accent
Laki-laki	Biru #E3F2FD	#1976D2
Perempuan	Pink #FCE4EC	#C2185B
Kosong	Abu #F3F4F6	#6B7280
Sortir Kamar (Bottom Sheet):

Nomor Kamar (default)

Tipe Kamar (quad → triple → double → single → quint)

Kamar Laki-laki (filter)

Kamar Perempuan (filter)

Export PDF (RoomListPdfExporter.kt):

Layout A4 profesional:

Header hijau brand (logo "IT")

Info block (trip, hotel, total, dicetak)

Tabel: NO / KAMAR / TIPE / KAP / PENGHUNI

Row alternating

Footer + page number (auto multi-page)

Share via FileProvider + Intent.ACTION_SEND

FileProvider di AndroidManifest.xml + res/xml/file_paths.xml

Bug Fix Sesi Ini
Bug	Root Cause	Fix
PERMISSION_DENIED Firestore	Admin belum login Firebase	Setup Firebase Auth (A1-A2)
/api/firebase-token-admin 401	Node.js cek r.data.admin_id, WP return id	Ganti cek ke r.data.id
setCustomUserClaims user not found	Firebase user belum ada	createUser dulu, abaikan kalau ada
2 proses Node.js rebutan port	lsnode (LiteSpeed) + node app.js manual	Kill lsnode lama, spawn manual 1x
Compile Unresolved reference: await	Missing import	kotlinx.coroutines.tasks.await
Compile Unresolved reference: AdminTextDark	Theme tidak punya constant	Ganti Color(0xFF111827)
File Baru
File	Fungsi
FirebaseAuthHelper.kt	Login/logout Firebase Auth
TripModels.kt	Data class Trip/Hotel/Room/dll
TripRepository.kt	CRUD Firestore
RoomAssignEngine.kt	Algoritma auto-assign
TripListScreen.kt	UI list trip
AddTripScreen.kt	UI form trip
TripDetailScreen.kt	UI detail trip (hotel list)
AddHotelScreen.kt	UI form hotel
HotelRoomScreen.kt	UI list kamar
GenerateRoomsScreen.kt	Flow 1 — Setup manual
GenerateKamarAutoScreen.kt	Flow 2 — Auto generate
RoomListPdfExporter.kt	Export PDF kamar
File Dimodifikasi
File	Perubahan
build.gradle (root)	Plugin google-services 4.4.2
app/build.gradle	Firebase BOM + Firestore + Auth
AndroidManifest.xml	+ FileProvider
AdminApi.kt	+ firebaseAdminToken endpoint
LoginScreen.kt	+ wire Firebase login
SayaScreen.kt	+ wire Firebase logout
MainActivity.kt	+ tab Trip + state editingTrip/editingHotel/generateMode
google-services.json	Baru (package admin)
res/xml/file_paths.xml	Baru (FileProvider config)
Commit Log
ce3906f (jamaah) feat(auth): Firebase Custom Auth (A2) — login/logout + endpoint firebaseToken

51cb798 feat(admin-D3): hilangkan lantai + edit/hapus trip & hotel

9c7e5c8 feat(admin-D3-flow2): auto-generate kamar dari input pax (quad→triple→double→single, tanpa quint)

(latest) feat(admin-D3): warna beda L/P + tombol BATAL/SIMPAN/HAPUS + sortir kamar + hapus AutoAssign FAB

Kandidat TAHAP 25
D2 Manifest Real-time — auto-populate jamaah dari WP (mapping jamaah_id → manifest)

D1 Planning Trip — Gantt chart timeline pre-departure

D4 Visa Tracking — status per jamaah (submitted/approved/ready)

D5 Dokumen Tersentral — paspor, KTP, KK, vaksin di Firebase Storage

Drag & drop editor untuk auto-assign manual adjustment

PDF Export Manifest — selain room list
