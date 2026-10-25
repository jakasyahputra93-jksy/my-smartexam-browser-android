# Checklist Uji My SmartExam Browser v0.3.0

Gunakan paket latihan, jangan ujian resmi sebelum seluruh kasus berikut lulus.

- [ ] Debug APK GitHub Actions berhasil dibangun dan diunduh.
- [ ] Terpasang berdampingan dengan APK lama tanpa menghapus aplikasi lama.
- [ ] Welcome Screen menunjukkan logo merah My SmartClass asli, palet ungu/magenta, footer identitas pengembang.
- [ ] Memilih Online langsung menampilkan server exam.mysmartclass.my.id tanpa kolom URL.
- [ ] Memasukkan `KM3P5` menampilkan judul ujian 10 soal dan dropdown 28 siswa (jika paket itu masih OPEN).
- [ ] Nama siswa yang sedang ACTIVE tampil sebagai sedang ujian.
- [ ] Dua HP memilih nama sama: yang kedua gagal, tanpa memperoleh jawaban/sesi milik HP pertama.
- [ ] Siswa memilih namanya, menekan Mulai, menyetujui Screen Pinning dan melihat soal.
- [ ] Pilihan ganda, uraian, timer, perpindahan soal dan penyimpanan berjalan.
- [ ] Soal yang belum dijawab muncul pada konfirmasi pengumpulan.
- [ ] Saat internet putus, jawaban tertunda tetap tersimpan di perangkat dan terkirim saat online kembali.
- [ ] Aplikasi ditutup paksa lalu dibuka: langsung mencoba memulihkan sesi dengan data perangkat.
- [ ] Guru dapat membuka kunci dan memindahkan sesi pada HP lain bila dibutuhkan.
- [ ] Ujian LAN melalui alamat IP privat tetap berfungsi.
- [ ] Guru berhasil menarik hasil Online dan memberikan nilai uraian melalui Desktop.
- [ ] Remedial membuat attempt baru, dengan urutan soal berbeda dari percobaan sebelumnya.
- [ ] Data lama di Worker hanya terhapus setelah persyaratan retensi dan konfirmasi sinkron dipenuhi.
