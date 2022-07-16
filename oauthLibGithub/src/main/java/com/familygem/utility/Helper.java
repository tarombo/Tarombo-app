package com.familygem.utility;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.util.Base64;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;

import com.familygem.action.GetUsernameTask;
import com.familygem.oauthLibGithub.GithubOauth;
import com.familygem.restapi.APIInterface;
import com.familygem.restapi.models.Commit;
import com.familygem.restapi.models.Content;
import com.familygem.restapi.models.CreateBlobResult;
import com.familygem.restapi.models.Pull;
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.TreeItem;
import com.familygem.restapi.models.TreeResult;
import com.familygem.restapi.models.User;
import com.familygem.restapi.requestmodels.CreateBlobRequestModel;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Response;

public class Helper {
    public static Boolean isLogin(Context context) {
        return isOauthTokenExist(context);
    }

    public static Boolean isOauthTokenExist(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("github_prefs", MODE_PRIVATE);
        String oauthToken = prefs.getString("oauth_token", null);
        return oauthToken != null;
    }


    public static String getEmail(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("email_prefs", MODE_PRIVATE);
        return prefs.getString("email", null);
    }

    public static void saveEmail(Context context, String email) {
        SharedPreferences prefs = context.getSharedPreferences("email_prefs", MODE_PRIVATE);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString("email", email);
        edit.apply();
    }

    public static String generateDeepLink(String repo) {
        return "https://tarombo.siboro.org/tarombo/" + repo;
    }

    public static Repo getRepo(File repoFile) {
        try {
            String json = FileUtils.readFileToString(repoFile, "UTF-8");
            Gson gson = new Gson();
            return gson.fromJson(json, Repo.class);
        } catch (IOException e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return null;
    }
    public static Content getContent(File contentFile) {
        try {
            String json = FileUtils.readFileToString(contentFile, "UTF-8");
            Gson gson = new Gson();
            return gson.fromJson(json, Content.class);
        } catch (IOException e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return null;
    }

    public static Pull getPR(File pullFile) {
        try {
            String json = FileUtils.readFileToString(pullFile, "UTF-8");
            Gson gson = new Gson();
            return gson.fromJson(json, Pull.class);
        } catch (IOException e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return null;
    }

    public static Commit getCommit(File commitFile) {
        try {
            String json = FileUtils.readFileToString(commitFile, "UTF-8");
            Gson gson = new Gson();
            return gson.fromJson(json, Commit.class);
        } catch (IOException e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return null;
    }

    public static User getUser(File userFile) {
        try {
            String json = FileUtils.readFileToString(userFile, "UTF-8");
            Gson gson = new Gson();
            return gson.fromJson(json, User.class);
        } catch (IOException e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
        return null;
    }

    /**
     * please execute this on UI thread
     * @param context
     * @param callback
     */
    public static void requireEmail(Context context, String descriptionText, String okText, String cancelText,  Consumer<String> callback) {
        final String email = Helper.getEmail(context);
        if (email != null && !email.equals("")) {
            callback.accept(email);
        } else {
//            AlertDialog.Builder emailDialogbuilder = new AlertDialog.Builder(context);
//            emailDialogbuilder.setTitle(descriptionText);
//            final EditText input = new EditText(context);
//            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
//            emailDialogbuilder.setView(input);
//            emailDialogbuilder.setPositiveButton(okText, (dialogEmail, which) -> {
//                final String newEmail = input.getText().toString();
//                Helper.saveEmail(context, newEmail);
//                callback.accept(newEmail);
//            });
//            emailDialogbuilder.setNegativeButton(cancelText, (dialogEmail, which) -> dialogEmail.cancel());
//            emailDialogbuilder.show();
            GetUsernameTask.execute(context, username -> {
                final String newEmail = "no-reply+" + username  + "@siboro.org";
                Helper.saveEmail(context, newEmail);
                callback.accept(newEmail);
            }, error -> Toast.makeText(context, error, Toast.LENGTH_LONG).show());
        }
    }

    /**
     * Copy a file from a location to another
     * @param sourceFile a File object represents the source file
     * @param destFile a File object represents the destination file
     * @throws IOException thrown if IO error occurred.
     */
    public static void copySingleFile(File sourceFile, File destFile)
            throws IOException {
        System.out.println("COPY FILE: " + sourceFile.getAbsolutePath()
                + " TO: " + destFile.getAbsolutePath());
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel sourceChannel = null;
        FileChannel destChannel = null;

        try {
            sourceChannel = new FileInputStream(sourceFile).getChannel();
            destChannel = new FileOutputStream(destFile).getChannel();
            sourceChannel.transferTo(0, sourceChannel.size(), destChannel);
        } finally {
            if (sourceChannel != null) {
                sourceChannel.close();
            }
            if (destChannel != null) {
                destChannel.close();
            }
        }
    }

    public static void deleteLocalFilesOfRepo(Context activity, int treeId) {
        // delete local files related with repo
        File repoFile = new File(activity.getFilesDir(), treeId + ".repo");
        if (repoFile.exists())
            repoFile.delete();
        File contentFile = new File(activity.getFilesDir(), treeId + ".content");
        if (contentFile.exists())
            contentFile.delete();
        File infoContentFile = new File(activity.getFilesDir(), treeId + ".info.content");
        if (infoContentFile.exists())
            infoContentFile.delete();
        File commitFile = new File(activity.getFilesDir(), treeId + ".commit");
        if (commitFile.exists())
            commitFile.delete();
        File behind0File = new File(activity.getFilesDir(), treeId + ".behind_0");
        if (behind0File.exists())
            behind0File.delete();
        File head0File = new File(activity.getFilesDir(), treeId + ".head_0");
        if (head0File.exists())
            head0File.delete();
        File parentFile = new File(activity.getFilesDir(), treeId + ".json.parent");
        if (parentFile.exists())
            parentFile.delete();
        File prFile = new File(activity.getFilesDir(), treeId + ".PRtoParent");
        if (prFile.exists())
            prFile.delete();
    }

    public static TreeResult getBaseTreeCall(APIInterface apiInterface, String username, String repoName) throws IOException {
        Call<TreeResult> getBaseTreeCall = apiInterface.getBaseTree(username, repoName);
        Response<TreeResult> getBaseTreeResponse = getBaseTreeCall.execute();
        TreeResult baseTree = getBaseTreeResponse.body();
        return baseTree;
    }

    public static CreateBlobResult createBlob(APIInterface apiInterface, String username, String repoName, byte[] bytes) throws IOException {
        CreateBlobRequestModel createBlobRequestModel = new CreateBlobRequestModel();
        createBlobRequestModel.content = Base64.encodeToString(bytes, Base64.DEFAULT);
        createBlobRequestModel.encoding = "base64";
        Call<CreateBlobResult> createBlobResultCall = apiInterface.createBlob(username, repoName, createBlobRequestModel);
        Response<CreateBlobResult> createBlobResultResponse = createBlobResultCall.execute();
        CreateBlobResult treeJsonBlob = createBlobResultResponse.body();
        return treeJsonBlob;
    }

    public static CreateBlobResult createBlob(APIInterface apiInterface, String username, String repoName, File file) throws IOException {
        int size = (int) file.length();
        byte[] bytes = new byte[size];
        BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
        buf.read(bytes, 0, bytes.length);
        buf.close();
        return createBlob(apiInterface, username, repoName, bytes);
    }

    public static TreeItem findTreeItem(TreeResult baseTree, String path) {
        if (baseTree == null || baseTree.tree == null)
            return null;
        for (TreeItem item: baseTree.tree) {
            if (path.equals(item.path))
                return item;
        }
        return null;
    }


    public static void showGithubOauthScreen(Context context, String repoFullName) {
        ArrayList<String> scopes = new ArrayList<String>(Arrays.asList(
                "repo",
                "repo:status",
                "public_repo",
                "delete_repo",
                "read:user",
                "user:email"
        ));
        GithubOauth
                .Builder()
                .withContext(context)
                .clearBeforeLaunch(true)
                .packageName("app.familygem")
                .nextActivity("app.familygem.Alberi")
                .withScopeList(scopes)
                .debug(true)
                .execute(repoFullName);
    }
}
