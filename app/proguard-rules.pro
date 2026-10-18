# kotlinx.serialization: keep generated serializers for the backup DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class com.malekroki.app.domain.backup.**$$serializer { *; }
-keepclassmembers class com.malekroki.app.domain.backup.** {
    *** Companion;
}
-keepclasseswithmembers class com.malekroki.app.domain.backup.** {
    kotlinx.serialization.KSerializer serializer(...);
}
