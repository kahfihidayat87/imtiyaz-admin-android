cd /c/Users/DELL/Documents/GitHub/imtiyaz-admin-android/

cat > HANDOFF.md << 'EOF'
# 🎯 HANDOFF — Imtiyaz Admin Android

**Update terakhir:** 25 Sep 2026
**Repo:** https://github.com/kahfihidayat87/imtiyaz-admin-android
**Local:** `C:\Users\DELL\Documents\GitHub\imtiyaz-admin-android`

---

## 🎯 Status Saat Ini

| Item | Nilai |
|---|---|
| Versi aktif | (belum bump dari 1.0.0) |
| Commit terakhir | (lihat `git log --oneline -3`) |
| Package | `com.imtiyaztour.admin` |
| Warna tema | Biru `#1E3A8A` (`AdminPrimary`) |
| Distribusi | Private APK (share langsung) |

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
