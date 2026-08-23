# The page talks to the shell through a WebMessageListener, not through a
# @JavascriptInterface bridge, so no members have to survive by name. Keep the
# annotation rule anyway: if a bridge is ever added, R8 must not strip it.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Line numbers make Play Console's deobfuscated crash reports usable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
