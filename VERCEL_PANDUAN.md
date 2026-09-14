# Backend Vercel untuk Dokumentasi Rumah

Backend ini dipakai oleh APK versi baru. APK lama tetap memakai Google Apps Script dan tidak terpengaruh.

## 1. Hubungkan repository

1. Masuk ke https://vercel.com menggunakan GitHub.
2. Pilih **Add New > Project**.
3. Import repository `abidin290/DESIL`.
4. Framework Preset pilih **Other** dan Root Directory biarkan kosong.

## 2. Environment Variables

Tambahkan variabel berikut di **Settings > Environment Variables** untuk Production. Jangan tulis nilainya di GitHub atau kirim melalui chat.

```text
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
GOOGLE_REFRESH_TOKEN
GOOGLE_ROOT_FOLDER_ID
PETUGAS_JSON
```

`PETUGAS_JSON` berisi daftar hash kode yang sama formatnya dengan Script Property pada Apps Script. Secret OAuth yang pernah dibagikan harus dicabut dan tidak boleh digunakan.

## 3. Deploy dan uji

Tekan **Deploy**. Buka `https://NAMA-PROJECT.vercel.app/api`. Respons sehat:

```json
{"ok":true,"service":"Dokumentasi Rumah Vercel","version":1}
```

Uji upload memakai folder pengujian terlebih dahulu. Setelah berhasil, berikan URL `/api` agar APK versi baru dapat dibuild. Jangan mematikan Apps Script.

Jika Environment Variable baru ditambahkan atau diubah, buka tab **Deployments**, pilih deployment terbaru, lalu **Redeploy**. Perubahan variabel tidak diterapkan ke deployment yang sudah berjalan. `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, dan `GOOGLE_REFRESH_TOKEN` wajib berasal dari OAuth Client Web yang sama.
