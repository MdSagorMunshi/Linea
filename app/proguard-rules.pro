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
