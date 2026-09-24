# Consumer rules for the audio library
-keep class com.airobot.agent.** { *; }
-keepclassmembers class com.airobot.agent.tools.codec.** {
    native <methods>;
}

