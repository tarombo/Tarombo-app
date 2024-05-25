package app.familygem;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.familygem.utility.Helper;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.CharacterSet;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.GedcomVersion;
import org.folg.gedcom.model.Generator;
import org.folg.gedcom.model.Header;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;

public class AlberoNuovo extends AppCompatActivity {

	View rotella;

	@Override
	protected void onCreate( Bundle bandolo ) {
		super.onCreate( bandolo );
		setContentView(R.layout.albero_nuovo);
		rotella = findViewById( R.id.nuovo_circolo );
		String referrer = Global.settings.referrer; // Dataid proveniente da una condivisione
		boolean esisteDataId = referrer != null && referrer.matches("[0-9]{14}");

		// Scarica l'albero condiviso
		Button scaricaCondiviso = findViewById( R.id.bottone_scarica_condiviso );
		if( esisteDataId )
			// Non ha bisogno di permessi perché scarica e decomprime solo nello storage esterno dell'app
			scaricaCondiviso.setOnClickListener( v -> Facciata.scaricaCondiviso(this, referrer, rotella) );
		else
			scaricaCondiviso.setVisibility( View.GONE );

		// Crea un albero vuoto
		Button alberoVuoto = findViewById( R.id.bottone_albero_vuoto );
		if( esisteDataId ) {
			if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP )
				alberoVuoto.setBackgroundTintList( ColorStateList.valueOf(getResources().getColor(R.color.primarioChiaro)) );
		}
		alberoVuoto.setOnClickListener( v -> {
			View vistaMessaggio = LayoutInflater.from(this).inflate(R.layout.albero_nomina, null);
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setView( vistaMessaggio ).setTitle( R.string.title );
			TextView vistaTesto = vistaMessaggio.findViewById( R.id.nuovo_nome_testo );
			vistaTesto.setText( R.string.modify_later );
			vistaTesto.setVisibility( View.VISIBLE );
			EditText nuovoNome = vistaMessaggio.findViewById( R.id.nuovo_nome_albero );
			builder.setPositiveButton( R.string.create, (dialog, id) -> newTree(nuovoNome.getText().toString()) )
					.setNeutralButton( R.string.cancel, null ).create().show();
			nuovoNome.setOnEditorActionListener( (view, action, event) -> {
				if( action == EditorInfo.IME_ACTION_DONE ) {
					newTree( nuovoNome.getText().toString() );
					return true; // completa le azioni di salva()
				}
				return false; // Eventuali altri action che non esistono
			});
			vistaMessaggio.postDelayed( () -> {
				nuovoNome.requestFocus();
				InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				inputMethodManager.showSoftInput(nuovoNome, InputMethodManager.SHOW_IMPLICIT);
			}, 300);
		});

		Button scaricaEsempio = findViewById(R.id.bottone_scarica_esempio);
		// Non ha bisogno di permessi
		scaricaEsempio.setOnClickListener( v -> scaricaEsempio() );

		Button importaGedcom = findViewById(R.id.bottone_importa_gedcom);
		importaGedcom.setOnClickListener( v -> {
			if(Build.VERSION.SDK_INT <= 32){
				int perm = ContextCompat.checkSelfPermission(v.getContext(),Manifest.permission.READ_EXTERNAL_STORAGE);
				if( perm == PackageManager.PERMISSION_DENIED )
					ActivityCompat.requestPermissions( (AppCompatActivity)v.getContext(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1390 );
				else if( perm == PackageManager.PERMISSION_GRANTED )
					importaGedcom();
			}else{
				// No permission required on API 33+
				importaGedcom();
			}
		});

		findViewById(R.id.button_import_link).setOnClickListener( v -> {
			View prompt = getLayoutInflater().inflate(R.layout.prompt_text, null);
			new AlertDialog.Builder(this)
					.setTitle(R.string.please_input_link)
					.setView(prompt)
					.setCancelable(false)
					.setPositiveButton(R.string.OK, (d, i) -> {
						EditText inputText = prompt.findViewById(R.id.inputText);
						String link = inputText.getText().toString();
						Uri uri = Uri.parse(link);
						Intent intent = new Intent(this, Facciata.class);
						intent.setAction(Intent.ACTION_VIEW);
						intent.setData(uri);
						startActivity(intent);
					})
					.setNegativeButton(R.string.cancel, (d, i) ->{

					})
					.show();
		});

		Button recuperaBackup = findViewById(R.id.bottone_recupera_backup);
		recuperaBackup.setOnClickListener( v -> {
			Intent intent = new Intent( Intent.ACTION_GET_CONTENT );
			intent.setType( "application/zip" );
			startActivityForResult( intent, 219 );
		});
	}

	// Elabora la risposta alle richieste di permesso
	@Override
	public void onRequestPermissionsResult( int codice, String[] permessi, int[] accordi ) { // If request is cancelled, the result arrays are empty
		super.onRequestPermissionsResult(codice, permessi, accordi);
		if( accordi.length > 0 && accordi[0] == PackageManager.PERMISSION_GRANTED ) {
			if( codice == 1390 ) {
				importaGedcom();
			}
		}
	}

	// Create a brand new tree
	void newTree(String title) {
		int num = Global.settings.max() + 1;
		File jsonFile = new File(getFilesDir(), num + ".json");
		Global.gc = new Gedcom();
		Global.gc.setHeader(creaTestata(jsonFile.getName()));
		Global.gc.createIndexes();
		JsonParser jp = new JsonParser();
		try {
			FileUtils.writeStringToFile(jsonFile, jp.toJson(Global.gc), "UTF-8");
		} catch( Exception e ) {
			Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
			return;
		}
		Global.settings.aggiungi(new Settings.Tree(num, title, null, 0, 0, null, null, 0, null));
		Global.settings.openTree = num;
		Global.settings.save();
		onBackPressed();
		Toast.makeText(this, R.string.tree_created, Toast.LENGTH_SHORT).show();
	}

	// Scarica da Google Drive il file zip dei Simpson nella cache esterna dell'app, quindi senza bisogno di permessi
	void scaricaEsempio() {
		rotella.setVisibility( View.VISIBLE );
		DownloadManager gestoreScarico = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
		// Evita download multipli
		Cursor curso = gestoreScarico.query( new DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_RUNNING) );
		if( curso.moveToFirst() ) {
			curso.close();
			findViewById(R.id.bottone_scarica_esempio).setEnabled(false);
			return;
		}
		String url = "https://tarombo.siboro.org/TheSimpsons.zip";
		String percorsoZip = getExternalCacheDir() + "/the_Simpsons.zip";
		File fileZip = new File(percorsoZip);
		if( fileZip.exists() )
			fileZip.delete();

		BroadcastReceiver alCompletamento = new BroadcastReceiver() {
			@Override
			public void onReceive( Context contesto, Intent intento ) {
				decomprimiZip( contesto, percorsoZip, null );
				unregisterReceiver( this );
			}
		};

		// ACTION_DOWNLOAD_COMPLETE intende il completamento di QUALSIASI download che è in corso, non solo questo.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
			registerReceiver( alCompletamento, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), RECEIVER_EXPORTED );
		}
		else{
			registerReceiver( alCompletamento, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
		}

		DownloadManager.Request richiesta = new DownloadManager.Request( Uri.parse( url ) )
				.setTitle( getString(R.string.simpsons_tree) )
				.setDescription( getString(R.string.family_gem_example) )
				.setMimeType( "application/zip" )
				.setNotificationVisibility( DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
				.setDestinationUri( Uri.parse( "file://" + percorsoZip ) );
		gestoreScarico.enqueue( richiesta );
	}

	// Decomprime il file zip nella memoria esterna e cartella col numero dell'albero
	// Usato ugualmente da: esempio Simpson, file di backup e alberi condivisi
	static boolean decomprimiZip(Context context, String percorsoZip, Uri uriZip) {
		int treeNumber = Global.settings.max() + 1;
		File mediaDir = context.getExternalFilesDir(String.valueOf(treeNumber));
		String sourceDir = context.getApplicationInfo().sourceDir;
		if( !sourceDir.startsWith("/data/") ) { // App installed not in internal memory (hopefully moved to SD-card)
			File[] externalFilesDirs = context.getExternalFilesDirs(String.valueOf(treeNumber));
			if( externalFilesDirs.length > 1 ) {
				mediaDir = externalFilesDirs[1];
			}
		}
		try {
			InputStream is;
			if( percorsoZip != null )
				is = new FileInputStream(percorsoZip);
			else
				is = context.getContentResolver().openInputStream(uriZip);
			ZipInputStream zis = new ZipInputStream(is);
			ZipEntry zipEntry;
			int len;
			byte[] buffer = new byte[1024];
			File newFile;
			while( (zipEntry = zis.getNextEntry()) != null ) {
				if( zipEntry.getName().equals("tree.json") )
					newFile = new File(context.getFilesDir(), treeNumber + ".json");
				else if( zipEntry.getName().equals("settings.json") )
					newFile = new File(context.getCacheDir(), "settings.json");
				else {// It's a file from the 'media' folder
					newFile = new File(mediaDir, zipEntry.getName().replace("media/", ""));
					try {
						ensureZipPathSafety(newFile, mediaDir);
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}
				}
				FileOutputStream fos = new FileOutputStream(newFile);
				while( (len = zis.read(buffer)) > 0 ) {
					fos.write(buffer, 0, len);
				}
				fos.close();
			}
			zis.closeEntry();
			zis.close();

			// TODO: convert gedcom to use guid as id
			Gedcom gedcom = Alberi.leggiJson(treeNumber);
			String root = "";
			if (gedcom != null) {

			}
			// TODO: rename media file name to guid

			// Legge le impostazioni e le salva nelle preferenze
			File settingsFile = new File(context.getCacheDir(), "settings.json");
			String json = FileUtils.readFileToString(settingsFile, "UTF-8");
			json = updateLanguage(json);
			Gson gson = new Gson();
			Settings.ZippedTree cassa = gson.fromJson(json, Settings.ZippedTree.class);
			Settings.Tree tree = new Settings.Tree(treeNumber, cassa.title, mediaDir.getPath(),
					cassa.persons, cassa.generations, cassa.root, cassa.shares, cassa.grade, null);
			Global.settings.aggiungi(tree);
			settingsFile.delete();
			//fileZip.delete();
			// Albero proveniente da condivisione destinato al confronto
			if( cassa.grade == 9 && confronta(context, tree,false) ) {
				tree.grade = 20; // lo marchia come derivato
			}
			// Il download è avvenuto dal dialogo del referrer in Alberi
			if( context instanceof Alberi ) {
				Alberi alberi = (Alberi)context;
				alberi.runOnUiThread( () -> {
					alberi.rotella.setVisibility(View.GONE);
					alberi.aggiornaLista();
				});
			} else // Albero di esempio (Simpson) o di backup (da Facciata o da AlberoNuovo)
				context.startActivity(new Intent(context, Alberi.class));
			Global.settings.save();
			U.tosta((Activity)context, R.string.tree_imported_ok);
			return true;
		} catch( Exception e ) {
			U.tosta((Activity)context, e.getLocalizedMessage());
		}
		return false;
	}

	private static void ensureZipPathSafety(final File outputFile, final File destDirectory) throws Exception {
		String destDirCanonicalPath = destDirectory.getCanonicalPath();
		String outputFileCanonicalPath = outputFile.getCanonicalPath();
		if (!outputFileCanonicalPath.startsWith(destDirCanonicalPath)) {
			throw new Exception(String.format("Found Zip Path Traversal Vulnerability with %s", outputFileCanonicalPath));
		}
	}

	// Replace Italian with English in the Json settings of ZIP backup
	// Added in Family Gem 0.8
	static String updateLanguage(String json) {
		json = json.replace("\"generazioni\":", "\"generations\":");
		json = json.replace("\"grado\":", "\"grade\":");
		json = json.replace("\"individui\":", "\"persons\":");
		json = json.replace("\"radice\":", "\"root\":");
		json = json.replace("\"titolo\":", "\"title\":");
		json = json.replace("\"condivisioni\":", "\"shares\":");
		json = json.replace("\"data\":", "\"dateId\":");
		return json;
	}

	// Fa scegliere un file Gedcom da importare
	void importaGedcom() {
		Intent intent = new Intent( Intent.ACTION_GET_CONTENT );
		// KitKat disabilita i file .ged nella cartella Download se il type è 'application/*'
		if( Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT )
			intent.setType( "*/*" );
		else
			intent.setType( "application/*" );
		startActivityForResult( intent,630 );
	}

	@Override
	protected void onActivityResult( int requestCode, int resultCode, final Intent data ) {
		super.onActivityResult( requestCode, resultCode, data );
		// Importa un file Gedcom scelto con SAF
		if( resultCode == RESULT_OK && requestCode == 630 ){
			try {
				// Legge l'input
				Uri uri = data.getData();
				InputStream input = getContentResolver().openInputStream(uri);
				Gedcom gc = new ModelParser().parseGedcom( input );
				if( gc.getHeader() == null ) {
					Toast.makeText( this, R.string.invalid_gedcom, Toast.LENGTH_LONG ).show();
					return;
				}
//				gc.createIndexes(); // necessario per poi calcolare le generazioni
				Helper.makeGuidGedcom(gc);
				// Salva il file Json
				int nuovoNum = Global.settings.max() + 1;
				PrintWriter pw = new PrintWriter( getFilesDir() + "/" + nuovoNum + ".json" );
				JsonParser jp = new JsonParser();
				pw.print( jp.toJson(gc) );
				pw.close();
				// Nome albero e percorso della cartella
				String percorso = F.uriPercorsoFile( uri );
				String nomeAlbero;
				String percorsoCartella = null;
				if( percorso != null && percorso.lastIndexOf('/') > 0 ) { // è un percorso completo del file gedcom
					File fileGedcom = new File( percorso );
					percorsoCartella = fileGedcom.getParent();
					nomeAlbero = fileGedcom.getName();
				} else if( percorso != null ) { // È solo il nome del file 'famiglia.ged'
					nomeAlbero = percorso;
				} else // percorso null
					nomeAlbero = getString( R.string.tree ) + " " + nuovoNum;
				if( nomeAlbero.lastIndexOf('.') > 0 ) // Toglie l'estensione
					nomeAlbero = nomeAlbero.substring(0, nomeAlbero.lastIndexOf('.'));
				// Salva le impostazioni in preferenze
				String idRadice = U.trovaRadice(gc);
				Global.settings.aggiungi(new Settings.Tree(nuovoNum, nomeAlbero, percorsoCartella,
						gc.getPeople().size(), InfoAlbero.quanteGenerazioni(gc,idRadice), idRadice, null, 0, null));
				Global.settings.save();
				// Se necessario propone di mostrare le funzioni avanzate
				if( !gc.getSources().isEmpty() && !Global.settings.expert ) {
					new AlertDialog.Builder(this).setMessage( R.string.complex_tree_advanced_tools )
							.setPositiveButton( android.R.string.ok, (dialog, i) -> {
								Global.settings.expert = true;
								Global.settings.save();
								concludiImportaGedcom();
							}).setNegativeButton( android.R.string.cancel, (dialog, i) -> concludiImportaGedcom() )
							.show();
				} else
					concludiImportaGedcom();
			} catch( Exception e ) {
				Toast.makeText( this, e.getLocalizedMessage(), Toast.LENGTH_LONG ).show();
			}
		}

		// Estrae un file ZIP di backup, mantenendo la compatibilità con il precedente tipo di backup:
		// - un backup di tipo vecchio SOSTITUISCE tutti gli alberi esistenti
		// - un backup nuovo AGGIUNGE un singolo albero
		if( resultCode == RESULT_OK && requestCode == 219 ){
			try {
				Uri uri = data.getData();
				boolean ceFileSettings = false;
				boolean ceFilePreferenze = false;
				final ZipInputStream zis = new ZipInputStream( getContentResolver().openInputStream(uri) );
				ZipEntry zipEntry;
				while( (zipEntry = zis.getNextEntry()) != null ) {
					if(zipEntry.getName().equals( "settings.json" ))
						ceFileSettings = true;
					if(zipEntry.getName().equals( "preferenze.json" ))
						ceFilePreferenze = true;
				}
				zis.closeEntry();
				zis.close();
				if( ceFileSettings ) {
					decomprimiZip( this, null, uri );
					/* todo nello strano caso che viene importato col backup ZIP lo stesso albero suggerito dal referrer
					    bisognerebbe annullare il referrer:
						if( decomprimiZip( this, null, uri ) ){
						String idData = Esportatore.estraiNome(uri); // che però non è statico
						if( Global.preferenze.referrer.equals(idData) ) {
							Global.preferenze.referrer = null;
							Global.preferenze.salva();
						}}
					 */
				} else if(ceFilePreferenze){
					AlertDialog.Builder builder = new AlertDialog.Builder( AlberoNuovo.this );
					builder.setTitle( R.string.warning );
					builder.setMessage( R.string.old_backup_overwrite );
					builder.setPositiveButton( android.R.string.yes, (dialog, id) -> {
						try {
							ZipInputStream zis2 = new ZipInputStream( getContentResolver().openInputStream(uri) );
							ZipEntry zipEntry2;
							int len;
							byte[] buffer = new byte[1024];
							while( (zipEntry2 = zis2.getNextEntry()) != null ) {
								File newFile = new File( getFilesDir(), zipEntry2.getName() );
								FileOutputStream fos = new FileOutputStream(newFile);
								while ((len = zis2.read(buffer)) > 0) {
									fos.write(buffer, 0, len);
								}
								fos.close();
							}
							zis2.closeEntry();
							zis2.close();
							Global.start( getApplicationContext() );
							startActivity( new Intent( AlberoNuovo.this, Alberi.class ));
						} catch( IOException e ) {
							e.printStackTrace();
						}
					}).setNeutralButton( android.R.string.cancel, null ).create().show();
				} else
					Toast.makeText( AlberoNuovo.this, R.string.backup_invalid, Toast.LENGTH_LONG ).show();
			} catch( Exception e ) {
				Toast.makeText( AlberoNuovo.this, e.getLocalizedMessage(), Toast.LENGTH_LONG ).show();
			}
		}
	}

	void concludiImportaGedcom() {
		onBackPressed();
		Toast.makeText( this, R.string.tree_imported_ok, Toast.LENGTH_SHORT ).show();
	}

	// Confronta le date di invio degli alberi esistenti
	// Se trova almeno un albero originario tra quelli esistenti restituisce true
	// ed eventualmente apre il comparatore
	static boolean confronta(Context contesto, Settings.Tree albero2, boolean apriCompara) {
		if( albero2.shares != null )
			for( Settings.Tree alb : Global.settings.trees )
				if( alb.id != albero2.id && alb.shares != null && alb.grade != 20 && alb.grade != 30 )
					for( int i = alb.shares.size() - 1; i >= 0; i-- ) { // Le condivisioni dall'ultima alla prima
						Settings.Share share = alb.shares.get(i);
						for( Settings.Share share2 : albero2.shares )
							if( share.dateId != null && share.dateId.equals(share2.dateId) ) {
								if( apriCompara )
									contesto.startActivity(new Intent(contesto, Compara.class)
											.putExtra("idAlbero", alb.id)
											.putExtra("idAlbero2", albero2.id)
											.putExtra("idData", share.dateId)
									);
								return true;
							}
					}
		return false;
	}

	// Crea l'intestazione standard per questa app
	public static Header creaTestata(String nomeFile) {
		Header testa = new Header();
		Generator app = new Generator();
		app.setValue("TAROMBO");
		app.setName("Tarombo");
		app.setVersion(BuildConfig.VERSION_NAME);
		testa.setGenerator(app);
		testa.setFile(nomeFile);
		GedcomVersion versione = new GedcomVersion();
		versione.setForm("LINEAGE-LINKED");
		versione.setVersion("5.5.1");
		testa.setGedcomVersion(versione);
		CharacterSet codifica = new CharacterSet();
		codifica.setValue("UTF-8");
		testa.setCharacterSet(codifica);
		Locale loc = new Locale(Locale.getDefault().getLanguage());
		// C'è anche   Resources.getSystem().getConfiguration().locale.getLanguage() che ritorna lo stesso 'it'
		testa.setLanguage(loc.getDisplayLanguage(Locale.ENGLISH));    // ok prende la lingua di sistema in inglese, non nella lingua locale
		// in header ci sono due campi data: TRANSMISSION_DATE un po' forzatamente può contenere la data di ultima modifica
		testa.setDateTime(U.dataTempoAdesso());
		return testa;
	}

	// Freccia indietro nella toolbar come quella hardware
	@Override
	public boolean onOptionsItemSelected( MenuItem i ) {
		onBackPressed();
		return true;
	}
}