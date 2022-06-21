package com.familygem.action;

import android.util.Base64;

import com.familygem.restapi.APIInterface;
import com.familygem.restapi.models.Content;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class DownloadFileByRefHelper {
    public static Content downloadFile(APIInterface apiInterface, String owner,
                                       String repoName, String fileName, String ref) throws IOException {
        Call<Content> downloadContentCall = apiInterface.downloadFileByRef(owner, repoName, fileName, ref);
        Response<Content> downloadContentResponse = downloadContentCall.execute();
        Content content = downloadContentResponse.body();
        if (content != null && (content.content == null || content.content.isEmpty())) {
            Call<ResponseBody> downloadRawContentCall = apiInterface.downloadFileByRef2(owner, repoName, fileName, ref);
            Response<ResponseBody> downloadRawContentResponse = downloadRawContentCall.execute();
            content.contentStr = new String(downloadRawContentResponse.body().bytes(), StandardCharsets.UTF_8);
        } else {
            byte[] jsonContentBytes = Base64.decode(content.content, Base64.DEFAULT);
            content.contentStr = new String(jsonContentBytes, StandardCharsets.UTF_8);
        }

        return content;
    }
}
