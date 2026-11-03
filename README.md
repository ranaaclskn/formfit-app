# FormFit

Kamerayla spor hareketlerinin doğru yapılıp yapılmadığını ölçen, doğru ve yanlış tekrarları sayan
Android ve iOS uygulaması. Kotlin Multiplatform ve Compose Multiplatform ile yazıldı. Vücut takibini
cihaz üzerinde MediaPipe Pose Landmarker yapıyor, görüntü telefondan dışarı çıkmıyor.

## Özellikler

- **Antrenman:** Hareketi seçersin, kamera açılır. İskelet görüntünün üzerine çizilir ve her tekrar
  anında **doğru** ya da **yanlış** diye sayılır. Yanlış tekrarda sebebi de gösterilir
  ("Yeterince inmedin", "Dirseğini gövdene sabitle" gibi).
- **Hareketler:** Her hareketin animasyonlu "nasıl yapılır" videosu var (oynat/durdur, yarı hız).
  Altında adım adım anlatım, dikkat edilmesi gerekenler, püf noktaları ve kamera yerleşimi yer alır.
- **Geçmiş:** Her antrenman tekrar tekrar kaydedilir: doğru/yanlış sayısı, doğruluk yüzdesi, süre,
  en sık yapılan hatalar. Harekete göre süzülebilir, kayıt silinebilir.

Desteklenen hareketler: Squat, Şınav, Lunge, Kalça Köprüsü, Biceps Curl, Omuz Press,
Yana Açış (Lateral Raise), Jumping Jack.

## Tekrarlar nasıl sayılıyor?

Her hareketin bir ana açısı var: squat'ta diz, şınavda dirsek, yana açışta kolun gövdeden açılması
gibi. Bu açı dinlenme konumundan *başlangıç eşiğini* geçince tekrar başlar, geri dönünce biter.
Tekrar boyunca şunlar kontrol edilir:

- **Hedef açıya ulaşıldı mı?** Ulaşılmadıysa yarım tekrar sayılır ve yanlış olur.
- **Form kuralları:** Gövde eğimi, dirsek sabitliği, vücut düzlüğü, iki kolun simetrisi gibi kurallar.
  Bir kural tekrarın belirgin bir kısmında bozulduysa tekrar yanlış sayılır.
- **Tempo:** Çok hızlı yapılan tekrar yanlış sayılır.

Açıdaki küçük titremelerin tekrar sayılmaması için başlangıç ve bitiş eşikleri farklı tutulur.
Kişi kadrajdan çıkarsa yarım kalan tekrar iptal edilir. Kurallar
`composeApp/src/commonMain/.../model/Exercises.kt` dosyasında, sayma mantığı
`analysis/RepCounter.kt` dosyasında. Birim testleri `composeApp/src/commonTest` klasöründe.

## Proje yapısı

```
composeApp/src/commonMain   Ortak kod: arayüz, hareketler, tekrar sayma, geçmiş
composeApp/src/androidMain  CameraX + MediaPipe (Android), pose modeli (assets/)
composeApp/src/iosMain      iOS köprüsü (kamera Swift'ten gelir)
iosApp/                     iOS uygulaması: SwiftUI kabuğu, AVFoundation + MediaPipe (PoseCamera.swift)
```

## Android'de çalıştırma

JDK 17 ve Android SDK gerekir.

```bash
./gradlew :composeApp:assembleDebug       # APK: composeApp/build/outputs/apk/debug/
./gradlew :composeApp:installDebug        # bağlı telefona kurar
./gradlew :composeApp:testDebugUnitTest   # tekrar sayma testleri
```

## iOS'ta çalıştırma (Mac gerekir)

[XcodeGen](https://github.com/yonaskolb/XcodeGen) ve [CocoaPods](https://cocoapods.org) gerekir
(`brew install xcodegen cocoapods`).

```bash
cd iosApp
xcodegen          # project.yml'den Xcode projesini üretir
pod install       # MediaPipeTasksVision kütüphanesini indirir
open iosApp.xcworkspace
```

Xcode'da kendi geliştirici hesabını seçip gerçek bir iPhone'da çalıştır. Simülatörde kamera yok.

## Kamera yerleşimi ipuçları

- Telefonu sabit bir yere koy, 2-3 metre uzaklaş. Baştan ayağa tüm vücudun görünmeli.
- Squat, şınav, lunge, kalça köprüsü ve curl için **yandan**; omuz press, yana açış ve jumping
  jack için **önden** çek.
- İyi ışık takibi belirgin şekilde iyileştirir.

## Lisans ve model

Pose modeli (`pose_landmarker_full.task`) Google MediaPipe'tan alındı ve Apache 2.0 lisanslı.
