# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.paywise.app.domain.model.** { *; }
-keep class com.paywise.app.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn com.google.firebase.**
