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
import com.familygem.restapi.models.Content;
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.User;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class GetMyReposTask {
    private static final String TAG = "GetMyRepoTask";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void execute(Context context, List<String> repoFullNames,
                               Consumer<List<FamilyGemTreeInfoModel>> afterExecution,
                               Consumer<String> errorExecution) {
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

                // get my repos (paginate)
                int page = 1;
                int perPage = 100;
                List<Repo> myRepos = new ArrayList<>();
                while (true) {
                    Response<List<Repo>> getMyReposResponse = apiInterface.getMyRepos(perPage, page).execute();
                    List<Repo> pageRepos = getMyReposResponse.body();
                    Log.d(TAG, "Repos response code (page " + page + "): " + getMyReposResponse.code());
                    Log.d(TAG, "Repos count (page " + page + "): " + (pageRepos == null ? "null" : pageRepos.size()));
                    if (pageRepos == null) {
                        if (page == 1) {
                            handler.post(() -> errorExecution.accept("Empty repo list from GitHub"));
                            return;
                        }
                        break;
                    }
                    myRepos.addAll(pageRepos);
                    if (pageRepos.size() < perPage) {
                        break;
                    }
                    page++;
                }
                List<FamilyGemTreeInfoModel> myTrees = new ArrayList<>();
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                for (Repo repo: myRepos) {
                    if (repo == null || repo.fullName == null) {
                        Log.d(TAG, "Skipping repo with null fullName");
                        continue;
                    }
                    boolean containsTarombo = repo.fullName.contains("tarombo");
                    boolean existsLocally = isExistInLocal(repoFullNames, repo.fullName);
                    Log.d(TAG, "Repo: " + repo.fullName + ", tarombo=" + containsTarombo + ", local=" + existsLocally);
                    if (containsTarombo && !existsLocally) {
                        String[] repoNameSegments = repo.fullName.split("/");
                        // get info.json
                        Call<Content> downloadInfoJsonCall = apiInterface.downloadFile(user.login, repoNameSegments[1], "info.json");
                        Response<Content> infoJsonContentResponse = downloadInfoJsonCall.execute();
                        Log.d(TAG, "info.json response for " + repo.fullName + ": " + infoJsonContentResponse.code());
                        if (infoJsonContentResponse.code() == 200) {
                            Content infoJsonContent = infoJsonContentResponse.body();
                            byte[] infoJsonContentBytes = Base64.decode(infoJsonContent.content, Base64.DEFAULT);
                            String infoJsonString = new String(infoJsonContentBytes, StandardCharsets.UTF_8);
                            FamilyGemTreeInfoModel treeInfoModel = gson.fromJson(infoJsonString, FamilyGemTreeInfoModel.class);
                            if (treeInfoModel.title != null) {
                                treeInfoModel.githubRepoFullName = repo.fullName;
                                myTrees.add(treeInfoModel);
                            } else {
                                Log.d(TAG, "info.json missing title for " + repo.fullName);
                            }
                        } else {
                            Log.d(TAG, "Skipping repo without info.json: " + repo.fullName);
                        }
                    }
                }
                handler.post(() -> afterExecution.accept(myTrees));
            }catch (Throwable ex) {
                Log.e(TAG, "GetMyRepoTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }

    private static boolean isExistInLocal(List<String> repoFullNames, String repoFullName) {
        for (String name : repoFullNames) {
            if (name.equals(repoFullName))
                return true;
        }
        return false;
    }
}
