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
import com.familygem.restapi.models.FileContent;
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.User;
import com.familygem.restapi.requestmodels.CommitterRequestModel;
import com.familygem.restapi.requestmodels.CreateRepoRequestModel;
import com.familygem.restapi.requestmodels.FileRequestModel;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class CreateRepoTask {
    private static final String TAG = "CreateRepoTask";
    public static void execute(Context context, int treeId, final String email, FamilyGemTreeInfoModel treeInfoModel,
                               Runnable beforeExecution, Consumer<String> afterExecution,
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

                // generate repoName
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
                Date date = new Date();
                String repoName = "tarombo-" + user.login + "-" + formatter.format(date);

                // call api create repo and get response
                Call<Repo> repoCall = apiInterface.createUserRepo(new CreateRepoRequestModel(repoName, repoName));
                Response<Repo> repoResponse = repoCall.execute();
                Log.d(TAG, "repo response code:" + repoResponse.code());
                Repo repo = repoResponse.body();
                Log.d(TAG, "repo full_name:" + repo.fullName);
                treeInfoModel.githubRepoFullName = repo.fullName;

                // save repo object to local json file [treeId].repo
                Gson gson = new Gson();
                String jsonRepo = gson.toJson(repo);
                FileUtils.writeStringToFile(new File(context.getFilesDir(), treeId + ".repo"), jsonRepo, "UTF-8");

                // read [treeId].json file  and convert to base64
                File file = new File(context.getFilesDir(), treeId + ".json");
                int size = (int) file.length();
                byte[] bytes = new byte[size];
                BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                buf.read(bytes, 0, bytes.length);
                buf.close();
                String treeFileContentBase64 = Base64.encodeToString(bytes, Base64.DEFAULT);

                // upload tree.json file
                FileRequestModel createTreeFileRequestModel = new FileRequestModel(
                        "initial commit",
                        treeFileContentBase64,
                        new CommitterRequestModel(user.getUserName(), email)
                );
                Call<FileContent> createTreeFileCall = apiInterface.createFile(user.login, repoName, "tree.json", createTreeFileRequestModel);
                Response<FileContent> fileContentResponse = createTreeFileCall.execute();
                FileContent treeFileContent = fileContentResponse.body();
                // save file content info to local json file [treeId].content (for update operation)
                String jsonTreeContentInfo = gson.toJson(treeFileContent.content);
                FileUtils.writeStringToFile(new File(context.getFilesDir(), treeId + ".content"), jsonTreeContentInfo, "UTF-8");

                // upload info.json file
                treeInfoModel.media = 0; //currently we dont upload media
                String jsonInfo = gson.toJson(treeInfoModel);
                byte[] jsonInfoBytes = jsonInfo.getBytes(StandardCharsets.UTF_8);
                String jsonInfoBase64 = Base64.encodeToString(jsonInfoBytes, Base64.DEFAULT);
                FileRequestModel createJsonInfoRequestModel = new FileRequestModel(
                        "initial commit",
                        jsonInfoBase64,
                        new CommitterRequestModel(user.getUserName(), email)
                );
                Call<FileContent> createJsonInfoCall = apiInterface.createFile(user.login, repoName, "info.json", createJsonInfoRequestModel);
                Response<FileContent> jsonInfoContentResponse = createJsonInfoCall.execute();
                FileContent jsonInfoFileContent = jsonInfoContentResponse.body();
                // save info.json content file (for update operation)
                String jsonInfoContent = gson.toJson(jsonInfoFileContent.content);
                FileUtils.writeStringToFile(new File(context.getFilesDir(), treeId + ".info.content"), jsonInfoContent, "UTF-8");

                // get last commit
                Call<List<Commit>> commitsCall = apiInterface.getLatestCommit(user.login, repoName);
                Response<List<Commit>> commitsResponse = commitsCall.execute();
                List<Commit> commits = commitsResponse.body();
                String commitStr = gson.toJson(commits.get(0));
                FileUtils.writeStringToFile(new File(context.getFilesDir(), treeId + ".commit"), commitStr, "UTF-8");

                // generate deeplink
                final String deeplinkUrl = Helper.generateDeepLink(repoName);

                //UI Thread work here
                handler.post(() -> afterExecution.accept(deeplinkUrl));
            } catch (Exception ex) {
                Log.e(TAG, "CreateRepoTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
