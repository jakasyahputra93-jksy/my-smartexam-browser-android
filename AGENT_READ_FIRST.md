# AGENT READ FIRST: My SmartExam Browser

Tanggal acuan 2026-10-25. Paket ini **source prototype v0.1.0**, bukan rilis produksi dan tidak berisi APK.

## Konteks dan instruksi pengembangan

- Baseline My SmartClass referensi: `My-SmartClass-v1.0.0-rc6.19.14-TYPECHECK-PDFJS-REPAIR-CANDIDATE.zip`.
- Model utama: Ujian Lokal (LAN), Online, dan Hybrid dalam **satu modul Ujian di My SmartClass**. Jangan buat dashboard bank soal terpisah.
- **Fungsi lokal wajib tetap dapat digunakan tanpa internet.** Jangan ubah database desktop lama secara destruktif.
- **Ujian ulang tidak dibatasi** jika guru membuka ujian. Kode server lama sengaja membuat attempt baru untuk siswa yang sudah menyelesaikan percobaan sebelumnya. Jaga riwayat seluruh attempt.
- Saat siswa hanya melanjutkan attempt aktif, pertahankan urutan soal yang berasal dari seededShuffle server. Saat remedial baru, server memakai attemptId baru yang menghasilkan urutan baru.
- Ketika siswa menekan Selesai, tampilkan konfirmasi jumlah/nomor soal kosong, beri pilihan kembali atau kumpulkan.
- PIN pengawas untuk membuka ulang ujian yang terlanjur terkumpul adalah rencana tahap lanjut. Jangan mengubah logika ulang menjadi pembatasan keras tanpa persetujuan guru.
- APK untuk HP pribadi, Android tidak memberikan penguncian absolut tanpa Device Owner. Jangan klaim hal yang tidak dapat dilakukan OS.
- Jangan mengirim file ujian, daftar siswa, kredensial guru, kunci server, atau data pribadi di dalam distribusi APK.
- Jangan mengaktifkan ONLINE sampai backend cloud menjamin autentikasi peserta, transport HTTPS, isolasi tenant sekolah, satu attempt aktif, dan sinkronisasi hasil LAN/cloud.

## Kontrak API yang sudah dipakai APK

- `GET /api/exam/public/info?token=...`
- `POST /api/exam/public/join` dengan `{ token, studentId, studentCode }`
- `GET /api/exam/public/attempt/{id}?key=...`
- `POST /api/exam/public/attempt/{id}/answer` dengan `{ key, questionId, answer }`
- `POST /api/exam/public/attempt/{id}/submit` dengan `{ key }`

Lihat `docs/KONTRAK-API-DAN-ROADMAP.md` untuk kontrak lengkap dan catatan keamanan. Struktur kode Android berada pada `app/src/main/java/id/mysmartclass/smartexam/`.

## Status QA

- Kotlin `ExamRules.kt` dibangun dengan kotlinc lokal dan tes terisolasi lulus.
- Tidak ada Android SDK/Gradle pada lingkungan penciptaan paket ini. APK belum dibangun atau diuji di perangkat Android.
- Workflow GitHub Actions menyediakan jalur build debug APK untuk pengujian.
- Kode desktop baseline sumber tidak diubah. Integrasi Cloudflare D1, login online, dan pemantauan belum dibuat.
