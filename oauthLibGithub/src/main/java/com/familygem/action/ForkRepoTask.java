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
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.User;
import com.familygem.utility.FamilyGemTreeInfoModel;
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

public class ForkRepoTask {
    private static final String TAG = "ForkRepoTask";
    public static void execute(Context context, String repoFullName, int nextTreeId,
                               Runnable beforeExecution, Consumer<FamilyGemTreeInfoModel> afterExecution,
                               Consumer<String> errorExecution) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            // background thread
            try {
                handler.post(beforeExecution);
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
                assert user != null;
                if (repoNameSegments[0].equals(user.login)) {
                    handler.post(() -> errorExecution.accept("E001"));
                    return;
                }

                // call api fork repo
                Call<Repo> repoCall = apiInterface.forkUserRepo(repoNameSegments[0], repoNameSegments[1]);
                Response<Repo> repoResponse = repoCall.execute();
                Log.d(TAG, "repo response code:" + repoResponse.code());
                Repo repo = repoResponse.body();
                assert repo != null;
                Log.d(TAG, "repo full_name:" + repo.fullName);

                // save repo object to local json file [treeId].repo
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String jsonRepo = gson.toJson(repo);
                FileUtils.writeStringToFile(new File(context.getFilesDir(), nextTreeId + ".repo"), jsonRepo, "UTF-8");

                // download file tree.json
                Call<Content> downloadTreeJsonCall = apiInterface.downloadFile(user.login, repoNameSegments[1], "tree.json");
                Response<Content> treeJsonContentResponse = downloadTreeJsonCall.execute();
                Content treeJsonContent = treeJsonContentResponse.body();
                // save tree.json to local directory
                assert treeJsonContent != null;
                byte[] treeJsonContentBytes = Base64.decode(treeJsonContent.content, Base64.DEFAULT);
                String treeJsonString = new String(treeJsonContentBytes, StandardCharsets.UTF_8);
                File treeJsonFile = new File(context.getFilesDir(), nextTreeId + ".json");
                FileUtils.writeStringToFile(treeJsonFile, treeJsonString, "UTF-8");
                // save file content info to local json file [treeId].content
                treeJsonContent.content = null; // remove the content because it is too big and we dont need it
                String treeJsonContentInfo = gson.toJson(treeJsonContent);
                FileUtils.writeStringToFile(new File(context.getFilesDir(), nextTreeId + ".content"), treeJsonContentInfo, "UTF-8");

                // download file info.json
                Call<Content> downloadInfoJsonCall = apiInterface.downloadFile(user.login, repoNameSegments[1], "info.json");
                Response<Content> infoJsonContentResponse = downloadInfoJsonCall.execute();
                Content infoJsonContent = infoJsonContentResponse.body();
                // create treeInfoModel instance
                byte[] infoJsonContentBytes = Base64.decode(infoJsonContent.content, Base64.DEFAULT);
                String infoJsonString = new String(infoJsonContentBytes, StandardCharsets.UTF_8);
                FamilyGemTreeInfoModel treeInfoModel = gson.fromJson(infoJsonString, FamilyGemTreeInfoModel.class);
                // save info.json content meta to [treeId].info.content
                infoJsonContent.content = null; // remove content base64 string
                String jsonContentInfo = gson.toJson(infoJsonContent);
                FileUtils.writeStringToFile(new File(context.getFilesDir(), nextTreeId + ".info.content"), jsonContentInfo, "UTF-8");

                // get last commit
                Call<List<Commit>> commitsCall = apiInterface.getLatestCommit(user.login, repoNameSegments[1]);
                Response<List<Commit>> commitsResponse = commitsCall.execute();
                List<Commit> commits = commitsResponse.body();
                String commitStr = gson.toJson(commits.get(0));
                FileUtils.writeStringToFile(new File(context.getFilesDir(), nextTreeId + ".commit"), commitStr, "UTF-8");

                //UI Thread work here
                treeInfoModel.githubRepoFullName = repo.fullName;
                treeInfoModel.filePath = treeJsonFile.getAbsolutePath();
                treeInfoModel.repoStatus = "identical";
                treeInfoModel.aheadBy = 0;
                treeInfoModel.behindBy = 0;
                treeInfoModel.totalCommits = 0;
                handler.post(() -> afterExecution.accept(treeInfoModel));
            } catch (Throwable ex) {
                Log.e(TAG, "ForkRepoTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
