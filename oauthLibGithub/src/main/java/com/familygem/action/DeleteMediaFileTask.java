package com.familygem.action;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

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
import java.util.concurrent.ExecutorService;

import retrofit2.Call;
import retrofit2.Response;

// upload media file silently
public class DeleteMediaFileTask {
    private static final String TAG = "DeleteMediaFileTask";
    public static void execute(Context context, final String repoFullName, final String email, int treeId,
                               Media media) {

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

                // parse media file name
                String filePath = media.getFile();
                if (filePath == null || filePath.isEmpty() )
                    return;

                String fileName = filePath.replace('\\', '/');
                if( fileName.lastIndexOf('/') > -1 ) {
                    fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
                }

                // get sha
                TreeResult baseTree = Helper.getBaseTreeCall(apiInterface, repoNameSegments[0], repoNameSegments[1]);
                TreeItem treeItem = Helper.findTreeItemMedia(apiInterface, repoNameSegments[0], repoNameSegments[1], baseTree, fileName);
                String shaTreeString = treeItem.sha;

                // request model
                FileRequestModel deleteMediaFileRequestModel = new FileRequestModel(
                        "delete media file " + fileName,
                        null,
                        new CommitterRequestModel(user.getUserName(), email)
                );
                deleteMediaFileRequestModel.sha = shaTreeString;
                Call<FileContent> deleteMediaFileCall = apiInterface.deleteMediaFile(repoNameSegments[0], repoNameSegments[1], "media/" + fileName, deleteMediaFileRequestModel);
                Response<FileContent> deleteMediaFileResponse = deleteMediaFileCall.execute();
                FileContent deleteMediaFileCommit = deleteMediaFileResponse.body();
                // get last commit
                Commit lastCommit = deleteMediaFileCommit.commit;
                if (deleteMediaFileResponse.code() == 409) {
                    // obsolete hash commit
                    FirebaseCrashlytics.getInstance().recordException(new Exception(context.getString(R.string.error_commit_hash_obsolete)));
                } else {
                    // get last commit
                    String commitStr = gson.toJson(lastCommit);
                    FileUtils.writeStringToFile(new File(context.getFilesDir(), treeId + ".commit"), commitStr, "UTF-8");
                }
            }catch (Throwable ex) {
                Log.e(TAG, "DeleteMediaFileTask is failed", ex);
                FirebaseCrashlytics.getInstance().recordException(ex);
            }
        });
    }
}
