package com.familygem.utility;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

import com.familygem.restapi.models.Repo;
import com.google.gson.Gson;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

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
        return "https://tarombo.siboro.com/tarombo/" + repo;
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
}
