# Keep kotlinx.serialization generated serializers
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.financebook.android.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.financebook.android.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
