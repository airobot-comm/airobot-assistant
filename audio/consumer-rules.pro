# Consumer rules for the audio library
-keep class com.airobot.audio.** { *; }
-keepclassmembers class com.airobot.audio.tools.codec.** {
    native <methods>;
}
