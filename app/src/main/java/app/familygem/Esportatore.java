package app.familygem;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.documentfile.provider.DocumentFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Header;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.visitors.GedcomWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import app.familygem.visita.ListaMedia;

public class Esportatore {

	private final Context contesto;
	private int idAlbero;
	private Gedcom gc;
	private Uri targetUri;
	private boolean useStandardId = false;
	public String messaggioErrore;  // Messaggio di eventuale errore
	public String messaggioSuccesso; // Messaggio del risultato ottenuto

	Esportatore(Context context) {
		this.contesto = context;
	}

	// Apre l'albero Json e restituisce true se c'è riuscito
	public boolean apriAlbero(int idAlbero) {
		this.useStandardId = false;
		this.idAlbero = idAlbero;
		gc = Alberi.apriGedcomTemporaneo(idAlbero, true);
		if( gc == null ) {
			return errore(R.string.no_useful_data);
		}
		return true;
	}

	// Scrive il solo GEDCOM nell'URI
	public boolean esportaGedcom(Uri targetUri) {
		this.targetUri = targetUri;
		aggiornaTestata(estraiNome(targetUri));
		ottimizzaGedcom();
		GedcomWriter scrittore = new GedcomWriter();
		File fileGc = new File(contesto.getCacheDir(), "temp.ged");
		try {
			scrittore.write(gc, fileGc);
			if(useStandardId){
				applyStandardId2(gc, fileGc);
			}
			OutputStream out = contesto.getContentResolver().openOutputStream(targetUri, "wt");
			FileUtils.copyFile(fileGc, out);
			out.flush();
			out.close();
		} catch( Exception e ) {
			return errore(e.getLocalizedMessage());
		}
		// Rende il file visibile da Windows
		// Ma pare inefficace in KitKat in cui il file rimane invisibile
		contesto.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, targetUri));
		Global.gc = Alberi.leggiJson(idAlbero); // Resetta le modifiche
		return successo(R.string.gedcom_exported_ok);
	}

	// Scrive il GEDCOM con i media in un file ZIP
	public boolean esportaGedcomZippato(Uri targetUri) {
		this.targetUri = targetUri;
		// Crea il file GEDCOM
		String titolo = Global.settings.getTree(idAlbero).title;
		String nomeFileGedcom = titolo.replaceAll("[\\\\/:*?\"<>|'$]", "_") + ".ged";
		aggiornaTestata(nomeFileGedcom);
		ottimizzaGedcom();
		GedcomWriter scrittore = new GedcomWriter();
		File fileGc = new File(contesto.getCacheDir(), nomeFileGedcom);
		try {
			scrittore.write(gc, fileGc);
		} catch( Exception e ) {
			return errore(e.getLocalizedMessage());
		}
		DocumentFile gedcomDocument = DocumentFile.fromFile(fileGc);
		// Aggiunge il GEDCOM alla raccolta di file media
		Map<DocumentFile, Integer> raccolta = raccogliMedia();
		raccolta.put(gedcomDocument, 0);
		if( !creaFileZip(raccolta) )
			return false;
		contesto.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, targetUri));
		Global.gc = Alberi.leggiJson(idAlbero);
		return successo(R.string.zip_exported_ok);
	}

	// Crea un file zippato con l'albero, i settaggi e i media
	public boolean esportaBackupZip(String radice, int grado, Uri targetUri) {
		this.targetUri = targetUri;
		// Media
		Map<DocumentFile, Integer> files = raccogliMedia();
		// Json dell'albero
		File fileTree = new File(contesto.getFilesDir(), idAlbero + ".json");
		files.put(DocumentFile.fromFile(fileTree), 1);
		// Json delle preferenze
		Settings.Tree tree = Global.settings.getTree(idAlbero);
		if( radice == null ) radice = tree.root;
		if( grado < 0 ) grado = tree.grade;
		// String titoloAlbero, String radice, int grado possono arrivare diversi da Condividi
		Settings.ZippedTree settaggi = new Settings.ZippedTree(
				tree.title, tree.persons, tree.generations, radice, tree.shares, grado, tree.createdAt, tree.updatedAt);
		File fileSettings = settaggi.salva();
		files.put(DocumentFile.fromFile(fileSettings), 0);
		if( !creaFileZip(files) )
			return false;
		contesto.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, targetUri));
		return successo(R.string.zip_exported_ok);
	}

	// Restituisce il numero di file media da allegare
	public int quantiFileMedia() {
		ListaMedia visitaMedia = new ListaMedia( gc, 0 );
		gc.accept( visitaMedia );
		int quantiFile = 0;
		for( Media med : visitaMedia.lista ) {
			if( F.percorsoMedia(idAlbero, med) != null || F.uriMedia( idAlbero, med ) != null )
				quantiFile++;
		}
		return quantiFile;
	}

	// Riceve l'id di un albero e restituisce una Map di DocumentFile dei media che riesce a rastrellare
	private Map<DocumentFile,Integer> raccogliMedia() {
		ListaMedia visitaMedia = new ListaMedia( gc, 0 );
		gc.accept( visitaMedia );
		/*  Capita che diversi Media puntino allo stesso file.
		*   E potrebbe anche capitare che diversi percorsi finiscano con nomi di file uguali,
		*   ad es. 'percorsoA/img.jpg' 'percorsoB/img.jpg'
		*   Bisogna evitare che nei media dello ZIP finiscano file con lo stesso nome.
		*   Questo loop crea una lista di percorsi con nome file univoci */
		Set<String> paths = new HashSet<>();
		Set<String> onlyFileNames = new HashSet<>(); // Nomi file di controllo
		for( Media med : visitaMedia.lista ) {
			String path = med.getFile();
			if( path != null && !path.isEmpty() ) {
				String fileName = path.replace('\\', '/');
				if( fileName.lastIndexOf('/') > -1 )
					fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
				if( !onlyFileNames.contains(fileName) )
					paths.add(path);
				onlyFileNames.add(fileName);
			}
		}
		Map<DocumentFile, Integer> collezione = new HashMap<>();
		for( String path : paths ) {
			Media med = new Media();
			med.setFile(FilenameUtils.getName(path));
			// Paths
			String percorsoMedia = F.percorsoMedia(idAlbero, med);
			if( percorsoMedia != null )
				collezione.put(DocumentFile.fromFile(new File(percorsoMedia)), 2); // todo canRead() ?
			else { // URIs
				Uri uriMedia = F.uriMedia(idAlbero, med);
				if( uriMedia != null )
					collezione.put(DocumentFile.fromSingleUri(contesto, uriMedia), 2);
			}
		}
		return collezione;
	}

	private void aggiornaTestata(String nomeFileGedcom) {
		Header testa = gc.getHeader();
		if( testa == null )
			gc.setHeader(AlberoNuovo.creaTestata(nomeFileGedcom));
		else {
			testa.setFile(nomeFileGedcom);
			testa.setDateTime(U.dataTempoAdesso());
		}
	}

	// Migliora il GEDCOM per l'esportazione
	void ottimizzaGedcom() {
		// Value dei nomi da given e surname
		for( Person pers : gc.getPeople() ) {
			for( Name n : pers.getNames() )
				if( n.getValue() == null && (n.getPrefix() != null || n.getGiven() != null
						|| n.getSurname() != null || n.getSuffix() != null) ) {
					String epiteto = "";
					if( n.getPrefix() != null )
						epiteto = n.getPrefix();
					if( n.getGiven() != null )
						epiteto += " " + n.getGiven();
					if( n.getSurname() != null )
						epiteto += " /" + n.getSurname() + "/";
					if( n.getSuffix() != null )
						epiteto += " " + n.getSuffix();
					n.setValue( epiteto.trim() );
				}
		}
	}

	// Estrae solo il nome del file da un URI
	private String estraiNome( Uri uri ) {
		// file://
		if( uri.getScheme() != null && uri.getScheme().equalsIgnoreCase("file") ) {
			return uri.getLastPathSegment();
		}
		// Cursore (di solito funziona questo)
		Cursor cursore = contesto.getContentResolver().query( uri, null, null, null, null);
		if( cursore != null && cursore.moveToFirst() ) {
			int indice = cursore.getColumnIndex( OpenableColumns.DISPLAY_NAME );
			String nomeFile = cursore.getString( indice );
			cursore.close();
			if( nomeFile != null ) return nomeFile;
		}
		// DocumentFile
		DocumentFile document = DocumentFile.fromSingleUri( contesto, targetUri );
		String nomeFile = document.getName();
		if( nomeFile != null ) return nomeFile;
		// Alla frutta
		return "tree.ged";
	}

	// Riceve la lista di DocumentFile e li mette in un file ZIP scritto nel targetUri
	// Restiuisce messaggio di errore o null se tutto a posto
	boolean creaFileZip(Map<DocumentFile, Integer> files) {
		byte[] buffer = new byte[128];
		try {
			ZipOutputStream zos = new ZipOutputStream(contesto.getContentResolver().openOutputStream(targetUri, "wt"));
			for( Map.Entry<DocumentFile, Integer> fileTipo : files.entrySet() ) {
				DocumentFile file = fileTipo.getKey();
				InputStream input = contesto.getContentResolver().openInputStream(file.getUri());
				String nomeFile = file.getName();   // File che non vengono rinominati ('settings.json', 'famiglia.ged')
				if( fileTipo.getValue() == 1 )
					nomeFile = "tree.json";
				else if( fileTipo.getValue() == 2 )
					nomeFile = "media/" + file.getName();
				zos.putNextEntry(new ZipEntry(nomeFile));
				int read;
				while( (read = input.read(buffer)) != -1 ) {
					zos.write(buffer, 0, read);
				}
				zos.closeEntry();
				input.close();
			}
			zos.close();
		} catch( IOException e ) {
			return errore(e.getLocalizedMessage());
		}
		return true;
	}

	public boolean successo( int messaggio ) {
		messaggioSuccesso = contesto.getString( messaggio );
		return true;
	}

	public boolean errore(int error) {
		return errore(contesto.getString(error));
	}
	public boolean errore(String error) {
		messaggioErrore = error;
		return false;
	}

	private void applyStandardId(){
		applyStandardIdToPeople();
		gc.createIndexes();
		applyStandardIdToFamilies();
		gc.createIndexes();
	}

	private void applyStandardIdToPeople(){
		List<Person> people = gc.getPeople();

		// Skip if all ID is standard
		boolean noGuidId = people.stream().noneMatch(x -> x.getId().contains("*"));
		if(noGuidId)
			return;

		// Convert standardId to guidId
		people.stream().filter(x -> !x.getId().contains("*")).forEach(x -> {
			String newId = U.nuovoId(gc, Person.class);
			U.changePersonId(x, newId, gc);
		});

		// If not then ensure all id has GUID. Assign new int ID ordered
		int id = 1;
		String prefix = U.getIdPrefix(Person.class);
		for(Person person: people){
			String newId = prefix + id++;
			U.changePersonId(person, newId, gc);
		}
	}

	private void applyStandardIdToFamilies(){
		List<Family> families = gc.getFamilies();

		// Skip if all ID is standard
		boolean noGuidId = families.stream().noneMatch(x -> x.getId().contains("*"));
		if(noGuidId)
			return;

		// Convert standardId to guidId
		families.stream().filter(x -> !x.getId().contains("*")).forEach(x -> {
			String newId = U.nuovoId(gc, Family.class);
			U.changeFamilyId(x, newId, gc);
		});

		// If not then ensure all id has GUID. Assign new int ID ordered
		int id = 1;
		String prefix = U.getIdPrefix(Family.class);
		for(Family family: families){
			String newId = prefix + id++;
			U.changeFamilyId(family, newId, gc);
		}
	}

	public void setUseStandardId(boolean value){
		this.useStandardId = value;
	}

	private void replaceIdsInFile(File file, Map<String, String> replaceMap){
		try {
			List<String> lines = FileUtils.readLines(file, StandardCharsets.UTF_8);
			Pattern pattern1 = Pattern.compile("(^\\d @)([FI]\\S+)(@ [A-Z]+)$");
			Pattern pattern2 = Pattern.compile("(^\\d [A-Z]+ @)([FI]\\S+)(@)$");
			List<Pattern> patterns = new ArrayList<>();
			patterns.add(pattern1);
			patterns.add(pattern2);
			for (int i = 0; i< lines.size(); i++){
				String line = lines.get(i);
				for(Pattern pattern : patterns){
					Matcher matcher = pattern.matcher(line);
					if(matcher.matches()){
						String key = matcher.group(2);
						if(replaceMap.containsKey(key)){
							String value = replaceMap.get(key);
							String line2 = matcher.group(1) + value + matcher.group(3);
							lines.set(i, line2);
							break;
						}
					}
				}
			}
			FileUtils.writeLines(file, lines);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<String, String> createPeopleReplaceMap(Gedcom gc){
		List<Person> people = gc.getPeople();
		Map<String, String> map = new HashMap<>();
		int index = 1;
		for(Person person: people){
			map.put(person.getId(), "I" + index ++);
		}

		return map;
	}

	private Map<String, String> createFamilyReplaceMap(Gedcom gc){
		List<Family> families = gc.getFamilies();
		Map<String, String> map = new HashMap<>();
		int index = 1;
		for(Family family: families){
			map.put(family.getId(), "F" + index ++);
		}

		return map;
	}

	private void applyStandardId2(Gedcom gc, File file){
		Map<String, String> peopleMap = createPeopleReplaceMap(gc);
		Map<String, String> familiesMap = createFamilyReplaceMap(gc);
		peopleMap.putAll(familiesMap);
		replaceIdsInFile(file, peopleMap);
	}
}
