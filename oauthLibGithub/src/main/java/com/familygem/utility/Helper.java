package com.familygem.utility;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.widget.Toast;

import androidx.core.util.Consumer;

import com.familygem.action.GetUsernameTask;
import com.familygem.oauthLibGithub.BuildConfig;
import com.familygem.oauthLibGithub.GithubOauth;
import com.familygem.restapi.APIInterface;
import com.familygem.restapi.ApiClient;
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
import org.folg.gedcom.model.Media;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;

import okhttp3.Headers;
import okhttp3.ResponseBody;
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

    public static TreeItem createItemBlob(APIInterface apiInterface, String username, String repoName, byte[] bytes, String path) throws IOException {
        CreateBlobResult blob = createBlob(apiInterface, username, repoName, bytes);
        if (blob != null) {
            TreeItem treeItem = new TreeItem();
            treeItem.mode = "100644";
            treeItem.path = path;
            treeItem.type = "blob";
            treeItem.sha = blob.sha;
            return treeItem;
        }
        return null;
    }
    public static TreeItem createItemBlob(APIInterface apiInterface, String username, String repoName, File file, String path) throws IOException {
        CreateBlobResult blob = createBlob(apiInterface, username, repoName, file);
        if (blob != null) {
            TreeItem treeItem = new TreeItem();
            treeItem.mode = "100644";
            treeItem.path = path;
            treeItem.type = "blob";
            treeItem.sha = blob.sha;
            return treeItem;
        }
        return null;
    }

    public static File getDirMedia(Context context, int treeId) {
        File dirMedia = context.getExternalFilesDir( String.valueOf(treeId) );
        return dirMedia;
    }

    public interface FWrapper {
        File getFileMedia( int idAlbero, Media m );
    }

    public static TreeItem createItemBlob(APIInterface apiInterface, String login, String repoName, Media media, File fileMedia) throws IOException {
        // parse media file name
        String filePath = media.getFile();
        if (filePath == null || filePath.isEmpty() )
            return null;

        String fileName = filePath.replace('\\', '/');
        if( fileName.lastIndexOf('/') > -1 ) {
            fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
        }

        if (fileMedia.exists()) {
            return createItemBlob(apiInterface, login, repoName, fileMedia, "media/" + fileName);
        }

        return null;
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

    public static TreeItem findTreeItemMedia(APIInterface apiInterface, String owner, String repoName, TreeResult baseTree, String path) throws IOException {
        if (baseTree == null || baseTree.tree == null)
            return null;
        // get folder "media" sha
        TreeResult mediaTree = getMediaTreeItems(baseTree, apiInterface, owner, repoName);
        if (mediaTree == null)
            return null;

        return findTreeItem(mediaTree, path);
    }

    public static  String getHeaderValue(Headers headers, String key) {
        for (String name : headers.toMultimap().keySet()) {
            if (key.equalsIgnoreCase(name)) {
                return headers.get(name);
            }
        }
        return null;
    }

    public final static String ERROR_RATE_LIMIT = "error-rate-limit";

    public static void downloadFileMedia(Context context, File dirMedia,
                                         APIInterface apiInterface, String owner,
                                       String repoName, String filename) throws IOException {
        Call<ResponseBody> downloadContentCall = apiInterface.downloadRawFile(owner, repoName, "main/media/" + filename);
        Response<ResponseBody> downloadContentResponse = downloadContentCall.execute();
        Headers headers = downloadContentResponse.headers();
        String rateLimitRemaining = getHeaderValue(headers, "x-ratelimit-remaining");
        if ("0".equals(rateLimitRemaining))
            throw new IOException(ERROR_RATE_LIMIT);
        byte[] data;
        data = downloadContentResponse.body().bytes();

        FileOutputStream fos = null;
        try {
            File imgFile = new File(dirMedia, filename);
//            fos = context.openFileOutput(imgFile.getAbsolutePath(), Context.MODE_PRIVATE);
            fos = new FileOutputStream(imgFile, false);
            fos.write(data);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static TreeResult getMediaTreeItems(TreeResult baseTree, APIInterface apiInterface, String username, String repoName) throws IOException {
        String media_sha = null;
        for (TreeItem item : baseTree.tree) {
            if ("media".equals(item.path) && "tree".equals(item.type)) {
                media_sha = item.sha;
                break;
            }
        }

        if (media_sha != null) {
            Call<TreeResult> getMediaFolderCall = apiInterface.getSubFolderTree(username, repoName, media_sha);
            Response<TreeResult> getMediaFolderResponse = getMediaFolderCall.execute();
            return getMediaFolderResponse.body();
        }
        return null;
    }

    public static void downloadAllMediaFiles(Context context, File dirMedia, TreeResult baseTree,
                                             APIInterface apiInterface, String username, String repoName) throws IOException {
        // get folder "media" sha
        TreeResult mediaTree = getMediaTreeItems(baseTree, apiInterface, username, repoName);
        if (mediaTree == null)
            return;

        APIInterface rawApiInterface = ApiClient.getClient(BuildConfig.GITHUB_BASE_RAW_URL, null).create(APIInterface.class);
        for (TreeItem item : mediaTree.tree) {
            if ("blob".equals(item.type)) {
                downloadFileMedia(context, dirMedia, rawApiInterface,username, repoName, item.path);
            }
        }
    }

    public static boolean amIRepoOwner(Context context, String repoFullName) {
        File userFile = new File(context.getFilesDir(), "user.json");
        if (userFile.exists()) {
            User user = Helper.getUser(userFile);
            if (user != null && user.login != null) {
                String[] repoNameSegments = repoFullName.split("/");
                if (user.login.equals(repoNameSegments[0]))
                    return true;
            }
        }
        return false;
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
