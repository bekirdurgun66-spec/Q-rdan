# =============================================================================
# AkaTrade AI - ProGuard / R8 Güvenlik ve Optimizasyon Kuralları
# =============================================================================

# --- BuildConfig proxy yapılandırma alanlarını gizle ---
# PROXY_BASE_URL ve PROXY_API_KEY'in decompile edilerek okunmasını zorlaştır
-assumenosideeffects class com.example.BuildConfig {
    static final java.lang.String PROXY_BASE_URL;
    static final java.lang.String PROXY_API_KEY;
}

# --- Retrofit ---
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**

# --- OkHttp ---
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# --- Room (Veritabanı) ---
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**
-keep class com.example.data.model.** { *; }

# --- Moshi (JSON serialization) ---
-keep class com.squareup.moshi.** { *; }
-keep class com.example.data.api.** { *; }
-keep @com.squareup.moshi.JsonClass class *
-keepclasseswithmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep class kotlin.reflect.jvm.internal.impl.builtins.** { *; }
-keep class kotlin.Metadata { *; }

# --- Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# --- AndroidX Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# --- Firebase ---
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# --- ViewModel ve LiveData ---
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# --- Kotlin reflection + Serializable ---
-keepattributes RuntimeVisibleAnnotations
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# --- Genel güvenlik: stack trace'lerden hassas verileri temizle ---
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
