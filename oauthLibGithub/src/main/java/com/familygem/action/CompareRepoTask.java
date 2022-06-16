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
import com.familygem.restapi.models.CompareCommit;
import com.familygem.restapi.models.Pull;
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.User;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

// compare forked repo to parent repo
public class CompareRepoTask {
    private static final String TAG = "CompareRepoTask";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void execute(Context context, final String repoFullName, int treeId, FamilyGemTreeInfoModel treeInfoModel,
                               Runnable afterExecution,
                               Consumer<String> errorExecution) {

        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            // background thread
            try {

                if (repoFullName == null || "".equals(repoFullName)) {
                    handler.post(afterExecution);
                    return;
                }
                String[] forkedRepoNameSegments = repoFullName.split("/");

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

                Repo repo = Helper.getRepo(new File(context.getFilesDir(), treeId + ".repo"));
                String[] repoParentNameSegments = repo.parent.fullName.split("/");

                if (repo.fork && repo.parent != null) {
                    String[] repoParentSegments = repo.parent.fullName.split("/");
                    // compare with original repo
                    String basehead = repoParentSegments[0] + ":main...main";
                    Call<CompareCommit> compareCommitCall = apiInterface.compareCommit(user.login, repoNameSegments[1], basehead);
                    Response<CompareCommit> compareCommitResponse = compareCommitCall.execute();
                    CompareCommit compareCommit = compareCommitResponse.body();
                    treeInfoModel.repoStatus = compareCommit.status;
                    treeInfoModel.aheadBy = compareCommit.aheadBy;
                    treeInfoModel.behindBy = compareCommit.behindBy;
                    treeInfoModel.totalCommits = compareCommit.totalCommits;

                    File prToParentfile = new File(context.getFilesDir(), treeId + ".PRtoParent");

                    // if aheadBy = 0 -> remove .PRtoParent  (if exist) also update Settings.json
                    if (compareCommit.aheadBy == 0) {
                        if (prToParentfile.exists()) {
                            // automatically syncwithparent and redownload if behindBy = 1 and PR is merged
                            if (compareCommit.behindBy == 1) {
                                Pull prLocal = Helper.getPR(prToParentfile);
                                Call<Pull> getPrCall = apiInterface.getPR(repoParentNameSegments[0], forkedRepoNameSegments[1], prLocal.number);
                                Response<Pull> getPrResponse = getPrCall.execute();
                                Pull prServer = getPrResponse.body();
                                if (prServer.mergedAt != null) {
                                    // already merged
                                    treeInfoModel.behindBy = 0;
                                    treeInfoModel.repoStatus = "identical";
                                    SyncWithParentTask.execute(context,repo.fullName, treeId, mergeable ->{
                                        if (mergeable)
                                            RedownloadRepoTask.execute(context, repo.fullName, treeId, info ->{}, error -> {});
                                    }, error -> {});
                                }
                            }
                            prToParentfile.delete();
                        }
                        treeInfoModel.submittedPRtoParent = false;
                        treeInfoModel.submittedPRtoParentMergeable = false;
                    } else if (compareCommit.aheadBy > 0 && prToParentfile.exists()) {
                        // get PR file
                        Pull pullLocal = Helper.getPR(prToParentfile);

                        // check status of pull request
                        Call<Pull> getPrCall = apiInterface.getPR(repoParentNameSegments[0], forkedRepoNameSegments[1], pullLocal.number);
                        Response<Pull> getPrResponse = getPrCall.execute();
                        Pull pullServer = getPrResponse.body();
                        if ("open".equals(pullServer.state)) {
                            treeInfoModel.submittedPRtoParent = true;
                            treeInfoModel.submittedPRtoParentMergeable = pullServer.mergeable;
                        } else if ("closed".equals(pullServer.state)) {
                            if (pullServer.mergedAt == null) {
                                // PR is closed most likely is rejected
                                treeInfoModel.submittedPRtoParent = true; // but rejected but already closed without merge
                                treeInfoModel.submittedPRtoParentRejected = true;
                                treeInfoModel.submittedPRtoParentMergeable = pullServer.mergeable;
                            } else {
                                // PR is closed because alread merged
                                treeInfoModel.submittedPRtoParent = false;
                                treeInfoModel.submittedPRtoParentRejected = false;
                                treeInfoModel.submittedPRtoParentMergeable = false;
                            }
                        }
                    }
                    // if behindBy = 0
                    if (compareCommit.behindBy == 0) {
                        File prfile = new File(context.getFilesDir(), treeId + ".PRfromParent");
                        if (prfile.exists())
                            prfile.delete();
                        treeInfoModel.submittedPRfromParent = false;
                        treeInfoModel.submittedPRfromParentMergeable = false;
                    }

                }

                handler.post(afterExecution);
            }catch (Throwable ex) {
                Log.e(TAG, "CompareRepoTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
