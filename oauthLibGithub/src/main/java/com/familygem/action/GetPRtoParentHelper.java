package com.familygem.action;

import android.content.Context;

import com.familygem.restapi.APIInterface;
import com.familygem.restapi.models.Pull;
import com.familygem.restapi.models.Repo;
import com.familygem.restapi.models.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class GetPRtoParentHelper {
    public static void execute(final Context context, final APIInterface apiInterface, final User user,
                               final Repo repo, final String repoFullName, final int treeId) throws IOException {
        String[] repoParentNameSegments = repo.parent.fullName.split("/");
        String[] repoNameSegments = repoFullName.split("/");
        String head = user.login + ":main";
        Call<List<Pull>> listPRCall = apiInterface.getPRtoParent(repoParentNameSegments[0], repoNameSegments[1], head);
        Response<List<Pull>> listPRResponse = listPRCall.execute();
        List<Pull> listPR = listPRResponse.body();
        if (listPR != null && listPR.size() > 0) {
            File prFile = new File(context.getFilesDir(), treeId + ".PRtoParent");
            Pull pr = listPR.get(0);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            String jsonPr = gson.toJson(pr);
            FileUtils.writeStringToFile(prFile, jsonPr, "UTF-8");
        }
    }
}
