# Aktivasi Drive pusat (Gmail biasa)

**Sudah memakai aplikasi dengan sukses?** Untuk v2.5 cukup ganti Code.gs pada proyek yang sama lalu Deploy > Manage deployments > Edit > New version. Pertahankan URL, ROOT_FOLDER_ID, dan PETUGAS_JSON. Jangan membuat ulang petugas. Langkah pembuatan proyek di bawah hanya untuk pemasangan pertama.

Lakukan langkah ini menggunakan Gmail pemilik Drive pusat. Tidak perlu mendaftarkan OAuth Android/package/SHA-1 seperti APK versi 1.

## 1. Buat folder dan proyek

1. Buat folder **Dokumentasi Rumah** di Google Drive. Biarkan akses folder **Restricted/Dibatasi**; tidak perlu dibagikan kepada petugas.
2. Salin ID folder dari alamat `https://drive.google.com/drive/folders/ID_FOLDER`.
3. Buka https://script.google.com dan buat **New project**. Beri nama `Dokumentasi Rumah Pusat`.
4. Ganti isi `Code.gs` dengan seluruh isi file [Code.gs](Code.gs) di folder ini.
5. Buka **Project Settings**, aktifkan **Show appsscript.json manifest file in editor**. Ganti isi `appsscript.json` dengan file manifest yang disertakan.
6. Pastikan **Services → Drive API v3** tersedia. Jika memakai proyek Cloud standar sendiri, aktifkan Google Drive API di proyek tersebut juga.
7. Di **Project Settings → Script Properties**, tambahkan `ROOT_FOLDER_ID` dengan nilai ID folder tadi. Jangan menambahkan ID folder dari akun petugas.

## 2. Buat dua puluh kode petugas

1. Pilih fungsi `setupPetugas` dari editor, lalu **Run**. Berikan izin Google menggunakan akun pemilik Drive.
2. Execution log menampilkan 20 kode acak, masing-masing 32 karakter. Simpan dan bagikan satu kode kepada masing-masing petugas melalui jalur pribadi.
3. Script hanya menyimpan hash kode dalam Script Property `PETUGAS_JSON`. Kode asli hanya ditampilkan saat pembuatan. Jangan membagikan execution log ke semua petugas.
4. Nama awal adalah Petugas 1 sampai 5. Anda dapat mengubah field `name` dalam JSON menjadi nama asli; jangan mengubah `id` karena draf dikaitkan dengan ID petugas.

`setupPetugas` menolak berjalan ulang jika daftar petugas sudah ada, supaya tidak memutus akses tanpa sengaja.

### Jika semua kode hilang

Pilih fungsi `resetDanBuat20Petugas`, lalu **Run** dari editor menggunakan akun pemilik Script. Fungsi ini mempertahankan `ROOT_FOLDER_ID`, tetapi mengganti seluruh daftar kode lama. Salin hasil Execution Log dan bagikan setiap kode secara pribadi. Jangan menjalankan fungsi ini jika masih ada petugas yang memakai kode lama.

## 3. Deploy

1. **Deploy → New deployment → Web app**.
2. **Execute as: Me** (Gmail pusat).
3. **Who has access: Anyone**. Pemeriksaan kode dilakukan oleh script pada seluruh aksi yang membaca/mengirim laporan. Pilihan yang mewajibkan login Google akan menghasilkan halaman login, bukan respons yang dibutuhkan APK.
4. Tekan **Deploy** dan salin **Web app URL** yang berakhir `/exec`, bukan `/dev`.
5. Buka URL tersebut di browser. Hasil yang diharapkan: JSON dengan `service: Dokumentasi Rumah Pusat`, `version: 4`. Ini hanya memeriksa endpoint, belum membuktikan izin menulis Drive.

## 4. Hubungkan lima HP

1. Pasang `dist/dokumentasi-rumah-pusat-debug.apk`.
2. Buka **Pengaturan > Kode akses petugas**.
3. Isi kode petugas yang berbeda pada masing-masing HP. URL `/exec` sudah tertanam di APK.
4. Tekan **Simpan**. Aplikasi memeriksa kode melalui server dan menampilkan nama petugas. Kode disimpan terenkripsi menggunakan Android Keystore.
5. Ambil lima foto, isi nama KK, dan upload. Admin memeriksa hasilnya di folder pusat; petugas mendapat ID bukti laporan, tanpa akses langsung ke Drive admin.

Untuk penggunaan berikutnya URL dan kode tidak perlu diketik ulang. URL dapat dibagikan kepada petugas; password/token Gmail admin tidak dibagikan atau dimasukkan ke APK.

## Nonaktifkan/ganti kode

- Menonaktifkan: di `PETUGAS_JSON`, ubah `active` petugas terkait menjadi `false`. Efek berlaku pada permintaan berikutnya, termasuk upload draf.
- Mengganti kode: edit variabel `workerId` dalam fungsi `rotateKodePetugas`, lalu jalankan fungsi itu dari editor. Salin kode baru dari log. ID petugas tetap sama, sehingga draf lama bisa dilanjutkan setelah kode baru dimasukkan.
- Jangan menghapus/mengubah ID petugas untuk rotasi kode. Jangan mengubah folder pusat atau URL saat ada upload parsial. Aplikasi mengunci server dan identitas petugas untuk draf berjalan.
- Perubahan Script Properties berlaku tanpa deploy ulang. Setelah mengubah kode script, gunakan **Deploy → Manage deployments → Edit → New version** agar URL yang sama tetap berlaku.

## Ketahanan upload dan batas penggunaan

- Upload dilakukan satu foto per permintaan, maksimal 4 MiB JPEG/foto. Lima foto tidak dikirim sebagai satu permintaan besar.
- Server membuat file sebagai pemilik Gmail pusat, sehingga kapasitas tersimpan memakai kuota akun pusat.
- ID folder, lima foto wajib, dan tiga lampiran opsional dialokasikan sebelum upload dan disimpan di draf HP. ID yang sama digunakan ketika melanjutkan upload, termasuk jika respons server terputus setelah file dibuat.
- Server mengunci perubahan selama satu permintaan. Jika petugas lain sedang mengunggah, aplikasi dapat menampilkan pesan server sibuk; tekan lanjutkan setelah beberapa saat.
- Nama foto tetap mengikuti PRD. Setiap laporan baru menghasilkan folder tersendiri meskipun nama KK sama. Description folder menyimpan ID laporan, petugas, dan waktu server.
- Foto/draf terkunci setelah pengiriman dimulai. Jangan mengubah/menghapus/memindahkan folder atau file laporan yang masih diunggah. Menghapus draf dari HP tidak menghapus bagian laporan yang sudah ada di Drive.
- Draf hanya dihapus setelah server memeriksa kelima file dan mengonfirmasi lengkap. Upload tidak terus berjalan setelah proses aplikasi dimatikan; buka aplikasi dan lanjutkan.
- Apps Script memiliki kuota/waktu eksekusi. Uji satu laporan sungguhan lalu uji lima HP sebelum penggunaan lapangan; keberhasilan unit test lokal tidak membuktikan kapasitas akun Google.
- URL endpoint bersifat publik dan kode 32 karakter adalah kredensial petugas. Jangan memakai kode pendek seperti `1234` atau menaruh kode dalam source APK. Request tanpa kode valid ditolak sebelum operasi Drive. Permintaan tak sah tetap dapat mengonsumsi kuota eksekusi Apps Script; sistem ini ditujukan untuk kelompok kecil yang terkendali.

## Uji penerimaan admin

1. Kode salah/disabled ditolak tanpa membuat folder.
2. P1 mengirim laporan lengkap; folder berisi lima JPEG wajib dengan nama benar dan description P1.
3. Putus koneksi setelah dua foto lalu lanjutkan: tetap satu folder dan lima file.
4. P2 tidak dapat melanjutkan draf P1; kode baru hasil rotasi untuk P1 tetap bisa melanjutkan.
5. Dua laporan dengan nama KK sama menghasilkan dua folder berbeda.
6. Coba beberapa HP bersamaan; tangani pesan sibuk dengan lanjutkan upload.

Referensi: https://developers.google.com/apps-script/guides/web dan https://developers.google.com/apps-script/guides/content (respons Content Service diarahkan ke `script.googleusercontent.com`).

## APK v2.1: kompresi otomatis

Server tidak perlu di-deploy ulang. Laporan baru dari APK v2.1 mengirim salinan JPEG Standar (maksimal 1.600 piksel dan 500 KiB). Foto diproses satu per satu di HP. Laporan parsial versi lama tetap menggunakan foto semula agar checksum retry cocok.

## Pembaruan server untuk APK v2.2

Untuk akun yang sudah berhasil dipakai, cukup ganti Code.gs dengan versi terbaru lalu Deploy > Manage deployments > Edit > New version. URL /exec tetap sama. Jangan membuat proyek baru, jangan mengubah ROOT_FOLDER_ID/PETUGAS_JSON, dan jangan menjalankan setupPetugas lagi.

Versi endpoint terbaru melaporkan version: 3. Description folder memuat BELUM LENGKAP (warna merah muda), lalu SELESAI (hijau muda) setelah kelima file diperiksa. Nama folder tetap nama KK. Folder lama mendapat status saat laporan dilanjutkan; tidak dilakukan pemindaian/migrasi massal. Tidak ada rekap admin atau Google Sheets.

Server juga mengirim field retryable untuk gangguan sementara yang dikenali. APK v2.2 mencoba ulang maksimal dua kali. Kesalahan izin/kode/data tidak dicoba ulang otomatis. Perubahan hanya pada file kode; manifest dan izin tidak berubah.

## Pembaruan server untuk APK v2.5

APK v2.5 menambah lampiran opsional KTP, KK, dan IDPEL Listrik. Server terbaru melaporkan version: 4, mengalokasikan 9 ID per laporan, dan menerima slot foto 0 sampai 7. Lima slot pertama tetap wajib untuk status SELESAI; tiga slot terakhir hanya disimpan bila dikirim aplikasi.

Untuk akun yang sudah berjalan, ganti Code.gs saja lalu Deploy > Manage deployments > Edit > New version. URL /exec tetap sama. Jangan mengubah ROOT_FOLDER_ID/PETUGAS_JSON dan jangan menjalankan setupPetugas ulang.

## APK v2.6: URL Web App tertanam

APK v2.6 memakai URL Web App tetap yang diberikan admin. Petugas hanya mengisi kode akses di aplikasi. Jika deployment Web App diganti dan URL /exec berubah, APK perlu dibuild ulang dengan URL baru.
