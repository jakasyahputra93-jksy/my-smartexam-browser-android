# Checklist Uji APK di HP Android

Tanggal tes: __________   Merek HP: __________   Android: __________   Guru penguji: __________

## Koneksi dan peserta

- [ ] APK bisa dipasang, dibuka, dan dijalankan ulang.
- [ ] Saat HP memakai Wi-Fi sekolah, alamat `http://IP-PC:PORT` terhubung.
- [ ] Tautan lengkap `/ujian-siswa?kode=...` mengisi kode otomatis jika kolom kode kosong.
- [ ] Kode yang salah menghasilkan penjelasan tanpa menutup aplikasi.
- [ ] Nama siswa cocok dengan daftar My SmartClass.
- [ ] Mode Online belum mengizinkan ujian sebelum backend cloud tersedia.

## Pengamanan dan soal

- [ ] Screen Pinning diminta ketika siswa mulai ujian.
- [ ] Siswa tidak dapat mengerjakan sebelum Screen Pinning berhasil.
- [ ] Siswa dapat melepas pin mengikuti prosedur Android (bukan dikunci permanen).
- [ ] Tangkapan layar normal dibatasi selama ujian.
- [ ] Pilihan ganda bisa dipilih dan diubah, uraian bisa diketik.
- [ ] Timer sesuai durasi server dan sisa waktu tidak di-reset ketika aplikasi dibuka ulang.
- [ ] Perpindahan soal mempertahankan jawaban.
- [ ] Tombol Selesai memperlihatkan nomor soal kosong.
- [ ] Kembali dari dialog Selesai tidak mengumpulkan ujian.

## Koneksi dan remedial

- [ ] Matikan lalu nyalakan Wi-Fi, jawaban yang tertunda dikirim setelah koneksi kembali, selama waktu ujian masih berlaku.
- [ ] Tutup paksa APK lalu buka lagi, sesi aktif pulih ketika server tersedia.
- [ ] Selesai ujian, rekap jawaban tampil di My SmartClass.
- [ ] Klik Ujian Ulang, server membuat attempt baru (bukan mengubah attempt lama).
- [ ] Percobaan ulang memakai urutan soal baru dari server.
- [ ] Guru masih dapat melihat dan memilih nilai semua percobaan dari My SmartClass.
- [ ] Koneksi hilang saat waktu habis: amati pesan error dan simpan laporan pengawas. Perilaku ini belum tuntas dalam v0.1.

## Uji beban awal

- [ ] 3 siswa bersamaan (tahap awal)
- [ ] 10 siswa bersamaan
- [ ] 30 siswa bersamaan (sebelum penggunaan riil)

Catat langkah untuk mengulang masalah dan foto pesan error tanpa menampilkan data pribadi siswa.
