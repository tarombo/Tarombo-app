-keep class app.familygem.Settings, app.familygem.Chiesa, app.familygem.Podio # for R8.fullMode
-keepclassmembernames class app.familygem.Settings, app.familygem.Settings$Tree, app.familygem.Settings$Diagram, app.familygem.Settings$ZippedTree, app.familygem.Settings$Share { *; }
-keepclassmembers class org.folg.gedcom.model.* { *; }
#-keeppackagenames org.folg.gedcom.model # Gedcom parser lo chiama come stringa eppure funziona anche senza
-keepnames class org.slf4j.LoggerFactory
-keep class org.jdom.input.* { *; } # per GeoNames
-keepattributes LineNumberTable,SourceFile # per avere i numeri di linea corretti in Android vitals

#-printusage build/usage.txt # risorse che vengono rimosse
#-printseeds build/seeds.txt # entrypoints

-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

