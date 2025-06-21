ISMoney - Aplikasi Manajemen Keuangan Pribadi

I. Deskripsi

ISMoney adalah aplikasi desktop berbasis JavaFX yang bertujuan membantu pengguna dalam mencatat, mengelola keuangan pribadi, dan manajemen tabungan secara efisien. Pengguna dapat menyimpan data transaksi harian seperti pendapatan dan pengeluaran, serta membuat kategori sesuai kebutuhan.

II. Fitur Utama

- Manajemen Transaksi pengeluaran dan pemasukan yang memiliki kategori pada masing-masing jenis transaksi, seperti pengeluaran untuk hiburan dan pemasukan dari gaji.
Input reuirement: nominal, tipe, kategori, catatan, dan tanggal.

- Manajemen atau fitur pencatatan tabungan, yang sedang berjalan atau yang sudah selesai.
input requirement: nama target, nominal yang sudah terkumpul, target nominal, kapan target harus dicapai, status target (aktif ata sudah selesai)

III. Cara Menjalankan Aplikasi

- Prasyarat
Java JDK 17+
JavaFX SDK 17+
Scene Builder (opsional)
IDE: IntelliJ IDEA / NetBeans / VSCode

- Langkah Instalasi
1. Clone / Ekstrak Proyek
git clone <https://github.com/rielyta/ISMOney.git> atau ekstrak file ISMoney.rar

2. Buka IDE
Buka folder proyek dengan IntelliJ / NetBeans.
Tambahkan JavaFX library ke module dependencies.

3. Jalankan Aplikasi
Jalankan file utama IsMoneyApp

GUI akan tampil, mulai dengan login atau registrasi.

IV. Dependencies

JavaFX (Controls, FXML)
BCrypt via org.mindrot.jbcrypt
JDBC (Java Database Connectivity)
Logger bawaan Java untuk debugging
