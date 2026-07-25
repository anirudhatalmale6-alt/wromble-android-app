# =============================================================
#  Wromble – R8/ProGuard regler
#  Minify er slaaet TIL (kode-optimering + shrink). Reglerne her
#  sikrer at Gson/Retrofit-modeller og reflektion IKKE broedes.
# =============================================================

# --- Behold ALLE data-/API-modeller (Gson bruger reflektion paa feltnavne) ---
-keep class dk.wromble.app.data.** { *; }
-keepclassmembers class dk.wromble.app.data.** { *; }

# Bevar generiske signaturer + annotationer (Gson/Retrofit kraever dem)
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# --- Gson ---
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers enum * { *; }

# --- Retrofit / OkHttp (har egne consumer-regler, men vi er eksplicitte) ---
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keepclasseswithmembers interface * { @retrofit2.http.* <methods>; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# --- osmdroid (kort) ---
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# --- Kotlin metadata / coroutines ---
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**
-keep class kotlin.Metadata { *; }

# --- ZXing QR-scanner (ren Java) ---
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**
-dontwarn com.journeyapps.barcodescanner.**

# --- Ryd op i stoej fra valgfri afhaengigheder ---
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
