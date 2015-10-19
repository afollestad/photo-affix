-keepattributes SourceFile,LineNumberTable
-keep class !android.support.v7.internal.view.menu.**,** {*;}
-keep class butterknife.** { *; }
-dontwarn butterknife.internal.**
-keep class **$$ViewInjector { *; }
-keepclasseswithmembernames class * {
    @butterknife.* <fields>;
}
-keepclasseswithmembernames class * {
    @butterknife.* <methods>;
}
-dontwarn
-ignorewarnings