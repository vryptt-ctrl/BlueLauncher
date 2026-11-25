# Keep JNI entry points available in optimized release builds.
-keep class com.bluelabs.bluelauncher.nativeengine.NativeEngine { *; }
-keepclasseswithmembernames class * { native <methods>; }
