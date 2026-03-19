# EarBrief ProGuard Rules

# --- Kotlin Serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.earbrief.app.**$$serializer { *; }
-keepclassmembers class com.earbrief.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.earbrief.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- ONNX Runtime ---
-keep class ai.onnxruntime.** { *; }
-keepclassmembers class ai.onnxruntime.** { *; }

# --- Ktor ---
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# --- OkHttp ---
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep class com.earbrief.app.data.local.db.**_Impl { *; }
-keep class com.earbrief.app.data.local.db.entity.** { *; }
-keep interface com.earbrief.app.data.local.db.dao.** { *; }
-dontwarn androidx.room.paging.**

# --- Hilt ---
-keep class dagger.hilt.** { *; }
-keep class dagger.hilt.internal.** { *; }
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep class javax.inject.** { *; }

# --- Privacy: Block FileOutputStream in audio processing ---
# (Additional static analysis via lint rules)

# --- Keep data classes for reflection ---
-keepclassmembers class com.earbrief.app.domain.model.** { *; }
-keepclassmembers class com.earbrief.app.data.remote.**.model.** { *; }

# --- Serialization models used by APIs/navigation ---
-keep class com.earbrief.app.data.remote.deepgram.model.** { *; }
-keep class com.earbrief.app.data.remote.elevenlabs.** { *; }
-keep class com.earbrief.app.presentation.navigation.** { *; }
