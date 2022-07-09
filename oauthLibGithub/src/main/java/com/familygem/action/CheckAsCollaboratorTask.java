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
import com.familygem.restapi.models.Invitation;
import com.familygem.restapi.models.User;
import com.familygem.utility.Helper;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class CheckAsCollaboratorTask {
    private static final String TAG = "CheckAsCollaboratorTask";
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static void execute(Context context, final String repoFullName,
                               Consumer<Boolean> afterExecution,
                               Consumer<String> errorExecution) {
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                // prepare api
                SharedPreferences prefs = context.getSharedPreferences("github_prefs", MODE_PRIVATE);
                String oauthToken = prefs.getString("oauth_token", null);
                APIInterface apiInterface = ApiClient.getClient(BuildConfig.GITHUB_BASE_URL, oauthToken).create(APIInterface.class);

                // get username API /user
                File userFile = new File(context.getFilesDir(), "user.json");
                User user = Helper.getUser(userFile);

                String[] repoNameSegments = repoFullName.split("/");
                Log.d(TAG, "owner:" + repoNameSegments[0] + " repo:" + repoNameSegments[1]);

                // check if already collaborator or not
                Call<Void> checkCollaboratorCall = apiInterface.checkCollaborator(repoNameSegments[0], repoNameSegments[1], user.login);
                Response<Void> responseCheckCollaborator = checkCollaboratorCall.execute();
                if (responseCheckCollaborator.code() == 204) {
                    handler.post(() -> afterExecution.accept(true));
                    return;
                }

                // check if there is invitation or not
                Call<List<Invitation>> invitationsCall = apiInterface.checkInvitationCollaborator();
                Response<List<Invitation>> invitationsResponse = invitationsCall.execute();
                List<Invitation> invitations = invitationsResponse.body();
                if (invitations != null) {
                    for (Invitation invitation : invitations) {
                        if (invitation.repository != null && repoFullName.equals(invitation.repository.fullName)) {
                            // automaticall accept invitation
                            Call<Void> acceptInvitationCall = apiInterface.acceptInvitationCollaborator(invitation.id);
                            acceptInvitationCall.execute();
                            break;
                        }
                    }
                }

                // check again if already collaborator or not
                checkCollaboratorCall = apiInterface.checkCollaborator(repoNameSegments[0], repoNameSegments[1], user.login);
                responseCheckCollaborator = checkCollaboratorCall.execute();

                if (responseCheckCollaborator.code() == 204) {
                    handler.post(() -> afterExecution.accept(true));
                } else {
                    handler.post(() -> afterExecution.accept(false));
                }
            } catch (Exception ex) {
                Log.e(TAG, "CheckAsCollaboratorTask is failed", ex);
                handler.post(() -> errorExecution.accept(ex.getLocalizedMessage()));
            }
        });
    }
}
