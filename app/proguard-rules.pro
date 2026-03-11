# ============================================================================
# AiRobot Assistant - ProGuard / R8 Rules
# ============================================================================

# ---- General ---------------------------------------------------------------

# Keep source file names & line numbers for crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations (required by many libraries)
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ---- JNI / Native Methods -------------------------------------------------
# OpusEncoder & OpusDecoder use native JNI methods - class & method names
# must match the C/C++ function signatures exactly.
-keep class com.airobot.assistant.audio.tools.codec.OpusEncoder {
    native <methods>;
    *;
}
-keep class com.airobot.assistant.audio.tools.codec.OpusDecoder {
    native <methods>;
    *;
}

# Keep all classes that declare native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# ---- Kotlin Serialization --------------------------------------------------
# Keep @Serializable classes and their companion serializers
-keepattributes RuntimeVisibleAnnotations
-keep,includedescriptorclasses class com.airobot.assistant.**$$serializer { *; }
-keepclassmembers class com.airobot.assistant.** {
    *** Companion;
}
-keepclasseswithmembers class com.airobot.assistant.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Kotlinx serialization core
-keep class kotlinx.serialization.** { *; }
-dontwarn kotlinx.serialization.**

# ---- Gson ------------------------------------------------------------------
# Keep fields used by Gson for JSON deserialization
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
# Keep data classes in model packages used with Gson
-keep class com.airobot.assistant.system.model.** { *; }
-keep class com.airobot.assistant.system.remote.** { *; }
-keep class com.airobot.assistant.comm.** { <fields>; }

# ---- OkHttp ----------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ---- Hilt / Dagger ---------------------------------------------------------
# Hilt generates code via KSP; keep entry points
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn dagger.hilt.**

# ---- Jetpack Compose -------------------------------------------------------
-dontwarn androidx.compose.**
# Keep Compose-generated lambda classes
-keep class androidx.compose.** { *; }

# ---- AndroidX / Lifecycle --------------------------------------------------
-keep class androidx.lifecycle.** { *; }

# ---- sherpa-onnx (AAR library) ---------------------------------------------
-keep class com.k2fsa.sherpa.onnx.** { *; }
-dontwarn com.k2fsa.sherpa.onnx.**

# ---- Noise library ---------------------------------------------------------
-keep class com.paramsen.noise.** { *; }
-dontwarn com.paramsen.noise.**

# ---- Enum classes ----------------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- Parcelable ------------------------------------------------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ---- R class ---------------------------------------------------------------
-keepclassmembers class **.R$* {
    public static <fields>;
}