# Keep generated kotlinx.serialization serializers.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class ca.nexacraft.homekeep.** {
    *** Companion;
}
-keepclasseswithmembers class ca.nexacraft.homekeep.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor / OkHttp are reflection-light, but keep platform bits happy.
-dontwarn org.slf4j.**
-dontwarn kotlinx.coroutines.**
