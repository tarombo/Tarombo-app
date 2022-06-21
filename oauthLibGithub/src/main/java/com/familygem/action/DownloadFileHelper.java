package com.familygem.action;

import android.util.Base64;

import com.familygem.restapi.APIInterface;
import com.familygem.restapi.models.Content;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class DownloadFileHelper {
    public static Content downloadFile(APIInterface apiInterface, String owner,
                                       String repoName, String fileName) throws IOException {
        Call<Content> downloadContentCall = apiInterface.downloadFile(owner, repoName, fileName);
        Response<Content> downloadContentResponse = downloadContentCall.execute();
        Content content = downloadContentResponse.body();
        if (content != null && (content.content == null || content.content.isEmpty())) {
            Call<ResponseBody> downloadRawContentCall = apiInterface.downloadFile2(owner, repoName, fileName);
            Response<ResponseBody> downloadRawContentResponse = downloadRawContentCall.execute();
            content.contentStr = new String(downloadRawContentResponse.body().bytes(), StandardCharsets.UTF_8);
        } else {
            byte[] jsonContentBytes = Base64.decode(content.content, Base64.DEFAULT);
            content.contentStr = new String(jsonContentBytes, StandardCharsets.UTF_8);
        }

        return content;
    }
}
