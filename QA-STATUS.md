# QA Status v0.1.0

Dilakukan pada sumber kode Android, bukan pada perangkat fisik.

1. **PASS**: Pemeriksaan statis kontrak lima endpoint Ujian LAN My SmartClass RC6.19.14 terhadap sumber `src/app/api/exam/public/...`.
2. **PASS**: `ExamRules.kt` dan `ExamRulesTest.kt` dikompilasi dengan `kotlinc` yang tersedia dan dieksekusi dengan Java.
3. **PASS**: Pengujian pemilihan server LAN/online, penolakan IP publik untuk LAN, parsing tautan kode, normalisasi token, daftar soal belum terjawab, format waktu, dan pilihan A-D.
4. **PASS**: Pemeriksaan sumber bahwa aplikasi menghormati percobaan ulang dari server dan tidak mengacak sendiri urutan soal per attempt.
5. **PENDING**: `gradle :app:assembleDebug`, karena Android SDK dan Gradle tidak tersedia di lingkungan penulisan sumber.
6. **PENDING**: Pengujian konektivitas My SmartClass yang aktif dan APK pada perangkat Android nyata.
7. **PENDING**: Pengujian sinkronisasi dan ujian serentak pada 30 siswa. Backend online belum dibuat.

Tidak ada kredensial sekolah atau data pribadi siswa di paket ini.
