# ML Kit publishes its consumer rules. Keep only application entry points that
# Android instantiates by class name; R8 can optimize the remaining code.
-keep class vn.kai.board.MainActivity { *; }
-keep class vn.kai.board.ime.KaiBoardImeService { *; }
-keep class vn.kai.board.voice.VoiceInputActivity { *; }
-keep class vn.kai.board.translation.TranslationActivity { *; }
-keep class vn.kai.board.translation.TranslationModelsActivity { *; }
-keep class vn.kai.board.ClipboardManagerActivity { *; }
-keep class vn.kai.board.LearnedWordsActivity { *; }

# ML Kit model registries rely on runtime class names and generic metadata.
-keep class com.google.mlkit.nl.translate.** { *; }
-keep class com.google.mlkit.common.model.** { *; }
-keep class com.google.android.gms.internal.mlkit_translate.** { *; }
