# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable,*Annotation*,Signature,InnerClasses,EnclosingMethod

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

# AppsFlyer SDK intentionally excluded from app
-dontwarn com.appsflyer.**

# ---------------------- CSC App Library & Configs ----------------------
-keep class com.cscapp.** { *; }
-keep class com.cscmobi.libraryads.** { *; }
-keep interface com.cscmobi.libraryads.** { *; }
-keepclassmembers class * implements com.cscmobi.libraryads.commons.remote.CSCKonfigModel { *; }
-keep class com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs { *; }
-keepclassmembers class com.money.tracker.budget.planner.moneymanager.ads.RemoteConfigs { *; }
-dontwarn com.cscapp.**
-dontwarn com.cscmobi.libraryads.**

# ---------------------- Adjust SDK ----------------------
-keep class com.adjust.sdk.** { *; }
-dontwarn com.adjust.sdk.**

# ---------------------- Ads Mediation Adapters ----------------------
# Meta / Facebook Audience Network
-keep class com.google.ads.mediation.facebook.** { *; }
-keep class com.facebook.ads.** { *; }
-keep class com.facebook.internal.** { *; }
-dontwarn com.facebook.ads.**
-dontwarn com.facebook.internal.**

# AppLovin Mediation
-keep class com.google.ads.mediation.applovin.** { *; }
-keep class com.applovin.** { *; }
-dontwarn com.applovin.**

# InMobi Mediation
-keep class com.google.ads.mediation.inmobi.** { *; }
-keep class com.inmobi.** { *; }
-dontwarn com.inmobi.**
-dontwarn com.squareup.picasso.**

# Pangle SDK
-keep class com.bytedance.sdk.** { *; }
-keep interface com.bytedance.sdk.** { *; }
-keep class com.bytedance.adsdk.** { *; }
-keep class com.bytedance.mobsec.** { *; }
-keep class com.bytedance.applog.** { *; }
-keep class com.apm.insight.** { *; }
-keep class com.bykv.vk.** { *; }
-keep interface com.bykv.vk.** { *; }
-keep class com.com.bytedance.overseas.sdk.** { *; }
-keep class com.iab.omid.library.bytedance2.** { *; }
-keep class com.pgl.ssdk.** { *; }
-keep class com.tiktok.** { *; }
-keep class com.bytedance.sdk.openadsdk.** { *; }
-keep class com.bykv.vk.openvk.** { *; }
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Pangle JNI & Native Security (Crucial for Bidding Token Generation)
-keep class ms.bd.o.Pgl.** { *; }
-keep class ms.bd.c.** { *; }
-keep class ms.bd.o.** { *; }
-keep class com.pgl.sys.ces.** { *; }

# Google AdMob Mediation Adapter for Pangle
-keep class com.google.ads.mediation.pangle.** { *; }

# Mintegral SDK
-keep class com.mbridge.** { *; }
-dontwarn com.mbridge.**
-keep interface com.mbridge.** { *; }
-keep class * extends com.mbridge.msdk.out.BaseExtraInterfaceForHandler { *; }
-keep class com.google.ads.mediation.mintegral.** { *; }

# Unity Ads Mediation
-keep class com.google.ads.mediation.unity.** { *; }
-keep class com.unity3d.ads.** { *; }
-dontwarn com.unity3d.ads.**
-keep class com.unity3d.services.** { *; }
-dontwarn com.unity3d.services.**

# Pubscale Adapter
-keep class com.pubscale.** { *; }
-dontwarn com.pubscale.**

# ---------------------- Gson Serialization & App Models ----------------------
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.money.tracker.budget.planner.moneymanager.model.** { *; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---------------------- EventBus 3.x ----------------------
-keepclassmembers class * {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }
-keepclassmembers class * extends org.greenrobot.eventbus.util.ThrowableFailureEvent {
    <init>(java.lang.Throwable);
}
-keep class com.money.tracker.budget.planner.moneymanager.utils.TransactionUpdateEvent { *; }

# ---------------------- MPAndroidChart Animations & Formatters ----------------------
-keep class com.github.mikephil.charting.animation.ChartAnimator { *; }
-keep class * extends com.github.mikephil.charting.formatter.ValueFormatter { *; }
