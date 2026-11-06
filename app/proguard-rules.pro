# Qixev FieldKit ProGuard / R8 Rules

# Room Database
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Dao interface * { *; }
-keep class * implements androidx.room.RoomDatabase
-keep class * implements com.qixev.qfieldkit.data.local.dao.* { *; }

# Room Entities & Type Converters
-keep class com.qixev.qfieldkit.data.local.entity.** { *; }
-keep class com.qixev.qfieldkit.data.local.database.FieldKitConverters { *; }
-keep class com.qixev.qfieldkit.data.local.model.** { *; }

# Backup Data Models (Preserve fields for deterministic JSON serialization)
-keep class com.qixev.qfieldkit.data.backup.model.** { *; }

# ViewModels (Instantiated via ViewModelProvider.Factory)
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}

