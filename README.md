# Dokumentasi Rumah - Drive Pusat v2.5

Aplikasi Android untuk sampai 20 petugas dengan kode akses masing-masing. Semua foto masuk ke satu Google Drive milik admin (Gmail biasa), melalui Google Apps Script. Petugas tidak login Google.

APK memeriksa manifest pembaruan dari repository GitHub `abidin290/DESIL`. Buka Pengaturan > Cek pembaruan aplikasi. Jika tersedia versi baru, aplikasi membuka halaman GitHub Releases; Android tetap meminta konfirmasi sebelum memasang APK.

## Berkas

- APK terbaru: [dokumentasi-rumah-pusat-debug.apk](dist/dokumentasi-rumah-pusat-debug.apk).
- **Mulai di sini:** [Panduan aktivasi server dan kode petugas](server/PANDUAN.md).
- Script server: [Code.gs](server/Code.gs) dan [appsscript.json](server/appsscript.json).
- [Hasil validasi](dist/VALIDASI.md).

## Cara memakai

1. Admin memasang Apps Script dan membuat lima kode mengikuti panduan.
2. Pasang APK di HP Android 8.0+. Buka Pengaturan > Kode akses petugas, isi kode petugas, lalu Simpan. URL server sudah tertanam di aplikasi.
3. Ambil foto depan, dalam, samping kiri, samping kanan, dan belakang. Saat pertama kali memotret, izinkan lokasi agar koordinat dapat ditempel pada foto upload. Ketuk foto untuk melihat atau mengambil ulang.
4. Bila tersedia, ambil lampiran opsional: KTP, KK, dan IDPEL Listrik. Lampiran ini memakai kamera CameraX; KTP memiliki bingkai rasio kartu dan panduan area NIK, sedangkan KK difoto penuh tanpa bingkai.
5. Isi nama kepala keluarga lalu upload. Draf tersimpan otomatis dan bisa dilanjutkan jika internet terputus.
6. Setelah lima foto rumah wajib dikonfirmasi server, tampil ID laporan dan form dikosongkan. Admin melihat hasil di Drive pusat.

URL Web App sudah diisi di source. Kode akses petugas tetap dimasukkan dari layar aplikasi. Foto lokal bisa diuji sebelum server dipasang; upload sungguhan baru tersedia setelah deployment admin selesai.

## Pembaruan dari versi 1

Package dan kunci debug tetap sama sehingga APK dapat dipasang sebagai update. Draf foto/nama dipertahankan. ID upload ke Drive pribadi dari versi 1 dilepas agar draf diunggah sebagai laporan baru ke pusat; foto yang telanjur ada di Drive pribadi tetap ada. Login Google dan dependency Google Identity di APK dihapus. Pendaftaran OAuth Android/SHA-1 tidak diperlukan untuk versi 2.

Kode akses disimpan terenkripsi di penyimpanan privat menggunakan Android Keystore; backup/transfer data aplikasi dinonaktifkan. Android tidak menyimpan token/password Drive admin. Server memeriksa petugas, laporan, kategori foto, ukuran JPEG, dan hasil lima upload. Kode dapat dinonaktifkan atau dirotasi oleh admin.

## Lampiran opsional KTP, KK, dan IDPEL Listrik (v2.6.2)

Form sekarang memiliki tiga foto tambahan: `KTP`, `KK`, dan `IDPEL LISTRIK`. Lampiran ini tidak wajib. Tombol Upload tetap mengikuti aturan lama: Nama KK dan lima foto rumah harus lengkap.

Ketiga lampiran memakai CameraX. KTP memakai bingkai rasio kartu seperti contoh: kotak NIK horizontal di bagian atas dan kotak foto tegak di sisi kanan. Setelah difoto, KTP otomatis di-crop mengikuti bingkai. KK tetap landscape tetapi tanpa bingkai dan tanpa crop agar lembar KK bisa difoto penuh. IDPEL Listrik memakai bingkai label/struk yang lebih lebar. File yang dikirim ke Drive bernama `KTP.jpg`, `KK.jpg`, dan `IDPEL_Listrik.jpg` bila tersedia.

Fitur ini menambah library CameraX dan izin `CAMERA`, sehingga ukuran APK naik dari sekitar 2,3 MB menjadi sekitar 4,5 MB. KTP dan KK dibuka dalam mode landscape. Tombol ambil foto KTP/KK berupa bulatan di sisi kanan layar agar lebih mudah ditekan saat HP landscape. Apps Script perlu diperbarui ke `Code.gs` terbaru agar server menerima 8 slot foto. URL Web App sudah tertanam di aplikasi; ROOT_FOLDER_ID dan kode petugas tetap dipertahankan.

## Server tertanam di aplikasi (v2.6)

APK memakai Web App URL tetap:

```text
https://script.google.com/macros/s/AKfycby0jklBdmBe2FATZPx2qd0Kv-N1Mu4yCdro7EHQaqN3N2QfFHCm1Agy2m55jq7OTdkd/exec
```

Petugas hanya mengisi kode akses. Jika URL deployment Apps Script diganti, APK perlu dibuild ulang dengan URL baru.

## Build dan test

Gunakan Android Studio/JDK 17+, SDK 36, Gradle wrapper yang tersedia:

```powershell
node server/test.cjs
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

APK memakai keystore debug lokal `debug.keystore`. Simpan keystore agar pembaruan tetap bisa dipasang. Gunakan signing release sendiri untuk distribusi produksi.

Test server memakai mock layanan Google dan tidak menulis ke akun Google. Uji deployment asli, kamera HP, dan lima petugas bersamaan tetap diperlukan; detailnya di panduan.

## Kompresi Standar otomatis (v2.1)

Saat Upload ditekan, kelima foto disiapkan satu per satu di executor latar belakang. Sisi terpanjang maksimal 1.600 piksel, rasio dipertahankan, dan foto kecil tidak diperbesar. JPEG dimulai pada kualitas 82; kualitas diturunkan sampai 66 bila perlu. Foto yang sangat rumit bisa dikecilkan lagi agar maksimal 500 KiB. Target praktis 250-500 KB; foto sederhana boleh lebih kecil, tidak diperbesar ukuran filenya.

Foto draf untuk preview tidak ditimpa. Salinan upload disimpan dalam penyimpanan privat aplikasi dan dipakai ulang pada retry. Nama cache mengikuti hash isi foto: mengambil ulang foto tidak memakai hasil kompresi foto lama. Cache dihapus setelah sukses, hapus draf, atau pengambilan ulang foto yang berhasil. Status menampilkan Menyiapkan foto 1/5 dan seterusnya sebelum pengiriman.

Upload parsial yang dimulai di versi 2.0 dilanjutkan dengan berkas semula untuk menjaga checksum server. Kompresi baru berlaku pada laporan baru. Jika salinan laporan berjalan hilang, aplikasi menghentikan upload agar tidak mengirim berkas berbeda dengan ID yang sama.

Tidak perlu mengubah Apps Script, URL server atau kode akses. Instal APK terbaru sebagai pembaruan. Waktu kompresi bergantung pada HP; uji kecepatan dan keterbacaan detail rumah di perangkat lapangan tetap diperlukan.

Uji encoder Android yang disertakan (APK instrumentasi) dapat dijalankan dengan:

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w id.desa.dokumentasirumah.test/id.desa.dokumentasirumah.CompressionInstrumentation
```

## UI dan pengiriman v2.2

- Judul/header dipadatkan. Tombol Upload tetap terlihat di bagian bawah, sementara form bisa digulir.
- Riwayat dan Pengaturan tersedia di atas. Kode akses dan Hapus draf berada dalam Pengaturan.
- Ketuk foto untuk preview layar penuh. Cubit, geser, ketuk dua kali, atau gunakan tombol + / - / Pas. Tombol Ambil ulang tersedia selama laporan belum mulai dikirim.
- Tahap progres: menyiapkan lima foto, menyiapkan folder, mengirim 1/5 sampai 5/5, lalu memeriksa laporan.
- Riwayat lokal menyimpan maksimal 200 laporan: nama KK, petugas, waktu HP, status, dan ID yang dapat disalin. Satu ID hanya satu entri. Status Selesai dicatat setelah server mengonfirmasi lengkap, sebelum draf dihapus.
- Draf yang belum pernah dikirim tampil sebagai Draf aktif pada Riwayat. Riwayat lama sebelum versi ini tidak diambil dari Drive. Foto lokal laporan sukses tidak disimpan dalam riwayat.
- Tombol Lanjutkan draf aktif kembali ke form; hanya satu draf foto yang aktif. Entri Draf dihapus bukan antrean yang bisa diunggah ulang karena foto lokalnya sudah dihapus.
- Riwayat bertahan setelah tutup/buka aplikasi, tetapi hilang jika aplikasi dihapus/clear data. Hapus draf tidak menghapus riwayat.
- Upload otomatis mencoba ulang maksimal 2 kali per permintaan, dengan jeda 2 dan 4 detik, hanya untuk timeout/koneksi, HTTP 408/429/5xx, atau respons server yang ditandai dapat dicoba ulang. Jika tetap gagal, draf tersedia untuk Lanjutkan upload. Kode ditolak, URL salah, dan konflik data tidak dicoba berulang.
- Retry tidak mengubah foto atau ID laporan. Kompresi Standar dan kompatibilitas draf lama tetap berlaku.

### Pembaruan server opsional untuk status folder

Ganti Code.gs di proyek Apps Script yang sudah dipakai, lalu Deploy > Manage deployments > Edit > New version. Pertahankan URL, ROOT_FOLDER_ID, dan PETUGAS_JSON. **Jangan jalankan setupPetugas ulang**.

Folder laporan baru/yang dilanjutkan memiliki baris Status: BELUM LENGKAP pada Description dan warna merah muda. Setelah lima foto diverifikasi, status menjadi SELESAI dan warna hijau muda. Nama folder tetap nama KK agar APK lama tetap kompatibel. Waktu penerimaan dan identitas petugas dipertahankan. Tidak ada Google Sheets atau rekap admin.

APK baru tetap dapat dipakai dengan script lama; indikator status folder memerlukan script terbaru. Riwayat HP dan UI baru tidak memerlukan deployment ulang. Status folder merupakan hasil verifikasi saat upload, bukan pemantauan terus-menerus apabila file kemudian diubah secara manual.

## Koordinat dan identitas pada foto upload (v2.4)

Saat foto diambil, aplikasi membaca lokasi terakhir dari GPS/jaringan HP dan menyimpannya bersama draf foto tersebut. Saat Upload ditekan, koordinat ditempel sebagai label pada salinan JPEG yang dikirim ke Drive pusat: latitude, longitude, akurasi, dan waktu lokasi. Khusus foto Depan Rumah, label juga memuat `Rumah <Nama KK>` dan `Desa Tombulang`. Foto draf yang dipakai untuk preview di aplikasi tidak ditimpa.

Contoh label pada foto Depan Rumah:

```text
Rumah Ahmat Sekian
Desa Tombulang
Lat: 1.234567
Lng: 124.567890
Akurasi: 8 m
Waktu: 09-09-2026 14:21
```

Fitur ini memakai API bawaan Android (`LocationManager`, `Canvas`, dan kompresi JPEG yang sudah ada), tanpa library baru. Manifest hanya menambah izin `ACCESS_FINE_LOCATION` dan `ACCESS_COARSE_LOCATION`; ukuran APK naik kecil karena tambahan kode aplikasi saja. Jika izin lokasi ditolak atau HP belum memiliki lokasi terakhir, foto tetap dapat dikirim tetapi caption menampilkan GPS belum tersedia dan salinan upload tidak mendapat label koordinat.

Tidak perlu mengubah Apps Script, URL server, ROOT_FOLDER_ID, atau kode petugas untuk fitur koordinat.
