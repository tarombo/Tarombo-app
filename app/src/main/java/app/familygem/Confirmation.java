// Attività finale all'importazione delle novità in un albero già esistente

package app.familygem;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.ChildRef;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.ParentFamilyRef;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.Repository;
import org.folg.gedcom.model.Source;
import org.folg.gedcom.model.SpouseFamilyRef;
import org.folg.gedcom.model.SpouseRef;
import org.folg.gedcom.model.Submitter;
import org.folg.gedcom.model.Visitable;
import java.io.File;
import java.io.IOException;
import app.familygem.visitors.MediaContainers;
import app.familygem.visitors.NoteContainers;
import app.familygem.visitors.SourceCitationList;
import app.familygem.visitors.MediaList;
import app.familygem.R;

public class Confirmation extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.confirmation);
		if (!Comparison.getList().isEmpty()) {

			// Albero vecchio
			CardView card = findViewById(R.id.confirmation_old);
			Settings.Tree tree = Global.settings.getTree(Global.settings.openTree);
			((TextView) card.findViewById(R.id.comparison_title)).setText(tree.title);
			String txt = Trees.writeInfo(this, tree);
			((TextView) card.findViewById(R.id.comparison_text)).setText(txt);
			card.findViewById(R.id.comparison_date).setVisibility(View.GONE);

			int added = 0;
			int replaced = 0;
			int deleted = 0;
			for (Comparison.DiffItem diffItem : Comparison.getList()) {
				switch (diffItem.action) {
					case 1:
						added++;
						break;
					case 2:
						replaced++;
						break;
					case 3:
						deleted++;
				}
			}
			String text = getString(R.string.accepted_news, added + replaced + deleted, added, replaced,
					deleted);
			((TextView) findViewById(R.id.confirmation_text)).setText(text);

			findViewById(R.id.confirmation_cancel).setOnClickListener(v -> {
				Comparison.reset();
				startActivity(new Intent(Confirmation.this, Trees.class));
			});

			findViewById(R.id.confirmation_ok).setOnClickListener(v -> {
				// Modifica l'id e tutti i ref agli oggetti con doppiaOpzione e destino da
				// aggiungere
				boolean changesMade = false;
				for (Comparison.DiffItem diffItem : Comparison.getList()) {
					if (diffItem.dualOption && diffItem.action == 1) {
						String newId;
						changesMade = true;
						switch (diffItem.type) {
							case 1: // Note
								newId = generateNewId(Note.class);
								Note n2 = (Note) diffItem.object2;
								new NoteContainers(Global.gc2, n2, newId); // aggiorna tutti i ref alla nota
								n2.setId(newId); // poi aggiorna l'id della nota
								break;
							case 2: // Submitter
								newId = generateNewId(Submitter.class);
								((Submitter) diffItem.object2).setId(newId);
								break;
							case 3: // Repository
								newId = generateNewId(Repository.class);
								Repository repo2 = (Repository) diffItem.object2;
								for (Source fon : Global.gc2.getSources())
									if (fon.getRepositoryRef() != null
											&& fon.getRepositoryRef().getRef().equals(repo2.getId()))
										fon.getRepositoryRef().setRef(newId);
								repo2.setId(newId);
								break;
							case 4: // Media
								newId = generateNewId(Media.class);
								Media m2 = (Media) diffItem.object2;
								new MediaContainers(Global.gc2, m2, newId);
								m2.setId(newId);
								break;
							case 5: // Source
								newId = generateNewId(Source.class);
								Source s2 = (Source) diffItem.object2;
								SourceCitationList citations = new SourceCitationList(Global.gc2, s2.getId());
								for (SourceCitationList.Triplet tri : citations.list)
									tri.citation.setRef(newId);
								s2.setId(newId);
								break;
							case 6: // Person
								newId = generateNewId(Person.class);
								Person p2 = (Person) diffItem.object2;
								for (Family fam : Global.gc2.getFamilies()) {
									for (SpouseRef sr : fam.getHusbandRefs())
										if (sr.getRef().equals(p2.getId()))
											sr.setRef(newId);
									for (SpouseRef sr : fam.getWifeRefs())
										if (sr.getRef().equals(p2.getId()))
											sr.setRef(newId);
									for (ChildRef cr : fam.getChildRefs())
										if (cr.getRef().equals(p2.getId()))
											cr.setRef(newId);
								}
								p2.setId(newId);
								break;
							case 7: // Family
								newId = generateNewId(Family.class);
								Family f2 = (Family) diffItem.object2;
								for (Person per : Global.gc2.getPeople()) {
									for (ParentFamilyRef pfr : per.getParentFamilyRefs())
										if (pfr.getRef().equals(f2.getId()))
											pfr.setRef(newId);
									for (SpouseFamilyRef sfr : per.getSpouseFamilyRefs())
										if (sfr.getRef().equals(f2.getId()))
											sfr.setRef(newId);
								}
								f2.setId(newId);
						}
					}
				}
				if (changesMade)
					U.saveJson(Global.gc2, Global.treeId2);

				// La regolare aggiunta/sostituzione/eliminazione dei record da albero2 ad
				// albero
				for (Comparison.DiffItem diffItem : Comparison.getList()) {
					switch (diffItem.type) {
						case 1: // Nota
							if (diffItem.action > 1)
								Global.gc.getNotes().remove(diffItem.object);
							if (diffItem.action > 0 && diffItem.action < 3) {
								Global.gc.addNote((Note) diffItem.object2);
								copyAllFiles(diffItem.object2);
							}
							break;
						case 2: // Submitter
							if (diffItem.action > 1)
								Global.gc.getSubmitters().remove(diffItem.object);
							if (diffItem.action > 0 && diffItem.action < 3)
								Global.gc.addSubmitter((Submitter) diffItem.object2);
							break;
						case 3: // Repository
							if (diffItem.action > 1)
								Global.gc.getRepositories().remove(diffItem.object);
							if (diffItem.action > 0 && diffItem.action < 3) {
								Global.gc.addRepository((Repository) diffItem.object2);
								copyAllFiles(diffItem.object2);
							}
							break;
						case 4: // Media
							if (diffItem.action > 1)
								Global.gc.getMedia().remove(diffItem.object);
							if (diffItem.action > 0 && diffItem.action < 3) {
								Global.gc.addMedia((Media) diffItem.object2);
								checkAndCopyFile((Media) diffItem.object2);
							}
							break;
						case 5: // Source
							if (diffItem.action > 1)
								Global.gc.getSources().remove(diffItem.object);
							if (diffItem.action > 0 && diffItem.action < 3) {
								Global.gc.addSource((Source) diffItem.object2);
								copyAllFiles(diffItem.object2);
							}
							break;
						case 6: // Person
							if (diffItem.action > 1)
								Global.gc.getPeople().remove(diffItem.object);
							if (diffItem.action > 0 && diffItem.action < 3) {
								Global.gc.addPerson((Person) diffItem.object2);
								copyAllFiles(diffItem.object2);
							}
							break;
						case 7: // Family
							if (diffItem.action > 1)
								Global.gc.getFamilies().remove(diffItem.object);
							if (diffItem.action > 0 && diffItem.action < 3) {
								Global.gc.addFamily((Family) diffItem.object2);
								copyAllFiles(diffItem.object2);
							}
					}
				}
				U.saveJson(Global.gc, Global.settings.openTree);

				// Se ha fatto tutto propone di eliminare l'albero importato
				boolean allOk = true;
				for (Comparison.DiffItem diffItem : Comparison.getList())
					if (diffItem.action == 0) {
						allOk = false;
						break;
					}
				if (allOk) {
					Global.settings.getTree(Global.treeId2).grade = 30;
					Global.settings.save();
					new AlertDialog.Builder(Confirmation.this)
							.setMessage(R.string.all_imported_delete)
							.setPositiveButton(android.R.string.ok, (d, i) -> {
								Trees.deleteTree(this, Global.treeId2);
								finishActivity();
							}).setNegativeButton(R.string.no, (d, i) -> finishActivity())
							.setOnCancelListener(dialog -> finishActivity()).show();
				} else
					finishActivity();
			});
		} else
			onBackPressed();
	}

	// Apre l'elenco degli alberi
	void finishActivity() {
		Comparison.reset();
		startActivity(new Intent(this, Trees.class));
	}

	// Calcola l'id più alto per una certa classe confrontando albero nuovo e
	// vecchio
	String generateNewId(Class clazz) {
		String id = U.newId(Global.gc, clazz); // id nuovo rispetto ai record dell'albero vecchio
		String id2 = U.newId(Global.gc2, clazz); // e dell'albero nuovo
		if (Integer.valueOf(id.substring(1)) > Integer.valueOf(id2.substring(1))) // toglie la lettera iniziale
			return id;
		else
			return id2;
	}

	// Se un oggetto nuovo ha dei media, valuta se copiare i file nella cartella
	// immagini dell'albero vecchio
	// comunque aggiorna il collegamento nel Media
	void copyAllFiles(Object object) {
		MediaList mediaList = new MediaList(Global.gc2, 2);
		((Visitable) object).accept(mediaList);
		for (Media media : mediaList.list) {
			checkAndCopyFile(media);
		}
	}

	void checkAndCopyFile(Media media) {
		String sourcePath = F.getMediaPath(Global.treeId2, media);
		if (sourcePath != null) {
			File sourceFile = new File(sourcePath);
			File storageDir = getExternalFilesDir(String.valueOf(Global.settings.openTree)); // dovrebbe stare fuori dal
																								// loop ma vabè
			String fileName = sourcePath.substring(sourcePath.lastIndexOf('/') + 1);
			File existingFile = new File(storageDir.getAbsolutePath(), fileName);
			if (existingFile.isFile() // se il file corrispondente esiste già
					&& existingFile.lastModified() == sourceFile.lastModified() // e hanno la stessa data
					&& existingFile.length() == sourceFile.length()) { // e la stessa dimensione
				// Allora utilizza il file già esistente
				media.setFile(existingFile.getName());
			} else { // Altrimenti copia il file nuovo
				File destFile = F.getUniqueFile(storageDir.getAbsolutePath(), fileName);
				try {
					FileUtils.copyFile(sourceFile, destFile);
				} catch (IOException e) {
					e.printStackTrace();
				}
				media.setFile(destFile.getName());
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem i) {
		onBackPressed();
		return true;
	}
}