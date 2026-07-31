# =============================================================
#  Wromble – R8/ProGuard regler
#
#  R8 er nu slaaet HELT til (shrink + optimize + obfuskering + ressource-
#  komprimering) for at faa mindre/hurtigere app og fjerne Play Console's
#  optimeringsadvarsler.
#
#  VIGTIGT: Netvaerks- og datalaget holdes 100% urort (fuld -keep), fordi
#  R8 full mode tidligere fjernede Retrofit's generiske signaturer og broed
#  API-kaldene. Ved at beholde retrofit2/okhttp3/okio/gson + hele
#  dk.wromble.app.data.** uroert, kan den fejl ikke opstaa. R8 optimerer/
#  slorer stadig resten af appen (UI/Compose), hvilket er sikkert.
# =============================================================

# --- Bevar attributter R8 full mode ellers kan smide (generics/annotationer) ---
-keepattributes Signature,*Annotation*,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,Exceptions

# --- Netvaerkslaget holdes HELT uroert (dette var det der broed sidst) ---
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-keep class com.google.gson.** { *; }
-keep interface com.google.gson.** { *; }

# --- Behold ALLE data-/API-modeller + service-interface (Gson bruger reflektion) ---
-keep class dk.wromble.app.data.** { *; }
-keepclassmembers class dk.wromble.app.data.** { *; }

# Gson @SerializedName-felter + enums
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keepclassmembers enum * { *; }

# Retrofit suspend-funktioner (Kotlin Continuation-generics)
-keep class kotlin.coroutines.Continuation { *; }
-keepclasseswithmembers interface * { @retrofit2.http.* <methods>; }

# --- osmdroid (kort) ---
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# --- ZXing QR-scanner ---
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**
-dontwarn com.journeyapps.barcodescanner.**

# --- Kotlin metadata / coroutines ---
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**

# --- Ryd op i stoej fra valgfri afhaengigheder ---
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
