# Room Database keep rules
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter *;
}
-keep class com.example.workoutbuddy.data.database.** { *; }

# Jetpack Compose state and metadata keep rules
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
-keep class androidx.compose.runtime.** { *; }
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
