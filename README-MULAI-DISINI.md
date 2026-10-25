# My SmartExam Browser v0.3.0, Cloud Auto-Connect

Kode sumber Android resmi pendamping My SmartClass Desktop. Berangkat dari baseline My-SmartExam-Android-v0.2.0-GITHUB-READY, dengan layar sambutan, warna dan logo asli desktop, serta login Online tanpa alamat server dan tanpa kode peserta pribadi.

**Status:** Revision Candidate. Belum lulus build Gradle Android / uji HP Android 9 pada paket ini. Gunakan hanya untuk ujian percobaan sampai pengujian seluruh alur selesai.

## Cara build APK Debug lewat GitHub

1. Buka repository `my-smartexam-browser-android` yang telah Anda gunakan sebelumnya.
2. Di GitHub Desktop, pilih Repository > Show in Explorer.
3. Ekstrak isi ZIP ini, lalu **salin seluruh file sumber** ke folder repository, termasuk folder `.github` dan `app`. Pilih Replace ketika diminta. Jangan hapus folder `.git`.
4. Kembali ke GitHub Desktop, isi Summary `My SmartExam Browser v0.3.0 Cloud Auto-Connect`, klik **Commit to main**, kemudian **Push origin**.
5. Buka repository di situs GitHub, pilih tab **Actions**, lalu pilih hasil terbaru dari `Build My SmartExam APK`.
6. Setelah build hijau, unduh artifact bernama `My-SmartExam-Browser-v0.3.0-cloud-debug-apk`, ekstrak, lalu pasang `app-debug.apk` pada HP percobaan.

Versi Debug memakai package suffix `.v03test` agar dapat dipasang **berdampingan** dengan APK uji v0.2.0. Ini mencegah konflik tanda tangan debug GitHub dan menjaga aplikasi lama sebagai cadangan. Jangan menghapus APK lama jika di dalamnya masih ada ujian atau jawaban yang belum tersinkron.

## Alur siswa

1. Saat belum ada sesi aktif, Welcome Screen menampilkan logo My SmartClass asli, identitas aplikasi, kredit pengembang dan tombol **Mulai Ujian**.
2. Siswa memilih **LAN Sekolah** atau **Online**.
3. Mode Online otomatis memakai `https://exam.mysmartclass.my.id`. Tidak ada kolom URL cloud dan tidak ada kode peserta pribadi.
4. Siswa memasukkan kode ujian bersama dari guru, misalnya `KM3P5`, lalu aplikasi meminta daftar peserta kepada Worker.
5. Siswa memilih namanya dari dropdown. Nama dengan sesi aktif ditandai. Server tetap menolak sesi kedua walaupun status di HP belum sempat diperbarui.
6. Android meminta persetujuan Screen Pinning. Aplikasi menampilkan soal, waktu, penyimpanan jawaban, konfirmasi soal kosong, dan submit.
7. Aplikasi yang dibuka kembali mencoba memulihkan sesi lama lebih dahulu, bukan memaksakan Welcome Screen.

## Ketentuan kompatibilitas

- Android minimal versi 9, API 28.
- Mode LAN menggunakan server desktop My SmartClass dengan `http://alamat-ip-sekolah:port` seperti sebelumnya.
- Mode Online memerlukan Worker v0.4.1 atau versi kompatibel dan ujian baru yang diterbitkan dengan `loginMode: ROSTER`.
- Paket online lama yang masih memakai kode peserta pribadi (`PRIVATE_CODE`) tidak otomatis berubah. Gunakan APK lama untuk paket lama atau terbitkan paket baru dari desktop agar muncul dropdown.
- Screen Pinning pada HP pribadi **tidak bisa menjamin blokir semua aplikasi secara absolut**. Pemilik perangkat masih bisa melepas sematan sesuai ketentuan Android. Pengawasan guru tetap diperlukan.
- Tidak ada password guru, API Key Resend, Cloudflare Secret, atau token administrator di APK.

## Periksa kesiapan sebelum ujian sungguhan

Gunakan `CHECKLIST-UJI-ANDROID.md` dan `QA-STATUS.md`. Pertahankan proyek desktop dan server produksi, jangan ubah migrasi D1 atau route Cloudflare untuk memasang APK siswa.
