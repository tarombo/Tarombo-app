// Class that represents the preferences saved in 'settings.json'

package app.familygem;

import android.content.Context;
import android.widget.Toast;

import com.familygem.restapi.models.Repo;
import com.familygem.utility.Helper;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class Settings {

	String referrer; // È 'start' appena installata l'app (cioè quando non esiste
						// 'files/settings.json')
						// Se l'installazione proviene da una condivisione accoglie un dateId tipo
						// '20191003215337'
						// Ben presto diventa null e rimane tale, a meno di cancellare tutti i dati
	List<Tree> trees;
	public int openTree; // Number of the tree currently opened. 0 means not a particular tree.
	// Must be consistent with the 'Global.gc' opened tree.
	// It is not reset by closing the tree, to be reused by 'Load last opened tree
	// at startup'.
	boolean autoSave;
	boolean loadTree;
	public boolean expert;
	boolean shareAgreement;
	public String kinshipTerms = "general"; // Can be "general" or "batak_toba"
	Diagram diagram;

	public int max() {
		int num = 0;
		for (Tree c : trees) {
			if (c.id > num)
				num = c.id;
		}
		return num;
	}

	public void addTree(Tree c) {
		trees.add(c);
	}

	public void rename(int id, String nuovoNome) {
		for (Tree c : trees) {
			if (c.id == id) {
				c.title = nuovoNome;
				break;
			}
		}
		save();
	}

	public void deleteTree(int id) {
		for (Tree c : trees) {
			if (c.id == id) {
				trees.remove(c);
				break;
			}
		}
		if (id == openTree) {
			openTree = 0;
		}
		save();
	}

	public void save() {
		try {
			Gson gson = new Gson();
			String json = gson.toJson(this);
			FileUtils.writeStringToFile(new File(Global.context.getFilesDir(), "settings.json"), json, "UTF-8");
		} catch (Exception e) {
			Toast.makeText(Global.context, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
	}

	// The tree currently open
	public Tree getCurrentTree() {
		for (Tree alb : trees) {
			if (alb.id == openTree)
				return alb;
		}
		return null;
	}

	public Tree getTree(int treeId) {
		/*
		 * Da quando ho installato Android Studio 4.0, quando compilo con minifyEnabled
		 * true
		 * misteriosamente 'alberi' qui è null.
		 * Però non è null se DOPO c'è 'trees = Global.settings.trees'
		 * Davvero incomprensibile!
		 */
		if (trees == null) {
			trees = Global.settings.trees;
		}
		if (trees != null)
			for (Tree tree : trees) {
				if (tree.id == treeId) {
					if (tree.uris == null) // traghettatore inserito in Family Gem 0.7.15
						tree.uris = new LinkedHashSet<>();
					return tree;
				}
			}
		return null;
	}

	static class Diagram {
		int ancestors;
		int uncles;
		int descendants;
		int siblings;
		int cousins;
		boolean spouses;
	}

	public void defaultDiagram() {
		diagram = new Diagram();
		diagram.ancestors = 3;
		diagram.uncles = 2;
		diagram.descendants = 3;
		diagram.siblings = 2;
		diagram.cousins = 1;
		diagram.spouses = true;
	}

	public void initCreatedAt(Context context) {
		if (trees == null)
			return;

		for (Tree tree : trees) {
			if (tree == null)
				continue;

			if (tree.createdAt == null || tree.createdAt.equals("")) {
				tree.initCreatedAt(context);
			}
		}
	}
	/*
	 * "grado":
	 * 0 albero creato da zero in Italia
	 * rimane 0 anche aggiungendo il submitter principale, condividendolo e
	 * ricevendo novità
	 * 9 albero spedito per la condivisione in attesa di marchiare con 'passato'
	 * tutti i submitter
	 * 10 albero ricevuto tramite condivisione in Australia
	 * non potrà mai più ritornare 0
	 * 20 albero ritornato in Italia dimostratosi un derivato da uno zero (o da uno
	 * 10).
	 * solo se è 10 può diventare 20. Se per caso perde lo status di derivato
	 * ritorna 10 (mai 0)
	 * 30 albero derivato da cui sono state estratte tutte le novità OPPURE privo di
	 * novità già all'arrivo (grigio). Eliminabile
	 */

	public static class Tree {
		int id;
		String title;
		LinkedHashSet<String> dirs;
		LinkedHashSet<String> uris;
		int persons;
		int generations;
		int media;
		String root;
		List<Share> shares; // dati identificativi delle condivisioni attraverso il tempo e lo spazio
		String shareRoot; // id della Person radice dell'albero in Condivisione
		int grade; // grado della condivisione
		String githubRepoFullName;
		Boolean isForked = false;

		/*
		 * "status": {
		 * "type": "string",
		 * "enum": [
		 * "diverged",
		 * "ahead",
		 * "behind",
		 * "identical"
		 * ],
		 */
		public String repoStatus;
		public Integer aheadBy;
		public Integer behindBy;
		public Integer totalCommits;

		public Boolean submittedPRtoParent;
		public Boolean submittedPRtoParentMergeable;
		public Boolean submittedPRtoParentRejected;
		public Boolean submittedMergeUpstream;
		public Boolean submittedMergeUpstreamMergeable;

		public Boolean hasOpenPR;

		public String createdAt;
		public String updatedAt;

		Tree(int id, String title, String dir, int persons, int generations, String root, List<Share> shares, int grade,
				String githubRepoFullName,
				String createdAt, String updatedAt) {
			this.id = id;
			this.title = title;
			dirs = new LinkedHashSet<>();
			if (dir != null)
				dirs.add(dir);
			uris = new LinkedHashSet<>();
			this.persons = persons;
			this.generations = generations;
			this.root = root;
			this.shares = shares;
			this.grade = grade;
			this.githubRepoFullName = githubRepoFullName;
			this.createdAt = createdAt;
			this.updatedAt = updatedAt;

			if (this.createdAt == null || this.createdAt.equals("")) {
				this.createdAt = getDateTimeNow();
			}

			if (this.updatedAt == null || this.updatedAt.equals("")) {
				this.updatedAt = getDateTimeNow();
			}
		}

		public void addShare(Share share) {
			if (shares == null)
				shares = new ArrayList<>();
			shares.add(share);
		}

		public static String getDateTimeNow() {
			return DateToIsoString(DateTime.now(DateTimeZone.UTC));
		}

		public static String DateToIsoString(DateTime dateTime) {
			DateTimeFormatter formatter = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);
			return formatter.print(dateTime);
		}

		public void initCreatedAt(Context context) {
			if (this.createdAt != null)
				return;
			;

			int treeId = this.id;
			final File file = new File(context.getFilesDir(), treeId + ".json");
			final File fileRepo = new File(context.getFilesDir(), treeId + ".repo");
			if (fileRepo.exists()) {
				Repo repo = Helper.getRepo(fileRepo);
				this.createdAt = repo.createdAt;
				this.updatedAt = repo.updatedAt;
			} else if (file.exists()) {
				DateTime now = new DateTime(file.lastModified()).withZone(DateTimeZone.UTC);
				String nowString = DateToIsoString(now);
				this.createdAt = nowString;
				this.updatedAt = nowString;
			}
		}
	}

	// The essential data of a share
	public static class Share {
		String dateId; // on compressed date and time format: YYYYMMDDhhmmss
		String submitter; // Submitter id

		Share(String dateId, String submitter) {
			this.dateId = dateId;
			this.submitter = submitter;
		}
	}

	// Blueprint of the file 'settings.json' inside a backup, share or example ZIP
	// file
	// It contains basic info of the zipped tree
	public static class ZippedTree {
		String title;
		int persons;
		int generations;
		String root;
		List<Share> shares;
		int grade; // il grado di destinazione dell'albero zippato
		public String createdAt;
		public String updatedAt;

		ZippedTree(String title, int persons, int generations, String root, List<Share> shares, int grade,
				String createdAt, String updatedAt) {
			this.title = title;
			this.persons = persons;
			this.generations = generations;
			this.root = root;
			this.shares = shares;
			this.grade = grade;
			this.createdAt = createdAt;
			this.updatedAt = updatedAt;
		}

		public File save() {
			File fileSettaggi = new File(Global.context.getCacheDir(), "settings.json");
			Gson gson = new Gson();
			String salvando = gson.toJson(this);
			try {
				FileUtils.writeStringToFile(fileSettaggi, salvando, "UTF-8");
			} catch (Exception e) {
			}
			return fileSettaggi;
		}
	}
}