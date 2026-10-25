# Build APK Debug lewat GitHub Desktop pada Windows

Anda tidak perlu membuka Android Studio atau mengetik Gradle secara manual.

1. Buka GitHub Desktop, pilih repository `my-smartexam-browser-android`, lalu klik **Repository > Show in Explorer**.
2. Buka folder ZIP proyek v0.3.0 yang sudah diekstrak dan salin semua isinya ke folder repository lokal. Pilih Replace jika Windows menanyakannya. Jangan hapus folder tersembunyi `.git` pada repository.
3. Kembali ke GitHub Desktop. Pastikan branch `main`. Periksa perubahan file di tab Changes.
4. Pada bagian Summary tulis `Upgrade My SmartExam v0.3.0` lalu klik **Commit to main** dan **Push origin**.
5. Buka GitHub web repo > Actions > `Build My SmartExam APK`. Pilih hasil terbaru. Jika tampil Hijau, unduh artifact `My-SmartExam-Browser-v0.3.0-cloud-debug-apk`.
6. Ekstrak artifact, transfer `app-debug.apk` ke HP Android, izinkan instalasi aplikasi dari sumber tersebut, lalu pasang.

Penting: package Debug berbeda (`.v03test`) dan dapat dipasang berdampingan dengan APK lama. Jangan hapus APK lama ketika ujian atau jawaban belum tuntas.

Jika Actions merah, buka run > job `apk` > step yang gagal. Kirim screenshot log bagian ERROR, bukan password, token, atau nama siswa.
