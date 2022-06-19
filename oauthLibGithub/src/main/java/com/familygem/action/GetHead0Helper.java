package com.familygem.action;

import android.content.Context;
import android.util.Base64;

import com.familygem.restapi.APIInterface;
import com.familygem.restapi.models.Commit;
import com.familygem.restapi.models.CompareCommit;
import com.familygem.restapi.models.Content;
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.User;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

// note: currently this is not used, this is just for documentation how to get real head_0
public class GetHead0Helper {
    public static void execute(final Context context, final APIInterface apiInterface, final User user,
                               final Repo repo, final String repoFullName, final int treeId)
            throws IOException {
        if (!(repo.fork && repo.parent != null)) {
            return; // head_0 only relevant with forked repo
        }

        // get head commit info --> owner:parent repo  base: parent repo   main: forked repo
        // e.g. https://api.github.com/repos/putrasto/tarombo-putrasto-20220619114159/compare/main...putrastotest:main?page=1&per_page=2
        String[] repoParentNameSegments = repo.parent.fullName.split("/");
        String[] repoNameSegments = repoFullName.split("/");
        String base = "main";
        String head = user.login + ":main";
        Call<CompareCommit> head1CommitCall = apiInterface.compareCommit(repoParentNameSegments[0], repoNameSegments[1], base + "..." + head);
        Response<CompareCommit> compareCommitResponse = head1CommitCall.execute();
        CompareCommit compareHead1Commit = compareCommitResponse.body();
        if (compareHead1Commit.aheadBy > 0 && compareHead1Commit.commits.size() > 0) {
            Commit commit1 = compareHead1Commit.commits.get(0); // the first commit (including after merge PR)
            String shaCommit1 = commit1.sha;

            // get head_0
            Call<List<Commit>> previousCommitCall = apiInterface.getPreviousCommitBeforeSha(user.login, repoNameSegments[1], shaCommit1);
            Response<List<Commit>> previousCommitResponse = previousCommitCall.execute();
            List<Commit> previousCommits = previousCommitResponse.body();
            if (previousCommits != null && previousCommits.size() > 1) {
                Commit head0Commit  = previousCommits.get(1);
                String shaHead0 = head0Commit.sha;

                // download file tree.json
                Call<Content> downloadTreeJsonCall = apiInterface.downloadFileByRef(user.login, repoNameSegments[1], "tree.json", shaHead0);
                Response<Content> treeJsonContentResponse = downloadTreeJsonCall.execute();
                Content treeJsonContent = treeJsonContentResponse.body();
                // save tree.json to local directory
                byte[] treeJsonContentBytes = Base64.decode(treeJsonContent.content, Base64.DEFAULT);
                String treeJsonString = new String(treeJsonContentBytes, StandardCharsets.UTF_8);
                File treeJsonFile = new File(context.getFilesDir(), treeId + ".head_0");
                FileUtils.writeStringToFile(treeJsonFile, treeJsonString, "UTF-8");
            }
        }
        // note: if aheadBy == 0 --> then file [treeId].head_0 = [treeId].json
    }
}
