# Kontrak API dan Roadmap Ujian Hybrid

## 1. Backend LAN My SmartClass rc.6.19.14

Endpoint yang sudah tersedia:

| Operasi | Metode | Endpoint | Balasan utama |
| --- | --- | --- | --- |
| Info ujian | GET | `/api/exam/public/info?token=KODE` | `{data:{exam,participants}}` |
| Masuk | POST | `/api/exam/public/join` | `{data:{attempt,school,student,exam,questions,answers,remainingSeconds}}` |
| Pulihkan sesi | GET | `/api/exam/public/attempt/{id}?key=KUNCI` | Payload attempt yang sama |
| Simpan jawaban | POST | `/api/exam/public/attempt/{id}/answer` | `{data:{savedAt}}` |
| Kumpulkan | POST | `/api/exam/public/attempt/{id}/submit` | `{data:{status}}` |

Payload join:

```json
{"token":"ABCD12","studentCode":"Nama Siswa","studentId":"ID_SISWA"}
```

Payload answer:

```json
{"key":"SESSION_ACCESS_KEY","questionId":"ID_SOAL","answer":"A"}
```

Objek `attempt` mencakup `id,accessKey,status,startedAt,expiresAt,attemptNumber`.
Objek `questions[]` mencakup `id,type,prompt,options,position,points`.
`type` = `MULTIPLE_CHOICE` atau `ESSAY`.
`answers` = peta `questionId -> jawaban`.

**Kebijakan:** Server menentukan urutan soal secara stabil untuk attempt yang sama melalui shuffle yang menggunakan attemptId. Siswa yang sudah menyelesaikan percobaan dan mengulang mendapat attempt baru. APK **tidak** melakukan shuffle lokal agar kunci jawaban dan urutan soal tetap konsisten.

## 2. Batasan API versi sekarang

Server LAN memuat `participants` berupa daftar nama siswa, NIS/NISN. Permintaan join dapat menggunakan `studentId` dan nama tanpa autentikasi tambahan. Perilaku ini **tidak cocok untuk internet publik**. Sebelum ONLINE dirilis, API cloud harus menggantinya dengan identitas siswa dan PIN unik serta hak akses sekolah yang benar.

`saveLanAnswer` hanya menerima sesi ACTIVE dan menolak jawaban setelah kedaluwarsa. Offline queue APK hanya boleh menjanjikan *best effort*. Kegiatan pemulihan setelah waktu habis memerlukan kebijakan server yang disetujui guru.

## 3. Desain integrasi berikutnya (belum diimplementasi)

My SmartClass Desktop tetap mengelola bank soal, paket ujian, remedial, nilai, dan urutan percobaan. Gunakan mode publikasi `LAN`, `ONLINE`, `HYBRID` di modul yang sama.

Skema objek anjuran:

- `exam_publications`: `id, exam_id, revision, mode, publish_state, created_at`
- `cloud_exam_sessions`: `session_id, publication_id, student_id, attempt_id, status, started_at, expires_at`
- `cloud_exam_answers`: `attempt_id, question_id, answer, client_revision, saved_at`
- `exam_sync_outbox`: `event_id, attempt_id, entity_type, payload_digest, sync_state`
- `exam_security_events`: `id, attempt_id, type, occurred_at, received_at, source`

Penggabungan data dilakukan di server dengan kunci unik `attemptId` dan `questionId`. Publikasi harus membekukan versi soal agar koreksi setelah penerbitan tidak mengubah attempt berjalan. Saat mode HYBRID, alokasikan satu attempt aktif ke tepat satu jalur (LAN atau cloud) sampai ada protokol handoff aman. Catat perpindahan jalur, cegah duplikasi, dan serahkan konflik ke guru bila dua sisi tidak sinkron.

Tambahkan endpoint otentikasi peserta berbasis PIN / one-time enrollment, endpoint monitoring, dan idempotency key untuk jawaban/kumpul. Worker Cloudflare hanya menyimpan kredensial administrasi melalui secrets di sisi server, bukan di APK.

## 4. Roadmap

- **v0.1.0**: Klien Android native untuk Ujian LAN existing. Penyiapan menu mode online namun belum aktif.
- **v0.2.0**: Backend online + aktivasi HTTPS + PIN siswa dan sinkronisasi hasil ke My SmartClass.
- **v0.3.0**: Mode Hybrid serentak, monitoring guru, kebijakan pindah jalur dan pemulihan gagal jaringan.
- **v1.0.0**: Uji perangkat nyata, audit izin Android, hardening key/session, stabilisasi 30+ peserta, APK release ditandatangani secara konsisten.
