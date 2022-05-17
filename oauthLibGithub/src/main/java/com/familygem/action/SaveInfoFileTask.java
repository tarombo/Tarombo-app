package com.familygem.action;

import static android.content.Context.MODE_PRIVATE;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.core.util.Consumer;

import com.familygem.oauthLibGithub.BuildConfig;
import com.familygem.restapi.APIInterface;
import com.familygem.restapi.ApiClient;
import com.familygem.restapi.models.Content;
import com.familygem.restapi.models.FileContent;
import com.familygem.restapi.models.User;
import com.familygem.restapi.requestmodels.CommitterRequestModel;
import com.familygem.restapi.requestmodels.FileRequestModel;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class SaveInfoFileTask {
    private static final String TAG = "SaveInfoFileTask";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void execute(Context context, final String repoFullName, final String email, int treeId, FamilyGemTreeInfoModel treeInfoModel,
                               Runnable beforeExecution, Runnable afterExecution,
                               Consumer<String> errorExecution) {

        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            // background thread
            try {
                handler.post(beforeExecution);

                if (repoFullName == null || "".equals(repoFullName)) {
                    handler.post(afterExecution);
                    return;
                }
                // prepare api
                SharedPreferences prefs = context.getSharedPreferences("github_prefs", MODE_PRIVATE);
                String oauthToken = prefs.getString("oauth_token", null);
                APIInterface apiInterface = ApiClient.getClient(BuildConfig.GITHUB_BASE_URL, oauthToken).create(APIInterface.class);

                // get username API /user
                Call<User> userInfoCall = apiInterface.doGeMyUserInfo();
                Response<User> userResponse = userInfoCall.execute();
                User user = userResponse.body();

                // check if the repo belongs to himself
                String[] repoNameSegments = repoFullName.split("/");
                Log.d(TAG, "owner:" + repoNameSegments[0] + " repo:" + repoNameSegments[1]);

                // get sha string for info.json
                File fileContent = new File(context.getFilesDir(), treeId + ".info.content");
                Content contentInfo = Helper.getContent(fileContent);
                String shaString = contentInfo.sha;

                // upload info.json file
                Gson gson = new Gson();
                treeInfoModel.media = 0; //currently we dont upload media
                String jsonInfo = gson.toJson(treeInfoModel);
                byte[] jsonInfoBytes = jsonInfo.getBytes(StandardCharsets.UTF_8);
                String jsonInfoBase64 = Base64.encodeToString(jsonInfoBytes, Base64.DEFAULT);
                FileRequestModel replaceJsonInfoRequestModel = new FileRequestModel(
                        "save info",
                        jsonInfoBase64,
                        new CommitterRequestModel(user.name, email)
                );
                replaceJsonInfoRequestModel.sha = shaString;
                Call<FileContent> replaceJsonInfoCall = apiInterface.replaceFile(user.login, repoNameSegments[1],
                        "info.json", replaceJsonInfoRequestModel);
                Response<FileContent> jsonInfoContentResponse = replaceJsonInfoCall.execute();
                FileContent jsonInfoFileContent = jsonInfoContentResponse.body();
                // save info.json content file (for update operation)
                String jsonInfoContent = gson.toJson(jsonInfoFileContent.content);
                FileUtils.writeStringToFile(fileContent, jsonInfoContent, "UTF-8");

                handler.post(afterExecution);
            }catch (Throwable ex) {
                Log.e(TAG, "ReplaceInfoFileTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
