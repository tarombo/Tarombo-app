package com.familygem.utility;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

public class Helper {
    public static Boolean isLogin(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("github_prefs", MODE_PRIVATE);
		String oauthToken = prefs.getString("oauth_token", null);
        return oauthToken != null;
    }
}
