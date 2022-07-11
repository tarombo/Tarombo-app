package app.familygem;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.familygem.action.CheckAsCollaboratorTask;
import com.familygem.action.DownloadFilesOnlyTask;
import com.familygem.action.ForkRepoTask;
import com.familygem.action.GetTreeJsonOfParentRepoTask;
import com.familygem.action.RedownloadRepoTask;
import com.familygem.oauthLibGithub.GithubOauth;
import com.familygem.oauthLibGithub.ResultCode;
import com.familygem.utility.Helper;

import org.apache.commons.net.ftp.FTPClient;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

public class Facciata extends AppCompatActivity {
	String repoFullName = null;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.facciata);

		/* Apertura in seguito al click su vari tipi di link:
		https://www.familygem.app/share.php?tree=20190802224208
			Messaggio breve
			Cliccato in Chrome nei vecchi Android apre la scelta dell'app tra cui Family Gem per importare direttamente l'albero
			Normalmente apre la pagina di condivisione del sito
		intent://www.familygem.app/condivisi/20200218134922.zip#Intent;scheme=https;end
			Link ufficiale nella pagina di condivisione del sito
			è l'unico che sembra avere certezza di funzionare, in Chrome, nel browser interno a Libero, nel Browser L90
		https://www.familygem.app/condivisi/20190802224208.zip
			URL diretto allo zip
			Funziona nei vecchi android, nei nuovi semplicemente il file viene scaricato
		*/
		Intent intent = getIntent();
		Uri uri = intent.getData();
		repoFullName = intent.getStringExtra("repoFullName");
		// Aprendo l'app da Task Manager, evita di re-importare un albero condiviso appena importato
		boolean fromHistory = (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) == Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY;
		if( uri != null && !fromHistory ) {
			String dataId = null;
			if( uri.getPath().equals( "/share.php" ) ) // click sul primo messaggio ricevuto
				dataId = uri.getQueryParameter("tree");
			else if( uri.getLastPathSegment().endsWith( ".zip" ) ) // click sulla pagina di invito
				dataId = uri.getLastPathSegment().replace(".zip","");
			else if (uri.getPath().indexOf("tarombo") > 0) {
				List<String> uriPathSegments = uri.getPathSegments();
				repoFullName = uriPathSegments.get(uriPathSegments.size() - 2) + "/" + uriPathSegments.get(uriPathSegments.size() - 1);
				handleDeeplink(repoFullName);
			} else {
				U.tosta( this, R.string.cant_understand_uri );
				return;
			}
			if( !BuildConfig.utenteAruba.isEmpty() ) {
				// Non ha bisogno di richiedere permessi
				scaricaCondiviso( this, dataId, null );
			}
		}  else if (repoFullName != null) {
			processRepo(repoFullName);
		} else {
			Intent treesIntent = new Intent(this, Alberi.class);
			// Open last tree at startup
			if( Global.settings.loadTree ) {
				treesIntent.putExtra("apriAlberoAutomaticamente", true);
				treesIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION); // forse inefficace ma tantè
			}
			startActivity(treesIntent);
		}
	}

	private void handleDeeplink(String repoFullName) {
		final AlertDialog alertDialog = new AlertDialog.Builder(Facciata.this)
				.setCancelable(false)
				.setMessage(R.string.collaborate_or_use_for_yourself)
				.setPositiveButton(R.string.collaborate, (dialog, id1) -> {
					dialog.dismiss();
					if (Helper.isLogin(this)) {
						// fork or download repo
						processRepo(repoFullName);
					} else {
						new AlertDialog.Builder(Facciata.this)
								.setTitle(R.string.find_errors)
								.setMessage(getString(R.string.please_login_before_click_deeplink))
								.setCancelable(false)
								.setPositiveButton(R.string.OK, (eDialog, which) -> {
									Helper.showGithubOauthScreen(Facciata.this, repoFullName);
									eDialog.dismiss();
//								finish();
								})
								.show();
					}
				})
				.setNegativeButton(getString(R.string.cancel), ((dialog, which) -> {
					dialog.dismiss();
					finish();
				}))
				.setNeutralButton(R.string.use_for_myself, null)
				.create();
		alertDialog.show();

		alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
			alertDialog.dismiss();
			int nextTreeId = Global.settings.max() + 1;
			DownloadFilesOnlyTask.execute(Facciata.this, repoFullName, nextTreeId, infoModel -> {
				// add tree info and save settings.json
				Settings.Tree tree = new Settings.Tree(nextTreeId,
						infoModel.title,
						infoModel.filePath,
						infoModel.persons,
						infoModel.generations,
						infoModel.root,
						null,
						infoModel.grade,
						null
				);
				tree.isForked = false;
				Global.settings.aggiungi(tree);
				Global.settings.openTree = nextTreeId;
				Global.settings.save();

				if (isFinishing())
					return;

				Intent treesIntent = new Intent(this, Alberi.class);
				// Open last tree at startup
				if( Global.settings.loadTree ) {
					treesIntent.putExtra("apriAlberoAutomaticamente", true);
					treesIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION); // forse inefficace ma tantè
				}
				startActivity(treesIntent);
				finish();
			}, error ->  {
				if (isFinishing())
					return;

				new AlertDialog.Builder(Facciata.this)
						.setTitle(R.string.find_errors)
						.setMessage(error)
						.setCancelable(false)
						.setPositiveButton(R.string.OK, (dialog, which) -> {
							dialog.dismiss();
							finish();
						}).show();
			});

		});
	}

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data ) {
		super.onActivityResult( requestCode, resultCode, data );
		if (requestCode == GithubOauth.REQUEST_CODE) {
			if (resultCode == ResultCode.SUCCESS && repoFullName != null) {
				processRepo(repoFullName);
			}
		}
	}

	private void processRepo(String repoFullName) {
		CheckAsCollaboratorTask.execute(Facciata.this, repoFullName, isCollaborator -> {
			if (isCollaborator) {
				downloadRepo(repoFullName);
			} else {
				forkRepo(repoFullName);
			}
		}, error -> {
			// show error message
			new AlertDialog.Builder(Facciata.this)
					.setTitle(R.string.find_errors)
					.setMessage(error)
					.setCancelable(false)
					.setPositiveButton(R.string.OK, (dialog, which) -> {
						dialog.dismiss();
						finish();
					})
					.show();
		});
	}

	private void downloadRepo(String repoFullName) {
		int nextTreeId = Global.settings.max() + 1;
		RedownloadRepoTask.execute(Facciata.this, repoFullName, nextTreeId, infoModel -> {
			// add tree info and save settings.json
			Settings.Tree tree = new Settings.Tree(nextTreeId,
					infoModel.title,
					infoModel.filePath,
					infoModel.persons,
					infoModel.generations,
					infoModel.root,
					null,
					infoModel.grade,
					infoModel.githubRepoFullName
			);
			tree.isForked = false;
			Global.settings.aggiungi(tree);
			Global.settings.openTree = nextTreeId;
			Global.settings.save();

			if (isFinishing())
				return;

			Intent treesIntent = new Intent(this, Alberi.class);
			// Open last tree at startup
			if( Global.settings.loadTree ) {
				treesIntent.putExtra("apriAlberoAutomaticamente", true);
				treesIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION); // forse inefficace ma tantè
			}
			startActivity(treesIntent);
			finish();
		}, error ->  {
			if (isFinishing())
				return;

			new AlertDialog.Builder(Facciata.this)
					.setTitle(R.string.find_errors)
					.setMessage(error)
					.setCancelable(false)
					.setPositiveButton(R.string.OK, (dialog, which) -> {
						dialog.dismiss();
						finish();
					}).show();
		});
	}

	private void forkRepo(String repoFullName) {
		int nextTreeId = Global.settings.max() + 1;
		ForkRepoTask.execute(Facciata.this,
				repoFullName, nextTreeId, () -> {
					// nothing yet
				}, infoModel  -> {
					// add tree info and save settings.json
					Settings.Tree tree = new Settings.Tree(nextTreeId,
							infoModel.title,
							infoModel.filePath,
							infoModel.persons,
							infoModel.generations,
							infoModel.root,
							null,
							infoModel.grade,
							infoModel.githubRepoFullName
					);
					tree.isForked = true;
					tree.repoStatus = infoModel.repoStatus;
					tree.aheadBy = infoModel.aheadBy;
					tree.behindBy = infoModel.behindBy;
					tree.totalCommits = infoModel.totalCommits;
					Global.settings.aggiungi(tree);
					Global.settings.openTree = nextTreeId;
					Global.settings.save();

					Intent treesIntent = new Intent(this, Alberi.class);
					// Open last tree at startup
					if( Global.settings.loadTree ) {
						treesIntent.putExtra("apriAlberoAutomaticamente", true);
						treesIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION); // forse inefficace ma tantè
					}
					startActivity(treesIntent);
					finish();
				}, error -> {
					String errorMessage = error;
					if (error.equals("E001"))
						errorMessage = getString(R.string.error_cant_fork_repo_of_ourself);
					else if (error.equals("E404"))
						errorMessage = getString(R.string.error_shared_not_found);
					// show error message
					new AlertDialog.Builder(Facciata.this)
							.setTitle(R.string.find_errors)
							.setMessage(errorMessage)
							.setCancelable(false)
							.setPositiveButton(R.string.OK, (dialog, which) -> {
								dialog.dismiss();
								finish();
							})
							.show();
				}
		);
	}

	// Si collega al server e scarica il file zip per importarlo
	static void scaricaCondiviso( Context contesto, String idData, View rotella ) {
		if( rotella != null )
			rotella.setVisibility( View.VISIBLE );
		// Un nuovo Thread è necessario per scaricare asincronicamente un file
		new Thread( () -> {
			try {
				FTPClient client = new FTPClient();
				client.connect( "89.46.104.211" );
				client.enterLocalPassiveMode();
				client.login( BuildConfig.utenteAruba, BuildConfig.passwordAruba );
				// Todo: Forse si potrebbe usare il download manager così da avere il file anche elencato in 'Downloads'
				String percorsoZip = contesto.getExternalCacheDir() + "/" + idData + ".zip";
				FileOutputStream fos = new FileOutputStream( percorsoZip );
				String percorso = "/www.familygem.app/condivisi/" + idData + ".zip";
				InputStream input = client.retrieveFileStream( percorso );
				if( input != null ) {
					byte[] data = new byte[1024];
					int count;
					while ((count = input.read(data)) != -1) {
						fos.write(data, 0, count);
					}
					fos.close();
					if( client.completePendingCommand()
							&& AlberoNuovo.decomprimiZip( contesto, percorsoZip, null )
							// Se l'albero è stato scaricato con l'install referrer
							&& Global.settings.referrer != null && Global.settings.referrer.equals(idData) ) {
						Global.settings.referrer = null;
						Global.settings.save();
					}
				} else // Non ha trovato il file sul server
					scaricamentoFallito( contesto, contesto.getString(R.string.something_wrong), rotella );
				client.logout();
				client.disconnect();
			} catch( Exception e ) {
				scaricamentoFallito( contesto, e.getLocalizedMessage(), rotella );
			}
		}).start();
	}

	// Conclusione negativa del metodo qui sopra
	static void scaricamentoFallito( Context contesto, String messaggio, View rotella ) {
		U.tosta( (Activity)contesto, messaggio );
		if( rotella != null )
			((Activity)contesto).runOnUiThread( () -> rotella.setVisibility( View.GONE ) );
		else
			contesto.startActivity( new Intent(contesto, Alberi.class) );
	}
}
