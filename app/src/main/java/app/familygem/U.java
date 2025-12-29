// Useful tools for the entire program
// Attrezzi utili per tutto il programma

package app.familygem;

import static app.familygem.TreeSplitter.cloneEventFact;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import app.familygem.visitors.NoteReferences;
import com.familygem.action.SaveInfoFileTask;
import com.familygem.action.SaveTreeFileTask;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;
import com.familygem.utility.PrivatePerson;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.folg.gedcom.model.Change;
import org.folg.gedcom.model.DateTime;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Header;
import org.folg.gedcom.model.Repository;
import org.folg.gedcom.model.SpouseFamilyRef;
import org.folg.gedcom.model.SpouseRef;
import org.folg.gedcom.model.Submitter;
import org.folg.gedcom.parser.GedcomTypeAdapter;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.ExtensionContainer;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.GedcomTag;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.MediaContainer;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.NoteContainer;
import org.folg.gedcom.model.NoteRef;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.Source;
import org.folg.gedcom.model.SourceCitation;
import org.folg.gedcom.model.SourceCitationContainer;
import org.folg.gedcom.parser.JsonParser;
import org.joda.time.Months;
import org.joda.time.Years;
import app.familygem.constants.Format;
import app.familygem.constants.Gender;
import app.familygem.dettaglio.ArchivioRef;
import app.familygem.dettaglio.Autore;
import app.familygem.dettaglio.Cambiamenti;
import app.familygem.dettaglio.CitazioneFonte;
import app.familygem.dettaglio.Famiglia;
import app.familygem.dettaglio.Fonte;
import app.familygem.dettaglio.Immagine;
import app.familygem.dettaglio.Nota;
import app.familygem.visitors.MediaContainerList;
import app.familygem.visitors.NoteReferences;
import app.familygem.visitors.FindStack;
import app.familygem.R;

/** Useful tools for the entire program */
public class U {

	static String getString(int id) {
		return Global.context.getString(id);
	}

	// Da usare dove capita che 'Global.gc' possa essere null per ricaricarlo
	static void getSafeGedcom(Gedcom gc) {
		if (gc == null)
			Global.gc = Trees.readJson(Global.settings.openTree);
	}

	// Id of the main person of a GEDCOM or null
	static String getRootId(Gedcom gedcom, Settings.Tree tree) {
		if (tree.root != null) {
			Person root = gedcom.getPerson(tree.root);
			if (root != null)
				return root.getId();
		}
		return findRoot(gedcom);
	}

	// restituisce l'id della Person iniziale di un Gedcom
	// Todo Integrate into getRootId(Gedcom,Tree) ???
	static String findRoot(Gedcom gc) {
		if (gc.getHeader() != null)
			if (getTagValue(gc.getHeader().getExtensions(), "_ROOT") != null)
				return getTagValue(gc.getHeader().getExtensions(), "_ROOT");
		if (!gc.getPeople().isEmpty())
			return gc.getPeople().get(0).getId();
		return null;
	}

	// riceve una Person e restituisce stringa con nome e cognome principale
	public static String getPrincipalName(Person p) {
		if (p != null && !p.getNames().isEmpty())
			return getFullName(p.getNames().get(0));
		return "[" + getString(R.string.no_name) + "]";
	}

	// The given name of a person or something
	public static String givenName(Person person) {
		if (person.getNames().isEmpty()) {
			return "[" + getString(R.string.no_name) + "]";
		} else {
			String given = "";
			Name name = person.getNames().get(0);
			if (name.getValue() != null) {
				String value = name.getValue().trim();
				if (value.indexOf('/') == 0 && value.lastIndexOf('/') == 1 && value.length() > 2) // Suffix only
					given = value.substring(2);
				else if (value.indexOf('/') == 0 && value.lastIndexOf('/') > 1) // Surname only
					given = value.substring(1, value.lastIndexOf('/'));
				else if (value.indexOf('/') > 0) // Name and surname
					given = value.substring(0, value.indexOf('/'));
				else if (!value.isEmpty()) // Name only
					given = value;
			} else if (name.getGiven() != null) {
				given = name.getGiven();
			} else if (name.getSurname() != null) {
				given = name.getSurname();
			}
			given = given.trim();
			return given.isEmpty() ? "[" + getString(R.string.empty_name) + "]" : given;
		}
	}

	// riceve una Person e restituisce il titolo nobiliare
	public static String getTitle(Person p) {
		// GEDCOM standard INDI.TITL
		for (EventFact ef : p.getEventsFacts())
			if (ef.getTag() != null && ef.getTag().equals("TITL") && ef.getValue() != null)
				return ef.getValue();
		// Così invece prende INDI.NAME._TYPE.TITL, vecchio metodo di org.folg.gedcom
		for (Name n : p.getNames())
			if (n.getType() != null && n.getType().equals("TITL") && n.getValue() != null)
				return n.getValue();
		return "";
	}

	// RestitleIduisce il nome e cognome addobbato di un Name
	static String getFullName(Name n) {
		String completo = "";
		if (n.getValue() != null) {
			String grezzo = n.getValue().trim();
			int slashPos = grezzo.indexOf('/');
			int lastSlashPos = grezzo.lastIndexOf('/');
			if (slashPos > -1) // Se c'è un cognome tra '/'
				completo = grezzo.substring(0, slashPos).trim(); // nome
			else // Oppure è solo nome senza cognome
				completo = grezzo;
			if (n.getNickname() != null)
				completo += " \"" + n.getNickname() + "\"";
			if (slashPos < lastSlashPos)
				completo += " " + grezzo.substring(slashPos + 1, lastSlashPos).trim(); // cognome
			if (lastSlashPos > -1 && grezzo.length() - 1 > lastSlashPos)
				completo += " " + grezzo.substring(lastSlashPos + 1).trim(); // dopo il cognome
		} else {
			if (n.getPrefix() != null)
				completo = n.getPrefix();
			if (n.getGiven() != null)
				completo += " " + n.getGiven();
			if (n.getNickname() != null)
				completo += " \"" + n.getNickname() + "\"";
			if (n.getSurname() != null)
				completo += " " + n.getSurname();
			if (n.getSuffix() != null)
				completo += " " + n.getSuffix();
		}
		completo = completo.trim();
		return completo.isEmpty() ? "[" + getString(R.string.empty_name) + "]" : completo;
	}

	// RestitleIduisce il cognome di una persona
	static String getSurname(Person p) {
		String cognome = "";
		if (!p.getNames().isEmpty()) {
			Name name = p.getNames().get(0);
			String grezzo = name.getValue();
			if (grezzo != null && grezzo.indexOf('/') < grezzo.lastIndexOf('/'))
				cognome = grezzo.substring(grezzo.indexOf('/') + 1, grezzo.lastIndexOf('/')).trim();
			else if (name.getSurname() != null)
				cognome = name.getSurname();
		}
		return cognome;
	}

	// Riceve una person e trova se è morto o seppellito
	public static boolean isDead(Person person) {
		for (EventFact eventFact : person.getEventsFacts()) {
			if (eventFact.getTag().equals("DEAT") || eventFact.getTag().equals("BURI"))
				return true;
		}
		return false;
	}

	// Check whether a family has a marriage event of type 'marriage'
	public static boolean areMarried(Family family) {
		if (family != null) {
			for (EventFact eventFact : family.getEventsFacts()) {
				String tag = eventFact.getTag();
				if (tag.equals("MARR")) {
					String type = eventFact.getType();
					if (type == null || type.isEmpty() || type.equals("marriage")
							|| type.equals("civil") || type.equals("religious") || type.equals("common law"))
						return true;
				} else if (tag.equals("MARB") || tag.equals("MARC") || tag.equals("MARL") || tag.equals("MARS"))
					return true;
			}
		}
		return false;
	}

	/**
	 * Write the basic dates of a person's life with the age
	 * 
	 * @param person   The dude to investigate
	 * @param vertical Dates and age can be written on multiple lines
	 * @return A string with date of birth an death
	 */
	public static String twoDates(Person person, boolean vertical) {
		String text = "";
		String endYear = "";
		Datatore start = null, end = null;
		boolean ageBelow = false;
		List<EventFact> facts = person.getEventsFacts();
		// Birth date
		for (EventFact fact : facts) {
			if (fact.getTag() != null && fact.getTag().equals("BIRT") && fact.getDate() != null) {
				start = new Datatore(fact.getDate());
				text = start.writeDate(false);
				break;
			}
		}
		// Death date
		for (EventFact fact : facts) {
			if (fact.getTag() != null && fact.getTag().equals("DEAT") && fact.getDate() != null) {
				end = new Datatore(fact.getDate());
				endYear = end.writeDate(false);
				if (!text.isEmpty() && !endYear.isEmpty()) {
					if (vertical && (text.length() > 7 || endYear.length() > 7)) {
						text += "\n";
						ageBelow = true;
					} else {
						text += " – ";
					}
				}
				text += endYear;
				break;
			}
		}
		// Otherwise find the first available date
		if (text.isEmpty()) {
			for (EventFact fact : facts) {
				if (fact.getDate() != null) {
					return new Datatore(fact.getDate()).writeDate(false);
				}
			}
		}
		// Add the age between parentheses
		if (start != null && start.isSingleKind() && !start.data1.isFormat(Format.D_M)) {
			LocalDate startDate = new LocalDate(start.data1.date); // Converted to joda time
			// If the person is still alive the end is now
			LocalDate now = LocalDate.now();
			if (end == null && startDate.isBefore(now)
					&& Years.yearsBetween(startDate, now).getYears() <= 120 && !isDead(person)) {
				end = new Datatore(now.toDate());
				endYear = end.writeDate(false);
			}
			if (end != null && end.isSingleKind() && !end.data1.isFormat(Format.D_M) && !endYear.isEmpty()) { // Plausible
																												// dates
				LocalDate endDate = new LocalDate(end.data1.date);
				if (startDate.isBefore(endDate) || startDate.isEqual(endDate)) {
					String units = "";
					int age = Years.yearsBetween(startDate, endDate).getYears();
					if (age < 2) {
						// Without day and/or month the years start at 1 January
						age = Months.monthsBetween(startDate, endDate).getMonths();
						units = " " + Global.context.getText(R.string.months);
						if (age < 2) {
							age = Days.daysBetween(startDate, endDate).getDays();
							units = " " + Global.context.getText(R.string.days);
						}
					}
					if (ageBelow)
						text += "\n";
					else
						text += " ";
					text += "(" + age + units + ")";
				}
			}
		}
		return text;
	}

	// Estrae i soli numeri da una stringa che può contenere anche lettere
	// Extracts only numbers from a string that can also contain letters
	// NumberOnly
	public static int extractNumbers(String id) {
		// ID format is [pre][running_number]*[guid]
		int asterixIndex = id.indexOf('*');
		// return Integer.parseInt( id.replaceAll("\\D+","") ); // sintetico ma lento
		int num = 0;
		int x = 1;
		for (int i = asterixIndex - 1; i >= 0; --i) {
			int c = id.charAt(i);
			if (c > 47 && c < 58) {
				num += (c - 48) * x;
				x *= 10;
			}
		}
		return num;
	}

	// Genera il nuovo id seguente a quelli già esistenti
	// Generate the new id following the existing ones
	static int max;

	public static String newId(Gedcom gc, Class classe) {
		max = 0;
		String pre = "";
		if (classe == Note.class) {
			pre = "N";
			for (Note n : gc.getNotes())
				calculateMax(n);
		} else if (classe == Submitter.class) {
			pre = "U";
			for (Submitter a : gc.getSubmitters())
				calculateMax(a);
		} else if (classe == Repository.class) {
			pre = "R";
			for (Repository r : gc.getRepositories())
				calculateMax(r);
		} else if (classe == Media.class) {
			pre = "M";
			for (Media m : gc.getMedia())
				calculateMax(m);
		} else if (classe == Source.class) {
			pre = "S";
			for (Source f : gc.getSources())
				calculateMax(f);
		} else if (classe == Person.class) {
			pre = "I";
			for (Person p : gc.getPeople())
				calculateMax(p);
		} else if (classe == Family.class) {
			pre = "F";
			for (Family f : gc.getFamilies())
				calculateMax(f);
		}
		// ID format is [pre][running_number]*[guid]
		String newId = Helper.appendGuidToId(pre + (max + 1));
		return newId;
	}

	public static String getIdPrefix(Class type) {
		String prefix;

		if (type == Note.class) {
			prefix = "N";
		} else if (type == Submitter.class) {
			prefix = "U";
		} else if (type == Repository.class) {
			prefix = "R";
		} else if (type == Media.class) {
			prefix = "M";
		} else if (type == Source.class) {
			prefix = "S";
		} else if (type == Person.class) {
			prefix = "I";
		} else if (type == Family.class) {
			prefix = "F";
		} else {
			prefix = "";
		}

		return prefix;
	}

	private static void calculateMax(Object obj) {
		try {
			String idStringa = (String) obj.getClass().getMethod("getId").invoke(obj);
			int num = extractNumbers(idStringa);
			if (num > max)
				max = num;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Copia text negli appunti
	static void copyToClipboard(CharSequence label, CharSequence text) {
		ClipboardManager clipboard = (ClipboardManager) Global.context.getSystemService(Context.CLIPBOARD_SERVICE);
		ClipData clip = ClipData.newPlainText(label, text);
		if (clipboard != null)
			clipboard.setPrimaryClip(clip);
	}

	// RestitleIduisce la lista di estensioni
	@SuppressWarnings("unchecked")
	public static List<Extension> findExtensions(ExtensionContainer container) {
		if (container.getExtension("folg.more_tags") != null) {
			List<Extension> lista = new ArrayList<>();
			for (GedcomTag est : (List<GedcomTag>) container.getExtension("folg.more_tags")) {
				String text = digExtension(est, 0);
				if (text.endsWith("\n"))
					text = text.substring(0, text.length() - 1);
				lista.add(new Extension(est.getTag(), text, est));
			}
			return lista;
		}
		return Collections.emptyList();
	}

	// Costruisce un text con il contenuto ricorsivo dell'extension
	public static String digExtension(GedcomTag tag, int grado) {
		String text = "";
		if (grado > 0)
			text += tag.getTag() + " ";
		if (tag.getValue() != null)
			text += tag.getValue() + "\n";
		else if (tag.getId() != null)
			text += tag.getId() + "\n";
		else if (tag.getRef() != null)
			text += tag.getRef() + "\n";
		for (GedcomTag unPezzo : tag.getChildren())
			text += digExtension(unPezzo, ++grado);
		return text;
	}

	public static void removeExtension(GedcomTag extension, Object container, View view) {
		if (container instanceof ExtensionContainer) { // IndividuoEventi
			ExtensionContainer exc = (ExtensionContainer) container;
			@SuppressWarnings("unchecked")
			List<GedcomTag> lista = (List<GedcomTag>) exc.getExtension("folg.more_tags");
			lista.remove(extension);
			if (lista.isEmpty())
				exc.getExtensions().remove("folg.more_tags");
			if (exc.getExtensions().isEmpty())
				exc.setExtensions(null);
		} else if (container instanceof GedcomTag) { // Dettaglio
			GedcomTag gt = (GedcomTag) container;
			gt.getChildren().remove(extension);
			if (gt.getChildren().isEmpty())
				gt.setChildren(null);
		}
		Memoria.invalidateInstances(extension);
		if (view != null)
			view.setVisibility(View.GONE);
	}

	// RestitleIduisce il valore di un determinato tag in una extension (GedcomTag)
	@SuppressWarnings("unchecked")
	static String getTagValue(Map<String, Object> extensionsMap, String tagName) {
		for (Map.Entry<String, Object> extension : extensionsMap.entrySet()) {
			List<GedcomTag> listaTag = (ArrayList<GedcomTag>) extension.getValue();
			for (GedcomTag unPezzo : listaTag) {
				// l( unPezzo.getTag() +" "+ unPezzo.getValue() );
				if (unPezzo.getTag().equals(tagName)) {
					if (unPezzo.getId() != null)
						return unPezzo.getId();
					else if (unPezzo.getRef() != null)
						return unPezzo.getRef();
					else
						return unPezzo.getValue();
				}
			}
		}
		return null;
	}

	// Metodi di creazione di elementi di lista

	// aggiunge a un Layout una generica voce titolo-text
	// Usato seriamente solo da dettaglio.Cambiamenti
	public static void addItem(LinearLayout layout, String title, String text) {
		View viewPezzo = LayoutInflater.from(layout.getContext()).inflate(R.layout.pezzo_fatto, layout, false);
		layout.addView(viewPezzo);
		((TextView) viewPezzo.findViewById(R.id.fatto_titolo)).setText(title);
		TextView viewTesto = viewPezzo.findViewById(R.id.fatto_text);
		if (text == null)
			viewTesto.setVisibility(View.GONE);
		else {
			viewTesto.setText(text);
			// ((TextView)viewPezzo.findViewById( R.id.fatto_edita )).setText( text );
		}
		// ((Activity)layout.getContext()).registerForContextMenu( viewPezzo );
	}

	// Compone il text coi dettagli di un individuo e lo mette nella view text
	// inoltre restituisce lo stesso text per Confrontatore
	public static String details(Person person, TextView detailsView) {
		String dates = twoDates(person, false);
		String places = Anagrafe.twoPlaces(person);
		if (dates.isEmpty() && places == null && detailsView != null) {
			detailsView.setVisibility(View.GONE);
		} else {
			if (!dates.isEmpty() && places != null && (dates.length() >= 10 || places.length() >= 20))
				dates += "\n" + places;
			else if (places != null)
				dates += "   " + places;
			if (detailsView != null) {
				detailsView.setText(dates.trim());
				detailsView.setVisibility(View.VISIBLE);
			}
		}
		return dates.trim();
	}

	public static View addPerson(LinearLayout layout, Person persona, String ruolo) {
		View viewIndi = LayoutInflater.from(layout.getContext()).inflate(R.layout.pezzo_individuo, layout, false);
		layout.addView(viewIndi);

		TextView viewRuolo = viewIndi.findViewById(R.id.indi_ruolo);
		if (ruolo == null || ruolo.trim().isEmpty()) {
			viewRuolo.setVisibility(View.GONE);
		} else {
			viewRuolo.setVisibility(View.VISIBLE);
			viewRuolo.setText(ruolo);
		}

		TextView viewNome = viewIndi.findViewById(R.id.indi_nome);
		String nome = getPrincipalName(persona);
		if (nome.isEmpty() && ruolo != null)
			viewNome.setVisibility(View.GONE);
		else
			viewNome.setText(nome);
		TextView viewTitolo = viewIndi.findViewById(R.id.indi_titolo);
		String title = getTitle(persona);
		if (title.isEmpty())
			viewTitolo.setVisibility(View.GONE);
		else
			viewTitolo.setText(title);
		details(persona, viewIndi.findViewById(R.id.indi_dettagli));
		F.showPrimaryPhoto(Global.gc, persona, viewIndi.findViewById(R.id.indi_foto));
		if (!isDead(persona))
			viewIndi.findViewById(R.id.indi_lutto).setVisibility(View.GONE);
		if (Gender.isMale(persona))
			viewIndi.findViewById(R.id.indi_bordo).setBackgroundResource(R.drawable.casella_bordo_maschio);
		else if (Gender.isFemale(persona))
			viewIndi.findViewById(R.id.indi_bordo).setBackgroundResource(R.drawable.casella_bordo_femmina);
		viewIndi.setTag(persona.getId());
		return viewIndi;
	}

	// Tutte le note di un obj
	public static void addNotes(LinearLayout layout, Object container, boolean dettagli) {
		for (final Note nota : ((NoteContainer) container).getAllNotes(Global.gc)) {
			addNote(layout, nota, dettagli);
		}
	}

	// Aggiunge una singola nota a un layout, con i dettagli o no
	static void addNote(final LinearLayout layout, final Note nota, boolean dettagli) {
		final Context context = layout.getContext();
		View noteView = LayoutInflater.from(context).inflate(R.layout.pezzo_nota, layout, false);
		layout.addView(noteView);
		TextView textNota = noteView.findViewById(R.id.nota_text);
		textNota.setText(nota.getValue());
		int quanteCitaFonti = nota.getSourceCitations().size();
		TextView viewCitaFonti = noteView.findViewById(R.id.nota_fonti);
		if (quanteCitaFonti > 0 && dettagli)
			viewCitaFonti.setText(String.valueOf(quanteCitaFonti));
		else
			viewCitaFonti.setVisibility(View.GONE);
		textNota.setEllipsize(TextUtils.TruncateAt.END);
		if (dettagli) {
			textNota.setMaxLines(10);
			noteView.setTag(R.id.tag_object, nota);
			if (context instanceof Individuo) { // Fragment individuoEventi
				((AppCompatActivity) context).getSupportFragmentManager()
						.findFragmentByTag("android:switcher:" + R.id.schede_persona + ":1") // non garantito in futuro
						.registerForContextMenu(noteView);
			} else if (layout.getId() != R.id.box_content) // nelle AppCompatActivity tranne che nella dispensa
				((AppCompatActivity) context).registerForContextMenu(noteView);
			noteView.setOnClickListener(v -> {
				if (nota.getId() != null)
					Memoria.setFirst(nota);
				else
					Memoria.add(nota);
				context.startActivity(new Intent(context, Nota.class));
			});
		} else {
			textNota.setMaxLines(3);
		}
	}

	static void unlinkNote(Note nota, Object container, View view) {
		List<NoteRef> lista = ((NoteContainer) container).getNoteRefs();
		for (NoteRef ref : lista)
			if (ref.getNote(Global.gc).equals(nota)) {
				lista.remove(ref);
				break;
			}
		((NoteContainer) container).setNoteRefs(lista);
		if (view != null)
			view.setVisibility(View.GONE);
	}

	// Elimina una Nota inlinea o condivisa
	// RestitleIduisce un array dei capostipiti modificati
	public static Object[] deleteNote(Note note, View view) {
		Set<Object> capi;
		if (note.getId() != null) { // OBJECT note
			NoteReferences noteEliminator = new NoteReferences(Global.gc, note.getId(), true);
			Global.gc.getNotes().remove(note);
			capi = noteEliminator.rootObjects;
			if (Global.gc.getNotes() != null && Global.gc.getNotes().isEmpty())
				Global.gc.setNotes(null);
		} else { // INLINE note
			Object container = null;
			if (view != null) {
				container = view.getTag(R.id.tag_container);
			}
			if (container == null) // In case of pressing the Delete button in Note activity
				container = Memoria.getObjectContainer();
			if (container == null) // In case of Revert in Change Proposal
				return null;
			NoteContainer nc = (NoteContainer) container;
			nc.getNotes().remove(note); // rimuove solo se è una nota locale, non se object note
			if (nc.getNotes().isEmpty())
				nc.setNotes(null);
			capi = new HashSet<>();
			capi.add(Memoria.getFirstObject());
			Memoria.goBack();
		}
		Memoria.invalidateInstances(note);
		if (view != null)
			view.setVisibility(View.GONE);
		return capi.toArray();
	}

	// Elenca tutti i media di un obj container
	public static void addMedia(LinearLayout layout, Object container, boolean dettagli) {
		RecyclerView griglia = new AdattatoreGalleriaMedia.RiciclaVista(layout.getContext(), dettagli);
		griglia.setHasFixedSize(true);
		RecyclerView.LayoutManager gestoreLayout = new GridLayoutManager(layout.getContext(), dettagli ? 2 : 3);
		griglia.setLayoutManager(gestoreLayout);
		List<MediaContainerList.MediaHolder> mediaList = new ArrayList<>();
		for (Media med : ((MediaContainer) container).getAllMedia(Global.gc))
			mediaList.add(new MediaContainerList.MediaHolder(med, container));
		AdattatoreGalleriaMedia adattatore = new AdattatoreGalleriaMedia(mediaList, dettagli);
		griglia.setAdapter(adattatore);
		layout.addView(griglia);
	}

	// Di un obj inserisce le citazioni alle fonti
	public static void citeSources(LinearLayout layout, Object container) {
		if (Global.settings.expert) {
			List<SourceCitation> listaCitaFonti;
			if (container instanceof Note) // Note non estende SourceCitationContainer
				listaCitaFonti = ((Note) container).getSourceCitations();
			else
				listaCitaFonti = ((SourceCitationContainer) container).getSourceCitations();
			for (final SourceCitation citaz : listaCitaFonti) {
				View viewCita = LayoutInflater.from(layout.getContext()).inflate(R.layout.source_citation_item,
						layout, false);
				layout.addView(viewCita);
				if (citaz.getSource(Global.gc) != null) // source CITATION
					((TextView) viewCita.findViewById(R.id.source_text))
							.setText(Biblioteca.titoloFonte(citaz.getSource(Global.gc)));
				else // source NOTE, oppure Citazione di fonte che è stata eliminata
					viewCita.findViewById(R.id.source_citation).setVisibility(View.GONE);
				String t = "";
				if (citaz.getValue() != null)
					t += citaz.getValue() + "\n";
				if (citaz.getPage() != null)
					t += citaz.getPage() + "\n";
				if (citaz.getDate() != null)
					t += citaz.getDate() + "\n";
				if (citaz.getText() != null)
					t += citaz.getText() + "\n"; // vale sia per sourceNote che per sourceCitation
				TextView viewTesto = viewCita.findViewById(R.id.citation_text);
				if (t.isEmpty())
					viewTesto.setVisibility(View.GONE);
				else
					viewTesto.setText(t.substring(0, t.length() - 1));
				// Tutto il resto
				LinearLayout layoutAltro = viewCita.findViewById(R.id.citation_notes);
				addNotes(layoutAltro, citaz, false);
				addMedia(layoutAltro, citaz, false);
				viewCita.setTag(R.id.tag_object, citaz);
				if (layout.getContext() instanceof Individuo) { // Fragment individuoEventi
					((AppCompatActivity) layout.getContext()).getSupportFragmentManager()
							.findFragmentByTag("android:switcher:" + R.id.schede_persona + ":1")
							.registerForContextMenu(viewCita);
				} else // AppCompatActivity
					((AppCompatActivity) layout.getContext()).registerForContextMenu(viewCita);

				viewCita.setOnClickListener(v -> {
					Intent intent = new Intent(layout.getContext(), CitazioneFonte.class);
					Memoria.add(citaz);
					layout.getContext().startActivity(intent);
				});
			}
		}
	}

	// Inserisce nella layout il richiamo ad una fonte, con dettagli o essenziale
	public static void addSource(final LinearLayout layout, final Source fonte, boolean dettagli) {
		View viewFonte = LayoutInflater.from(layout.getContext()).inflate(R.layout.source_item, layout, false);
		layout.addView(viewFonte);
		TextView viewTesto = viewFonte.findViewById(R.id.source_text);
		String txt = "";
		if (dettagli) {
			if (fonte.getTitle() != null)
				txt = fonte.getTitle() + "\n";
			else if (fonte.getAbbreviation() != null)
				txt = fonte.getAbbreviation() + "\n";
			if (fonte.getType() != null)
				txt += fonte.getType().replaceAll("\n", " ") + "\n";
			if (fonte.getPublicationFacts() != null)
				txt += fonte.getPublicationFacts().replaceAll("\n", " ") + "\n";
			if (fonte.getText() != null)
				txt += fonte.getText().replaceAll("\n", " ");
			if (txt.endsWith("\n"))
				txt = txt.substring(0, txt.length() - 1);
			LinearLayout layoutAltro = viewFonte.findViewById(R.id.source_content);
			addNotes(layoutAltro, fonte, false);
			addMedia(layoutAltro, fonte, false);
			viewFonte.setTag(R.id.tag_object, fonte);
			((AppCompatActivity) layout.getContext()).registerForContextMenu(viewFonte);
		} else {
			viewTesto.setMaxLines(2);
			txt = Biblioteca.titoloFonte(fonte);
		}
		viewTesto.setText(txt);
		viewFonte.setOnClickListener(v -> {
			Memoria.setFirst(fonte);
			layout.getContext().startActivity(new Intent(layout.getContext(), Fonte.class));
		});
	}

	// La view ritornata è usata da Condivisione
	public static View linkPerson(LinearLayout layout, Person p, int scheda) {
		View viewPersona = LayoutInflater.from(layout.getContext()).inflate(R.layout.pezzo_individuo_piccolo, layout,
				false);
		layout.addView(viewPersona);
		F.showPrimaryPhoto(Global.gc, p, viewPersona.findViewById(R.id.collega_foto));
		((TextView) viewPersona.findViewById(R.id.collega_nome)).setText(getPrincipalName(p));
		String dati = twoDates(p, false);
		TextView viewDettagli = viewPersona.findViewById(R.id.collega_dati);
		if (dati.isEmpty())
			viewDettagli.setVisibility(View.GONE);
		else
			viewDettagli.setText(dati);
		if (!isDead(p))
			viewPersona.findViewById(R.id.collega_lutto).setVisibility(View.GONE);
		if (Gender.isMale(p))
			viewPersona.findViewById(R.id.collega_bordo).setBackgroundResource(R.drawable.casella_bordo_maschio);
		else if (Gender.isFemale(p))
			viewPersona.findViewById(R.id.collega_bordo).setBackgroundResource(R.drawable.casella_bordo_femmina);
		viewPersona.setOnClickListener(v -> {
			Memoria.setFirst(p);
			Intent intent = new Intent(layout.getContext(), Individuo.class);
			intent.putExtra("scheda", scheda);
			layout.getContext().startActivity(intent);
		});
		return viewPersona;
	}

	static String familyText(Context context, Gedcom gc, Family fam, boolean oneLine) {
		String text = "";
		for (Person husband : fam.getHusbands(gc))
			text += getPrincipalName(husband) + "\n";
		for (Person wife : fam.getWives(gc))
			text += getPrincipalName(wife) + "\n";
		if (fam.getChildren(gc).size() == 1) {
			text += getPrincipalName(fam.getChildren(gc).get(0));
		} else if (fam.getChildren(gc).size() > 1)
			text += context.getString(R.string.num_children, fam.getChildren(gc).size());
		if (text.endsWith("\n"))
			text = text.substring(0, text.length() - 1);
		if (oneLine)
			text = text.replaceAll("\n", ", ");
		if (text.isEmpty())
			text = "[" + context.getString(R.string.empty_family) + "]";
		return text;
	}

	// Usato da dispensa
	static void linkFamily(LinearLayout layout, Family fam) {
		View familyView = LayoutInflater.from(layout.getContext()).inflate(R.layout.pezzo_famiglia_piccolo, layout,
				false);
		layout.addView(familyView);
		((TextView) familyView.findViewById(R.id.famiglia_text))
				.setText(familyText(layout.getContext(), Global.gc, fam, false));
		familyView.setOnClickListener(v -> {
			Memoria.setFirst(fam);
			layout.getContext().startActivity(new Intent(layout.getContext(), Famiglia.class));
		});
	}

	// Usato da dispensa
	static void linkMedia(LinearLayout layout, Media media) {
		View mediaView = LayoutInflater.from(layout.getContext()).inflate(R.layout.pezzo_media, layout, false);
		layout.addView(mediaView);
		AdattatoreGalleriaMedia.arredaMedia(media, mediaView.findViewById(R.id.media_text),
				mediaView.findViewById(R.id.media_num));
		LinearLayout.LayoutParams parami = (LinearLayout.LayoutParams) mediaView.getLayoutParams();
		parami.height = dpToPx(80);
		F.loadMediaImage(media, mediaView.findViewById(R.id.media_img), mediaView.findViewById(R.id.media_circolo));
		mediaView.setOnClickListener(v -> {
			Memoria.setFirst(media);
			layout.getContext().startActivity(new Intent(layout.getContext(), Immagine.class));
		});
	}

	// Aggiunge un autore al layout
	static void linkAuthor(LinearLayout layout, Submitter autor) {
		Context context = layout.getContext();
		View view = LayoutInflater.from(context).inflate(R.layout.pezzo_nota, layout, false);
		layout.addView(view);
		TextView textNota = view.findViewById(R.id.nota_text);
		textNota.setText(autor.getName());
		view.findViewById(R.id.nota_fonti).setVisibility(View.GONE);
		view.setOnClickListener(v -> {
			Memoria.setFirst(autor);
			context.startActivity(new Intent(context, Autore.class));
		});
	}

	// Aggiunge al layout un container generico con uno o più collegamenti a
	// record capostipiti
	public static void addCard(LinearLayout layout, Object item, int titleId) {
		View view = LayoutInflater.from(layout.getContext()).inflate(R.layout.box_card, layout, false);
		TextView viewTit = view.findViewById(R.id.box_title);
		viewTit.setText(titleId);
		viewTit.setBackground(AppCompatResources.getDrawable(layout.getContext(), R.drawable.sghembo)); // per android
																										// 4
		layout.addView(view);
		LinearLayout dispensa = view.findViewById(R.id.box_content);
		if (item instanceof Object[]) {
			for (Object o : (Object[]) item)
				mettiQualsiasi(dispensa, o);
		} else
			mettiQualsiasi(dispensa, item);
	}

	// Riconosce il tipo di record e aggiunge il link appropriato alla layout
	static void mettiQualsiasi(LinearLayout layout, Object record) {
		if (record instanceof Person)
			linkPerson(layout, (Person) record, 1);
		else if (record instanceof Source)
			addSource(layout, (Source) record, false);
		else if (record instanceof Family)
			linkFamily(layout, (Family) record);
		else if (record instanceof Repository)
			ArchivioRef.mettiArchivio(layout, (Repository) record);
		else if (record instanceof Note)
			addNote(layout, (Note) record, true);
		else if (record instanceof Media)
			linkMedia(layout, (Media) record);
		else if (record instanceof Submitter)
			linkAuthor(layout, (Submitter) record);
	}

	// Aggiunge al layout il pezzo con la data e tempo di Cambiamento
	public static View cambiamenti(final LinearLayout layout, final Change change) {
		View changeView = null;
		if (change != null && Global.settings.expert) {
			changeView = LayoutInflater.from(layout.getContext()).inflate(R.layout.change_date_item, layout,
					false);
			layout.addView(changeView);
			TextView textView = changeView.findViewById(R.id.changes_text);
			if (change.getDateTime() != null) {
				String txt = "";
				if (change.getDateTime().getValue() != null)
					txt = new Datatore(change.getDateTime().getValue()).writeDateLong();
				if (change.getDateTime().getTime() != null)
					txt += " - " + change.getDateTime().getTime();
				textView.setText(txt);
			}
			LinearLayout layoutNote = changeView.findViewById(R.id.changes_notes);
			for (Extension altroTag : findExtensions(change))
				addItem(layoutNote, altroTag.name, altroTag.text);
			// Grazie al mio contributo la data cambiamento può avere delle note
			addNotes(layoutNote, change, false);
			changeView.setOnClickListener(v -> {
				Memoria.add(change);
				layout.getContext().startActivity(new Intent(layout.getContext(), Cambiamenti.class));
			});
		}
		return changeView;
	}

	// Chiede conferma di eliminare un elemento
	public static boolean preserva(Object item) {
		// todo Confirmation elimina
		return false;
	}

	// RestitleIduisce un DateTime con data e ora aggiornate
	public static DateTime dataTempoAdesso() {
		DateTime dataTempo = new DateTime();
		Date now = new Date();
		dataTempo.setValue(String.format(Locale.ENGLISH, "%te %<Tb %<tY", now));
		dataTempo.setTime(String.format(Locale.ENGLISH, "%tT", now));
		return dataTempo;
	}

	// Aggiorna la data di cambiamento del/dei record
	public static void updateDate(Object... oggetti) {
		return;
		// ignore modification of CHAN
		// for( Object aggiornando : oggetti ) {
		// try { // se aggiornando non ha il metodo get/setChange, passa oltre
		// silenziosamente
		// Change chan = (Change)aggiornando.getClass().getMethod( "getChange" ).invoke(
		// aggiornando );
		// if( chan == null ) // il record non ha ancora un CHAN
		// chan = new Change();
		// chan.setDateTime( dataTempoAdesso() );
		// aggiornando.getClass().getMethod( "setChange", Change.class ).invoke(
		// aggiornando, chan );
		// // Extension con l'id della zona, una stringa tipo 'America/Sao_Paulo'
		// chan.putExtension( "zone", TimeZone.getDefault().getID() );
		// } catch( Exception e ) {}
		// }
	}

	// Eventualmente salva il Json
	public static void saveJson(boolean refresh, Object... objects) {
		if (objects != null)
			updateDate(objects);
		if (refresh)
			Global.edited = true;

		// al primo salvataggio marchia gli autori
		if (Global.settings.getCurrentTree().grade == 9) {
			for (Submitter autore : Global.gc.getSubmitters())
				autore.putExtension("passed", true);
			Global.settings.getCurrentTree().grade = 10;
			Global.settings.save();
			Settings.Tree tree = Global.settings.getCurrentTree();
			if (tree.githubRepoFullName != null)
				Helper.requireEmail(Global.context, Global.context.getString(R.string.set_email_for_commit),
						Global.context.getString(R.string.OK), Global.context.getString(R.string.cancel), email -> {
							FamilyGemTreeInfoModel infoModel = new FamilyGemTreeInfoModel(
									tree.title,
									tree.persons,
									tree.generations,
									tree.media,
									tree.root,
									tree.grade,
									tree.createdAt,
									tree.updatedAt);
							SaveInfoFileTask.execute(Global.context, tree.githubRepoFullName, email, tree.id, infoModel,
									() -> {
									}, () -> {
									}, error -> {
										Toast.makeText(Global.context, error, Toast.LENGTH_LONG).show();
									});
						});
		}

		if (Global.settings.autoSave)
			saveJson(Global.gc, Global.settings.openTree);
		else { // mostra il tasto Salva
			Global.daSalvare = true;
			if (Global.principalView != null) {
				NavigationView menu = Global.principalView.findViewById(R.id.menu);
				menu.getHeaderView(0).findViewById(R.id.menu_salva).setVisibility(View.VISIBLE);
			}
		}
	}

	static void saveJson(Gedcom gc, int idAlbero) {
		try {
			final Settings.Tree tree = Global.settings.getTree(idAlbero);
			tree.updatedAt = Settings.Tree.getDateTimeNow();

			List<PrivatePerson> privatePersons = new ArrayList<>();
			String privateJsonStr = null;
			if (!tree.isForked && tree.githubRepoFullName != null) {
				// handle privacy
				// take out private person (for saving tree.json in repo)
				for (Person person : gc.getPeople()) {
					if (isPrivate(person)) {
						PrivatePerson privatePerson = U.setPrivate(gc, person);
						privatePersons.add(privatePerson);
					}
				}
				privateJsonStr = savePrivatePersons(idAlbero, privatePersons);
			}

			// get string of tree.json
			String gcJsonString = new JsonParser().toJson(gc);
			FileUtils.writeStringToFile(
					new File(Global.context.getFilesDir(), idAlbero + ".json"),
					gcJsonString, "UTF-8");

			// put back
			if (!tree.isForked && tree.githubRepoFullName != null) {
				for (PrivatePerson privatePerson : privatePersons) {
					Person person = gc.getPerson(privatePerson.personId);
					if (person != null) {
						person.setEventsFacts(privatePerson.eventFacts);
						person.setMedia(privatePerson.mediaList);
					}
				}
			}

			if (tree.githubRepoFullName != null && !"".equals(tree.githubRepoFullName)) {
				// replace tree.json in repo
				Context context = Global.context;
				final String _privateJsonStr = privateJsonStr;
				Helper.requireEmail(context, context.getString(R.string.set_email_for_commit),
						context.getString(R.string.OK), context.getString(R.string.cancel),
						email -> SaveTreeFileTask.execute(
								context, tree.githubRepoFullName, email,
								tree.id, gcJsonString, _privateJsonStr, tree.title, () -> {
									// do nothing
								}, () -> {
									// do nothing
								}, error -> {
									Toast.makeText(context, error, Toast.LENGTH_LONG).show();
								}));
			}
		} catch (IOException e) {
			Toast.makeText(Global.context, e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
		}
	}

	static int castaJsonInt(Object ignoto) {
		if (ignoto instanceof Integer)
			return (int) ignoto;
		else
			return ((JsonPrimitive) ignoto).getAsInt();
	}

	static String castaJsonString(Object ignoto) {
		if (ignoto == null)
			return null;
		else if (ignoto instanceof String)
			return (String) ignoto;
		else
			return ((JsonPrimitive) ignoto).getAsString();
	}

	public static int dpToPx(float dips) {
		return (int) (dips * Global.context.getResources().getDisplayMetrics().density + 0.5f);
	}

	// Valuta se ci sono individui collegabili rispetto a un individuo.
	// Usato per decidere se far comparire 'Collega persona esistente' nel menu
	static boolean areLinkablePersons(Person person) {
		int total = Global.gc.getPeople().size();
		if (total > 0 && (Global.settings.expert // gli esperti possono sempre
				|| person == null)) // in una famiglia vuota unRappresentanteDellaFamiglia è null
			return true;
		int kin = Anagrafe.countRelatives(person);
		return total > kin + 1;
	}

	// Chiede se referenziare un autore nell'header
	static void principalAuthor(Context context, final String idAutore) {
		final Header[] testa = { Global.gc.getHeader() };
		if (testa[0] == null || testa[0].getSubmitterRef() == null) {
			new AlertDialog.Builder(context).setMessage(R.string.make_main_submitter)
					.setPositiveButton(android.R.string.yes, (dialog, id) -> {
						if (testa[0] == null) {
							testa[0] = NewTree.creaTestata(Global.settings.openTree + ".json");
							Global.gc.setHeader(testa[0]);
						}
						testa[0].setSubmitterRef(idAutore);
						saveJson(true);
					}).setNegativeButton(R.string.no, null).show();
		}
	}

	// RestitleIduisce il primo autore non passato
	static Submitter autoreFresco(Gedcom gc) {
		for (Submitter autore : gc.getSubmitters()) {
			if (autore.getExtension("passed") == null)
				return autore;
		}
		return null;
	}

	// Verifica se un autore ha partecipato alle condivisioni, per non farlo
	// eliminare
	static boolean autoreHaCondiviso(Submitter autore) {
		List<Settings.Share> condivisioni = Global.settings.getCurrentTree().shares;
		boolean inviatore = false;
		if (condivisioni != null)
			for (Settings.Share share : condivisioni)
				if (autore.getId().equals(share.submitter))
					inviatore = true;
		return inviatore;
	}

	// Elenco di stringhe dei membri rappresentativi delle famiglie
	static String[] listFamilies(List<Family> listaFamiglie) {
		List<String> famigliePerno = new ArrayList<>();
		for (Family fam : listaFamiglie) {
			String label = familyText(Global.context, Global.gc, fam, true);
			famigliePerno.add(label);
		}
		return famigliePerno.toArray(new String[0]);
	}

	/*
	 * Per un pivot che è figlio in più di una famiglia chiede which famiglia
	 * mostrare
	 * itemAprire:
	 * 0 diagramma della famiglia precedente, senza chiedere which famiglia (primo
	 * click su Diagram)
	 * 1 diagramma chiedendo eventualmente which famiglia
	 * 2 famiglia chiedendo eventualmente which famiglia
	 */
	public static void qualiGenitoriMostrare(Context context, Person pivot, int itemAprire) {
		if (pivot == null)
			concludeParentSelection(context, null, 1, 0);
		else {
			List<Family> famiglie = pivot.getParentFamilies(Global.gc);
			if (famiglie.size() > 1 && itemAprire > 0) {
				new AlertDialog.Builder(context).setTitle(R.string.which_family)
						.setItems(listFamilies(famiglie), (dialog, which) -> {
							concludeParentSelection(context, pivot, itemAprire, which);
						}).show();
			} else
				concludeParentSelection(context, pivot, itemAprire, 0);
		}

	}

	private static void concludeParentSelection(Context context, Person pivot, int itemAprire, int whichFamiglia) {
		if (pivot != null)
			Global.indi = pivot.getId();
		if (itemAprire > 0) // Viene impostata la famiglia da mostrare
			Global.familyNum = whichFamiglia; // normalmente è la 0
		if (itemAprire < 2) { // Mostra il diagramma
			if (context instanceof Principal) { // Diagram, Anagrafe o Principal stesso
				FragmentManager fm = ((AppCompatActivity) context).getSupportFragmentManager();
				// Nome del fragment precedente nel backstack
				String previousName = fm.getBackStackEntryAt(fm.getBackStackEntryCount() - 1).getName();
				if (previousName != null && previousName.equals("diagram"))
					fm.popBackStack(); // Ricliccando su Diagram rimuove dalla storia il fragment di diagramma
										// predente
				fm.beginTransaction().replace(R.id.fragment_container, new Diagram()).addToBackStack("diagram")
						.commit();
			} else { // Da individuo o da famiglia
				context.startActivity(new Intent(context, Principal.class));
			}
		} else { // Viene mostrata la famiglia
			Family family = pivot.getParentFamilies(Global.gc).get(whichFamiglia);
			if (context instanceof Famiglia) { // Passando di Famiglia in Famiglia non accumula attività nello stack
				Memoria.replaceFirst(family);
				((Activity) context).recreate();
			} else {
				Memoria.setFirst(family);
				context.startActivity(new Intent(context, Famiglia.class));
			}
		}
	}

	// Per un pivot che ha molteplici matrimoni chiede which mostrare
	public static void qualiConiugiMostrare(Context context, Person pivot, Family famiglia) {
		if (pivot.getSpouseFamilies(Global.gc).size() > 1 && famiglia == null) {
			new AlertDialog.Builder(context).setTitle(R.string.which_family)
					.setItems(listFamilies(pivot.getSpouseFamilies(Global.gc)), (dialog, which) -> {
						concludeSpouseSelection(context, pivot, null, which);
					}).show();
		} else {
			concludeSpouseSelection(context, pivot, famiglia, 0);
		}
	}

	private static void concludeSpouseSelection(Context context, Person pivot, Family famiglia, int which) {
		Global.indi = pivot.getId();
		famiglia = famiglia == null ? pivot.getSpouseFamilies(Global.gc).get(which) : famiglia;
		if (context instanceof Famiglia) {
			Memoria.replaceFirst(famiglia);
			((Activity) context).recreate(); // Non accumula activity nello stack
		} else {
			Memoria.setFirst(famiglia);
			context.startActivity(new Intent(context, Famiglia.class));
		}
	}

	/** check out MultiWeddings */
	// Usato per collegare una persona ad un'altra, solo in modalità inesperto
	// Verifica se il pivot potrebbe avere o ha molteplici matrimoni e chiede a
	// which attaccare un coniuge o un figlio
	// È anche responsabile di settare 'idFamiglia' oppure 'collocazione'
	static boolean controllaMultiMatrimoni(Intent intent, Context context, Fragment fragment) {
		String idPerno = intent.getStringExtra("idIndividuo");
		Person pivot = Global.gc.getPerson(idPerno);
		List<Family> famGenitori = pivot.getParentFamilies(Global.gc);
		List<Family> famSposi = pivot.getSpouseFamilies(Global.gc);
		int relazione = intent.getIntExtra("relazione", 0);
		ArrayAdapter<NuovoParente.VoceFamiglia> adapter = new ArrayAdapter<>(context,
				android.R.layout.simple_list_item_1);

		// Genitori: esiste già una famiglia che abbia almeno uno spazio vuoto
		if (relazione == 1 && famGenitori.size() == 1
				&& (famGenitori.get(0).getHusbandRefs().isEmpty() || famGenitori.get(0).getWifeRefs().isEmpty()))
			intent.putExtra("idFamiglia", famGenitori.get(0).getId()); // aggiunge 'idFamiglia' all'intent esistente
		// se questa famiglia è già piena di genitori, 'idFamiglia' rimane null
		// quindi verrà cercata la famiglia esistente del destinatario oppure si crearà
		// una famiglia nuova

		// Genitori: esistono più famiglie
		if (relazione == 1 && famGenitori.size() > 1) {
			for (Family fam : famGenitori)
				if (fam.getHusbandRefs().isEmpty() || fam.getWifeRefs().isEmpty())
					adapter.add(new NuovoParente.VoceFamiglia(context, fam));
			if (adapter.getCount() == 1)
				intent.putExtra("idFamiglia", adapter.getItem(0).famiglia.getId());
			else if (adapter.getCount() > 1) {
				new AlertDialog.Builder(context).setTitle(R.string.which_family_add_parent)
						.setAdapter(adapter, (dialog, which) -> {
							intent.putExtra("idFamiglia", adapter.getItem(which).famiglia.getId());
							concludeMultiMarriage(context, intent, fragment);
						}).show();
				return true;
			}
		}
		// Fratello
		else if (relazione == 2 && famGenitori.size() == 1) {
			intent.putExtra("idFamiglia", famGenitori.get(0).getId());
		} else if (relazione == 2 && famGenitori.size() > 1) {
			new AlertDialog.Builder(context).setTitle(R.string.which_family_add_sibling)
					.setItems(listFamilies(famGenitori), (dialog, which) -> {
						intent.putExtra("idFamiglia", famGenitori.get(which).getId());
						concludeMultiMarriage(context, intent, fragment);
					}).show();
			return true;
		}
		// Coniuge
		else if (relazione == 3 && famSposi.size() == 1) {
			if (famSposi.get(0).getHusbandRefs().isEmpty() || famSposi.get(0).getWifeRefs().isEmpty()) // Se c'è uno
																										// slot libero
				intent.putExtra("idFamiglia", famSposi.get(0).getId());
		} else if (relazione == 3 && famSposi.size() > 1) {
			for (Family fam : famSposi) {
				if (fam.getHusbandRefs().isEmpty() || fam.getWifeRefs().isEmpty())
					adapter.add(new NuovoParente.VoceFamiglia(context, fam));
			}
			// Nel caso di zero famiglie papabili, idFamiglia rimane null
			if (adapter.getCount() == 1) {
				intent.putExtra("idFamiglia", adapter.getItem(0).famiglia.getId());
			} else if (adapter.getCount() > 1) {
				// adapter.add(new NuovoParente.VoceFamiglia(context,pivot) );
				new AlertDialog.Builder(context).setTitle(R.string.which_family_add_spouse)
						.setAdapter(adapter, (dialog, which) -> {
							intent.putExtra("idFamiglia", adapter.getItem(which).famiglia.getId());
							concludeMultiMarriage(context, intent, fragment);
						}).show();
				return true;
			}
		}
		// Figlio: esiste già una famiglia con o senza figli
		else if (relazione == 4 && famSposi.size() == 1) {
			intent.putExtra("idFamiglia", famSposi.get(0).getId());
		} // Figlio: esistono molteplici famiglie coniugali
		else if (relazione == 4 && famSposi.size() > 1) {
			new AlertDialog.Builder(context).setTitle(R.string.which_family_add_child)
					.setItems(listFamilies(famSposi), (dialog, which) -> {
						intent.putExtra("idFamiglia", famSposi.get(which).getId());
						concludeMultiMarriage(context, intent, fragment);
					}).show();
			return true;
		}
		// Non avendo trovato una famiglia di pivot, dice ad Anagrafe di cercare di
		// collocare pivot nella famiglia del destinatario
		if (intent.getStringExtra("idFamiglia") == null && intent.getBooleanExtra("anagrafeScegliParente", false))
			intent.putExtra("collocazione", "FAMIGLIA_ESISTENTE");
		return false;
	}

	/** check out MultiWeddings */
	// Usato per collegare una persona ad un'altra, solo in modalità inesperto
	// Verifica se il pivot potrebbe avere o ha molteplici matrimoni e chiede a
	// which attaccare un coniuge o un figlio
	public static boolean controllaMultiMatrimoni2(String idPerno, int relazione, Context context, Callback callback) {
		Person pivot = Global.gc.getPerson(idPerno);
		List<Family> famGenitori = pivot.getParentFamilies(Global.gc);
		List<Family> famSposi = pivot.getSpouseFamilies(Global.gc);
		String familyId = null;
		String placement = null;
		ArrayAdapter<NuovoParente.VoceFamiglia> adapter = new ArrayAdapter<>(context,
				android.R.layout.simple_list_item_1);

		// Genitori: esiste già una famiglia che abbia almeno uno spazio vuoto
		if (relazione == 1 && famGenitori.size() == 1
				&& (famGenitori.get(0).getHusbandRefs().isEmpty() || famGenitori.get(0).getWifeRefs().isEmpty()))
			familyId = famGenitori.get(0).getId();
		// se questa famiglia è già piena di genitori, 'idFamiglia' rimane null
		// quindi verrà cercata la famiglia esistente del destinatario oppure si crearà
		// una famiglia nuova

		// Genitori: esistono più famiglie
		if (relazione == 1 && famGenitori.size() > 1) {
			for (Family fam : famGenitori)
				if (fam.getHusbandRefs().isEmpty() || fam.getWifeRefs().isEmpty())
					adapter.add(new NuovoParente.VoceFamiglia(context, fam));
			if (adapter.getCount() == 1)
				familyId = adapter.getItem(0).famiglia.getId();
			else if (adapter.getCount() > 1) {
				new AlertDialog.Builder(context).setTitle(R.string.which_family_add_parent)
						.setAdapter(adapter, (dialog, which) -> {
							callback.invoke(adapter.getItem(which).famiglia.getId(), null);
						}).show();
				return true;
			}
		}
		// Fratello
		else if (relazione == 2 && famGenitori.size() == 1) {
			familyId = famGenitori.get(0).getId();
		} else if (relazione == 2 && famGenitori.size() > 1) {
			new AlertDialog.Builder(context).setTitle(R.string.which_family_add_sibling)
					.setItems(listFamilies(famGenitori), (dialog, which) -> {
						callback.invoke(famGenitori.get(which).getId(), null);
					}).show();
			return true;
		}
		// Coniuge
		else if (relazione == 3 && famSposi.size() == 1) {
			if (famSposi.get(0).getHusbandRefs().isEmpty() || famSposi.get(0).getWifeRefs().isEmpty()) // Se c'è uno
																										// slot libero
				familyId = famSposi.get(0).getId();
		} else if (relazione == 3 && famSposi.size() > 1) {
			for (Family fam : famSposi) {
				if (fam.getHusbandRefs().isEmpty() || fam.getWifeRefs().isEmpty())
					adapter.add(new NuovoParente.VoceFamiglia(context, fam));
			}
			// Nel caso di zero famiglie papabili, idFamiglia rimane null
			if (adapter.getCount() == 1) {
				familyId = adapter.getItem(0).famiglia.getId();
			} else if (adapter.getCount() > 1) {
				// adapter.add(new NuovoParente.VoceFamiglia(context,pivot) );
				new AlertDialog.Builder(context).setTitle(R.string.which_family_add_spouse)
						.setAdapter(adapter, (dialog, which) -> {
							callback.invoke(adapter.getItem(which).famiglia.getId(), null);
						}).show();
				return true;
			}
		}
		// Figlio: esiste già una famiglia con o senza figli
		else if (relazione == 4 && famSposi.size() == 1) {
			familyId = famSposi.get(0).getId();
		} // Figlio: esistono molteplici famiglie coniugali
		else if (relazione == 4 && famSposi.size() > 1) {
			new AlertDialog.Builder(context).setTitle(R.string.which_family_add_child)
					.setItems(listFamilies(famSposi), (dialog, which) -> {
						callback.invoke(famSposi.get(which).getId(), null);
					}).show();
			return true;
		}

		callback.invoke(familyId, placement);
		return false;
	}

	public interface Callback {
		void invoke(String familyId, String placement);
	}

	// Conclusione della funzione precedente
	static void concludeMultiMarriage(Context context, Intent intent, Fragment fragment) {
		if (intent.getBooleanExtra("anagrafeScegliParente", false)) {
			// apre Anagrafe
			if (fragment != null)
				fragment.startActivityForResult(intent, 1401);
			else
				((Activity) context).startActivityForResult(intent, 1401);
		} else // apre EditaIndividuo
			context.startActivity(intent);
	}

	// Controlla che una o più famiglie siano vuote e propone di eliminarle
	// 'ancheKo' dice di eseguire 'cheFare' anche cliccando Cancel o fuori dal
	// dialogo
	static boolean checkEmptyFamilies(Context context, Runnable cheFare, boolean ancheKo, Family... famiglie) {
		List<Family> vuote = new ArrayList<>();
		for (Family fam : famiglie) {
			int membri = fam.getHusbandRefs().size() + fam.getWifeRefs().size() + fam.getChildRefs().size();
			if (membri <= 1 && fam.getEventsFacts().isEmpty() && fam.getAllMedia(Global.gc).isEmpty()
					&& fam.getAllNotes(Global.gc).isEmpty() && fam.getSourceCitations().isEmpty()) {
				vuote.add(fam);
			}
		}
		if (vuote.size() > 0) {
			new AlertDialog.Builder(context).setMessage(R.string.empty_family_delete)
					.setPositiveButton(android.R.string.yes, (dialog, i) -> {
						for (Family fam : vuote)
							Chiesa.deleteFamily(fam); // Così capita di salvare più volte insieme... ma vabè
						if (cheFare != null)
							cheFare.run();
					}).setNeutralButton(android.R.string.cancel, (dialog, i) -> {
						if (ancheKo)
							cheFare.run();
					}).setOnCancelListener(dialog -> {
						if (ancheKo)
							cheFare.run();
					}).show();
			return true;
		}
		return false;
	}

	// Mostra un message Toast anche da un thread collaterale
	static void toast(Activity context, int message) {
		toast(context, context.getString(message));
	}

	static void toast(Activity context, String message) {
		context.runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
	}

	static List<String> getListOfCurrentRepoFullNames() {
		List<String> repoFullNames = new ArrayList<>();
		for (Settings.Tree tree : Global.settings.trees) {
			if (tree.githubRepoFullName != null)
				repoFullNames.add(tree.githubRepoFullName);
		}
		return repoFullNames;
	}

	static boolean isConnector(Person person) {
		if (person == null)
			return false;
		for (EventFact fatto : person.getEventsFacts()) {
			if (fatto.getTag().equals(CONNECTOR_TAG))
				return true;
		}
		return false;
	}

	public static boolean isPrivate(Person person) {
		if (person == null)
			return false;
		for (EventFact fatto : person.getEventsFacts()) {
			if (fatto.getTag().equals(PRIVATE_TAG))
				return true;
		}
		return false;
	}

	static boolean canBeConnector(Person person, Gedcom gedcom) {
		// jika person tsb sama sekali tidak punya spouse yg punya parents atau
		// siblings,
		// dan tidak punya parents dan tidak punya siblings
		// intinya jika T-T2=0 maka tidak ada gunanya dipotong

		// has parent
		List<Family> parentFamilies = person.getParentFamilies(gedcom);
		if (parentFamilies != null && parentFamilies.size() > 0)
			return true;

		// has spouse that has parent or sibling
		List<Family> spouseFamilies = person.getSpouseFamilies(gedcom);
		if (spouseFamilies != null && spouseFamilies.size() > 0) {
			for (Family family : spouseFamilies) {
				// check as wife
				for (Person spouse : family.getHusbands(gedcom)) {
					if (!spouse.getId().equals(person.getId())) {
						List<Family> spouseParentFamilies = spouse.getParentFamilies(gedcom);
						if (spouseFamilies != null && spouseParentFamilies.size() > 0)
							return true;
					}
				}
				// check as husband
				for (Person spouse : family.getWives(gedcom)) {
					if (!spouse.getId().equals(person.getId())) {
						List<Family> spouseParentFamilies = spouse.getParentFamilies(gedcom);
						if (spouseFamilies != null && spouseParentFamilies.size() > 0)
							return true;
					}
				}
			}
		}

		// has sibling
		if (parentFamilies != null) {
			for (Family family : parentFamilies) {
				if (family.getChildRefs().size() > 1) {
					return true;
				}
			}
		}

		return false;
	}

	static String getSubTreeUrl(Person person) {
		for (EventFact fatto : person.getEventsFacts()) {
			if (fatto.getTag() != null && fatto.getTag().equals(CONNECTOR_TAG))
				return fatto.getValue();
		}
		return null;
	}

	// return new but cloned person (same properties including personId)
	public static PrivatePerson setPrivate(Gedcom gedcom, Person person) {
		// clone person
		PrivatePerson clone = new PrivatePerson();
		clone.personId = person.getId();
		clone.mediaList = new ArrayList<>();
		List<Media> mediaList = person.getAllMedia(gedcom);
		clone.mediaList.addAll(mediaList);
		List<EventFact> eventFacts = new ArrayList<>();
		for (EventFact eventFact : person.getEventsFacts()) {
			eventFacts.add(cloneEventFact(eventFact));
		}
		clone.eventFacts = eventFacts;

		// clear all fields of the person (except names)
		person.setEventsFacts(new ArrayList<>());
		person.setMedia(new ArrayList<>());
		// add tag
		EventFact privacy = new EventFact();
		privacy.setTag(U.PRIVATE_TAG);
		privacy.setValue("");
		person.addEventFact(privacy);

		return clone;
	}

	public static void setPrivate(Person person) {
		if (isPrivate(person))
			return; // already private
		EventFact privacy = new EventFact();
		privacy.setTag(U.PRIVATE_TAG);
		privacy.setValue("");
		person.addEventFact(privacy);
	}

	public static void setNonPrivate(Person person) {
		// if (!isPrivate(person))
		// return; // already not private
		for (EventFact fatto : person.getEventsFacts()) {
			if (fatto.getTag().equals(PRIVATE_TAG)) {
				person.getEventsFacts().remove(fatto);
				return;
			}
		}
	}

	public static void setNotPrivate(Person person, PrivatePerson privatePerson) {
		// copy media
		for (Media media : privatePerson.mediaList) {
			person.addMedia(media);
		}
		// copy event facts
		List<EventFact> eventFacts = new ArrayList<>();
		for (EventFact eventFact : privatePerson.eventFacts) {
			eventFacts.add(cloneEventFact(eventFact));
		}
		person.setEventsFacts(eventFacts);
	}

	public static String getJson(File file) throws IOException {
		InputStream inputStream = new FileInputStream(file);
		InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		StringBuilder text = new StringBuilder();

		BufferedReader br = new BufferedReader(inputStreamReader);
		String line;
		while ((line = br.readLine()) != null) {
			text.append(line);
			text.append('\n');
		}
		br.close();

		String json = text.toString();
		return json;
	}

	public static List<PrivatePerson> getPrivatePersons(int idAlbero) {
		List<PrivatePerson> privatePeoples = new ArrayList<>();
		try {
			File file = new File(Global.context.getFilesDir(), idAlbero + ".private.json");
			if (file.exists()) {
				String jsonStr = getJson(file);
				if (jsonStr == null || jsonStr.trim().isEmpty())
					return privatePeoples;
				Gson gson = new GsonBuilder()
						.setPrettyPrinting()
						.registerTypeAdapter(Gedcom.class, new GedcomTypeAdapter())
						.create();
				Type userListType = new TypeToken<ArrayList<PrivatePerson>>() {
				}.getType();
				privatePeoples = gson.fromJson(jsonStr, userListType);
				return privatePeoples;
			}
		} catch (Exception ex) {
			FirebaseCrashlytics.getInstance().recordException(ex);
			ex.printStackTrace();
		}
		return privatePeoples;
	}

	public static boolean doesForkedRepoContainPrivatePerson(Gedcom gedcom) {
		if (gedcom == null)
			return false;
		for (Person person : gedcom.getPeople()) {
			if (isPrivate(person)) {
				return true;
			}
		}
		return false;
	}

	public static String savePrivatePersons(int idAlbero, List<PrivatePerson> privatePersons) {
		try {
			Gson gson = new GsonBuilder()
					.setPrettyPrinting()
					.registerTypeAdapter(Gedcom.class, new GedcomTypeAdapter())
					.create();
			String jsonString = gson.toJson(privatePersons);
			// save private.json
			FileUtils.writeStringToFile(
					new File(Global.context.getFilesDir(), idAlbero + ".private.json"),
					jsonString, "UTF-8");
			return jsonString;
		} catch (Exception ex) {
			FirebaseCrashlytics.getInstance().recordException(ex);
			ex.printStackTrace();
		}
		return null;
	}

	public static void changePersonId(Person person, String newId, Gedcom gedcom) {
		String oldId = person.getId();
		List<Family> families = new ArrayList<>();
		families.addAll(person.getParentFamilies(gedcom));
		families.addAll(person.getSpouseFamilies(gedcom));

		for (Family family : families) {
			List<SpouseRef> spouseRefs = new ArrayList<>();
			spouseRefs.addAll(family.getHusbandRefs());
			spouseRefs.addAll(family.getWifeRefs());
			spouseRefs.addAll(family.getChildRefs());

			for (SpouseRef ref : spouseRefs) {
				if (Objects.equals(ref.getRef(), oldId)) {
					ref.setRef(newId);
				}
			}
		}

		person.setId(newId);
	}

	public static void changeFamilyId(Family family, String newId, Gedcom gedcom) {
		String oldId = family.getId();
		List<Person> members = new ArrayList<>();
		members.addAll(family.getHusbands(gedcom));
		members.addAll(family.getWives(gedcom));
		members.addAll(family.getChildren(gedcom));

		for (Person person : members) {
			List<SpouseFamilyRef> spouseFamilyRefs = new ArrayList<>();
			spouseFamilyRefs.addAll(person.getParentFamilyRefs());
			spouseFamilyRefs.addAll(person.getSpouseFamilyRefs());

			for (SpouseFamilyRef ref : spouseFamilyRefs) {
				if (Objects.equals(ref.getRef(), oldId)) {
					ref.setRef(newId);
				}
			}
		}

		family.setId(newId);
	}

	public static void AlertError(Activity activity, String message) {
		new AlertDialog.Builder(activity)
				.setTitle(R.string.find_errors)
				.setMessage(message)
				.setCancelable(false)
				.setPositiveButton(R.string.OK, (dialog, which) -> {
					dialog.dismiss();
					activity.finish();
				}).show();
	}

	public static void AlertError(Activity activity, @StringRes int resId) {
		AlertError(activity, activity.getString(resId));
	}

	final static String CONNECTOR_TAG = "_CONN";
	final static String PRIVATE_TAG = "_PRIV";
}
