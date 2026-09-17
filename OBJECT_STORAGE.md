# Object Storage MinIO untuk backend Vercel

APK tetap memakai URL backend Vercel yang sama. Laporan baru masuk ke Object Storage, sedangkan draf lama dengan ID Google Drive tetap diteruskan ke Google Drive.

## Environment Variables Vercel

Buka **Vercel > Project DESIL > Settings > Environment Variables**, lalu tambahkan untuk lingkungan **Production**:

```text
STORAGE_DRIVER=s3
S3_ENDPOINT=https://s3.natanet.my.id
S3_REGION=ap-southeast-3a
S3_BUCKET=takara
S3_ACCESS_KEY_ID=<isi langsung di Vercel>
S3_SECRET_ACCESS_KEY=<isi langsung di Vercel>
S3_FORCE_PATH_STYLE=true
```

Jangan masukkan Console Username, Console Password, Access Key, atau Secret Key ke source code, GitHub, APK, maupun chat. Console Username dan Console Password tidak dipakai oleh backend.

Pertahankan variabel Google berikut selama masih ada draf lama yang memakai Drive:

```text
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
GOOGLE_REFRESH_TOKEN
GOOGLE_ROOT_FOLDER_ID
PETUGAS_JSON
```

`PETUGAS_JSON` tetap dipakai untuk memeriksa kode akses pada kedua jenis penyimpanan.

## Struktur object

Setiap laporan disimpan secara privat dengan struktur tetap:

```text
laporan/<Nama KK>/<reportId>/manifest.json
laporan/<Nama KK>/<reportId>/Depan_Rumah.jpg
laporan/<Nama KK>/<reportId>/Dalam_Rumah.jpg
laporan/<Nama KK>/<reportId>/Samping_Kiri.jpg
laporan/<Nama KK>/<reportId>/Samping_Kanan.jpg
laporan/<Nama KK>/<reportId>/Belakang.jpg
laporan/<Nama KK>/<reportId>/KTP.jpg
laporan/<Nama KK>/<reportId>/KK.jpg
laporan/<Nama KK>/<reportId>/IDPEL_Listrik.jpg
```

Nama KK dibersihkan dari karakter garis miring dan karakter kontrol sebelum dijadikan folder. `reportId` tetap dipakai sebagai subfolder agar dua keluarga dengan nama yang sama tidak saling menimpa. Draf Object Storage lama dengan struktur `laporan/<reportId>/` tetap dapat dilanjutkan.

Tiga lampiran terakhir hanya dibuat bila pengguna memotretnya. Bucket sebaiknya tetap **private**. Backend memakai operasi S3 `PutObject`, `GetObject`, dan `HeadObject`, sehingga access key perlu izin read/write object pada bucket `takara`.

## Mengaktifkan dengan aman

1. Tambahkan seluruh variabel S3 di Vercel tanpa menghapus variabel Google.
2. Redeploy commit backend terbaru.
3. Buka `https://desil-eight.vercel.app/api`. Respons harus menampilkan `"version":2` dan `"storage":"s3"`.
4. Kirim satu laporan uji dari aplikasi. Pastikan folder `laporan/<Nama KK>/<ID laporan>/` berisi `manifest.json` dan lima foto wajib.
5. Bila gagal, ubah `STORAGE_DRIVER=drive` lalu redeploy untuk mengembalikan laporan baru ke Drive. Draf yang sudah memperoleh ID `obj_` harus dilanjutkan setelah konfigurasi S3 diperbaiki.

Untuk pindah operator S3-compatible di kemudian hari, ubah `S3_ENDPOINT`, `S3_REGION`, `S3_BUCKET`, access key, secret key, dan bila perlu `S3_FORCE_PATH_STYLE`. APK tidak perlu dibangun ulang. Object lama tidak otomatis dipindahkan ke operator baru.
