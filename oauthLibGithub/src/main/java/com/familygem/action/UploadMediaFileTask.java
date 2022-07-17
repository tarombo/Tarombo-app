package com.familygem.action;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.core.util.Consumer;

import com.familygem.oauthLibGithub.BuildConfig;
import com.familygem.oauthLibGithub.R;
import com.familygem.restapi.APIInterface;
import com.familygem.restapi.ApiClient;
import com.familygem.restapi.models.Commit;
import com.familygem.restapi.models.FileContent;
import com.familygem.restapi.models.TreeItem;
import com.familygem.restapi.models.TreeResult;
import com.familygem.restapi.models.User;
import com.familygem.restapi.requestmodels.CommitterRequestModel;
import com.familygem.restapi.requestmodels.FileRequestModel;
import com.familygem.utility.Helper;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.Media;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;

import retrofit2.Call;
import retrofit2.Response;

// upload media file silently
public class UploadMediaFileTask {
    private static final String TAG = "UploadMediaFileTask";
    public static void execute(Context context, final String repoFullName, final String email, int treeId,
                               Media media, File fileMedia) {

//        Handler handler = new Handler(Looper.getMainLooper());
        final ExecutorService executor = ExecutorSingleton.getInstance().getExecutor();
        executor.execute(() -> {
            // background thread
            try {
                if (repoFullName == null || "".equals(repoFullName)) {
                    return;
                }

                // prepare api
                SharedPreferences prefs = context.getSharedPreferences("github_prefs", MODE_PRIVATE);
                String oauthToken = prefs.getString("oauth_token", null);
                APIInterface apiInterface = ApiClient.getClient(BuildConfig.GITHUB_BASE_URL, oauthToken).create(APIInterface.class);

                // get username API /user
                File userFile = new File(context.getFilesDir(), "user.json");
                User user = Helper.getUser(userFile);

                // parset repo full name
                String[] repoNameSegments = repoFullName.split("/");
                Log.d(TAG, "owner:" + repoNameSegments[0] + " repo:" + repoNameSegments[1]);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                // get media file bytes
                // parse media file name
                String filePath = media.getFile();
                if (filePath == null || filePath.isEmpty() )
                    return;

                String fileName = filePath.replace('\\', '/');
                if( fileName.lastIndexOf('/') > -1 ) {
                    fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                }

                if (!fileMedia.exists()) {
                    return;
                }
                int size = (int) fileMedia.length();
                byte[] bytes = new byte[size];
                BufferedInputStream buf = new BufferedInputStream(new FileInputStream(fileMedia));
                buf.read(bytes, 0, bytes.length);
                buf.close();

                // media file
                String mediaBase64 = Base64.encodeToString(bytes, Base64.DEFAULT);
                FileRequestModel createReadmeRequestModel = new FileRequestModel(
                        "save media file " + fileName,
                        mediaBase64,
                        new CommitterRequestModel(user.getUserName(), email)
                );
                Call<FileContent> createMediaFileCall = apiInterface.createFile(repoNameSegments[0], repoNameSegments[1], "media/" + fileName, createReadmeRequestModel);
                Response<FileContent> createReadmeFileResponse = createMediaFileCall.execute();
                FileContent createReadmeCommit = createReadmeFileResponse.body();
                // get last commit
                Commit lastCommit = createReadmeCommit.commit;
                if (createReadmeFileResponse.code() == 409) {
                    // obsolete hash commit
                    FirebaseCrashlytics.getInstance().recordException(new Exception(context.getString(R.string.error_commit_hash_obsolete)));
                } else {
                    // get last commit
                    String commitStr = gson.toJson(lastCommit);
                    FileUtils.writeStringToFile(new File(context.getFilesDir(), treeId + ".commit"), commitStr, "UTF-8");
                }
            }catch (Throwable ex) {
                Log.e(TAG, "UploadMediaFileTask is failed", ex);
                FirebaseCrashlytics.getInstance().recordException(ex);
            }
        });
    }
}
