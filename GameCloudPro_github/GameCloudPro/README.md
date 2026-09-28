# GameCloud Pro — Android (v3.0)

Kotlin / Jetpack Compose ile oyun kayıtlarını buluta yedekleme altyapısı.

## Mimari
- UI → ViewModel → Repository → Data source
- DataStore ile oyun bazlı kalıcı yapılandırma
- SAF (`OpenDocumentTree`) + kalıcı URI izni ile kayıt klasörü seçimi
- Dosya tarama ve SHA-256 manifest üretimi (Dispatchers.IO, ilerleme bildirimi)
- Firebase, `CloudBackupRepository` arkasında soyutlanmış (şu an stub)

## Derleme
Gradle 8.9 (wrapper), AGP 8.7.3, Kotlin 2.0.21, JDK 17. Klasörü Android Studio ile açın, sync bitince Build > Generate App Bundles or APKs kullanılabilir.

## Gerçek Firebase'i etkinleştirme
1. Firebase Console'da Android uygulaması oluştur (paket: `com.gamecloud.pro`).
2. `google-services.json` dosyasını `app/` içine koy.
3. `app/build.gradle.kts` içindeki Firebase bağımlılıklarının yorumunu kaldır, google-services plugin'ini ekle.
4. Authentication, Firestore ve Storage'ı aç; kuralları UID bazında sınırla.
5. `FirebaseCloudBackupRepository` içindeki `error(...)` çağrılarını gerçek upload/list/restore ile değiştir.

Storage: `users/{uid}/games/{packageName}/backups/{backupId}/{relativePath}`
Firestore: `users/{uid}/games/{packageName}/backups/{backupId}` (id, packageName, appVersion, createdAt, totalBytes, fileCount, files[relativePath, sizeBytes, sha256])

## Güvenlik
- Erişim UID ile sınırlandırılmalı, App Check açılmalı.
- Hash'ler değişmeyen dosyaları tekrar yüklememek için kullanılmalı; büyük dosyalarda resumable upload.

## Android kısıtlaması
Android 11+ sürümünde başka uygulamaların `Android/data` ve `Android/obb` klasörleri SAF ile bile seçilemez.
Kayıtlar erişilebilir bir klasördeyse (ör. Documents, oyunun kendi dışa aktarım klasörü) yedeklenebilir; aksi halde zorla okunamaz.

## Sonraki adımlar
Giriş (Google/Email) → değişen dosyaları gönderme → upload progress → versioning → cloud restore → çakışma çözümü → otomatik yedek → istemci tarafı şifreleme.

## Android Studio olmadan APK (GitHub Actions)
Projeyi GitHub'a yükle → **Actions** sekmesi → "APK derle" → çalışma bitince **Artifacts** altından `GameCloudPro-debug-apk` indir.
