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
import com.familygem.restapi.models.Content;
import com.familygem.restapi.models.FileContent;
import com.familygem.restapi.models.User;
import com.familygem.restapi.requestmodels.CommitterRequestModel;
import com.familygem.restapi.requestmodels.FileRequestModel;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;

import retrofit2.Call;
import retrofit2.Response;

public class SaveInfoFileTask {
    private static final String TAG = "SaveInfoFileTask";
    public static void execute(Context context, final String repoFullName, final String email, int treeId, FamilyGemTreeInfoModel treeInfoModel,
                               Runnable beforeExecution, Runnable afterExecution,
                               Consumer<String> errorExecution) {

        Handler handler = new Handler(Looper.getMainLooper());
        final ExecutorService executor = ExecutorSingleton.getInstance().getExecutor();
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
                File userFile = new File(context.getFilesDir(), "user.json");
                User user = Helper.getUser(userFile);

                // check if the repo belongs to himself
                String[] repoNameSegments = repoFullName.split("/");
                Log.d(TAG, "owner:" + repoNameSegments[0] + " repo:" + repoNameSegments[1]);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();

                // download file info.json
                Content infoJsonContent = DownloadFileHelper.downloadFile(apiInterface, repoNameSegments[0], repoNameSegments[1], "info.json");
                // create treeInfoModel instance
                FamilyGemTreeInfoModel treeInfoModelInServer = gson.fromJson(infoJsonContent.contentStr, FamilyGemTreeInfoModel.class);
                if (treeInfoModelInServer.generations == treeInfoModel.generations
                        && treeInfoModelInServer.grade == treeInfoModel.grade
                        && treeInfoModelInServer.persons == treeInfoModel.persons
                        && ((treeInfoModelInServer.root == null && treeInfoModel.root == null)
                        || (treeInfoModelInServer.root != null && treeInfoModelInServer.root.equals(treeInfoModel.root)))
                        && treeInfoModelInServer.title.equals(treeInfoModel.title)
                ) {

                    handler.post(afterExecution);
                    // the file is the same just return dont save to server
                    return;
                }

                // get sha string for info.json
                File fileContent = new File(context.getFilesDir(), treeId + ".info.content");
                Content contentInfo = Helper.getContent(fileContent);
                String shaString = contentInfo.sha;

                // upload info.json file
                treeInfoModel.media = 0; //currently we dont upload media
                String jsonInfo = gson.toJson(treeInfoModel);
                byte[] jsonInfoBytes = jsonInfo.getBytes(StandardCharsets.UTF_8);
                String jsonInfoBase64 = Base64.encodeToString(jsonInfoBytes, Base64.DEFAULT);
                FileRequestModel replaceJsonInfoRequestModel = new FileRequestModel(
                        "save info",
                        jsonInfoBase64,
                        new CommitterRequestModel(user.getUserName(), email)
                );
                replaceJsonInfoRequestModel.sha = shaString;
                Call<FileContent> replaceJsonInfoCall = apiInterface.replaceFile(repoNameSegments[0], repoNameSegments[1],
                        "info.json", replaceJsonInfoRequestModel);
                Response<FileContent> jsonInfoContentResponse = replaceJsonInfoCall.execute();
                if (jsonInfoContentResponse.code() == 409) {
                    handler.post(() -> errorExecution.accept(context.getString(R.string.error_commit_hash_obsolete)));
                } else {
                    FileContent jsonInfoFileContent = jsonInfoContentResponse.body();
                    // save info.json content file (for update operation)
                    String jsonInfoContent = gson.toJson(jsonInfoFileContent.content);
                    FileUtils.writeStringToFile(fileContent, jsonInfoContent, "UTF-8");

                    // get last commit
                    Call<List<Commit>> commitsCall = apiInterface.getLatestCommit(repoNameSegments[0], repoNameSegments[1]);
                    Response<List<Commit>> commitsResponse = commitsCall.execute();
                    List<Commit> commits = commitsResponse.body();
                    String commitStr = gson.toJson(commits.get(0));
                    FileUtils.writeStringToFile(new File(context.getFilesDir(), treeId + ".commit"), commitStr, "UTF-8");

                    handler.post(afterExecution);
                }
            }catch (Throwable ex) {
                Log.e(TAG, "ReplaceInfoFileTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
