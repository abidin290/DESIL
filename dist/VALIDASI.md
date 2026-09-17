# Validasi v2.10.0 - Object Storage S3/MinIO

- APK: dokumentasi-rumah-pusat-debug.apk, versionCode 18 / versionName 2.10.0, 4.472.396 byte.
- SHA-256: 43BC8A4E8D10B1C063FD81866BE4D3E98B315DFEB71A4C470A377DCB5721B3F1.
- Backend Vercel versi 2 telah dikonfirmasi aktif dengan `storage: s3` oleh admin.
- APK tetap memakai endpoint Vercel yang sama; perpindahan operator S3-compatible berikutnya tidak memerlukan perubahan APK.
- Tulisan UI `Drive pusat` diganti menjadi `penyimpanan pusat` agar sesuai dengan Object Storage dan draf Google Drive lama.
- `update.json` diselaraskan ke versionCode 18 / versionName 2.10.0.
- Gradle `testDebugUnitTest`, `lintDebug`, dan `assembleDebug` LULUS.
- `apksigner verify` LULUS menggunakan APK Signature Scheme v2.

## Batas v2.10.0

Build dan protokol backend sudah diperiksa. Upload foto nyata ke bucket `takara` tetap perlu diuji dari satu HP sebelum dipakai serentak oleh petugas.

---

# Validasi v2.6.2 - KK tanpa bingkai dan tombol ambil bulat samping

- APK: dokumentasi-rumah-pusat-debug.apk, versionCode 12 / versionName 2.6.2, 4.456.012 byte.
- SHA-256: 62433AF541553B2EB1C499FC606DCCFDA0451404A97A1E125289705F5E894D3C.
- Kamera KK tetap landscape, tetapi bingkai KK dihapus. Tampilan hanya memberi panduan singkat `Foto KK penuh dan jelas`.
- Foto KK tidak lagi di-crop otomatis setelah diambil.
- Kamera KTP tetap landscape, tetap memakai bingkai rasio asli KTP, dan tetap di-crop otomatis sesuai bingkai.
- Tombol ambil foto untuk KTP/KK diganti menjadi tombol bulat di sisi kanan layar agar lebih mudah ditekan saat HP landscape.
- Build assembleDebug LULUS.
- Unit test JVM LULUS.
- apksigner verify LULUS, v2 signature valid.

## Batas v2.6.2

Perubahan layout kamera sudah lulus build dan unit test. Posisi tombol bulat kanan dan kenyamanan ambil foto tetap perlu dicek di HP fisik.

---

# Validasi v2.6.1 - Perbaikan ukuran bingkai KTP dan KK

- APK: dokumentasi-rumah-pusat-debug.apk, versionCode 11 / versionName 2.6.1, 4.456.012 byte.
- SHA-256: C7B6E12146E93AFF9FBDDC3553579C1234700BEFBF05C8750803AF4089310DB4.
- Bingkai KTP dibuat mengikuti rasio fisik KTP 85,60 x 53,98 mm, dengan ukuran lebih proporsional seperti kartu asli.
- Bingkai KK dibuat jauh lebih besar, hampir memenuhi layar landscape, dengan rasio kertas landscape agar lembar KK lebih mudah disejajarkan.
- Crop otomatis KTP/KK tetap memakai posisi bingkai overlay yang sama.
- Build assembleDebug LULUS.
- Unit test JVM LULUS.
- apksigner verify LULUS, v2 signature valid.

## Batas v2.6.1

Perubahan ini sudah lulus build dan unit test. Posisi nyaman bingkai kamera tetap perlu dicek di HP fisik karena ukuran layar dan preview kamera setiap perangkat bisa sedikit berbeda.

---

# Validasi v2.6 - KTP/KK landscape, crop bingkai, dan URL tertanam

- APK: dokumentasi-rumah-pusat-debug.apk, versionCode 10 / versionName 2.6, 4.456.020 byte.
- SHA-256: 9657C33B69E0FA0BE72424BCC5261DA3F1937C9D77E13B8DA9E787EB020DDEA1.
- Web App URL tertanam di APK: `https://script.google.com/macros/s/AKfycby0jklBdmBe2FATZPx2qd0Kv-N1Mu4yCdro7EHQaqN3N2QfFHCm1Agy2m55jq7OTdkd/exec`. UI Pengaturan hanya meminta kode akses petugas.
- KTP dan KK memakai CameraX landscape. Setelah foto diambil, file otomatis di-crop mengikuti bingkai overlay sebelum kembali ke form.
- Build assembleDebug LULUS.
- Unit test JVM: 7 test, 0 gagal.
- Lint LULUS: 0 error, 33 warning.
- APK instrumentasi assembleDebugAndroidTest LULUS.
- Uji instrumentasi Android API 34 LULUS untuk kompresi JPEG, watermark GPS/nama rumah, batas ukuran, cache, draf, dan riwayat.
- Uji server node server/test.cjs LULUS.
- APK berhasil dipasang di emulator sebagai versionCode 10 / versionName 2.6. MainActivity terbuka, logcat tidak menunjukkan FATAL EXCEPTION dari aplikasi, dan apksigner verify LULUS.

## Batas v2.6

Crop KTP/KK dihitung dari posisi bingkai pada PreviewView CameraX mode FILL_CENTER. Uji visual CameraX tetap perlu dilakukan di HP fisik agar posisi crop sesuai dengan kamera perangkat lapangan. Emulator headless masih menampilkan ANR System UI lama, tetapi proses aplikasi tidak mencatat fatal crash.

---

# Validasi v2.5.2 - Template KTP mengikuti contoh

- APK: dokumentasi-rumah-pusat-debug.apk, versionCode 9 / versionName 2.5.2, 4.456.040 byte.
- SHA-256: 93C23E4EB86874CE1ED1558356F369B26B4DAAF0905E9456EB1EE252D6E37C98.
- Template KTP CameraX diubah mengikuti contoh: bingkai luar kartu, kotak NIK horizontal di bagian atas tengah-kiri, teks `NIK` di tengah kotak, dan kotak foto tegak di sisi kanan.
- Build assembleDebug LULUS.
- Unit test JVM: 7 test, 0 gagal.
- Lint LULUS: 0 error, 33 warning.
- apksigner verify LULUS, v2 signature valid.

## Batas v2.5.2

Smoke test visual CameraX belum berhasil di emulator headless karena Activity dokumen tidak exported dan UiAutomator emulator sedang gagal memberi root node. Perubahan overlay berada pada `DocumentOverlayView` dan sudah lulus kompilasi/lint; tampilan akhir tetap perlu dicek di HP fisik.

---

# Validasi v2.5.1 - Perbaikan crash saat aplikasi dibuka

- APK: dokumentasi-rumah-pusat-debug.apk, versionCode 8 / versionName 2.5.1, 4.456.040 byte.
- SHA-256: 955B55196EA02F8C59C47EDCECE1E850E2D97C7A70CAF363A42C7C821644B4FE.
- Penyebab crash v2.5: array cache thumbnail masih berukuran 5, sementara form sudah berisi 8 item foto. Saat refresh mencapai KTP, aplikasi keluar batas array.
- Perbaikan: cache thumbnail sekarang mengikuti `FormRules.LABELS.length`, sehingga lima foto rumah dan tiga lampiran opsional aman direfresh.
- Build assembleDebug LULUS.
- Unit test JVM: 7 test, 0 gagal.
- Lint LULUS: 0 error, 33 warning.
- Uji server node server/test.cjs LULUS.
- APK berhasil dipasang di emulator sebagai versionCode 8 / versionName 2.5.1. MainActivity terbuka dan logcat tidak menunjukkan FATAL EXCEPTION atau ArrayIndexOutOfBoundsException dari aplikasi. Logcat mencatat `Displayed id.desa.dokumentasirumah/.MainActivity`.
- apksigner verify LULUS, v2 signature valid.

## Batas v2.5.1

Emulator headless masih menampilkan ANR System UI yang sudah pernah muncul pada pengujian sebelumnya, tetapi proses aplikasi berhasil menampilkan MainActivity dan tidak mencatat fatal crash. Kamera CameraX tetap perlu diuji pada HP fisik.

---

# Validasi v2.5 - CameraX KTP, KK, dan IDPEL Listrik opsional

- APK: dokumentasi-rumah-pusat-debug.apk, versionCode 7 / versionName 2.5, 4.493.808 byte.
- SHA-256: B02282CCD7CE0011962DE829AC7AC672593658F783077BBBCC5E0CE2876516A1.
- Build assembleDebug LULUS.
- Unit test JVM: 7 test, 0 gagal. FormRules menerima tiga lampiran opsional tanpa mengubah syarat lima foto rumah.
- Lint LULUS: 0 error, 33 warning. Warning bertambah karena dependency CameraX/AndroidX dan orientasi portrait kamera dokumen.
- Uji server node server/test.cjs LULUS. Server sekarang melaporkan version 4, reserve mengalokasikan 9 ID, menerima slot KTP/KK/IDPEL, dan complete tetap hanya mewajibkan lima foto rumah.
- Uji instrumentasi Android API 34 LULUS untuk kompresi JPEG, watermark GPS/nama rumah, batas ukuran, cache, draf, dan riwayat.
- apksigner verify LULUS, v2 signature valid.

## Batas v2.5

KTP, KK, dan IDPEL Listrik bersifat opsional. Fitur ini memakai CameraX, menambah izin CAMERA, dan menaikkan ukuran APK dari sekitar 2,3 MB menjadi sekitar 4,5 MB. Apps Script harus diperbarui ke Code.gs terbaru dan di-deploy sebagai versi baru agar lampiran opsional dapat diterima server. URL, ROOT_FOLDER_ID, dan PETUGAS_JSON tetap sama. Emulator headless sempat menampilkan ANR System UI saat smoke test visual; uji kamera CameraX dan pembacaan template perlu dilakukan di HP fisik.

---

# Validasi v2.4 - Nama KK dan Desa Tombulang pada foto depan

- APK: dokumentasi-rumah-pusat-debug.apk, versionCode 6 / versionName 2.4, 2.324.534 byte.
- SHA-256: 502F3E6C69ACA1451ACAB35CF91955E0693C9421C914CB070D93DF361952EE61.
- Build assembleDebug LULUS.
- Unit test JVM: 7 test, 0 gagal.
- Lint LULUS: 0 error, 19 warning yang sama seperti sebelumnya.
- APK instrumentasi assembleDebugAndroidTest LULUS.
- Uji instrumentasi Android API 34 LULUS: kompresi JPEG nyata, watermark GPS, watermark foto depan dengan `Rumah Ahmat Sekian` dan `Desa Tombulang`, batas ukuran/dimensi, rasio, draf tidak berubah, cache digunakan ulang, ambil ulang mengganti cache, foto potret kecil tidak diperbesar, input rusak ditolak, cache dibersihkan, riwayat tersimpan, tidak duplikat, sukses tidak ditimpa status lama.
- Sampel sintetis 2400x1800: 4.389.310 -> 510.416 byte, 2.936 ms pada emulator. Angka ini bukan benchmark HP atau bukti mutu detail foto rumah.
- apksigner verify LULUS, v2 signature valid.

## Batas v2.4

Identitas `Rumah <Nama KK>` dan `Desa Tombulang` hanya ditempel pada foto Depan Rumah saat upload laporan baru. Empat foto lain tetap hanya memakai koordinat bila GPS tersedia. Apps Script tidak berubah. Uji lapangan tetap perlu untuk memastikan hasil tulisan terbaca pada foto rumah asli dan GPS HP sudah akurat saat memotret.

---

# Validasi v2.3 - Koordinat pada foto upload

- APK: dokumentasi-rumah-pusat-debug.apk, versionCode 5 / versionName 2.3, 2.324.186 byte.
- SHA-256: 565B29F9FF6E240E49523207AAF11E4200CD77FAEC68359B8A68AEBF88DC527B.
- Build assembleDebug LULUS.
- Unit test JVM: 7 test, 0 gagal.
- Lint LULUS: 0 error, 19 warning yang sama seperti sebelumnya.
- APK instrumentasi assembleDebugAndroidTest LULUS.
- Uji instrumentasi Android API 34 LULUS: kompresi JPEG nyata, watermark GPS pada salinan upload, batas ukuran/dimensi, rasio, draf tidak berubah, cache digunakan ulang, ambil ulang mengganti cache, foto potret kecil tidak diperbesar, input rusak ditolak, cache dibersihkan, riwayat tersimpan, tidak duplikat, sukses tidak ditimpa status lama.
- Sampel sintetis 2400x1800: 4.389.310 -> 510.416 byte, 780 ms pada emulator. Angka ini bukan benchmark HP atau bukti mutu detail foto rumah.
- apksigner verify LULUS, v2 signature valid.
- APK terpasang di emulator sebagai versionCode 5 / versionName 2.3 dan MainActivity terbuka. Emulator masih sempat menampilkan ANR System UI seperti pengujian sebelumnya; fokus aplikasi tetap tercatat pada id.desa.dokumentasirumah/.MainActivity.

## Batas v2.3

Fitur koordinat memakai LocationManager dan Canvas bawaan Android; tidak ada library baru. Manifest menambah izin ACCESS_FINE_LOCATION dan ACCESS_COARSE_LOCATION. Apps Script tidak berubah. Uji watermark dilakukan pada encoder Android/emulator; uji lapangan tetap perlu untuk memastikan GPS HP mendapat lokasi akurat saat petugas memotret.

---

# Validasi v2.2 - UI, riwayat lokal, dan retry

- APK: dokumentasi-rumah-pusat-debug.apk, versionCode 4 / versionName 2.2, 2.350.356 byte.
- SHA-256: C331FB7D92784FA9600835F3105A8ACFD01013A6DE250D6B65E4D70BCB55EC85.
- Build final dan lint LULUS: 0 error, 19 warning (terutama lokalisasi/versi dependency).
- Unit test JVM: 7 test, 0 gagal. Termasuk retry sementara, batas percobaan, jeda, interupsi, request identik, penolakan kode tanpa retry, validasi URL dan formulir.
- Uji server node server/test.cjs LULUS, termasuk status BELUM LENGKAP -> SELESAI, metadata petugas/waktu tetap ada, retry tidak menambah baris status atau mengganti waktu selesai. Mock mengikuti fields yang diminta saat membuat folder.
- Uji instrumentasi Android API 34 LULUS: kompresi JPEG nyata, ukuran/dimensi/rasio, draf tidak berubah, cache digunakan ulang, ambil ulang mengganti cache, foto potret kecil tidak diperbesar, input rusak ditolak, cache dibersihkan, riwayat tersimpan, tidak duplikat, sukses tidak ditimpa status lama. Log: testing/v22-instrumentation.txt.
- Sampel sintetis 2400x1800: 4.389.310 -> 510.416 byte, 1.413 ms pada emulator. Angka ini bukan benchmark HP atau bukti mutu detail foto rumah.
- APK final berhasil dipasang sebagai update, Activity terbuka, dan apksigner verify LULUS.
- UI emulator: layar utama ringkas, tombol Upload tetap di bawah pada 1080x2400 dan 720x1280, form dapat digulir, preview + memperbesar gambar, preview dapat ditutup, Riwayat menampilkan draf aktif. Screenshot ada di testing/v22-*.png. Preview diuji dengan gambar sintetis berlabel FOTO UJI PREVIEW, bukan foto laporan sungguhan.
- System UI emulator sempat ANR saat boot; setelah ditutup pengujian aplikasi berjalan. Pengujian encoder dan riwayat selesai sukses.

## Batas dan pemasangan

Tidak ada rekap admin/Google Sheets. Riwayat hanya pada HP ini, maksimal 200 laporan; laporan lama tidak ditarik dari Drive. Status folder baru memerlukan pembaruan Code.gs pada deployment yang sudah dipakai, tanpa mengganti URL atau Script Properties. APK tetap kompatibel dengan server lama. Script terbaru belum di-deploy atau diuji terhadap Drive pengguna dalam sesi ini; server diuji dengan mock. Perlu uji lapangan untuk kamera HP, pinch/geser multitouch fisik, dan upload dengan beberapa petugas.

---

## Catatan versi sebelumnya (historis)

# Validasi v2.1 - Kompresi Standar otomatis

- APK terbaru: `dokumentasi-rumah-pusat-debug.apk`, versionCode 3 / versionName 2.1, 2.312.922 byte.
- Build `assembleDebug` dan APK uji `assembleDebugAndroidTest`: LULUS.
- Unit test JVM: LULUS. Lint: 0 error, 19 warning.
- `apksigner verify`: LULUS; kunci debug sama agar dapat dipasang sebagai pembaruan.
- SHA-256: `6AA74C1BBDD7CC7B81397BB1E6B5B2E27FEE25607DA1E6ED55A65A2C25EECBF8`.
- Uji instrumentasi encoder JPEG Android sudah dibuat dan dijalankan, tetapi hasil akhir tidak berhasil diperoleh karena sesi emulator terhenti. Uji tersebut BELUM dinyatakan lulus. Cara menjalankannya tersedia di README.
- Kecepatan kompresi, kualitas detail rumah, dan upload hasil kompresi pada HP fisik masih perlu diuji. Pengguna telah mengonfirmasi versi sebelum kompresi berhasil; konfirmasi itu bukan pengujian fitur kompresi baru.

## Perubahan

Kompresi dilakukan berurutan di executor upload: sisi terpanjang maksimal 1.600 piksel, batas 500 KiB, kualitas JPEG adaptif. Foto sangat rumit dapat diturunkan resolusinya lagi; foto sederhana dapat lebih kecil dari 250 KB. Foto draf tidak ditimpa. Cache disimpan berdasarkan hash foto dan digunakan ulang untuk retry. Draf versi 2.0 yang sudah memiliki ID laporan tetap memakai berkas semula agar checksum server tetap cocok. Tidak diperlukan perubahan Apps Script.

---

## Catatan versi sebelumnya (historis)

# Validasi versi 2 - Drive pusat (8 September 2026)

- APK: dokumentasi-rumah-pusat-debug.apk; versionCode 2 / versionName 2.0; sekitar 2,3 MB.
- Build assembleDebug: LULUS.
- Unit test Android/JVM: 3 test, 0 gagal (form, URL server, respons sukses/error, redirect tak diizinkan, HTML login).
- node server/test.cjs: LULUS. Menguji kode salah/nonaktif, kepemilikan laporan, perubahan metadata, laporan belum lengkap, format JPEG, lima kategori, retry setelah respons hilang, penolakan foto berbeda pada retry, file di sampah, error Drive, dan server terkunci.
- Lint: 0 error, 19 warning. Warning berupa lokalisasi teks, versi dependency/target SDK dan preferensi sinkron.
- Tanda tangan APK: valid; memakai keystore debug yang sama dengan versi 1.
- SHA-256 APK: 7B74D88A874916918BBCB5ECDF93206043975D4E0AE919126DAB8A1EDFB26665.
- Emulator API 34: update APK berhasil (Success), Activity terbuka (Status: ok). Layar utama dan dialog URL/kode akses telah dilihat melalui screenshot; hierarki UI berhasil dibaca. System UI emulator sempat mengalami gangguan saat boot, kemudian dialog aplikasi bisa dibuka.
- Perbaikan terakhir hanya mengganti karakter pemisah label Drive pusat menjadi tanda minus; build dan lint diulang setelah perubahan tersebut.

## Belum diverifikasi

Apps Script belum dipasang di akun Gmail pemilik. Pengujian server menggunakan mock Google Drive, bukan akun Google sungguhan. Otorisasi admin, pembuatan lima kode nyata, upload sungguhan, penyimpanan kode melalui respons server nyata, kamera HP fisik, dan pemakaian lima HP bersamaan masih perlu diuji setelah mengikuti server/PANDUAN.md.

APK lama dokumentasi-rumah-debug.apk adalah versi 1 dengan Drive per pengguna. Gunakan APK berakhiran pusat-debug.apk untuk rancangan terbaru.
