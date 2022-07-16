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
import com.familygem.restapi.models.TreeResult;
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

public class RedownloadRepoTask {
    private static final String TAG = "RedownloadRepoTask";
    public static void execute(Context context, String repoFullName, int treeId,
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

                // get repo
                Call<Repo> getRepoCall = apiInterface.getRepo(repoNameSegments[0], repoNameSegments[1]);
                Response<Repo> repoResponse = getRepoCall.execute();
                Log.d(TAG, "repo response code:" + repoResponse.code());
                Repo repo = repoResponse.body();
                String jsonRepo = gson.toJson(repo);
                FileUtils.writeStringToFile(new File(context.getFilesDir(), treeId + ".repo"), jsonRepo, "UTF-8");

                // download file tree.json
                Content treeJsonContent = DownloadFileHelper.downloadFile(apiInterface,repoNameSegments[0], repoNameSegments[1], "tree.json");
                // save tree.json to local directory
                File treeJsonFile = new File(context.getFilesDir(), treeId + ".json");
                FileUtils.writeStringToFile(treeJsonFile, treeJsonContent.contentStr, "UTF-8");
                File treeJsonFileHead0 = new File(context.getFilesDir(), treeId + ".head_0");
                File treeJsonFileBehind0 = new File(context.getFilesDir(), treeId + ".behind_0");
                Helper.copySingleFile(treeJsonFile, treeJsonFileHead0);
                Helper.copySingleFile(treeJsonFile, treeJsonFileBehind0);
                // get the real one (if possible)
//                GetHead0Helper.execute(context, apiInterface, user, repo, repoFullName, treeId); >> not needed because we always compared to last merged upstream
                GetBehind0Helper.execute(context, apiInterface, user, repo, repoFullName, treeId);
                GetPRtoParentHelper.execute(context, apiInterface, user, repo, repoFullName, treeId);


                // remove [treeId].json.parent if exist (this is tree.json from parent repo)
                File treeJsonParent = new File(context.getFilesDir(), treeId + ".json.parent");
                if (treeJsonParent.exists())
                    treeJsonParent.delete();

                // download file info.json
                Content infoJsonContent = DownloadFileHelper.downloadFile(apiInterface,repoNameSegments[0], repoNameSegments[1], "info.json");
                // create treeInfoModel instance
                FamilyGemTreeInfoModel treeInfoModel = gson.fromJson(infoJsonContent.contentStr, FamilyGemTreeInfoModel.class);

                // download all media files
                TreeResult baseTree = Helper.getBaseTreeCall(apiInterface, repoNameSegments[0], repoNameSegments[1]);
                File dirMedia = Helper.getDirMedia(context, treeId);
                Helper.downloadAllMediaFiles(context, dirMedia, baseTree, apiInterface, repoNameSegments[0], repoNameSegments[1]);

                // get last commit
                Call<List<Commit>> commitsCall = apiInterface.getLatestCommit(repoNameSegments[0], repoNameSegments[1]);
                Response<List<Commit>> commitsResponse = commitsCall.execute();
                List<Commit> commits = commitsResponse.body();
                String commitStr = gson.toJson(commits.get(0));
                FileUtils.writeStringToFile(new File(context.getFilesDir(), treeId + ".commit"), commitStr, "UTF-8");

                //UI Thread work here
                treeInfoModel.githubRepoFullName = repoFullName;
                treeInfoModel.filePath = treeJsonFile.getAbsolutePath();
                treeInfoModel.isForked = repo.fork;

                handler.post(() -> afterExecution.accept(treeInfoModel));
            } catch (Throwable ex) {
                Log.e(TAG, "RedownloadRepoTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
