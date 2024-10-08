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
import com.familygem.restapi.APIInterface;
import com.familygem.restapi.ApiClient;
import com.familygem.restapi.models.Commit;
import com.familygem.restapi.models.Content;
import com.familygem.restapi.models.TreeResult;
import com.familygem.restapi.models.User;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class MergePRTask {
    private static final String TAG = "MergePRTask";
    public static void execute(Context context, String repoFullName, int treeId, int prNumber,
                               Consumer<FamilyGemTreeInfoModel> afterExecution,
                               Consumer<String> errorExecution) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            // background thread
            try {
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

                // merge PR info
                Call<Void> mergePrCall = apiInterface.mergePR(user.login, repoNameSegments[1], prNumber);
                Response<Void> mergePrResponse = mergePrCall.execute();
                if (mergePrResponse.code() == 200) {
                    // redownload everything
                    // download file tree.json
                    Content treeJsonContent = DownloadFileHelper.downloadFile(apiInterface, user.login, repoNameSegments[1], "tree.json");
                    // save tree.json to local directory
                    File treeJsonFile = new File(context.getFilesDir(), treeId + ".json");
                    FileUtils.writeStringToFile(treeJsonFile, treeJsonContent.contentStr, "UTF-8");
                    File treeJsonFileHead0 = new File(context.getFilesDir(), treeId + ".head_0");
                    File treeJsonFileBehind0 = new File(context.getFilesDir(), treeId + ".behind_0");
                    Helper.copySingleFile(treeJsonFile, treeJsonFileHead0);
                    Helper.copySingleFile(treeJsonFile, treeJsonFileBehind0);
                    // remove [treeId].json.parent if exist (this is tree.json from parent repo)
                    File treeJsonParent = new File(context.getFilesDir(), treeId + ".json.parent");
                    if (treeJsonParent.exists())
                        treeJsonParent.delete();

                    // download file info.json
                    Content infoJsonContent = DownloadFileHelper.downloadFile(apiInterface, user.login, repoNameSegments[1], "info.json");
                    // create treeInfoModel instance
                    FamilyGemTreeInfoModel treeInfoModel = gson.fromJson(infoJsonContent.contentStr, FamilyGemTreeInfoModel.class);

                    // re-download all media files
                    TreeResult baseTree = Helper.getBaseTreeCall(apiInterface, user.login, repoNameSegments[1]);
                    File dirMedia = Helper.getDirMedia(context, treeId);
                    Helper.downloadAllMediaFiles(context, dirMedia, baseTree, apiInterface, repoNameSegments[0], repoNameSegments[1]);

                    // get last commit
                    Call<List<Commit>> commitsCall = apiInterface.getLatestCommit(user.login, repoNameSegments[1]);
                    Response<List<Commit>> commitsResponse = commitsCall.execute();
                    List<Commit> commits = commitsResponse.body();
                    String commitStr = gson.toJson(commits.get(0));
                    FileUtils.writeStringToFile(new File(context.getFilesDir(), treeId + ".commit"), commitStr, "UTF-8");

                    treeInfoModel.githubRepoFullName = repoFullName;
                    treeInfoModel.filePath = treeJsonFile.getAbsolutePath();
                    handler.post(() -> afterExecution.accept(treeInfoModel));
                } else {
                    handler.post(() -> errorExecution.accept("Failed to merge PR"));
                }
            } catch (Throwable ex) {
                Log.e(TAG, "MergePRTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
