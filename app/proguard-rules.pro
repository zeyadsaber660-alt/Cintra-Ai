# Cintra Release Obfuscation & Anti-Reverse-Engineering Rules
-repackageclasses 'cintra.internal'
-allowaccessmodification
-optimizationpasses 5
-overloadaggressively

# Strip all debug logs in release builds
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Obfuscate security & network packages aggressively
-keepclassmembers class com.cintra.app.data.security.** { *; }