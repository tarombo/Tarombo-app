package com.familygem.restapi;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Credentials;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {
    private static Retrofit retrofit = null;

    public static Retrofit getClient(String baseUrl, String oauthToken) {

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();

        // TODO only in DEBUG mode
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(interceptor)
                .addInterceptor(chain -> {
                    Request originalRequest = chain.request();
//                    String oauthToken = "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";
                    Request.Builder builder = originalRequest.newBuilder().header("Authorization",
                            "token " + oauthToken);

                    Request newRequest = builder.build();
                    return chain.proceed(newRequest);
                })
                .build();


        retrofit = new Retrofit.Builder()
//                .baseUrl("https://api.github.com")
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();



        return retrofit;
    }
}
