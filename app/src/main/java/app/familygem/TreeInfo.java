package app.familygem;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.familygem.action.SaveInfoFileTask;
import com.familygem.restapi.models.Repo;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;

import org.folg.gedcom.model.CharacterSet;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.GedcomVersion;
import org.folg.gedcom.model.Generator;
import org.folg.gedcom.model.Header;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.Submitter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.File;
import java.util.Locale;

import app.familygem.visitors.MediaList;
import app.familygem.R;
import app.familygem.BuildConfig;

public class TreeInfo extends AppCompatActivity {

	Gedcom gc;

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.info_albero);
		LinearLayout box = findViewById(R.id.info_scatola);

		final int treeId = getIntent().getIntExtra("idAlbero", 1);
		final Settings.Tree tree = Global.settings.getTree(treeId);
		final File file = new File(getFilesDir(), treeId + ".json");

		final File fileRepo = new File(getFilesDir(), treeId + ".repo");
		boolean isGithubInfoFileExist = fileRepo.exists();
		String title = getText(R.string.title) + ": " + tree.title;
		((TextView) findViewById(R.id.info_title)).setText(title);
		TextView linkTextView = findViewById(R.id.info_link);
		linkTextView.setVisibility(View.GONE);
		String i = "";

		DateTimeZone localeTz = DateTimeZone.getDefault();
		DateTimeFormatter formatter = DateTimeFormat.mediumDateTime().withZone(localeTz);

		String createdAt = "";
		String updatedAt = "";
		String ownerName = null;

		if (tree.createdAt != null) {
			createdAt = formatter.print(DateTime.parse(tree.createdAt));
		}

		if (tree.updatedAt != null) {
			updatedAt = formatter.print(DateTime.parse(tree.updatedAt));
		}

		TextView tvCreated = findViewById(R.id.info_created);
		tvCreated.setText(String.format("%s: %s", getString(R.string.created), createdAt));
		TextView tcUpdated = findViewById(R.id.info_updated);
		tcUpdated.setText(String.format("%s: %s", getString(R.string.last_updated_date_time), updatedAt));

		if (!file.exists()) {
			i += "\n\n" + getText(R.string.item_exists_but_file) + "\n" + file.getAbsolutePath();
		} else {
			String fileInfo = getText(R.string.file) + ": " + file.getAbsolutePath();
			((TextView) findViewById(R.id.info_file)).setText(fileInfo);

			String type = getString(R.string.offline);
			TextView infoType = findViewById(R.id.info_type);

			if (isGithubInfoFileExist) {
				Repo repo = Helper.getRepo(fileRepo);
				if (repo.fork) {
					String sourceLink = Helper.generateDeepLink(repo.source.fullName);
					type = String.format("%s %s", getString(R.string.subscribed_from), sourceLink);
					if (repo.source != null && repo.source.owner != null) {
						ownerName = repo.source.owner.login;
					}
				} else {
					if (repo.forksCount > 0) {
						type = getString(R.string.shared);
					} else {
						type = getString(R.string.online);
					}
					if (repo.owner != null) {
						ownerName = repo.owner.login;
					}
				}

				String deeplinkUrl = Helper.generateDeepLink(repo.fork ? repo.source.fullName : repo.fullName);
				String deeplinkInfo = getText(R.string.deeplink) + ": " + deeplinkUrl;
				TextView deeplinkTextView = (TextView) findViewById(R.id.info_deeplink);
				deeplinkTextView.setText(deeplinkInfo);
				deeplinkTextView.setOnClickListener(v -> {
					ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText(getString(R.string.deeplink), deeplinkUrl);
					clipboard.setPrimaryClip(clip);
					Toast.makeText(this, String.format(getString(R.string.copied_to_clipboard), deeplinkUrl),
							Toast.LENGTH_LONG).show();
				});

				if (repo.fullName != null || (repo.source != null && repo.source.fullName != null)) {
					String repoFullName = repo.fullName != null ? repo.fullName : repo.source.fullName;
					final String repoUrl = "https://github.com/" + repoFullName;
					String linkInfo = getText(R.string.link) + ": " + repoUrl;
					linkTextView.setText(linkInfo);
					linkTextView.setVisibility(View.VISIBLE);
					linkTextView.setOnClickListener(v -> {
						ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
						ClipData clip = ClipData.newPlainText(getString(R.string.link), repoUrl);
						clipboard.setPrimaryClip(clip);
						Toast.makeText(this, String.format(getString(R.string.copied_to_clipboard), repoUrl),
								Toast.LENGTH_LONG).show();
					});
				}
			}

			if (ownerName != null && !ownerName.isEmpty()) {
				type = type + " (" + ownerName + ")";
			}
			infoType.setText(String.format("%s: %s", getText(R.string.type), type));

			gc = Trees.openTemporaryGedcom(treeId, false);
			if (gc == null)
				i += "\n\n" + getString(R.string.no_useful_data);
			else {
				// Aggiornamento dei dati automatico o su richiesta
				if (tree.persons < 100) {
					refreshData(gc, tree);
				} else {
					Button bottoneAggiorna = findViewById(R.id.info_aggiorna);
					bottoneAggiorna.setVisibility(View.VISIBLE);
					bottoneAggiorna.setOnClickListener(v -> {
						refreshData(gc, tree);
						recreate();
					});
				}
				i += "\n" + getText(R.string.persons) + ": " + tree.persons
						+ "\n" + getText(R.string.families) + ": " + gc.getFamilies().size()
						+ "\n" + getText(R.string.generations) + ": " + tree.generations
						+ "\n" + getText(R.string.media) + ": " + tree.media
						+ "\n" + getText(R.string.sources) + ": " + gc.getSources().size()
						+ "\n" + getText(R.string.repositories) + ": " + gc.getRepositories().size();
				if (tree.root != null) {
					i += "\n" + getText(R.string.root) + ": " + U.getPrincipalName(gc.getPerson(tree.root));
				}
				if (tree.shares != null && !tree.shares.isEmpty()) {
					i += "\n\n" + getText(R.string.shares) + ":";
					for (Settings.Share share : tree.shares) {
						i += "\n" + dateIdToDate(share.dateId);
						if (gc.getSubmitter(share.submitter) != null)
							i += " - " + authorName(gc.getSubmitter(share.submitter));
					}
				}
			}
		}
		((TextView) findViewById(R.id.info_statistiche)).setText(i);

		Button buttonHeader = box.findViewById(R.id.info_gestisci_testata);
		if (gc != null) {
			Header h = gc.getHeader();
			if (h == null) {
				buttonHeader.setText(R.string.create_header);
				buttonHeader.setOnClickListener(view -> {
					gc.setHeader(NewTree.creaTestata(file.getName()));
					U.saveJson(gc, treeId);
					recreate();
				});
			} else {
				box.findViewById(R.id.info_testata).setVisibility(View.VISIBLE);
				if (h.getFile() != null)
					put(getText(R.string.file), h.getFile());
				if (h.getCharacterSet() != null) {
					put(getText(R.string.characrter_set), h.getCharacterSet().getValue());
					put(getText(R.string.version), h.getCharacterSet().getVersion());
				}
				space(); // uno spazietto
				put(getText(R.string.language), h.getLanguage());
				space();
				put(getText(R.string.copyright), h.getCopyright());
				space();
				if (h.getGenerator() != null) {
					put(getText(R.string.software), h.getGenerator().getName() != null ? h.getGenerator().getName()
							: h.getGenerator().getValue());
					put(getText(R.string.version), h.getGenerator().getVersion());
					if (h.getGenerator().getGeneratorCorporation() != null) {
						put(getText(R.string.corporation), h.getGenerator().getGeneratorCorporation().getValue());
						if (h.getGenerator().getGeneratorCorporation().getAddress() != null)
							put(getText(R.string.address),
									h.getGenerator().getGeneratorCorporation().getAddress().getDisplayValue()); // non è
																												// male
						put(getText(R.string.telephone), h.getGenerator().getGeneratorCorporation().getPhone());
						put(getText(R.string.fax), h.getGenerator().getGeneratorCorporation().getFax());
					}
					space();
					if (h.getGenerator().getGeneratorData() != null) {
						put(getText(R.string.source), h.getGenerator().getGeneratorData().getValue());
						put(getText(R.string.date), h.getGenerator().getGeneratorData().getDate());
						put(getText(R.string.copyright), h.getGenerator().getGeneratorData().getCopyright());
					}
				}
				space();
				if (h.getSubmitter(gc) != null)
					put(getText(R.string.submitter), authorName(h.getSubmitter(gc))); // todo: renderlo cliccabile?
				if (gc.getSubmission() != null)
					put(getText(R.string.submission), gc.getSubmission().getDescription()); // todo: cliccabile
				space();
				if (h.getGedcomVersion() != null) {
					put(getText(R.string.gedcom), h.getGedcomVersion().getVersion());
					put(getText(R.string.form), h.getGedcomVersion().getForm());
				}
				put(getText(R.string.destination), h.getDestination());
				space();
				if (h.getDateTime() != null) {
					put(getText(R.string.date), h.getDateTime().getValue());
					put(getText(R.string.time), h.getDateTime().getTime());
				}
				space();
				for (Extension est : U.findExtensions(h)) { // ogni estensione nella sua riga
					put(est.name, est.text);
				}
				space();
				if (divider != null)
					((TableLayout) findViewById(R.id.info_tabella)).removeView(divider);

				// Bottone per aggiorna l'header GEDCOM coi parametri di Family Gem
				buttonHeader.setOnClickListener(view -> {
					h.setFile(treeId + ".json");
					CharacterSet caratteri = h.getCharacterSet();
					if (caratteri == null) {
						caratteri = new CharacterSet();
						h.setCharacterSet(caratteri);
					}
					caratteri.setValue("UTF-8");
					caratteri.setVersion(null);

					Locale loc = new Locale(Locale.getDefault().getLanguage());
					h.setLanguage(loc.getDisplayLanguage(Locale.ENGLISH));

					Generator programma = h.getGenerator();
					if (programma == null) {
						programma = new Generator();
						h.setGenerator(programma);
					}
					programma.setValue("FAMILY_GEM");
					programma.setName(getString(R.string.app_name));
					// programma.setVersion( BuildConfig.VERSION_NAME ); // lo farà saveJson()
					programma.setGeneratorCorporation(null);

					GedcomVersion versioneGc = h.getGedcomVersion();
					if (versioneGc == null) {
						versioneGc = new GedcomVersion();
						h.setGedcomVersion(versioneGc);
					}
					versioneGc.setVersion("5.5.1");
					versioneGc.setForm("LINEAGE-LINKED");
					h.setDestination(null);

					U.saveJson(gc, treeId);
					recreate();
				});

				U.addNotes(box, h, true);
			}
			// Estensioni del Gedcom, ovvero tag non standard di livello 0 zero
			for (Extension est : U.findExtensions(gc)) {
				U.addItem(box, est.name, est.text);
			}
		} else
			buttonHeader.setVisibility(View.GONE);
	}

	String dateIdToDate(String id) {
		if (id == null)
			return "";
		return id.substring(0, 4) + "-" + id.substring(4, 6) + "-" + id.substring(6, 8) + " "
				+ id.substring(8, 10) + ":" + id.substring(10, 12) + ":" + id.substring(12);
	}

	static String authorName(Submitter autor) {
		String nome = autor.getName();
		if (nome == null)
			nome = "[" + Global.context.getString(R.string.no_name) + "]";
		else if (nome.isEmpty())
			nome = "[" + Global.context.getString(R.string.empty_name) + "]";
		return nome;
	}

	// Refresh the data displayed below the tree title in Trees list
	static void refreshData(Gedcom gedcom, Settings.Tree treeItem) {
		treeItem.persons = gedcom.getPeople().size();
		treeItem.generations = countGenerations(gedcom, U.getRootId(gedcom, treeItem));
		MediaList visitaMedia = new MediaList(gedcom, 0);
		gedcom.accept(visitaMedia);
		treeItem.media = visitaMedia.list.size();
		Global.settings.save();
		if (treeItem.githubRepoFullName != null)
			Helper.requireEmail(Global.context, Global.context.getString(R.string.set_email_for_commit),
					Global.context.getString(R.string.OK), Global.context.getString(R.string.cancel), email -> {
						FamilyGemTreeInfoModel infoModel = new FamilyGemTreeInfoModel(
								treeItem.title,
								treeItem.persons,
								treeItem.generations,
								treeItem.media,
								treeItem.root,
								treeItem.grade,
								treeItem.createdAt,
								treeItem.updatedAt);
						SaveInfoFileTask.execute(Global.context, treeItem.githubRepoFullName, email, treeItem.id,
								infoModel,
								() -> {
								}, () -> {
								},
								error -> Toast.makeText(Global.context, error, Toast.LENGTH_LONG).show());
					});
	}

	boolean textAdded; // impedisce di mettere più di uno spazietto consecutivo

	void put(CharSequence title, String text) {
		if (text != null) {
			TableRow row = new TableRow(this);
			TextView cell1 = new TextView(this);
			cell1.setTextSize(14);
			cell1.setTypeface(null, Typeface.BOLD);
			cell1.setPaddingRelative(0, 0, 10, 0);
			cell1.setGravity(Gravity.END); // Does not work on RTL layout
			cell1.setText(title);
			row.addView(cell1);
			TextView cell2 = new TextView(this);
			cell2.setTextSize(14);
			cell2.setPadding(0, 0, 0, 0);
			cell2.setGravity(Gravity.START);
			cell2.setText(text);
			row.addView(cell2);
			((TableLayout) findViewById(R.id.info_tabella)).addView(row);
			textAdded = true;
		}
	}

	TableRow divider;

	void space() {
		if (textAdded) {
			divider = new TableRow(getApplicationContext());
			View cella = new View(getApplicationContext());
			cella.setBackgroundResource(R.color.primario);
			divider.addView(cella);
			TableRow.LayoutParams param = (TableRow.LayoutParams) cella.getLayoutParams();
			param.weight = 1;
			param.span = 2;
			param.height = 1;
			param.topMargin = 5;
			param.bottomMargin = 5;
			cella.setLayoutParams(param);
			((TableLayout) findViewById(R.id.info_tabella)).addView(divider);
			textAdded = false;
		}
	}

	static int genMin;
	static int genMax;

	public static int countGenerations(Gedcom gc, String root) {
		if (gc.getPeople().isEmpty())
			return 0;
		genMin = 0;
		genMax = 0;
		ascendGenerations(gc.getPerson(root), gc, 0);
		descendGenerations(gc.getPerson(root), gc, 0);
		// Rimuove dalle persone l'estensione 'gen' per permettere successivi conteggi
		for (Person person : gc.getPeople()) {
			person.getExtensions().remove("gen");
			if (person.getExtensions().isEmpty())
				person.setExtensions(null);
		}
		return 1 - genMin + genMax;
	}

	// riceve una Person e trova il numero della generazione di antenati più remota
	static void ascendGenerations(Person person, Gedcom gc, int gen) {
		if (gen < genMin)
			genMin = gen;
		// aggiunge l'estensione per indicare che è passato da questa Persona
		person.putExtension("gen", gen);
		// se è un capostipite va a contare le generazioni di discendenti o risale su
		// eventuali altri matrimoni
		if (person.getParentFamilies(gc).isEmpty())
			descendGenerations(person, gc, gen);
		for (Family family : person.getParentFamilies(gc)) {
			// intercetta eventuali fratelli della radice
			for (Person sibling : family.getChildren(gc))
				if (sibling.getExtension("gen") == null)
					descendGenerations(sibling, gc, gen);
			for (Person father : family.getHusbands(gc))
				if (father.getExtension("gen") == null)
					ascendGenerations(father, gc, gen - 1);
			for (Person mother : family.getWives(gc))
				if (mother.getExtension("gen") == null)
					ascendGenerations(mother, gc, gen - 1);
		}
	}

	// riceve una Person e trova il numero della generazione più remota di
	// discendenti
	static void descendGenerations(Person person, Gedcom gc, int gen) {
		if (gen > genMax)
			genMax = gen;
		person.putExtension("gen", gen);
		for (Family family : person.getSpouseFamilies(gc)) {
			// individua anche la famiglia dei coniugi
			for (Person wife : family.getWives(gc))
				if (wife.getExtension("gen") == null)
					ascendGenerations(wife, gc, gen);
			for (Person husband : family.getHusbands(gc))
				if (husband.getExtension("gen") == null)
					ascendGenerations(husband, gc, gen);
			for (Person child : family.getChildren(gc))
				if (child.getExtension("gen") == null)
					descendGenerations(child, gc, gen + 1);
		}
	}

	// freccia indietro nella toolbar come quella hardware
	@Override
	public boolean onOptionsItemSelected(MenuItem i) {
		onBackPressed();
		return true;
	}
}
