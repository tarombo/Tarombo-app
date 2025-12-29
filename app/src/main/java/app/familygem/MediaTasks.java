package app.familygem;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.folg.gedcom.model.Media;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Helper class to replace AsyncTask
public class MediaTasks {
    public static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler uiHandler = new Handler(Looper.getMainLooper());

    public static void cacheImage(Media media, URL url) {
        executor.execute(() -> {
            try {
                File cartellaCache = new File(Global.context.getCacheDir().getPath() + "/" + Global.settings.openTree);
                if (!cartellaCache.exists()) {
                    // Elimina extension "cache" da tutti i Media
                    // Note: This logic was in the original AsyncTask. It modifies shared state
                    // (Global.gc) which might be risky if not synchronized.
                    // But we follow the original logic here.
                    // Accessing Global.gc from background thread might be unsafe if UI is modifying
                    // it.
                    // However, we'll keep it as is for now as a direct translation.
                    app.familygem.visitors.MediaList visitaMedia = new app.familygem.visitors.MediaList(Global.gc, 0);
                    Global.gc.accept(visitaMedia);
                    for (Media m : visitaMedia.list)
                        if (m.getExtension("cache") != null)
                            m.putExtension("cache", null);
                    cartellaCache.mkdir();
                }
                String estensione = FilenameUtils.getName(url.getPath());
                if (estensione.lastIndexOf('.') > 0)
                    estensione = estensione.substring(estensione.lastIndexOf('.') + 1);
                String ext;
                switch (estensione) {
                    case "png":
                        ext = "png";
                        break;
                    case "gif":
                        ext = "gif";
                        break;
                    case "bmp":
                        ext = "bmp";
                        break;
                    case "jpg":
                    case "jpeg":
                    default:
                        ext = "jpg";
                }
                File cache = F.getUniqueFile(cartellaCache.getPath(), "img." + ext);
                FileUtils.copyURLToFile(url, cache);

                String path = cache.getPath();
                if (path != null) {
                    uiHandler.post(() -> media.putExtension("cache", path));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void downloadMediaImage(ImageView vistaImmagine, ProgressBar circo, Media media, String urlString) {
        final int vistaImmagineWidth = vistaImmagine.getWidth();
        executor.execute(() -> {
            Bitmap bitmap = null;
            int tagTipoFile = 0;
            URL finalUrl = null;
            try {
                Connection connessione = Jsoup.connect(urlString);
                Document doc = connessione.get();
                List<Element> lista = doc.select("img");
                if (lista.isEmpty()) {
                    tagTipoFile = 3;
                    finalUrl = java.net.URI.create(urlString).toURL();
                    // generateIcon needs UI thread or Context? It uses vista.getContext()
                    // So we must do this on UI thread or pass context.
                    // But generateIcon returns a Bitmap. "createBitmap" should be fine on bg
                    // thread?
                    // No, it uses LayoutInflater which needs Looper.
                    // We'll handle this case specially on UI thread or post it.
                } else {
                    int maxDimensioniConAlt = 1;
                    int maxDimensioni = 1;
                    int maxLunghezzaAlt = 0;
                    int maxLunghezzaSrc = 0;
                    Element imgGrandeConAlt = null;
                    Element imgGrande = null;
                    Element imgAltLungo = null;
                    Element imgSrcLungo = null;
                    for (Element img : lista) {
                        int larga, alta;
                        if (img.attr("width").isEmpty())
                            larga = 1;
                        else
                            larga = Integer.parseInt(img.attr("width"));
                        if (img.attr("height").isEmpty())
                            alta = 1;
                        else
                            alta = Integer.parseInt(img.attr("height"));
                        if (larga * alta > maxDimensioniConAlt && !img.attr("alt").isEmpty()) {
                            imgGrandeConAlt = img;
                            maxDimensioniConAlt = larga * alta;
                        }
                        if (larga * alta > maxDimensioni) {
                            imgGrande = img;
                            maxDimensioni = larga * alta;
                        }
                        if (img.attr("alt").length() > maxLunghezzaAlt) {
                            imgAltLungo = img;
                            maxLunghezzaAlt = img.attr("alt").length();
                        }
                        if (img.attr("src").length() > maxLunghezzaSrc) {
                            imgSrcLungo = img;
                            maxLunghezzaSrc = img.attr("src").length();
                        }
                    }
                    String percorso = null;
                    if (imgGrandeConAlt != null)
                        percorso = imgGrandeConAlt.absUrl("src");
                    else if (imgGrande != null)
                        percorso = imgGrande.absUrl("src");
                    else if (imgAltLungo != null)
                        percorso = imgAltLungo.absUrl("src");
                    else if (imgSrcLungo != null)
                        percorso = imgSrcLungo.absUrl("src");

                    finalUrl = java.net.URI.create(percorso).toURL();
                    InputStream inputStream = finalUrl.openConnection().getInputStream();
                    BitmapFactory.Options opzioni = new BitmapFactory.Options();
                    opzioni.inJustDecodeBounds = true;
                    BitmapFactory.decodeStream(inputStream, null, opzioni);
                    if (opzioni.outWidth > vistaImmagineWidth)
                        opzioni.inSampleSize = opzioni.outWidth / (vistaImmagineWidth + 1);
                    inputStream = finalUrl.openConnection().getInputStream();
                    opzioni.inJustDecodeBounds = false;
                    bitmap = BitmapFactory.decodeStream(inputStream, null, opzioni);
                    tagTipoFile = 1;
                }
            } catch (Exception e) {
                // Returns null bitmap
            }

            final Bitmap finalBitmap = bitmap;
            final int finalTagTipoFile = tagTipoFile;
            final URL resultUrl = finalUrl;

            uiHandler.post(() -> {
                vistaImmagine.setTag(R.id.tag_tipo_file, finalTagTipoFile);
                if (finalBitmap != null) {
                    vistaImmagine.setImageBitmap(finalBitmap);
                    vistaImmagine.setTag(R.id.tag_path, resultUrl.toString());
                    if (finalTagTipoFile == 1) {
                        cacheImage(media, resultUrl);
                    }
                } else if (finalTagTipoFile == 3 && resultUrl != null) {
                    // Handle the icon generation case
                    Bitmap icon = F.generateIcon(vistaImmagine, R.layout.media_mondo, resultUrl.getProtocol());
                    vistaImmagine.setImageBitmap(icon);
                    // original code didn't set tag_path here but it returned a bitmap
                }

                if (circo != null)
                    circo.setVisibility(View.GONE);
            });
        });
    }
}
