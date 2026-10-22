# Local history is stored as JSON through Gson reflection (AnalysisHistoryCodec).
# Renaming or removing these fields would make history saved by an earlier version unreadable
# after an update, so the model classes and the history envelope keep their names and fields.
-keep class com.silverguard.app.model.** { *; }
-keep class com.silverguard.app.data.AnalysisHistoryCodec$Envelope { *; }

# Gson reads generic field types such as List<AnalysisHistoryEntry> from signatures.
-keepattributes Signature, InnerClasses, EnclosingMethod

# ML Kit lists its components in the manifest and creates them by reflection through their
# no-argument constructors. Without this, R8 full mode removes those constructors and OCR
# fails at runtime in release builds only.
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
