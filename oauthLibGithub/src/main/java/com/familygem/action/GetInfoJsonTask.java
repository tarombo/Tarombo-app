package com.familygem.action;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.util.Consumer;

import com.familygem.oauthLibGithub.BuildConfig;
import com.familygem.restapi.APIInterface;
import com.familygem.restapi.ApiClient;
import com.familygem.restapi.models.Content;
import com.familygem.restapi.models.TreeResult;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GetInfoJsonTask {
    private static final String TAG = "GetInfoJsonTask";
    public static void execute(String repoFullName,
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

                // download file info.json
                Content infoJsonContent = DownloadFileHelper.downloadFile(apiInterface,repoNameSegments[0], repoNameSegments[1], "info.json");
                // create treeInfoModel instance
                FamilyGemTreeInfoModel treeInfoModel = gson.fromJson(infoJsonContent.contentStr, FamilyGemTreeInfoModel.class);

                //UI Thread work here
                handler.post(() -> afterExecution.accept(treeInfoModel));
            } catch (Throwable ex) {
                Log.e(TAG, TAG + " is Failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
