# QA Status, My SmartExam Browser v0.3.0

**Revision Candidate, belum siap rilis produksi.**

LULUS:
- Kotlin ExamRulesTest melalui compiler kotlinc: validasi LAN, URL Cloud tetap, format waktu, jawaban kosong, dan kode ujian 5 karakter.
- Tes otomatis `worker-central.test.mjs` Cloud v0.4.1, dengan migrasi 0001 sampai 0003 yang digunakan server, termasuk daftar siswa, lock sesi, remedial dan sinkron.
- Validasi struktur ZIP dan keberadaan ikon dari baseline My SmartClass.

BELUM TERUJI:
- Kompilasi Android Gradle `:app:assembleDebug` oleh GitHub Actions.
- Instal dan tes fisik Android 9, Android 13 dan terbaru.
- Uji Online dari kode `KM3P5` ke daftar 28 peserta pada server milik pengguna (tidak ada panggilan ke server produksi dalam pengembangan ini).
- Uji pengembalian jawaban dan tarik hasil ke My SmartClass Desktop.
- Uji pembatasan overlay, kehilangan jaringan, reboot, dan kembalinya sesi setelah Android membunuh proses.

CATATAN:
- Mode Online memerlukan Cloud v0.4.1 dengan penerbitan `ROSTER`, bukan format lama `PRIVATE_CODE`.
- Paket ini bukan file APK yang sudah ditandatangani. GitHub Actions akan membangun APK Debug.
