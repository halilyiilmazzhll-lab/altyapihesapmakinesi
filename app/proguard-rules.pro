# Proguard / R8 configuration for Eğim Hesabı
# Add project specific ProGuard rules here.

# Keep Kotlin Serialization models if any
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Keep Navigation and Compose models
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
