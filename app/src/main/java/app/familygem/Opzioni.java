package app.familygem;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.familygem.oauthLibGithub.GithubOauth;
import com.familygem.oauthLibGithub.ResultCode;
import com.familygem.utility.Helper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Opzioni extends AppCompatActivity {
	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.opzioni);

		// Salvataggio automatico
		Switch salva = findViewById(R.id.opzioni_salva);
		salva.setChecked(Global.settings.autoSave);
		salva.setOnCheckedChangeListener((coso, attivo) -> {
			Global.settings.autoSave = attivo;
			Global.settings.save();
		});

		// Carica albero all'avvio
		Switch carica = findViewById(R.id.opzioni_carica);
		carica.setChecked(Global.settings.loadTree);
		carica.setOnCheckedChangeListener((coso, attivo) -> {
			Global.settings.loadTree = attivo;
			Global.settings.save();
		});

		// Modalità esperto
		Switch esperto = findViewById(R.id.opzioni_esperto);
		esperto.setChecked(Global.settings.expert);
		esperto.setOnCheckedChangeListener((coso, attivo) -> {
			Global.settings.expert = attivo;
			Global.settings.save();
		});

		findViewById(R.id.opzioni_lapide).setOnClickListener(view -> startActivity(
				new Intent(Opzioni.this, Lapide.class)
		));

		showLoginLogoutText();
		findViewById(R.id.login_logout).setOnClickListener(v -> {
			if (Helper.isLogin( Opzioni.this)) {
				logoutGithub();
			} else {
				showGithubOauthScreen();
			}
		});
	}

	private void showLoginLogoutText() {
		TextView loginLogoutTextView = findViewById(R.id.login_logout);
		loginLogoutTextView.setText(
				Helper.isLogin(this) ? R.string.logout : R.string.login
		);
		TextView recoverTrees = findViewById(R.id.recover_trees);
		if (Helper.isLogin(Opzioni.this)) {
			recoverTrees.setVisibility(View.VISIBLE);
			recoverTrees.setOnClickListener( v -> {
				startActivity(new Intent(Opzioni.this, RecoverTreesActivity.class));
			});
		} else {
			recoverTrees.setVisibility(View.GONE);
		}
	}

	private void logoutGithub() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			deleteSharedPreferences("github_prefs");
			deleteSharedPreferences("email_prefs");
		} else {
			getSharedPreferences("github_prefs", MODE_PRIVATE).edit().clear().apply();
			File dir = new File(getApplicationInfo().dataDir, "shared_prefs");
			new File(dir, "github_prefs.xml").delete();

			getSharedPreferences("email_prefs", MODE_PRIVATE).edit().clear().apply();
			dir = new File(getApplicationInfo().dataDir, "email_prefs");
			new File(dir, "email_prefs.xml").delete();
		}

		List<Integer> treeIdRepos = new ArrayList<>();
		for (Settings.Tree tree: Global.settings.trees) {
			if (tree.githubRepoFullName != null) {
				Log.d("Opzioni", "should delete local repo:" + tree.githubRepoFullName);
				treeIdRepos.add(tree.id);
			}
		}
		for (Integer treeId : treeIdRepos) {
			Log.d("Opzioni", "delete local repo of treeId:" + treeId);
			Helper.deleteLocalFilesOfRepo(Opzioni.this, treeId);
			Alberi.deleteTree(Opzioni.this, treeId);
		}
		showLoginLogoutText();
	}

	private void showGithubOauthScreen() {
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
				.withContext(this)
				.clearBeforeLaunch(true)
				.packageName("app.familygem")
				.nextActivity("app.familygem.Alberi")
				.withScopeList(scopes)
				.debug(true)
				.execute();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == GithubOauth.REQUEST_CODE) {
			if (resultCode == ResultCode.SUCCESS) {
				Toast.makeText(this, R.string.login_is_succeed, Toast.LENGTH_LONG).show();
			} else if (resultCode == ResultCode.ERROR)  {
				// something went wrong :-(
				Toast.makeText(this, R.string.login_is_failed, Toast.LENGTH_LONG).show();
			}
			showLoginLogoutText();
		}
	}


//	private void getUserRepo() {
//		SharedPreferences prefs = getSharedPreferences("github_prefs", MODE_PRIVATE);
//		String oauthToken = prefs.getString("oauth_token", null);
//		Log.d("Opzioni", "getUserRepo oauth_token:" + oauthToken);
//		if (oauthToken != null) {
//			APIInterface apiInterface = ApiClient.getClient(BuildConfig.GITHUB_BASE_URL, oauthToken).create(APIInterface.class);
//			Call<List<Repo>> call = apiInterface.doGetListUserRepos();
//			call.enqueue(new Callback<List<Repo>>() {
//				@Override
//				public void onResponse(Call<List<Repo>> call, Response<List<Repo>> response) {
//					Log.d("Opzioni", "response code:" + response.code());
//					if (response.code() == 200) {
//						List<Repo> repos = response.body();
//						Log.d("Opzioni", "repos count:" + repos.size());
//					}
//				}
//
//				@Override
//				public void onFailure(Call<List<Repo>> call, Throwable t) {
//					Log.d("Opzioni", t.toString());
//					call.cancel();
//				}
//			});
//		}
//
//	}
}
