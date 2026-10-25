# Kontrak APK v0.3.0 terhadap My SmartExam Cloud v0.4.1

## Endpoint siswa tanpa admin key

| Metode | Endpoint | Data |
|---|---|---|
| GET | `/api/exam/public/info?token=KM3P5` | `data.exam`, `data.participants[]` dengan `id`, `name`, `status` |
| POST | `/api/exam/public/join` | `{ "token": "KM3P5", "studentId": "ID_DARI_ROSTER" }` |
| GET | `/api/exam/public/attempt/:id` | Header `Authorization: Bearer <accessKey>` pada HTTPS |
| POST | `/api/exam/public/attempt/:id/answer` | `{ "key": "...", "questionId": "...", "answer": "..." }` |
| POST | `/api/exam/public/attempt/:id/submit` | `{ "key": "..." }` |

Semua hasil sukses memiliki envelope `{ "data": ... }`. Gagal memiliki `{ "error": "..." }`.

Server mengembalikan `409` jika nama peserta sedang digunakan. Data roster yang tampil sebagai `ACTIVE` juga dinonaktifkan pada alur masuk di perangkat, tetapi pemeriksaan server tetap menentukan agar perlindungan tidak bergantung pada tampilan.

**Perhatian:** Data peserta hanya boleh tampil pada paket ujian OPEN yang kode ujiannya diketahui. Kode siswa pribadi tidak diperlukan untuk paket ROSTER. Jangan mengirim EXAM_ADMIN_KEY atau token guru melalui APK.

## Integrasi desktop

- Publikasi guru mengirim paket `loginMode: ROSTER` (My SmartClass rc6.19.17).
- Guru mengelola paket ujian dan status peserta melalui desktop.
- Jawaban dari Cloudflare D1 diimpor ke desktop untuk koreksi, baru desktop mengonfirmasi sinkron selesai.
- Cleanup 72 jam berjalan di Worker setelah ujian tutup dan hasil aman.

## Lanjutan QA

Bangun APK di GitHub Actions, uji pada Android 9, lalu uji dua perangkat dan offline. Jangan memasang di seluruh HP siswa sebelum QA selesai.
