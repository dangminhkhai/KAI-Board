# ML Kit publishes its consumer rules. Keep only application entry points that
# Android instantiates by class name; R8 can optimize the remaining code.
-keep class vn.kai.board.MainActivity { *; }
-keep class vn.kai.board.ime.KaiBoardImeService { *; }
-keep class vn.kai.board.voice.VoiceInputActivity { *; }
-keep class vn.kai.board.translation.TranslationActivity { *; }
-keep class vn.kai.board.translation.TranslationModelsActivity { *; }
-keep class vn.kai.board.ClipboardManagerActivity { *; }
-keep class vn.kai.board.LearnedWordsActivity { *; }

# ML Kit discovers registrars and component dependencies at runtime. Keeping
# only Translate classes lets R8 merge common component interfaces, causing
# MlKitInitProvider to fail before Application.onCreate on some OEM builds.
-keepattributes Signature,RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault
-keep class com.google.mlkit.** { *; }
-keep class com.google.firebase.components.** { *; }
-keep class com.google.android.gms.internal.mlkit_translate.** { *; }
