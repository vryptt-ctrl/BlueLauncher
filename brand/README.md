# BlueLauncher brand assets

Konsep: **Focus Grid**. Kisi 2×2 kotak aplikasi, satu kotak menyala dengan cincin fokus. Menggambarkan launcher berbasis D-pad.

## Warna

| Peran | Hex |
| --- | --- |
| Navy (latar) | `#0B1220` |
| Biru utama | `#2F7DFF` |
| Biru terang | `#5AA9FF` |
| Cyan (cincin fokus, glow) | `#7CE1FF` |
| Kotak redup, latar terang | `#25406E` |
| Kotak redup, latar gelap | `#3A5C94` |
| Teks sekunder di latar gelap | `#7CC4FF` |

## Tipografi

Wordmark memakai **Inter** (Bold untuk "Blue", Regular untuk "Launcher"). File SVG berisi teks live dan akan jatuh ke Helvetica/Arial kalau Inter tidak terpasang. Semua PNG sudah dirender dengan Inter.

## Isi folder

```
svg/        sumber vektor (edit di sini)
  icon.svg                  ikon dengan sudut membulat
  icon-square.svg           ikon persegi penuh (Play Store, topeng sistem)
  icon-round.svg            ikon bulat
  mark-color.svg            tanda saja, latar terang
  mark-color-on-dark.svg    tanda saja, latar gelap
  mark-white.svg / mark-dark.svg   satu warna
  logo-horizontal-light.svg / -dark.svg   tanda + nama
  banner-tv.svg             banner Android TV 320×180
  social-preview.svg        gambar pratinjau GitHub 1280×640
png/        hasil ekspor siap pakai (ikon 16–1024, logo, banner, favicon)
android/res/  drop-in untuk app/src/main/res
```

## Pasang di proyek Android

1. Salin isi `android/res/` ke `app/src/main/res/` (gabungkan folder).
2. Di `AndroidManifest.xml`, pada `<application>`:

   ```xml
   android:icon="@mipmap/ic_launcher"
   android:roundIcon="@mipmap/ic_launcher_round"
   android:banner="@drawable/tv_banner"
   ```

3. `minSdk 23`: perangkat Android 7.x memakai PNG di `mipmap-*dpi`, Android 8+ memakai adaptive icon XML. Android 13+ otomatis memakai layer monochrome untuk ikon bertema.
4. Banner TV harus berukuran 320×180 px di `drawable-xhdpi` dan memuat nama aplikasi. Itu sudah terpenuhi.

## Pemakaian

- Beri ruang kosong di sekeliling logo minimal setinggi satu kotak kecil.
- Jangan putar, regangkan, atau ganti warna kotak yang menyala. Cincin fokus selalu ada di kotak kanan atas.
- Ukuran ikon minimum: 24 px. Di bawah itu pakai `mark-color.svg` tanpa latar.
- Logo ini orisinal, bukan turunan merek lain.

## Lisensi

Aset brand dilisensikan di bawah **CC BY 4.0** — lihat [LICENSE-ASSETS.md](LICENSE-ASSETS.md) untuk ketentuan lengkap dan cara atribusi. Kode program tetap di bawah GPL-3.0; kedua lisensi saling independen.
