# =============================================================
#  Wromble – R8/ProGuard regler
#
#  VIGTIGT: R8 koeres som en REN gennemgang (pass-through). Vi slaar
#  minify TIL i build.gradle, men slaar selve shrink/optimize/obfuskering
#  FRA her med de tre -dont-regler nedenfor. Resultatet:
#    * DEX-koden er 100% identisk med den build der virker nu
#      (ingen API-kald kan broedes – det var det R8 "full mode" gjorde
#       sidst da den fjernede Retrofit's generiske signaturer),
#    * MEN R8 genererer stadig en mapping-fil (deobfuskeringsfil) som
#      Gradle laegger ind i AAB'en -> Play Console's gule advarsel
#      "Der er ikke knyttet nogen fil til fjernelse af sloering" forsvinder.
#  Bedste af begge verdener: advarslen vaek, nul risiko for net-laget.
# =============================================================

# --- R8 som ren gennemgang: ingen shrink/optimize/obfuskering ---
-dontshrink
-dontoptimize
-dontobfuscate

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
