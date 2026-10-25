# Panduan membangun APK Debug My SmartExam Browser

Paket ini berisi kode sumber Android dan GitHub Actions yang mengompilasi APK secara otomatis. Ini bukan file APK siap instal.

## Metode paling mudah: GitHub Actions

1. Buka https://github.com/new dan masuk ke akun GitHub Anda.
2. Isi nama repository `my-smartexam-browser-android`. Pilih **Private**. Jangan aktifkan opsi membuat README, .gitignore, atau license. Klik **Create repository**.
3. Ekstrak file `My-SmartExam-Browser-v0.1.0-GITHUB-READY.zip` pada komputer Anda.
4. Buka folder hasil ekstraksi. Pilih semua isi folder, termasuk `.github`, `app`, `build.gradle.kts`, dan `settings.gradle.kts`. Seret seluruh isinya ke area unggah GitHub melalui **uploading an existing file** atau **Add file > Upload files**. Jangan unggah file ZIP utuh. Jangan membuat folder luar tambahan di dalam repository.
5. Pastikan `.github/workflows/build-android-apk.yml` dan `app/build.gradle.kts` akan berada di root repository. Klik **Commit changes** untuk menyimpan file ke branch `main`.
6. Klik tab **Actions**. Pilih **Build My SmartExam APK**. Workflow dapat berjalan otomatis setelah unggahan. Jika belum berjalan, klik **Run workflow**, pilih `main`, lalu konfirmasi.
7. Jika tanda hijau muncul, klik workflow run yang berhasil. Di bagian **Artifacts**, unduh `My-SmartExam-Browser-v0.1.0-debug-apk`.
8. Ekstrak berkas artifacts yang Anda unduh. Ambil `app-debug.apk` dan salin ke HP Android. Buka file APK dari HP lalu ikuti izin pemasangan aplikasi dari sumber tersebut.

## Menguji dengan My SmartClass LAN

1. Jalankan My SmartClass pada PC guru. Buka modul Ujian, terbitkan ujian latihan, dan pastikan server LAN aktif.
2. Pastikan HP dan PC berada pada jaringan Wi-Fi sekolah yang dapat saling terhubung.
3. Pada HP buka My SmartExam Browser. Pilih **LAN Sekolah**, masukkan alamat LAN dan kode ujian sesuai tampilan My SmartClass.
4. Periksa kode, pilih peserta uji, dan ikuti permintaan Screen Pinning dari Android.
5. Kerjakan contoh soal, periksa peringatan jawaban kosong, kumpulkan, dan coba ujian ulang untuk menguji remedial.

Contoh alamat LAN: `http://192.168.1.10:3210/ujian-siswa?kode=ABCD12`. Ini contoh, bukan alamat PC guru Anda. Jangan masukkan localhost atau 127.0.0.1 pada HP.

## Jika menemui kendala

- Tidak ada tab Actions atau workflow: pastikan `.github/workflows/build-android-apk.yml` ada di root repository, bukan tersimpan di dalam folder ekstra.
- Workflow merah: klik run yang gagal, buka langkah dengan tanda merah, lalu kirim tangkapan layar pesan error kepada asisten untuk dianalisis.
- Workflow hijau tapi APK belum terlihat: buka detail run dan cari bagian **Artifacts**. Unduh lalu ekstrak ZIP artifacts.
- Android menolak APK: pastikan perangkat memakai Android 10 atau lebih baru. Berikan izin instalasi untuk aplikasi pengelola file atau browser yang Anda gunakan hanya saat diperlukan.
- Koneksi LAN gagal: pastikan PC dan HP berada di jaringan yang sama, pakai alamat IP PC yang benar, dan periksa firewall Windows.
- APK debug dari build GitHub berbeda dapat memakai sertifikat debug berbeda. Bila pembaruan gagal karena tanda tangan berbeda, hapus versi uji lama hanya setelah semua jawaban tersinkronisasi.

## Batasan saat ini

Aplikasi ini masih kandidat uji, bukan aplikasi produksi untuk ujian nyata. Mode Online menunggu server cloud dan autentikasi ujian dari My SmartClass. APK biasa pada HP pribadi tidak dapat menjamin penguncian penuh perangkat. GitHub Actions membutuhkan akses internet dan mungkin memakai kuota menit runner akun Anda.
