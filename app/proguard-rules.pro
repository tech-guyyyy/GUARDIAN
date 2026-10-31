# GUARDIAN ProGuard & R8 Rules

# Preserve Room database models and schemas
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Preserve Keystore and Cryptographic algorithms
-keepclassmembers class * {
    @javax.crypto.* <methods>;
    @java.security.* <methods>;
}

# Preserve Device Admin and Accessibility receiver classes
-keep public class com.guardian.app.service.GuardianDeviceAdminReceiver { *; }
-keep public class com.guardian.app.service.GuardianAccessibilityService { *; }
-keep public class com.guardian.app.service.BootCompletedReceiver { *; }

# Compose runtime rules are bundled with Compose dependencies automatically
