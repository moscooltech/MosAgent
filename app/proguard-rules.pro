# Moscool AI Agent ProGuard/R8 rules.
# Release builds currently run with minifyEnabled=false; these rules keep the
# build safe if minification is enabled later.

# --- kotlinx.serialization ---
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.moscool.agent.**$$serializer { *; }
-keepclassmembers class com.moscool.agent.** {
    *** Companion;
}
-keepclasseswithmembers class com.moscool.agent.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- OkHttp ---
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
