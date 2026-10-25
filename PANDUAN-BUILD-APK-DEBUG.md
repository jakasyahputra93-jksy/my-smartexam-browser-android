# Panduan Build My SmartExam Browser v0.3.0

Proyek ini menggunakan GitHub Actions. Bacalah file `BUILD-README-WINDOWS.md` untuk langkah lengkap mengganti isi repo melalui GitHub Desktop, Commit, Push, lalu mengunduh Artifact APK Debug.

File utama yang akan dibangun:
- `app/src/main/java/id/mysmartclass/smartexam/MainActivity.kt`
- `.github/workflows/build-android-apk.yml`
- `app/build.gradle.kts`

Versi Debug memakai identitas aplikasi `id.mysmartclass.smartexam.v03test` dan memakai ikon resmi dari file `mysmartclass_logo.png`. **APK Debug belum dibuat di paket ini.** Setelah GitHub Actions berhasil, unduh artifact `My-SmartExam-Browser-v0.3.0-cloud-debug-apk`.

Mode Online langsung memakai `https://exam.mysmartclass.my.id` dengan kode ujian bersama dan daftar nama. Mode LAN tetap memakai alamat IP privat server guru.

Untuk menjaga data peserta, uji memakai paket latihan sebelum ujian sungguhan dan jangan menghapus APK lama bila masih ada jawaban tertunda.
