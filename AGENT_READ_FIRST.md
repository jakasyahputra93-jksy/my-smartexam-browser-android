# AGENT READ FIRST: MY SMARTEXAM BROWSER v0.3.0

Project: APK ujian Android pendamping My SmartClass Desktop. Versi sumber saat ini v0.3.0 Revision Candidate, berasal dari v0.2.0 Android Github-Ready.

## Kesepakatan produk yang wajib dipertahankan

- Server Cloud Online resmi: https://exam.mysmartclass.my.id, URL bawaan tanpa input siswa.
- Siswa masuk hanya dengan kode ujian bersama dan memilih nama melalui daftar peserta dari server, tidak perlu ID atau kode pribadi.
- Server menerapkan satu percobaan ACTIVE per paket dan peserta, konflik tidak boleh mengembalikan accessKey ke HP lain. Pemulihan hanya dengan ID attempt dan accessKey yang tersimpan pada perangkat yang sama. Guru bisa buka kunci peserta dari Desktop.
- Mode LAN tetap bekerja di IP privat sekolah, mendukung ujian lokal dan hybrid.
- Remedial membuat attempt baru tanpa menimpa riwayat; server mengacak urutan soal pada tiap attempt.
- Screen Pinning dengan persetujuan Android adalah best effort di HP milik siswa, bukan kiosk MDM.
- Sambutan, logo dan palet dari My SmartClass Desktop; footer mencantumkan Jaka Syahputra,S.Sos.,Gr. dan kontak yang telah dipublikasikan di footer desktop.
- Minimum Android 9 (API 28).

## Arsitektur terkini

- Desktop: My SmartClass rc6.19.17.1 dan cloud pengguna otomatis.
- Cloudflare Worker: My SmartExam Cloud v0.4.1, endpoint publik `/api/exam/public/info`, `/api/exam/public/join`, `/api/exam/public/attempt/:id`, `/answer`, `/submit`.
- Student mode tidak mengakses endpoint teacher/owner dan tidak membawa secret administrator.
- D1 memegang paket soal sementara, jawaban, attempt. Cron retensi 72 jam setelah ujian tutup dan konfirmasi sinkron desktop.
- Android code native Kotlin dengan programmatic UI; tidak ada dependency Compose atau WebView.

## Status dan keterbatasan

- Kotlin aturan dasar menjalankan tes dengan kotlinc, backend cloud tes Node telah diuji terpisah.
- Android Gradle `assembleDebug`, tes layar fisik Android 9, uji LAN dan Online end-to-end masih wajib di komputer / GitHub Actions dan HP pengguna.
- Debug applicationIdSuffix `.v03test` agar bisa berdampingan dengan versi sebelumnya.

## Prinsip revisi

Jangan mengganti database desktop / Worker tanpa uji kompatibilitas. Pertahankan alur pengembalian hasil ke desktop. Jangan mengklaim PINNING sebagai device-owner kiosk. Jangan memperlihatkan Secret, Password, NISN lengkap, atau token sesi melalui log.
