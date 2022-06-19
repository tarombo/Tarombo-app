package com.familygem.action;

import android.content.Context;
import android.util.Base64;

import com.familygem.restapi.APIInterface;
import com.familygem.restapi.models.Commit;
import com.familygem.restapi.models.CompareCommit;
import com.familygem.restapi.models.Content;
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.User;
import com.familygem.utility.Helper;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

// get behind_0 actually same with get last merge upstream file
public class GetBehind0Helper {
    public static void execute(final Context context, final APIInterface apiInterface, final User user,
                               final Repo repo, final String repoFullName, final int treeId)
            throws IOException {
        if (!(repo.fork && repo.parent != null)) {
            return; // head_0 only relevant with forked repo
        }

        // get head commit info --> owner:forked repo  base: forked repo   main: parent repo
        String[] repoParentNameSegments = repo.parent.fullName.split("/");
        String[] repoNameSegments = repoFullName.split("/");
        String base = "main";
        String head = repoParentNameSegments[0] + ":main";
        Call<CompareCommit> behind1CommitCall = apiInterface.compareCommit(user.login, repoNameSegments[1], base + "..." + head);
        Response<CompareCommit> compareCommitResponse = behind1CommitCall.execute();
        CompareCommit compareBehind1Commit = compareCommitResponse.body();
        if (compareBehind1Commit.behindBy > 0 && compareBehind1Commit.commits.size() > 0) {
            Commit commit1 = compareBehind1Commit.commits.get(0); // the first commit (including after merge PR)
            String shaCommit1 = commit1.sha;

            // get behind_0
            Call<List<Commit>> previousCommitCall = apiInterface.getPreviousCommitBeforeSha(user.login, repoNameSegments[1], shaCommit1);
            Response<List<Commit>> previousCommitResponse = previousCommitCall.execute();
            List<Commit> previousCommits = previousCommitResponse.body();
            if (previousCommits != null && previousCommits.size() > 1) {
                Commit behind0Commit  = previousCommits.get(1);
                String shaHead0 = behind0Commit.sha;

                // download file tree.json
                Call<Content> downloadTreeJsonCall = apiInterface.downloadFileByRef(user.login, repoNameSegments[1], "tree.json", shaHead0);
                Response<Content> treeJsonContentResponse = downloadTreeJsonCall.execute();
                Content treeJsonContent = treeJsonContentResponse.body();
                // save tree.json to local directory
                byte[] treeJsonContentBytes = Base64.decode(treeJsonContent.content, Base64.DEFAULT);
                String treeJsonString = new String(treeJsonContentBytes, StandardCharsets.UTF_8);
                File treeJsonFile = new File(context.getFilesDir(), treeId + ".behind_0");
                FileUtils.writeStringToFile(treeJsonFile, treeJsonString, "UTF-8");
                File treeJsonFileHead0 = new File(context.getFilesDir(), treeId + ".head_0");
                Helper.copySingleFile(treeJsonFile, treeJsonFileHead0);
                // note: currently we assume comparison to ahead_0 same with behind_0
            }
        }
        // note: if aheadBy == 0 --> then file [treeId].head_0 = [treeId].json
    }
}
