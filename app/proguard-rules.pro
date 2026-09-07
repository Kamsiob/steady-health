# Steady Health, release rules.
#
# The app is small and almost entirely its own code, so most of what R8 needs to
# be told about is the libraries that find things by name at runtime. Every rule
# below names what it protects and why, because a keep rule nobody can justify is
# a keep rule nobody can ever remove.

# SQLCipher's native bridge. The JNI layer looks these up by name from C, so R8
# cannot see the reference and will happily rename or remove them. Getting this
# wrong is an UnsatisfiedLinkError on the first query, in release only.
-keep class net.zetetic.database.** { *; }
-keep class net.sqlcipher.** { *; }

# Room generates implementations and looks up entity constructors reflectively.
-keep class * extends androidx.room3.RoomDatabase { <init>(); }
-keep @androidx.room3.Entity class * { *; }
-dontwarn androidx.room3.paging.**

# kotlinx.serialization keeps its generated serializers on the companion.
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# WorkManager instantiates workers by class name from the database.
-keep class com.kamsiob.steadyhealth.remind.ReminderWorker { <init>(...); }

# Line numbers, so a stack trace somebody sends is worth reading. The source file
# name is renamed rather than kept, which is what the Android default does.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
