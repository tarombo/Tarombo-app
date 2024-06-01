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
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.TreeItem;
import com.familygem.restapi.models.TreeResult;
import com.familygem.restapi.models.User;
import com.familygem.restapi.requestmodels.CommitterRequestModel;
import com.familygem.restapi.requestmodels.CreateRepoRequestModel;
import com.familygem.restapi.requestmodels.FileRequestModel;
import com.familygem.utility.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;

import retrofit2.Call;
import retrofit2.Response;

public class SaveTreeFileTask {
    private static final String TAG = "SaveTreeFileTask";
    public static void execute(Context context, final String repoFullName, final String email, int treeId,
                               String gcJsonString, String privateJsonStr, String title,
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

                // upload tree.json
                TreeResult baseTree = Helper.getBaseTreeCall(apiInterface, repoNameSegments[0], repoNameSegments[1]);
                TreeItem treeItem = Helper.findTreeItem(baseTree, "tree.json");
                String shaTreeString = treeItem.sha;
                String treeFileContentBase64 = Base64.encodeToString(gcJsonString.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
                FileRequestModel replaceTreeJsonRequestModel = new FileRequestModel(
                        "save data tree.json",
                        treeFileContentBase64,
                        new CommitterRequestModel(user.getUserName(), email)
                );
                replaceTreeJsonRequestModel.sha = shaTreeString;
                Call<FileContent> replaceTreeJsonCall = apiInterface.replaceFile(repoNameSegments[0], repoNameSegments[1],
                        "tree.json", replaceTreeJsonRequestModel);
                Response<FileContent> treeJsonResponse = replaceTreeJsonCall.execute();
                if (treeJsonResponse.code() == 409) {
                    RefreshRepoTask.refreshRepo(context, apiInterface, repoNameSegments[0], repoNameSegments[1], treeId);
                    handler.post(() -> errorExecution.accept(context.getString(R.string.error_commit_hash_obsolete)));
                } else {
                    // get last commit
                    Call<List<Commit>> commitsCall = apiInterface.getLatestCommit(repoNameSegments[0], repoNameSegments[1]);
                    Response<List<Commit>> commitsResponse = commitsCall.execute();
                    List<Commit> commits = commitsResponse.body();
                    String commitStr = gson.toJson(commits.get(0));
                    FileUtils.writeStringToFile(new File(context.getFilesDir(), treeId + ".commit"), commitStr, "UTF-8");

                    // upload private.json (if needed) to private repo
                    if (privateJsonStr != null) {
                        // get base tree of private repo
                        TreeResult privateBaseTree = Helper.retrievePrivateBaseTree(context, treeId, apiInterface,
                                user, email, repoNameSegments, title);
                        TreeItem privateTreeItem = Helper.findTreeItem(privateBaseTree, "tree-private.json");
                        // create or update tree-private.json
                        String privateTreeFileContentBase64 = Base64.encodeToString(privateJsonStr.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT);
                        FileRequestModel privateTreeRequestModel = new FileRequestModel(
                                "save private-tree",
                                privateTreeFileContentBase64,
                                new CommitterRequestModel(user.getUserName(), email)
                        );
                        if (privateTreeItem != null) {
                            privateTreeRequestModel.sha = privateTreeItem.sha;
                            Call<FileContent> privateTreeJsonCall = apiInterface.replaceFile(repoNameSegments[0], repoNameSegments[1] + "-private",
                                    "tree-private.json", privateTreeRequestModel);
                            privateTreeJsonCall.execute();
                        } else {
                            Call<FileContent> privateTreeJsonCall = apiInterface.createFile(repoNameSegments[0], repoNameSegments[1] + "-private",
                                    "tree-private.json", privateTreeRequestModel);
                            privateTreeJsonCall.execute();
                        }
                    }

                    RefreshRepoTask.refreshRepo(context, apiInterface, repoNameSegments[0], repoNameSegments[1], treeId);
                    handler.post(afterExecution);
                }
            }catch (Throwable ex) {
                Log.e(TAG, "SaveTreeAndInfoFileTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
