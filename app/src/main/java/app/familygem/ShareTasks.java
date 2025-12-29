package app.familygem;

import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.familygem.BuildConfig;
import app.familygem.Condivisione;
import app.familygem.R;
import app.familygem.Settings;
import app.familygem.U;

public class ShareTasks {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler uiHandler = new Handler(Looper.getMainLooper());

    public static void shareTree(Condivisione activity) {
        executor.execute(() -> {
            try {
                // Post Data
                URL url = java.net.URI.create("https://www.familygem.app/inserisci.php").toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                OutputStream out = new BufferedOutputStream(conn.getOutputStream());
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
                String dati = "password=" + URLEncoder.encode(BuildConfig.passwordAruba, "UTF-8") +
                        "&titoloAlbero=" + URLEncoder.encode(activity.tree.title, "UTF-8") +
                        "&nomeAutore=" + URLEncoder.encode(activity.nomeAutore, "UTF-8") +
                        "&accessibile=" + activity.accessible;
                writer.write(dati);
                writer.flush();
                writer.close();
                out.close();

                // Response
                BufferedReader lettore = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String linea1 = lettore.readLine();
                lettore.close();
                conn.disconnect();

                if (linea1 != null && linea1.startsWith("20")) {
                    activity.dataId = linea1.replaceAll("[-: ]", "");
                    Settings.Share share = new Settings.Share(activity.dataId, activity.idAutore);
                    activity.tree.addShare(share);
                    Global.settings.save();
                }

                uiHandler.post(() -> {
                    if (activity.dataId != null && activity.dataId.startsWith("20")) {
                        File fileTree = new File(activity.getCacheDir(), activity.dataId + ".zip");
                        if (activity.esporter.esportaBackupZip(activity.tree.shareRoot, 9, Uri.fromFile(fileTree))) {
                            uploadFtp(activity); // Continue directly to upload
                        } else {
                            Toast.makeText(activity, activity.esporter.messaggioErrore, Toast.LENGTH_LONG).show();
                            resetUI(activity);
                        }
                    } else {
                        // Un Toast di errore qui sostituirebbe il messaggio di toast() in catch()
                        resetUI(activity);
                    }
                });

            } catch (Exception e) {
                uiHandler.post(() -> {
                    U.toast(activity, e.getLocalizedMessage());
                    resetUI(activity);
                });
            }
        });
    }

    private static void uploadFtp(Condivisione activity) {
        executor.execute(() -> {
            try {
                FTPClient ftpClient = new FTPClient();
                ftpClient.connect("89.46.104.211", 21);
                ftpClient.enterLocalPassiveMode();
                ftpClient.login(BuildConfig.utenteAruba, BuildConfig.passwordAruba);
                ftpClient.changeWorkingDirectory("/www.familygem.app/condivisi");
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                BufferedInputStream buffIn;
                String nomeZip = activity.dataId + ".zip";
                buffIn = new BufferedInputStream(new FileInputStream(activity.getCacheDir() + "/" + nomeZip));
                activity.uploadSuccesso = ftpClient.storeFile(nomeZip, buffIn);
                buffIn.close();
                ftpClient.logout();
                ftpClient.disconnect();
            } catch (Exception e) {
                uiHandler.post(() -> U.toast(activity, e.getLocalizedMessage()));
            }

            uiHandler.post(() -> {
                if (activity.uploadSuccesso) {
                    Toast.makeText(activity, R.string.correctly_uploaded, Toast.LENGTH_SHORT).show();
                    activity.concludi();
                } else {
                    resetUI(activity);
                }
            });
        });
    }

    private static void resetUI(Condivisione activity) {
        activity.findViewById(R.id.bottone_condividi).setEnabled(true);
        activity.findViewById(R.id.condividi_circolo).setVisibility(View.INVISIBLE);
    }
}
