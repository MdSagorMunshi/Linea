# Linea Proguard Rules
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-dontwarn javax.annotation.**

# Room Database & Entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class com.ryanshelby.linea.data.local.entities.** { *; }

# Preferences & Models
-keep class com.ryanshelby.linea.data.preferences.** { *; }
-keep class com.ryanshelby.linea.qr_code.QrCodeEngine$ContactPayload { *; }

# NFC & Telecom Services
-keep class com.ryanshelby.linea.nfc.LineaBeamApduService { *; }
-keep class com.ryanshelby.linea.telecom.** { *; }

# ErrorProne & Annotations
-dontwarn com.google.errorprone.annotations.**

# Google ML Kit Barcode Scanning & Component Discovery
-keep class com.google.mlkit.** { *; }
-keep interface com.google.mlkit.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_barcode.** { *; }
-keep class com.google.android.gms.internal.mlkit_vision_common.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
-keep class * extends com.google.firebase.components.ComponentRegistrar { <init>(); }
-keep class com.google.mlkit.common.internal.MlKitComponentDiscoveryService { *; }
-dontwarn com.google.mlkit.**
-dontwarn com.google.android.gms.internal.mlkit_**
-dontwarn com.google.firebase.components.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**
