package app.familygem;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.PopupMenu;
import androidx.work.WorkManager;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.familygem.action.CheckLastCommitTask;
import com.familygem.action.CompareRepoTask;
import com.familygem.action.CreatePRtoParentTask;
import com.familygem.action.CreateRepoTask;
import com.familygem.action.DeletePRtoParentTask;
import com.familygem.action.DeleteRepoTask;
import com.familygem.action.DoesOpenPRExistTask;
import com.familygem.action.ForkRepoTask;
import com.familygem.action.GetTreeJsonOfParentRepoTask;
import com.familygem.action.RedownloadRepoTask;
import com.familygem.action.SaveInfoFileTask;
import com.familygem.action.SyncWithParentTask;
import com.familygem.restapi.models.Repo;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;
import com.familygem.utility.PrivatePerson;

import org.folg.gedcom.model.ChildRef;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.MediaRef;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.ParentFamilyRef;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.SpouseFamilyRef;
import org.folg.gedcom.model.SpouseRef;
import org.folg.gedcom.parser.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.familygem.importnode.SelectPersonActivity;
import app.familygem.visita.ListaMedia;

public class Alberi extends AppCompatActivity {

	private static final String TAG = "Alberi";
	List<Map<String,String>> elencoAlberi;
	SimpleAdapter adapter;
	View rotella;
	Fabuloso welcome;
	Esportatore esportatore;
	private boolean autoOpenedTree; // To open automatically the tree at startup only once
	// The birthday notification IDs are stored to display the relative person only once
	private ArrayList<Integer> consumedNotifications = new ArrayList<>();

	// Menus
	private static final int MENU_ID_IMPORT_GEDCOM  = 15;
	private static final int MENU_ID_EXPORT_GEDCOM  = 7;

	@Override
	protected void onCreate(Bundle savedState) {
		super.onCreate(savedState);
		setContentView(R.layout.alberi);
		ListView vistaLista = findViewById(R.id.lista_alberi);
		rotella = findViewById(R.id.alberi_circolo);
		welcome = new Fabuloso(this, R.string.tap_add_tree);
		esportatore = new Esportatore(Alberi.this);

		// Al primissimo avvio
		String referrer = Global.settings.referrer;
		if( referrer != null && referrer.equals("start") )
			recuperaReferrer();
		// Se è stato memorizzato un dataid (che appena usato sarà cancellato)
		else if( referrer != null && referrer.matches("[0-9]{14}") ) {
			new AlertDialog.Builder(this).setTitle(R.string.a_new_tree)
					.setMessage(R.string.you_can_download)
					.setPositiveButton(R.string.download, (dialog, id) -> {
						Facciata.scaricaCondiviso(this, referrer, rotella);
					}).setNeutralButton(R.string.cancel, null).show();
		} // Se non c'è nessun albero
		else if( Global.settings.trees.isEmpty() )
			welcome.show();

		if( savedState != null ) {
			autoOpenedTree = savedState.getBoolean("autoOpenedTree");
			consumedNotifications = savedState.getIntegerArrayList("consumedNotifications");
		}

		if( Global.settings.trees != null ) {

			// Lista degli alberi genealogici
			elencoAlberi = new ArrayList<>();

			// Dà i dati in pasto all'adattatore
			adapter = new SimpleAdapter( this, elencoAlberi,
					R.layout.pezzo_albero,
					new String[] { "titolo", "dati" },
					new int[] { R.id.albero_titolo, R.id.albero_dati }) {
				// Individua ciascuna vista dell'elenco
				@Override
				public View getView( final int posiz, View convertView, ViewGroup parent ) {
					View vistaAlbero = super.getView( posiz, convertView, parent );
					int treeId = Integer.parseInt(elencoAlberi.get(posiz).get("id"));
					Settings.Tree tree = Global.settings.getTree(treeId);
					boolean derivato = tree.grade == 20;
					boolean esaurito = tree.grade == 30;
					if( derivato ) {
						vistaAlbero.setBackgroundColor(getResources().getColor(R.color.evidenziaMedio));
						((TextView)vistaAlbero.findViewById(R.id.albero_dati)).setTextColor(getResources().getColor(R.color.text));
						vistaAlbero.setOnClickListener(v -> {
							if( !AlberoNuovo.confronta(Alberi.this, tree, true) ) {
								tree.grade = 10; // viene retrocesso
								Global.settings.save();
								aggiornaLista();
								Toast.makeText(Alberi.this, R.string.something_wrong, Toast.LENGTH_LONG).show();
								if (tree.githubRepoFullName != null)
									Helper.requireEmail(Global.context, getString(R.string.set_email_for_commit),
											getString(R.string.OK), getString(R.string.cancel), email -> {
												FamilyGemTreeInfoModel infoModel = new FamilyGemTreeInfoModel(
														tree.title,
														tree.persons,
														tree.generations,
														tree.media,
														tree.root,
														tree.grade
												);
												SaveInfoFileTask.execute(Alberi.this, tree.githubRepoFullName, email, tree.id, infoModel,  () -> {}, () -> {}, error -> {
													Toast.makeText(Global.context, error, Toast.LENGTH_LONG).show();
												});
										}
									);
							}
						});
					} else if( esaurito ) {
						vistaAlbero.setBackgroundColor(getResources().getColor(R.color.consumed));
						((TextView)vistaAlbero.findViewById(R.id.albero_titolo)).setTextColor(getResources().getColor(R.color.grayText));
						vistaAlbero.setOnClickListener(v -> {
							if( !AlberoNuovo.confronta(Alberi.this, tree, true) ) {
								tree.grade = 10; // viene retrocesso
								Global.settings.save();
								aggiornaLista();
								Toast.makeText(Alberi.this, R.string.something_wrong, Toast.LENGTH_LONG).show();
								if (tree.githubRepoFullName != null)
									Helper.requireEmail(Global.context, getString(R.string.set_email_for_commit),
											getString(R.string.OK), getString(R.string.cancel), email -> {
												FamilyGemTreeInfoModel infoModel = new FamilyGemTreeInfoModel(
														tree.title,
														tree.persons,
														tree.generations,
														tree.media,
														tree.root,
														tree.grade
												);
												SaveInfoFileTask.execute(Alberi.this, tree.githubRepoFullName, email, tree.id, infoModel,  () -> {}, () -> {}, error -> {
													Toast.makeText(Global.context, error, Toast.LENGTH_LONG).show();
												});
											}
									);
							}
						});
					} else {
						vistaAlbero.setBackgroundColor(getResources().getColor(R.color.back_element));
						vistaAlbero.setOnClickListener(v -> {
							rotella.setVisibility(View.VISIBLE);
							if( !(Global.gc != null && treeId == Global.settings.openTree) ) { // se non è già aperto
								if( !apriGedcom(treeId, true) ) {
									rotella.setVisibility(View.GONE);
									return;
								}
							}
							if (tree.githubRepoFullName != null) {
								// check if commit is obsolete or not
								CheckLastCommitTask.execute(Alberi.this, tree.githubRepoFullName, tree.id,
										isLocalCommitObsolete -> {
									if (!isLocalCommitObsolete) {
										startActivity(new Intent(Alberi.this, Principal.class));
									} else {
										// show dialog to download
										new AlertDialog.Builder(Alberi.this)
												.setTitle(tree.title)
												.setMessage(R.string.error_commit_hash_obsolete)
												.setCancelable(false)
												.setPositiveButton(R.string.get_updates, (eDialog, which) -> {
													eDialog.dismiss();
													RedownloadRepoTask.execute(Alberi.this, tree.githubRepoFullName, tree.id,
															infoModel -> {
																// save settings.json
																tree.title = infoModel.title;
																tree.persons = infoModel.persons;
																tree.generations = infoModel.generations;
																tree.root = infoModel.root;
																tree.grade = infoModel.grade;
																File dirMedia = Helper.getDirMedia(Alberi.this, treeId);
																tree.dirs.add(dirMedia.getPath());
																if( !apriGedcom(treeId, true) ) {
																	rotella.setVisibility(View.GONE);
																	return;
																}

																startActivity(new Intent(Alberi.this, Principal.class));
													}, error -> {
														rotella.setVisibility(View.INVISIBLE);
														new AlertDialog.Builder(Alberi.this)
																.setTitle(R.string.find_errors)
																.setMessage(error)
																.setCancelable(false)
																.setPositiveButton(R.string.OK, (gDialog, gwhich) -> gDialog.dismiss())
																.show();

													});
												})
												.setNeutralButton(R.string.cancel, (eDialog, which) -> {
													rotella.setVisibility(View.INVISIBLE);
												})
												.show();
									}
								}, error -> {
											rotella.setVisibility(View.INVISIBLE);
											// On error don't change github status #12
											//ree.githubRepoFullName = null;
											tree.isForked = false;
											tree.repoStatus = null;
											Global.settings.save();
											if ("E404".equals(error)) {
												updateListGithubRepo();
												// show error message
												new AlertDialog.Builder(Alberi.this)
														.setTitle(R.string.find_errors)
														.setMessage(getString(R.string.repo_is_deleted))
														.setCancelable(false)
														.setPositiveButton(R.string.OK, (gDialog, gwhich) -> gDialog.dismiss())
														.show();
											} else {
												new AlertDialog.Builder(Alberi.this)
														.setTitle(R.string.find_errors)
														.setMessage(error)
														.setCancelable(false)
														.setPositiveButton(R.string.OK, (gDialog, gwhich) -> gDialog.dismiss())
														.show();
											}
										});
							} else {
								startActivity(new Intent(Alberi.this, Principal.class));
							}
						});
					}
					vistaAlbero.findViewById(R.id.green_round_icon).setVisibility(tree.githubRepoFullName != null ? View.VISIBLE : View.INVISIBLE);
					vistaAlbero.findViewById(R.id.albero_menu).setOnClickListener( vista -> {
						boolean esiste = new File( getFilesDir(), treeId + ".json" ).exists();
						PopupMenu popup = new PopupMenu( Alberi.this, vista );
						Menu menu = popup.getMenu();
						if( treeId == Global.settings.openTree && Global.daSalvare )
							menu.add(0, -1, 0, R.string.save);
						if( (Global.settings.expert && derivato) || (Global.settings.expert && esaurito) )
							menu.add(0, 0, 0, R.string.open);
						if( !esaurito || Global.settings.expert )
							menu.add(0, 1, 0, R.string.tree_info);
						if( (!derivato && !esaurito) || Global.settings.expert )
							menu.add(0, 2, 0, R.string.rename);
						if( esiste && (!derivato || Global.settings.expert) && !esaurito )
							menu.add(0, 3, 0, R.string.media_folders);
						if( !esaurito )
							menu.add(0, 4, 0, R.string.find_errors);
//						if( esiste && !derivato && !esaurito ) // non si può ri-condividere un albero ricevuto indietro, anche se sei esperto..
//							menu.add(0, 5, 0, R.string.share_tree);
						if( esiste && !derivato && !esaurito && Global.settings.expert && Global.settings.trees.size() > 1
								&& tree.shares != null && tree.grade != 0 ) // cioè dev'essere 9 o 10
							menu.add(0, 6, 0, R.string.compare);

						menu.add(0, MENU_ID_IMPORT_GEDCOM, 0, R.string.import_gedcom);

						if( esiste && Global.settings.expert && !esaurito )
							menu.add(0, MENU_ID_EXPORT_GEDCOM, 0, R.string.export_gedcom);
						if( esiste && Global.settings.expert )
							menu.add(0, 8, 0, R.string.make_backup);
						if (esiste && Helper.isLogin(Alberi.this)) {
							boolean isGithubInfoFileExist = new File( getFilesDir(), treeId + ".repo" ).exists();
							if (!isGithubInfoFileExist) {
								menu.add(0, 10, 0, R.string.upload_to_server);
							} else {
								if (Helper.amIRepoOwner(Alberi.this, tree.githubRepoFullName)) {
									menu.add(0, 14, 0, R.string.contributors);
								}
								menu.add(0, 5, 0, R.string.share_tree);
							}
						}
						if (tree.hasOpenPR != null && tree.hasOpenPR) {
							menu.add(0, 13, 0, R.string.change_proposals);
						}
						if (tree.isForked != null && tree.isForked && tree.aheadBy != null && tree.aheadBy > 0) {
							menu.add(0, 11, 0, R.string.submit_changes);
						}
						if (tree.isForked != null && tree.isForked && tree.behindBy != null && tree.behindBy > 0) {
							menu.add(0, 12, 0, R.string.get_changes);
						}
						menu.add(0, 9, 0, R.string.delete);
						popup.show();
						popup.setOnMenuItemClickListener(item -> {
							int id = item.getItemId();
							if( id == -1 ) { // Salva
								U.salvaJson(Global.gc, treeId);
								Global.daSalvare = false;
							} else if( id == 0 ) { // Apre un albero derivato
								apriGedcom(treeId, true);
								startActivity(new Intent(Alberi.this, Principal.class));
							} else if( id == 1 ) { // Info Gedcom
								Intent intento = new Intent(Alberi.this, InfoAlbero.class);
								intento.putExtra("idAlbero", treeId);
								startActivity(intento);
							} else if( id == 2 ) { // Rinomina albero
								AlertDialog.Builder builder = new AlertDialog.Builder(Alberi.this);
								View vistaMessaggio = getLayoutInflater().inflate(R.layout.albero_nomina, vistaLista, false);
								builder.setView(vistaMessaggio).setTitle(R.string.title);
								EditText editaNome = vistaMessaggio.findViewById(R.id.nuovo_nome_albero);
								editaNome.setText(elencoAlberi.get(posiz).get("titolo"));
								AlertDialog dialogo = builder.setPositiveButton(R.string.rename, (dialog, i1) -> {
									if (Helper.isLogin(Alberi.this)) {
										Helper.requireEmail(Alberi.this,
												getString(R.string.set_email_for_commit),
												getString(R.string.OK), getString(R.string.cancel), email -> {
													renameTitle(tree, editaNome, email);
												});
									} else {
										Global.settings.rinomina(tree.id, editaNome.getText().toString());
										aggiornaLista();
									}
								}).setNeutralButton(R.string.cancel, null).create();
								editaNome.setOnEditorActionListener((view, action, event) -> {
									if( action == EditorInfo.IME_ACTION_DONE )
										dialogo.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
									return false;
								});
								dialogo.show();
								vistaMessaggio.postDelayed( () -> {
									editaNome.requestFocus();
									editaNome.setSelection(editaNome.getText().length());
									InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
									inputMethodManager.showSoftInput(editaNome, InputMethodManager.SHOW_IMPLICIT);
								}, 300);
							} else if( id == 3 ) { // Media folders
								startActivity(new Intent(Alberi.this, CartelleMedia.class)
										.putExtra("idAlbero", treeId)
								);
							} else if( id == 4 ) { // Correggi errori
								findErrors(treeId, false);
							} else if( id == 5 ) { // Condividi albero
//								startActivity(new Intent(Alberi.this, Condivisione.class)
//										.putExtra("idAlbero", treeId)
//								);
								// sharing link
								Repo repo = Helper.getRepo(new File( getFilesDir(), treeId + ".repo" ));
								if (repo != null) {
									String repoDeepLink = Helper.generateDeepLink(repo.fullName);
									Log.d("Share", "url: " + repoDeepLink);
									Intent i = new Intent(Intent.ACTION_SEND);
									i.setType("text/plain");
									i.putExtra(
											Intent.EXTRA_SUBJECT,
											getText(R.string.sharing_link)
									);
									i.putExtra(Intent.EXTRA_TEXT, repoDeepLink);
									startActivity(
											Intent.createChooser(
													i,
													getText(R.string.sharing_link)
											)
									);
								}
							} else if( id == 6 ) { // Confronta con alberi esistenti
								if( AlberoNuovo.confronta(Alberi.this, tree, false) ) {
									tree.grade = 20;
									aggiornaLista();
								} else
									Toast.makeText(Alberi.this, R.string.no_results, Toast.LENGTH_LONG).show();
							} else if( id == MENU_ID_IMPORT_GEDCOM ) {
								importGedcomToNode(treeId);
							} else if( id == MENU_ID_EXPORT_GEDCOM ) {
								if( esportatore.apriAlbero(treeId) ) {
									String mime = "application/octet-stream";
									String ext = "ged";
									int code = 636;
									if( esportatore.quantiFileMedia() > 0 ) {
										mime = "application/zip";
										ext = "zip";
										code = 6219;
									}
									F.salvaDocumento(Alberi.this, null, treeId, mime, ext, code);
								}
							} else if( id == 8 ) { // Fai backup
								if( esportatore.apriAlbero(treeId) )
									F.salvaDocumento(Alberi.this, null, treeId, "application/zip", "zip", 327);
							} else if( id == 9 ) {    // Elimina albero
								new AlertDialog.Builder(Alberi.this).setMessage(R.string.really_delete_tree)
										.setPositiveButton(R.string.delete, (dialog, id1) -> {
											final ProgressDialog pd = new ProgressDialog(Alberi.this);
											DeleteRepoTask.execute(Alberi.this, treeId, tree.githubRepoFullName, () -> {
												pd.setMessage(getString(R.string.deleting));
												pd.show();
											}, () -> {
												deleteTree(Alberi.this, treeId);
												aggiornaLista();
												pd.dismiss();
											}, error -> {
												pd.dismiss();
												// show error message
												if (!error.equals("E000"))
													new AlertDialog.Builder(Alberi.this)
														.setTitle(R.string.find_errors)
														.setMessage(error)
														.setCancelable(false)
														.setPositiveButton(R.string.OK, (eDialog, which) -> eDialog.dismiss())
														.show();
											});
										}).setNeutralButton(R.string.cancel, null).show();
							} else if (id == 10) { // create repo and upload the json
								Helper.requireEmail(Alberi.this,
										getString(R.string.set_email_for_commit),
										getString(R.string.OK), getString(R.string.cancel), email -> {
											createRepo(email, treeId);
										});
							} else if (id == 11) {
								// create pull request
								createPRtoParentRepo(tree);
							} else if (id == 12) {
								// merge-upstream
								showMergeUpstreamConfirmation(tree);
							} else if (id == 13) {
								// show change proposals screen (a.k.a PR list)
								showChangePropasals(tree);
							} else if (id == 14) {
								Intent intent = new Intent(Alberi.this, ContributorsActivity.class);
								intent.putExtra("repoFullName", tree.githubRepoFullName);
								startActivity(intent);
							} else {
								return false;
							}
							return true;
						});
					});
					return vistaAlbero;
				}
			};
			vistaLista.setAdapter(adapter);
			aggiornaLista();
		}

		// Barra personalizzata
		ActionBar barra = getSupportActionBar();
		View barraAlberi = getLayoutInflater().inflate(R.layout.alberi_barra, null);
		barraAlberi.findViewById(R.id.alberi_opzioni).setOnClickListener(v -> startActivity(
				new Intent(Alberi.this, Opzioni.class))
		);
		barra.setCustomView(barraAlberi);
		barra.setDisplayShowCustomEnabled(true);

		// FAB
		findViewById(R.id.fab).setOnClickListener(v -> {
			welcome.hide();
			startActivity(new Intent(Alberi.this, AlberoNuovo.class));
		});

		// Automatic load of last opened tree of previous session
		if( !birthdayNotifyTapped(getIntent()) && !autoOpenedTree
				&& getIntent().getBooleanExtra("apriAlberoAutomaticamente", false) && Global.settings.openTree > 0 ) {
			vistaLista.post(() -> {
				if( Alberi.apriGedcom(Global.settings.openTree, false) ) {
					rotella.setVisibility(View.VISIBLE);
					autoOpenedTree = true;
					startActivity(new Intent(this, Principal.class));
				}
			});
		}
	}

	private void renameTitle(Settings.Tree tree, EditText editaNome, String email) {
		FamilyGemTreeInfoModel infoModel = new FamilyGemTreeInfoModel(
				editaNome.getText().toString(),
				tree.persons,
				tree.generations,
				tree.media,
				tree.root,
				tree.grade
		);
		final ProgressDialog pd = new ProgressDialog(Alberi.this);
		SaveInfoFileTask.execute(Alberi.this, tree.githubRepoFullName, email, tree.id, infoModel,  () -> {
			pd.setMessage(getString(R.string.renaming));
			pd.show();
		}, () -> {
			Global.settings.rinomina(tree.id, editaNome.getText().toString());
			aggiornaLista();
			pd.dismiss();
			updateListGithubRepo();
		}, error -> {
			pd.dismiss();
			// show error message
			new AlertDialog.Builder(Alberi.this)
					.setTitle(R.string.find_errors)
					.setMessage(error)
					.setCancelable(false)
					.setPositiveButton(R.string.OK, (eDialog, which) -> eDialog.dismiss())
					.show();
		});

		Log.d(TAG, "onCreate");
	}

	private void showChangePropasals(Settings.Tree tree) {
		Intent intent = new Intent(Alberi.this, ChangeProposalActivity.class);
		intent.putExtra("treeId", tree.id);
		intent.putExtra("repoFullName", tree.githubRepoFullName);
		startActivity(intent);
	}

	private void showMergeUpstreamConfirmation(Settings.Tree tree) {
		final AlertDialog alertDialog = new AlertDialog.Builder(Alberi.this)
			.setCancelable(false)
			.setTitle(getString(R.string.get_changes))
			.setMessage(getString(R.string.are_you_sure_to_get_changes))
			.setNegativeButton(getString(R.string.cancel), null)
			.setPositiveButton(getString(R.string.get_changes), (dialog0, id0) -> {
				if (isFinishing())
					return;
				dialog0.dismiss();
				mergeUpstream(tree);
			})
			.setNeutralButton(getString(R.string.review_changes), null).create();
		alertDialog.show();
		alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
			final ProgressDialog pd = new ProgressDialog(Alberi.this);
			pd.setMessage(getString(R.string.getting_changes));
			pd.show();
			GetTreeJsonOfParentRepoTask.execute(Alberi.this,tree.id, () -> {
				if (isFinishing())
					return;
				pd.dismiss();
				Intent intent = new Intent(Alberi.this, CompareChangesActivity.class);
				intent.putExtra("compareType", CompareChangesActivity.CompareType.GetChanges);
				// before json: last time commit and behind 0
				String jsonFileNameBefore = tree.id + ".behind_0";
				intent.putExtra("jsonFileNameBefore", jsonFileNameBefore);
				// after json: current json file
				String jsonFileNameAfter = tree.id + ".json.parent";
				intent.putExtra("jsonFileNameAfter", jsonFileNameAfter);
				intentLauncherCompareChanges.launch(intent);
			}, error -> {
				pd.dismiss();
				new AlertDialog.Builder(Alberi.this)
						.setTitle(R.string.find_errors)
						.setMessage(error)
						.setCancelable(false)
						.setPositiveButton(R.string.OK, (gDialog, gwhich) -> gDialog.dismiss())
						.show();
			});

		});
	}

	private void mergeUpstream(Settings.Tree tree) {
		final ProgressDialog pd = new ProgressDialog(Alberi.this);
		pd.setMessage(getString(R.string.getting_changes));
		pd.show();

		SyncWithParentTask.execute(Alberi.this, tree.githubRepoFullName, tree.id,
		mergeable -> {
			// save settings.json
			tree.submittedPRtoParent = true;
			tree.submittedPRtoParentMergeable = mergeable;
			Global.settings.save();
			pd.dismiss();

			if (isFinishing())
				return;

			if (!mergeable) {
				// can't be synced with upstream
				new AlertDialog.Builder(Alberi.this)
						.setTitle(R.string.get_changes)
						.setMessage(R.string.there_is_conflict)
						.setPositiveButton(R.string.get_changes_and_discard_mine, (dialog, id1) -> {
							pd.show();
							reFork(tree, pd, dialog);
						}).setNeutralButton(R.string.ignore_conflict, null).show();
			} else {
				pd.show();
				// redownload file on server
				RedownloadRepoTask.execute(Alberi.this, tree.githubRepoFullName, tree.id,
						infoModel -> {
							// save settings.json
							tree.title = infoModel.title;
							tree.persons = infoModel.persons;
							tree.generations = infoModel.generations;
							tree.root = infoModel.root;
							tree.grade = infoModel.grade;
							if( !apriGedcom(tree.id, true) ) {
								rotella.setVisibility(View.GONE);
								return;
							}

							updateListGithubRepo();
							pd.dismiss();
						}, error -> {
							pd.dismiss();
							new AlertDialog.Builder(Alberi.this)
									.setTitle(R.string.find_errors)
									.setMessage(error)
									.setCancelable(false)
									.setPositiveButton(R.string.OK, (gDialog, gwhich) -> gDialog.dismiss())
									.show();
						});
			}
		},error -> {
			if (isFinishing())
				return;
			pd.dismiss();
			// show error message
			new AlertDialog.Builder(Alberi.this)
					.setTitle(R.string.find_errors)
					.setMessage(error)
					.setCancelable(false)
					.setPositiveButton(R.string.OK, (dialog, which) -> dialog.dismiss())
					.show();
		});
	}

	private void createPRtoParentRepo(Settings.Tree tree) {
		final ProgressDialog pd = new ProgressDialog(Alberi.this);
		final AlertDialog alertDialog = new AlertDialog.Builder(Alberi.this)
				.setCancelable(false)
				.setTitle(R.string.submit_changes)
				.setMessage(R.string.are_you_sure_to_submit_changes)
				.setNegativeButton(getString(R.string.cancel), null)
				.setPositiveButton(R.string.submit_changes, (dialog0, id0) -> {
					if (isFinishing())
						return;
					dialog0.dismiss();

					CreatePRtoParentTask.execute(Alberi.this, tree.githubRepoFullName,tree.id,
							() ->  {
								pd.setMessage(getString(R.string.submitting_changes));
								pd.show();
							}, mergeable -> {
								// save settings.json
								tree.submittedPRtoParent = true;
								tree.submittedPRtoParentMergeable = mergeable;
								Global.settings.save();
								pd.dismiss();

								if (isFinishing())
									return;

								if (!mergeable) {
									// can't be merged -> to close PR or not
									new AlertDialog.Builder(Alberi.this)
											.setTitle(R.string.submit_changes)
											.setMessage(R.string.there_is_conflict)
											.setPositiveButton(R.string.discard_changes, (dialog, id1) -> {
												DeletePRtoParentTask.execute(Alberi.this, tree.githubRepoFullName, tree.id, () -> {
													pd.setMessage(getString(R.string.discard_changes));
													pd.show();
												}, () -> {
													reFork(tree, pd, dialog);
												}, error -> {
													pd.dismiss();
													dialog.dismiss();
													// show error message
													new AlertDialog.Builder(Alberi.this)
															.setTitle(R.string.find_errors)
															.setMessage(error)
															.setCancelable(false)
															.setPositiveButton(R.string.OK, (eDialog, which) -> eDialog.dismiss())
															.show();
												});
											}).setNeutralButton(R.string.ignore_conflict, null).show();
								} else {
									updateListGithubRepo();
								}

							},error -> {
								pd.dismiss();
								String errorMessage = error;
								if (error.equals("E404"))
									errorMessage = getString(R.string.error_shared_not_found);
								// show error message
								new AlertDialog.Builder(Alberi.this)
										.setTitle(R.string.find_errors)
										.setMessage(errorMessage)
										.setCancelable(false)
										.setPositiveButton(R.string.OK, (dialog, which) -> {
											dialog.dismiss();
											finish();
										})
										.show();
							});
				})
				.setNeutralButton(R.string.review_changes, null).create();
		alertDialog.show();
		// override button
		alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
			if (isFinishing())
				return;

			Intent intent = new Intent(Alberi.this, CompareChangesActivity.class);
			intent.putExtra("compareType", CompareChangesActivity.CompareType.SubmitChanges);
			// before json: last time commit and head 0
			String jsonFileNameBefore = tree.id + ".head_0";
			intent.putExtra("jsonFileNameBefore", jsonFileNameBefore);
			// after json: current json file
			String jsonFileNameAfter = tree.id + ".json";
			intent.putExtra("jsonFileNameAfter", jsonFileNameAfter);
			intentLauncherCompareChanges.launch(intent);
		});
	}

	private ActivityResultLauncher<Intent> intentLauncherCompareChanges = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
			result -> {
		if (result.getResultCode() == Activity.RESULT_OK) {
			// do nothing --> because the action from ReviewChangesActivity is only close
		}
	});

	private void reFork(Settings.Tree tree, ProgressDialog pd, DialogInterface dialog) {
		// delete current repo and fork again
		final Repo repo = Helper.getRepo(new File(getFilesDir(), tree.id + ".repo"));
		final String parentRepoName = repo.parent.fullName;
		DeleteRepoTask.execute(Alberi.this,tree.id, tree.githubRepoFullName,() ->{},
				() -> {
					ForkRepoTask.execute(Alberi.this,
							parentRepoName, tree.id, () -> {
								// nothing yet
							}, infoModel  -> {
								// update tree info and save settings.json
								tree.submittedPRtoParent = false;
								tree.submittedPRtoParentMergeable = false;
								tree.submittedPRtoParentRejected = false;
								tree.title = infoModel.title;
								tree.persons =	infoModel.persons;
								tree.generations =	infoModel.generations;
								tree.root =	infoModel.root;
								tree.grade = infoModel.grade;
								tree.githubRepoFullName	= infoModel.githubRepoFullName;
								tree.isForked = true;
								tree.repoStatus = infoModel.repoStatus;
								tree.aheadBy = infoModel.aheadBy;
								tree.behindBy = infoModel.behindBy;
								tree.totalCommits = infoModel.totalCommits;
								Global.settings.save();
								if( !apriGedcom(tree.id, true) ) {
									rotella.setVisibility(View.GONE);
									return;
								}
								updateListGithubRepo();
								dialog.dismiss();
								pd.dismiss();
							}, error -> {
								updateListGithubRepo();
								dialog.dismiss();
								pd.dismiss();
								// show error message
								new AlertDialog.Builder(Alberi.this)
										.setTitle(R.string.find_errors)
										.setMessage(error)
										.setCancelable(false)
										.setPositiveButton(R.string.OK, (cdialog, cwhich) -> {
											cdialog.dismiss();
											rotella.setVisibility(View.GONE);
										})
										.show();
							}
					);
				},
				error -> {
					dialog.dismiss();
					pd.dismiss();
					new AlertDialog.Builder(Alberi.this)
							.setTitle(R.string.find_errors)
							.setMessage(error)
							.setCancelable(false)
							.setPositiveButton(R.string.OK, (gDialog, gwhich) -> gDialog.dismiss())
							.show();
				});
	}

	private void createRepo(String email, int treeId) {
		final ProgressDialog pd = new ProgressDialog(Alberi.this);
		Settings.Tree tree = Global.settings.getTree(treeId);
		// create summary info json, upload to repo
		FamilyGemTreeInfoModel treeInfoModel = new FamilyGemTreeInfoModel(
				tree.title, tree.persons,tree.generations,
				tree.media, tree.root, tree.grade
		);
		Gedcom treeGedcom = leggiJson(treeId);
		CreateRepoTask.execute(Alberi.this,
				treeId, email, treeInfoModel, treeGedcom,
				(_id, _m) -> {
					String filePath = F.percorsoMedia(_id, _m);
					if (filePath != null)
						return new File(filePath);
					else
						return null;
				},
				() -> {
					pd.setMessage(getString(R.string.uploading));
					pd.show();
				}, deeplink -> {
					// it should set repoFullName in settings.json file
					tree.githubRepoFullName = treeInfoModel.githubRepoFullName;
					Global.settings.save();
					pd.dismiss();
					View finishedDialogView = LayoutInflater.from(Alberi.this).inflate(R.layout.finished_dialog, null);
					AlertDialog.Builder finishedDialogBuilder = new AlertDialog.Builder(Alberi.this);
					finishedDialogBuilder.setView(finishedDialogView);
					final AppCompatButton okBtn = (AppCompatButton) finishedDialogView.findViewById(R.id.ok_btn);
					AppCompatTextView deeplinkTextView = (AppCompatTextView) finishedDialogView.findViewById(R.id.deeplink_url);
					deeplinkTextView.setText(deeplink);
					deeplinkTextView.setOnClickListener(v -> {
						ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
						ClipData clip = ClipData.newPlainText(getString(R.string.deeplink), deeplink);
						clipboard.setPrimaryClip(clip);
						Toast.makeText(Alberi.this, String.format(getString(R.string.copied_to_clipboard), deeplink), Toast.LENGTH_LONG).show();
					});
					updateListGithubRepo();
					final AlertDialog finishedDialog = finishedDialogBuilder.create();
					finishedDialog.show();
					okBtn.setOnClickListener(v -> finishedDialog.dismiss());
				}, error -> {
					pd.dismiss();
					// show error message
					new AlertDialog.Builder(Alberi.this)
							.setTitle(R.string.find_errors)
							.setMessage(error)
							.setCancelable(false)
							.setPositiveButton(R.string.OK, (dialog, which) -> dialog.dismiss())
							.show();
				}
		);
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
		// Nasconde la rotella, in particolare quando si ritorna indietro a questa activity
		rotella.setVisibility(View.GONE);
		new Handler(Looper.getMainLooper()).postDelayed(() -> {
			if (!isFinishing())
				updateListGithubRepo();
		}, 2000);
	}

	// Essendo Alberi launchMode=singleTask, onRestart viene chiamato anche con startActivity (tranne il primo)
	// però ovviamente solo se Alberi ha chiamato onStop (facendo veloce chiama solo onPause)
	@Override
	protected void onRestart() {
		super.onRestart();
		Log.d(TAG, "onRestart");
		aggiornaLista();
	}

	// New intent coming from a tapped notification
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		birthdayNotifyTapped(intent);
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		outState.putBoolean("autoOpenedTree", autoOpenedTree);
		outState.putIntegerArrayList("consumedNotifications", consumedNotifications);
		super.onSaveInstanceState(outState);
	}

	// If a birthday notification was tapped loads the relative tree and returns true
	private boolean birthdayNotifyTapped(Intent intent) {
		int treeId = intent.getIntExtra(Notifier.TREE_ID_KEY, 0);
		int notifyId = intent.getIntExtra(Notifier.NOTIFY_ID_KEY, 0);
		if( treeId > 0 && !consumedNotifications.contains(notifyId)) {
			new Handler().post(() -> {
				if( Alberi.apriGedcom(treeId, true) ) {
					rotella.setVisibility(View.VISIBLE);
					Global.indi = intent.getStringExtra(Notifier.INDI_ID_KEY);
					consumedNotifications.add(notifyId);
					startActivity(new Intent(this, Principal.class));
				}
			});
			return true;
		}
		return false;
	}

	// Cerca di recuperare dal Play Store il dataID casomai l'app sia stata installata in seguito ad una condivisione
	// Se trova il dataid propone di scaricare l'albero condiviso
	void recuperaReferrer() {
		InstallReferrerClient irc = InstallReferrerClient.newBuilder(this).build();
		irc.startConnection( new InstallReferrerStateListener() {
			@Override
			public void onInstallReferrerSetupFinished( int risposta ) {
				switch( risposta ) {
					case InstallReferrerClient.InstallReferrerResponse.OK:
						try {
							ReferrerDetails dettagli = irc.getInstallReferrer();
							// Normalmente 'referrer' è una stringa tipo 'utm_source=google-play&utm_medium=organic'
							// Ma se l'app è stata installata dal link nella pagina di condivisione sarà un data-id come '20191003215337'
							String referrer = dettagli.getInstallReferrer();
							if( referrer != null && referrer.matches("[0-9]{14}") ) { // It's a dateId
								Global.settings.referrer = referrer;
								new AlertDialog.Builder( Alberi.this ).setTitle( R.string.a_new_tree )
										.setMessage( R.string.you_can_download )
										.setPositiveButton( R.string.download, (dialog, id) -> {
											Facciata.scaricaCondiviso( Alberi.this, referrer, rotella );
										}).setNeutralButton( R.string.cancel, (di, id) -> welcome.show() )
										.setOnCancelListener( d -> welcome.show() ).show();
							} else { // È qualunque altra cosa
								Global.settings.referrer = null; // lo annulla così non lo cercherà più
								welcome.show();
							}
							Global.settings.save();
							irc.endConnection();
						} catch( Exception e ) {
							U.tosta( Alberi.this, e.getLocalizedMessage() );
						}
						break;
					// App Play Store inesistente sul device o comunque risponde in modo errato
					case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:
					// Questo non l'ho mai visto comparire
					case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:
						Global.settings.referrer = null; // così non torniamo più qui
						Global.settings.save();
						welcome.show();
				}
			}
			@Override
			public void onInstallReferrerServiceDisconnected() {
				// Mai visto comparire
				U.tosta( Alberi.this, "Install Referrer Service Disconnected" );
			}
		});
	}

	void aggiornaLista() {
		elencoAlberi.clear();
		for( Settings.Tree alb : Global.settings.trees ) {
			Map<String, String> dato = new HashMap<>(3);
			dato.put("id", String.valueOf(alb.id));
			dato.put("titolo", alb.title);
			// Se Gedcom già aperto aggiorna i dati
			if( Global.gc != null && Global.settings.openTree == alb.id && alb.persons < 100 )
				InfoAlbero.refreshData(Global.gc, alb);
			if (alb.isForked)
				dato.put("dati", scriviDati(this, alb) + getForkStatusString(alb));
			else if (alb.hasOpenPR != null && alb.hasOpenPR)
				dato.put("dati", scriviDati(this, alb) + " - " + getString(R.string.changes_proposed));
			else
				dato.put("dati", scriviDati(this, alb));
			elencoAlberi.add(dato);
		}
		adapter.notifyDataSetChanged();
	}

	void updateListGithubRepo() {
		for( Settings.Tree alb : Global.settings.trees ) {
			if (alb.githubRepoFullName == null || alb.githubRepoFullName.isEmpty())
				continue;

			// update tree list data
			for (Map<String, String> dato : elencoAlberi) {
				String datoId = dato.get("id");
				if (String.valueOf(alb.id).equals(datoId)) {
					FamilyGemTreeInfoModel infoModel = new FamilyGemTreeInfoModel(
							alb.title,
							alb.persons,
							alb.generations,
							alb.media,
							alb.root,
							alb.grade
					);
					if (isFinishing())
						return;
					if (alb.isForked) {
						// compare with parent repo
						CompareRepoTask.execute(Alberi.this, alb.githubRepoFullName, alb.id, infoModel,
								() -> {
									if (isFinishing())
										return;
									// save commit info
									alb.repoStatus = infoModel.repoStatus;
									alb.aheadBy = infoModel.aheadBy;
									alb.behindBy = infoModel.behindBy;
									alb.totalCommits = infoModel.totalCommits;
									alb.submittedPRtoParent = infoModel.submittedPRtoParent;
									alb.submittedPRtoParentRejected = infoModel.submittedPRtoParentRejected;
									alb.submittedPRtoParentMergeable = infoModel.submittedPRtoParentMergeable;
									alb.submittedMergeUpstream = infoModel.submittedPRfromParent;
									alb.submittedMergeUpstreamMergeable = infoModel.submittedPRfromParentMergeable;
									Global.settings.save();

									dato.put("titolo", alb.title);
									dato.put("dati", scriviDati(this, alb) + getForkStatusString(alb));

									// ask listview to refresh its data
									adapter.notifyDataSetChanged();
								},
								error -> {
//								Toast.makeText(Global.context, error, Toast.LENGTH_LONG).show();
								});
						break;
					} else {
						// get repo status based on open PR
						DoesOpenPRExistTask.execute(Alberi.this, alb.githubRepoFullName, hasOpenPR -> {
							alb.hasOpenPR = hasOpenPR;
							if (hasOpenPR)
								dato.put("dati", scriviDati(this, alb) + " - " + getString(R.string.changes_proposed));
							else
								dato.put("dati", scriviDati(this, alb));

							Global.settings.save();

							// ask listview to refresh its data
							adapter.notifyDataSetChanged();
						}, error -> {
							// do nothing
						});

					}
				}
			}
		}

	}

	String getForkStatusString(Settings.Tree tree) {
		if (tree.isForked) {
			/*
			"diverged",
			"ahead",
			"behind",
			"identical"
			 */
			String aheadInfo = null;
			if (tree.aheadBy != null && tree.aheadBy > 0) {
				if (tree.submittedPRtoParent == null || !tree.submittedPRtoParent)
					aheadInfo = tree.aheadBy + " " + getString(R.string.ahead);
				else if (tree.submittedPRtoParentMergeable != null && tree.submittedPRtoParentMergeable) {
					if (tree.submittedPRtoParentRejected != null && tree.submittedPRtoParentRejected)
						aheadInfo = getString(R.string.ahead_changes_submitted_rejected);
					else
						aheadInfo = getString(R.string.ahead_changes_submitted);
				} else {
					if (tree.submittedPRtoParentMergeable == null)
						aheadInfo = getString(R.string.ahead_and_waiting);
					else if (tree.submittedPRtoParentRejected != null && tree.submittedPRtoParentRejected)
						aheadInfo = getString(R.string.ahead_and_conflict_submitted_rejected);
					else
						aheadInfo = getString(R.string.ahead_and_conflict_submitted);
				}
			}

			String behindInfo = null;
			if (tree.behindBy != null && tree.behindBy > 0) {
				if (tree.submittedMergeUpstream == null || !tree.submittedMergeUpstream)
					behindInfo = tree.behindBy + " " + getString(R.string.behind);
				else if (tree.submittedMergeUpstreamMergeable != null && !tree.submittedMergeUpstreamMergeable)
					behindInfo = getString(R.string.behind_and_conflict_submitted);
			}

			if (aheadInfo != null && behindInfo != null)
				return " - " + aheadInfo + ", " + behindInfo;
			else if (aheadInfo != null)
				return " - " + aheadInfo;
			else if (behindInfo != null)
				return " - " + behindInfo;


			if ("identical".equals(tree.repoStatus))
				return  " - " + getString(R.string.identical);
//			else if ("diverged".equals(tree.repoStatus))
//				return  " - " + tree.aheadBy + " " + getString(R.string.ahead) + ", " +  tree.behindBy + " " + getString(R.string.behind);
		}
		return  "";
	}

	static String scriviDati(Context contesto, Settings.Tree alb) {
		String dati = alb.persons + " " +
				contesto.getString(alb.persons == 1 ? R.string.person : R.string.persons).toLowerCase();
		if( alb.persons > 1 && alb.generations > 0 )
			dati += " - " + alb.generations + " " +
					contesto.getString(alb.generations == 1 ? R.string.generation : R.string.generations).toLowerCase();
		if( alb.media > 0 )
			dati += " - " + alb.media + " " + contesto.getString(R.string.media).toLowerCase();
		return dati;
	}

	// Apertura del Gedcom temporaneo per estrarne info in Alberi
	static Gedcom apriGedcomTemporaneo(int idAlbero, boolean mettiInGlobale) {
		Gedcom gc;
		if( Global.gc != null && Global.settings.openTree == idAlbero )
			gc = Global.gc;
		else {
			gc = leggiJson(idAlbero);
			if( mettiInGlobale ) {
				Global.gc = gc; // per poter usare ad esempio U.unaFoto()
				Global.settings.openTree = idAlbero; // così Global.gc e Global.preferenze.idAprendo sono sincronizzati
			}
		}
		return gc;
	}

	// Apertura del Gedcom per editare tutto in Family Gem
	public static boolean apriGedcom(int idAlbero, boolean salvaPreferenze) {
		Global.gc = leggiJson(idAlbero);
		if( Global.gc == null )
			return false;
		if( salvaPreferenze ) {
			Global.settings.openTree = idAlbero;
			Global.settings.save();
		}
		Global.indi = Global.settings.getCurrentTree().root;
		Global.familyNum = 0; // eventualmente lo resetta se era > 0
		Global.daSalvare = false; // eventualmente lo resetta se era true
		return true;
	}

	// Legge il Json e restituisce un Gedcom
	static Gedcom leggiJson(int treeId) {
		Gedcom gedcom;
		File file = new File(Global.context.getFilesDir(), treeId + ".json");
		StringBuilder text = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line;
			while( (line = br.readLine()) != null ) {
				text.append(line);
				text.append('\n');
			}
			br.close();
		} catch( Exception | Error e ) {
			String message = e instanceof OutOfMemoryError ? Global.context.getString(R.string.not_memory_tree) : e.getLocalizedMessage();
			Toast.makeText(Global.context, message, Toast.LENGTH_LONG).show();
			return null;
		}
		String json = text.toString();
//		json = updateLanguage(json);
		gedcom = new JsonParser().fromJson(json);
		if( gedcom == null ) {
			Toast.makeText(Global.context, R.string.no_useful_data, Toast.LENGTH_LONG).show();
			return null;
		}

		// handle privacy
		Settings.Tree tree = Global.settings.getTree(treeId);
		if (tree != null && !tree.isForked && tree.githubRepoFullName != null) {
			gedcom.createIndexes();
			List<PrivatePerson> privatePersons = U.getPrivatePersons(treeId);
			for (PrivatePerson priv : privatePersons) {
				Person p = gedcom.getPerson(priv.personId);
				if (p != null) {
					p.setMedia(priv.mediaList);
					p.setEventsFacts(priv.eventFacts);
				}
			}
		}
		return gedcom;
	}

	// Replace Italian with English in Json tree data
	// Introduced in Family Gem 0.8
	static String updateLanguage(String json) {
		json = json.replace("\"zona\":", "\"zone\":");
		json = json.replace("\"famili\":", "\"kin\":");
		json = json.replace("\"passato\":", "\"passed\":");
		return json;
	}

	static void deleteTree(Context context, int treeId) {
		File treeFile = new File(context.getFilesDir(), treeId + ".json");
		treeFile.delete();
		File mediaDir = context.getExternalFilesDir(String.valueOf(treeId));
		deleteFilesAndDirs(mediaDir);
		if( Global.settings.openTree == treeId ) {
			Global.gc = null;
		}
		Global.settings.deleteTree(treeId);
		WorkManager.getInstance(context).cancelAllWorkByTag(Notifier.WORK_TAG + treeId);
	}

	static void deleteFilesAndDirs(File fileOrDirectory) {
		if( fileOrDirectory.isDirectory() ) {
			for( File child : fileOrDirectory.listFiles() )
				deleteFilesAndDirs(child);
		}
		fileOrDirectory.delete();
	}

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data ) {
		super.onActivityResult( requestCode, resultCode, data );
		if( resultCode == AppCompatActivity.RESULT_OK ) {
			Uri uri = data.getData();
			boolean result = false;
			if( requestCode == 636 ) { // Esporta il GEDCOM
				result = esportatore.esportaGedcom( uri );
			} else if( requestCode == 6219 ) { // Esporta il GEDCOM zippato coi media
				result = esportatore.esportaGedcomZippato( uri );
			} // Esporta il backup ZIP
			else if( requestCode == 327 ) {
				result = esportatore.esportaBackupZip( null, -1, uri );
			}
			if( result )
				Toast.makeText( Alberi.this, esportatore.messaggioSuccesso, Toast.LENGTH_SHORT ).show();
			else
				Toast.makeText( Alberi.this, esportatore.messaggioErrore, Toast.LENGTH_LONG ).show();
		}
	}

	Gedcom findErrors(final int treeId, final boolean correct) {
		Gedcom gc = leggiJson(treeId);
		if( gc == null ) {
			// todo fai qualcosa per recuperare un file introvabile..?
			return null;
		}
		int errors = 0;
		int num;
		// Radice in preferenze
		Settings.Tree albero = Global.settings.getTree(treeId);
		Person radica = gc.getPerson(albero.root);
		// Radice punta ad una persona inesistente
		if( albero.root != null && radica == null ) {
			if( !gc.getPeople().isEmpty() ) {
				if( correct ) {
					albero.root = U.trovaRadice(gc);
					Global.settings.save();
				} else errors++;
			} else { // albero senza persone
				if( correct ) {
					albero.root = null;
					Global.settings.save();
				} else errors++;
			}
		}
		// Oppure non è indicata una radice in preferenze pur essendoci persone nell'albero
		if( radica == null && !gc.getPeople().isEmpty() ) {
			if( correct ) {
				albero.root = U.trovaRadice(gc);
				Global.settings.save();
			} else errors++;
		}
		// O in preferenze è indicata una radiceCondivisione che non esiste
		Person radicaCondivisa = gc.getPerson(albero.shareRoot);
		if( albero.shareRoot != null && radicaCondivisa == null ) {
			if( correct ) {
				albero.shareRoot = null; // la elimina e basta
				Global.settings.save();
			} else errors++;
		}
		// Cerca famiglie vuote o con un solo membro per eliminarle
		for( Family f : gc.getFamilies() ) {
			if( f.getHusbandRefs().size() + f.getWifeRefs().size() + f.getChildRefs().size() <= 1 ) {
				if( correct ) {
					gc.getFamilies().remove(f); // così facendo lasci i ref negli individui orfani della famiglia a cui si riferiscono...
					// ma c'è il resto del correttore che li risolve
					break;
				} else errors++;
			}
		}
		// Silently delete empty list of families
		if( gc.getFamilies().isEmpty() && correct ) {
			gc.setFamilies(null);
		}
		// Riferimenti da una persona alla famiglia dei genitori e dei figli
		for( Person p : gc.getPeople() ) {
			for( ParentFamilyRef pfr : p.getParentFamilyRefs() ) {
				Family fam = gc.getFamily( pfr.getRef() );
				if( fam == null ) {
					if( correct ) {
						p.getParentFamilyRefs().remove( pfr );
						break;
					} else errors++;
				} else {
					num = 0;
					for( ChildRef cr : fam.getChildRefs() )
						if( cr.getRef() == null ) {
							if( correct ) {
								fam.getChildRefs().remove(cr);
								break;
							} else errors++;
						} else if( cr.getRef().equals(p.getId()) ) {
							num++;
							if( num > 1 && correct ) {
								fam.getChildRefs().remove( cr );
								break;
							}
						}
					if( num != 1 ) {
						if( correct && num == 0 ) {
							p.getParentFamilyRefs().remove( pfr );
							break;
						} else errors++;
					}
				}
			}
			// Remove empty list of parent family refs
			if( p.getParentFamilyRefs().isEmpty() && correct ) {
				p.setParentFamilyRefs(null);
			}
			for( SpouseFamilyRef sfr : p.getSpouseFamilyRefs() ) {
				Family fam = gc.getFamily(sfr.getRef());
				if( fam == null ) {
					if( correct ) {
						p.getSpouseFamilyRefs().remove(sfr);
						break;
					} else errors++;
				} else {
					num = 0;
					for( SpouseRef sr : fam.getHusbandRefs() )
						if( sr.getRef() == null ) {
							if( correct ) {
								fam.getHusbandRefs().remove(sr);
								break;
							} else errors++;
						} else if( sr.getRef().equals(p.getId()) ) {
							num++;
							if( num > 1 && correct ) {
								fam.getHusbandRefs().remove(sr);
								break;
							}
						}
					for( SpouseRef sr : fam.getWifeRefs() ) {
						if( sr.getRef() == null ) {
							if( correct ) {
								fam.getWifeRefs().remove(sr);
								break;
							} else errors++;
						} else if( sr.getRef().equals(p.getId()) ) {
							num++;
							if( num > 1 && correct ) {
								fam.getWifeRefs().remove(sr);
								break;
							}
						}
					}
					if( num != 1 ) {
						if( num == 0 && correct ) {
							p.getSpouseFamilyRefs().remove(sfr);
							break;
						} else errors++;
					}
				}
			}
			// Remove empty list of spouse family refs
			if( p.getSpouseFamilyRefs().isEmpty() && correct ) {
				p.setSpouseFamilyRefs(null);
			}
			// Riferimenti a Media inesistenti
			// ok ma SOLO per le persone, forse andrebbe fatto col Visitor per tutti gli altri
			num = 0;
			for( MediaRef mr : p.getMediaRefs() ) {
				Media med = gc.getMedia( mr.getRef() );
				if( med == null ) {
					if( correct ) {
						p.getMediaRefs().remove( mr );
						break;
					} else errors++;
				} else {
					if( mr.getRef().equals( med.getId() ) ) {
						num++;
						if( num > 1 )
							if( correct ) {
								p.getMediaRefs().remove( mr );
								break;
							} else errors++;
					}
				}
			}
		}
		// References from each family to the persons belonging to it
		for( Family f : gc.getFamilies() ) {
			// Husbands refs
			for( SpouseRef sr : f.getHusbandRefs() ) {
				Person husband = gc.getPerson(sr.getRef());
				if( husband == null ) {
					if( correct ) {
						f.getHusbandRefs().remove(sr);
						break;
					} else errors++;
				} else {
					num = 0;
					for( SpouseFamilyRef sfr : husband.getSpouseFamilyRefs() )
						if( sfr.getRef() == null ) {
							if( correct ) {
								husband.getSpouseFamilyRefs().remove(sfr);
								break;
							} else errors++;
						} else if( sfr.getRef().equals(f.getId()) ) {
							num++;
							if( num > 1 && correct ) {
								husband.getSpouseFamilyRefs().remove(sfr);
								break;
							}
						}
					if( num != 1 ) {
						if( num == 0 && correct ) {
							f.getHusbandRefs().remove(sr);
							break;
						} else errors++;
					}

				}
			}
			// Remove empty list of husband refs
			if( f.getHusbandRefs().isEmpty() && correct ) {
				f.setHusbandRefs(null);
			}
			// Wives refs
			for( SpouseRef sr : f.getWifeRefs() ) {
				Person wife = gc.getPerson(sr.getRef());
				if( wife == null ) {
					if( correct ) {
						f.getWifeRefs().remove(sr);
						break;
					} else errors++;
				} else {
					num = 0;
					for( SpouseFamilyRef sfr : wife.getSpouseFamilyRefs() )
						if( sfr.getRef() == null ) {
							if( correct ) {
								wife.getSpouseFamilyRefs().remove(sfr);
								break;
							} else errors++;
						} else if( sfr.getRef().equals(f.getId()) ) {
							num++;
							if( num > 1 && correct ) {
								wife.getSpouseFamilyRefs().remove(sfr);
								break;
							}
						}
					if( num != 1 ) {
						if( num == 0 && correct ) {
							f.getWifeRefs().remove(sr);
							break;
						} else errors++;
					}
				}
			}
			// Remove empty list of wife refs
			if( f.getWifeRefs().isEmpty() && correct ) {
				f.setWifeRefs(null);
			}
			// Children refs
			for( ChildRef cr : f.getChildRefs() ) {
				Person child = gc.getPerson( cr.getRef() );
				if( child == null ) {
					if( correct ) {
						f.getChildRefs().remove( cr );
						break;
					} else errors++;
				} else {
					num = 0;
					for( ParentFamilyRef pfr : child.getParentFamilyRefs() )
						if( pfr.getRef() == null ) {
							if( correct ) {
								child.getParentFamilyRefs().remove(pfr);
								break;
							} else errors++;
						} else if( pfr.getRef().equals(f.getId()) ) {
							num++;
							if( num > 1 && correct ) {
								child.getParentFamilyRefs().remove(pfr);
								break;
							}
						}
					if( num != 1 ) {
						if( num == 0 && correct ) {
							f.getChildRefs().remove(cr);
							break;
						} else errors++;
					}
				}
			}
			// Remove empty list of child refs
			if( f.getChildRefs().isEmpty() && correct ) {
				f.setChildRefs(null);
			}
		}

		// Aggiunge un tag 'TYPE' ai name type che non l'hanno
		for( Person person : gc.getPeople() ) {
			for( Name name : person.getNames() ) {
				if( name.getType() != null && name.getTypeTag() == null ) {
					if( correct ) name.setTypeTag("TYPE");
					else errors++;
				}
			}
		}

		// Aggiunge un tag 'FILE' ai Media che non l'hanno
		ListaMedia visitaMedia = new ListaMedia(gc, 0);
		gc.accept(visitaMedia);
		for( Media med : visitaMedia.lista ) {
			if( med.getFileTag() == null ) {
				if( correct ) med.setFileTag("FILE");
				else errors++;
			}
		}

		if( !correct ) {
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);
			dialog.setMessage(errors == 0 ? getText(R.string.all_ok) : getString(R.string.errors_found, errors));
			if( errors > 0 ) {
				dialog.setPositiveButton(R.string.correct, (dialogo, i) -> {
					dialogo.cancel();
					Gedcom gcCorretto = findErrors(treeId, true);
					U.salvaJson(gcCorretto, treeId);
					Global.gc = null; // così se era aperto poi lo ricarica corretto
					findErrors(treeId, false);    // riapre per ammirere il risultato
					aggiornaLista();
				});
			}
			dialog.setNeutralButton(android.R.string.cancel, null).show();
		}
		return gc;
	}

	private void importGedcomToNode(int treeId){
		Intent intent = new Intent(Alberi.this, SelectPersonActivity.class);
		intent.putExtra(SelectPersonActivity.EXTRA_TREE_ID, treeId);
		startActivity(intent);
	}
}