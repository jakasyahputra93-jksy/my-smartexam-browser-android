# MY SMARTEXAM BROWSER v0.1.0
## Prototype Android terhubung dengan My SmartClass

**Status: SOURCE CANDIDATE. Bukan APK siap produksi.**

Aplikasi ini merupakan klien ujian Android **native Kotlin** untuk server Ujian LAN yang sudah tersedia dalam baseline **My SmartClass v1.0.0-rc.6.19.14**. Paket sumber ini tidak mengubah aplikasi desktop atau database My SmartClass.

### Yang sudah dibuat

- Pilihan jalur LAN / Online pada halaman awal. Saat ini **LAN berfungsi jika server My SmartClass aktif**; Online sengaja belum diaktifkan karena server ujian cloud serta login peserta yang aman belum ada.
- Masukkan IP komputer guru dan kode ujian. Aplikasi juga dapat membaca `kode` dari tautan seperti `http://192.168.1.10:3210/ujian-siswa?kode=ABCD12`.
- Verifikasi kode dan pilih nama peserta dari API lokal My SmartClass.
- Ambil soal pilihan ganda/uraian dan sisa waktu langsung dari server. Per urutan soal mengikuti pengacakan server sesuai `attemptId`.
- Simpan jawaban ke server, dengan antrean jawaban lokal ketika koneksi terganggu dan pengiriman ulang saat tersedia.
- Konfirmasi sebelum pengumpulan, termasuk jumlah dan nomor soal yang belum dijawab.
- Remedial/ujian ulang membuat attempt baru melalui API server; riwayat lama tidak ditimpa.
- `FLAG_SECURE`, permintaan Screen Pinning dan pembatasan overlay Android 12 ke atas.
- Tombol kembali tidak keluar dari sesi aktif; perangkat pribadi tetap dapat melepaskan Screen Pinning menurut aturan Android.
- Pulihkan sesi sebelumnya memakai identitas attempt dan akses kunci yang tersimpan hanya dalam penyimpanan privat aplikasi. Backup Android untuk data ini dinonaktifkan.

### Langkah mencoba pada LAN

1. Jalankan My SmartClass pada PC guru dan pastikan server LAN dapat diakses melalui Wi-Fi yang sama.
2. Di My SmartClass, buka **Ujian**, pilih paket, buka ujian, catat alamat LAN dan kode ujian.
3. Pasang APK debug yang Anda bangun dari proyek ini pada HP Android 10 atau lebih baru.
4. Nyalakan **Sematkan Layar / Screen Pinning** melalui menu keamanan Android. Nama menu dapat berbeda pada setiap merek.
5. Di APK, pilih **LAN Sekolah**, tempel tautan siswa lengkap atau alamat server, masukkan kode jika tidak otomatis terbaca.
6. Tekan **Periksa Kode Ujian**, pilih nama siswa, lalu **Masuk dan Aktifkan Penguncian**.
7. Izinkan permintaan Screen Pinning, kemudian jawab soal dan pilih **Selesai dan Kumpulkan Ujian**.
8. Coba **Ujian Ulang / Remedial** setelah ujian selesai. My SmartClass harus tetap membuka paket ujian.

**Contoh alamat:** `http://192.168.1.10:3210/ujian-siswa?kode=ABCD12`. IP dan kode hanyalah contoh, gunakan yang benar dari PC guru. HP tidak boleh memakai `localhost` atau `127.0.0.1` untuk menjangkau PC guru.

### Cara membuat APK

**Pilihan A: GitHub Actions (paling mudah jika tidak memiliki Android Studio)**

1. Ekstrak folder proyek ini dan unggah seluruh isinya ke repository GitHub baru sebagai isi root repository.
2. Buka tab **Actions** > **Build My SmartExam APK** > **Run workflow**. Workflow juga berjalan saat push ke branch `main`.
3. Setelah workflow berhasil, buka hasil run > **Artifacts** > `My-SmartExam-Browser-v0.1.0-debug-apk`.
4. Unduh, ekstrak hasil artifacts, lalu pasang `app-debug.apk` pada HP uji. Ini APK debug untuk pengujian, bukan rilis final ke siswa.

**Pilihan B: Android Studio / Windows**

- Gunakan Android Studio dengan JDK 17, Android SDK Platform 35, Gradle 8.11.1, dan koneksi untuk mengambil dependency Android Gradle Plugin 8.9.2 serta Kotlin Plugin 2.1.20.
- Buka folder ini sebagai proyek Android/Gradle. Jika sistem meminta Gradle distribution, pilih Gradle 8.11.1.
- Build dengan perintah `gradle :app:assembleDebug` atau menu Build APK(s) di Android Studio.
- APK hasil build berada di `app/build/outputs/apk/debug/app-debug.apk`.
- Paket saat ini **tidak** menyertakan Gradle Wrapper JAR karena lingkungan authoring tidak memiliki distribusi Gradle untuk mengekstraknya. GitHub Actions menggunakan Gradle yang disiapkan otomatis.

### Penting tentang keamanan dan koneksi

- Mode **LAN** mengizinkan HTTP hanya ke alamat IP privat. HTTP LAN tidak terenkripsi. Gunakan jaringan kelas yang diawasi, bukan jaringan Wi-Fi publik. Server ujian yang dapat diakses internet harus memakai HTTPS.
- Mode **Online** belum aktif. Jangan membuka API Ujian LAN yang sekarang ke internet karena masih mengandalkan kode dan pemilihan nama dari daftar siswa.
- Android HP pribadi memungkinkan siswa melepas Screen Pinning. Aplikasi tidak memakai Accessibility, Device Owner, VPN, atau teknik yang mengambil alih HP.
- Bila koneksi hilang sampai batas waktu ujian habis, server bisa menolak jawaban yang terlambat. Antrean jawaban lokal tidak menjamin pemulihan setelah sesi kedaluwarsa. Kita perlu kebijakan server khusus untuk itu pada revisi berikutnya.
- Aplikasi mencatat indikasi kehilangan fokus **hanya secara lokal**. Belum ada layanan pengiriman laporan pelanggaran ke dashboard guru.
- Identitas sesi disimpan di preferensi privat dan di luar backup Android. Untuk perangkat nyata tetap pertimbangkan enkripsi key via Android Keystore pada tahap hardening.

### Sisa pekerjaan sebelum rilis produksi

1. Bangun dan uji APK di Android SDK, lalu uji beberapa merek HP.
2. Integrasikan mode online dengan Cloudflare Worker dan database D1; terapkan PIN peserta dan otorisasi server.
3. Tambahkan API peristiwa keamanan dan monitoring guru pada My SmartClass.
4. Tambahkan kebijakan reconnect/submit ketika jaringan terputus dan waktu server sudah habis.
5. Finalisasi tampilan, audit keamanan, tanda tangan APK release yang tetap, dan pengujian 30 siswa secara bersamaan.

**Catatan pengujian:** Aturan Kotlin murni sudah diuji melalui `tests/ExamRulesTest.kt`. Lingkungan pembuatan arsip tidak memiliki Android SDK atau Gradle sehingga **build APK dan pengujian HP belum dapat diverifikasi**.
