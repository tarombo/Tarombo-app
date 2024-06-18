package com.familygem.action;

import android.util.Base64;

import com.familygem.restapi.APIInterface;
import com.familygem.restapi.models.Content;
import com.familygem.utility.Helper;
import com.google.gson.Gson;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.Headers;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class DownloadFileHelper {
    public static Content downloadFile(APIInterface apiInterface, String owner,
                                       String repoName, String fileName) throws IOException {
        Call<Content> downloadContentCall = apiInterface.downloadFile(owner, repoName, fileName);
        Response<Content> response = downloadContentCall.execute();

        Headers headers = response.headers();
        String rateLimitRemaining = Helper.getHeaderValue(headers, "x-ratelimit-remaining");
        if ("0".equals(rateLimitRemaining))
            throw new IOException(Helper.ERROR_RATE_LIMIT);

        if(!response.isSuccessful())
            throw  new IOException(response.message());

        Content content = response.body();
        if(content == null)
            return content;

        if (content.content == null || content.content.isEmpty()) {
            Call<ResponseBody> downloadRawContentCall = apiInterface.downloadFile2(owner, repoName, fileName);
            Response<ResponseBody> rawResponse = downloadRawContentCall.execute();

            if(!rawResponse.isSuccessful())
                throw  new IOException(rawResponse.message());

            content.contentStr = new String(rawResponse.body().bytes(), StandardCharsets.UTF_8);
        } else {
            byte[] jsonContentBytes = Base64.decode(content.content, Base64.DEFAULT);
            content.contentStr = new String(jsonContentBytes, StandardCharsets.UTF_8);
        }

        return content;
    }
}
