package com.familygem.utility;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.util.Consumer;

import com.familygem.action.GetUsernameTask;
import com.familygem.restapi.models.Commit;
import com.familygem.restapi.models.Content;
import com.familygem.restapi.models.Pull;
import com.familygem.restapi.models.Repo;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class Helper {
    public static Boolean isLogin(Context context) {
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
            Repo repo = gson.fromJson(json, Repo.class);
            return repo;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static Content getContent(File contentFile) {
        try {
            String json = FileUtils.readFileToString(contentFile, "UTF-8");
            Gson gson = new Gson();
            Content content = gson.fromJson(json, Content.class);
            return content;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Pull getPR(File pullFile) {
        try {
            String json = FileUtils.readFileToString(pullFile, "UTF-8");
            Gson gson = new Gson();
            Pull pull = gson.fromJson(json, Pull.class);
            return pull;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Commit getCommit(File commitFile) {
        try {
            String json = FileUtils.readFileToString(commitFile, "UTF-8");
            Gson gson = new Gson();
            Commit pull = gson.fromJson(json, Commit.class);
            return pull;
        } catch (IOException e) {
            e.printStackTrace();
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
    }
}
