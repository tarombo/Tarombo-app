package com.familygem.action;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.util.Consumer;

import com.familygem.oauthLibGithub.BuildConfig;
import com.familygem.restapi.APIInterface;
import com.familygem.restapi.ApiClient;
import com.familygem.restapi.models.Commit;
import com.familygem.restapi.models.Content;
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.User;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class DownloadFilesOnlyTask {
    private static final String TAG = "DownloadFilesOnlyTask";
    public static void execute(Context context, String repoFullName, int treeId,
                               Consumer<FamilyGemTreeInfoModel> afterExecution,
                               Consumer<String> errorExecution) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            // background thread
            try {
                // prepare api
                APIInterface apiInterface = ApiClient.getClient(BuildConfig.GITHUB_BASE_URL, null).create(APIInterface.class);


                // check if the repo belongs to himself
                String[] repoNameSegments = repoFullName.split("/");
                Log.d(TAG, "owner:" + repoNameSegments[0] + " repo:" + repoNameSegments[1]);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();


                // download file tree.json
                Content treeJsonContent = DownloadFileHelper.downloadFile(apiInterface,repoNameSegments[0], repoNameSegments[1], "tree.json");
                // save tree.json to local directory
                File treeJsonFile = new File(context.getFilesDir(), treeId + ".json");
                FileUtils.writeStringToFile(treeJsonFile, treeJsonContent.contentStr, "UTF-8");

                // download file info.json
                Content infoJsonContent = DownloadFileHelper.downloadFile(apiInterface,repoNameSegments[0], repoNameSegments[1], "info.json");
                // create treeInfoModel instance
                FamilyGemTreeInfoModel treeInfoModel = gson.fromJson(infoJsonContent.contentStr, FamilyGemTreeInfoModel.class);

                treeInfoModel.filePath = treeJsonFile.getAbsolutePath();

                //UI Thread work here
                handler.post(() -> afterExecution.accept(treeInfoModel));
            } catch (Throwable ex) {
                Log.e(TAG, "DownloadFilesOnlyTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
