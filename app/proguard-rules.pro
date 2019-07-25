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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# for DexGuard only
#-keepresourcexmlelements manifest/application/meta-data@value=GlideModule

#指定代码的压缩级别
-optimizationpasses 5

#包明不混合大小写
#dontusemixedcaseclassnames

#不去忽略非公共的库类
#-dontskipnonpubliclibraryclasses

#优化 不优化输入的类文件
#-dontoptimize

#预校验
#-dontpreverify

#混淆时是否记录日志
#-verbose

#混淆时所采用的算法
-optimizations !code/simplification/arithmetic,!field/,!class/merging/

#保护注解
#-keepattributes Annotation
#保持哪些类不被混淆
-keep public class * extends android.app.Fragment
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class com.android.vending.licensing.ILicensingService
#如果有引用v4包可以添加下面这行
-keep public class * extends android.support.v4.app.Fragment


#忽略警告
#-ignorewarning

#记录生成的日志数据,gradle build时在本项目根目录输出
#apk 包内所有 class 的内部结构
-dump class_files.txt
#未混淆的类和成员
#-printseeds seeds.txt

#列出从 apk 中删除的代码
#printusage unused.txt

#混淆前后的映射
-printmapping mapping.txt

# 保持 native 方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

# OkHttp3
-dontwarn okhttp3.**
-keep class okhttp3.internal.**{*;}
-dontwarn okio.**

-keep class cn.bingoogolapple.**{*;}
-dontwarn cn.bingoogolapple.**
# Admob
#-keep public class com.google.**{*;}
#-dontwarn com.google.**

# 不混淆 kochava ++
#-keep class com.kochava.**{*;}
# kochava --
