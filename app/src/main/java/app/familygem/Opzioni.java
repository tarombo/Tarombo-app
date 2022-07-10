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

import com.familygem.action.GetUsernameTask;
import com.familygem.oauthLibGithub.GithubOauth;
import com.familygem.oauthLibGithub.ResultCode;
import com.familygem.restapi.models.User;
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
	}

	private void showLoginLogoutText() {
		TextView loginLogoutTextView = findViewById(R.id.login_logout);
		TextView recoverTrees = findViewById(R.id.recover_trees);
		if (Helper.isOauthTokenExist(Opzioni.this)) {
			recoverTrees.setVisibility(View.VISIBLE);
			recoverTrees.setOnClickListener( v -> {
				startActivity(new Intent(Opzioni.this, RecoverTreesActivity.class));
			});
			GetUsernameTask.execute(this, username -> {
				loginLogoutTextView.setText(getText(R.string.logout) + " (" + username + ")");
			}, (error) -> loginLogoutTextView.setText(R.string.logout));

		} else {
			recoverTrees.setVisibility(View.GONE);
			loginLogoutTextView.setText(R.string.login);
		}
		loginLogoutTextView.setOnClickListener(v -> {
			if (Helper.isLogin( Opzioni.this)) {
				logoutGithub();
			} else {
				Helper.showGithubOauthScreen(Opzioni.this, null);
			}
		});
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
		File userFile = new File(getFilesDir(), "user.json");
		if (userFile.exists())
			userFile.delete();
		showLoginLogoutText();
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

	@Override
	protected void onResume() {
		super.onResume();
		showLoginLogoutText();
	}
}
