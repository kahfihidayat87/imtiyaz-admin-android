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