// Gestisce le pile di oggetti gerarchici per scrivere la bava in Dettaglio

package app.familygem;

import org.folg.gedcom.model.Address;
import org.folg.gedcom.model.Change;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.GedcomTag;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.Repository;
import org.folg.gedcom.model.RepositoryRef;
import org.folg.gedcom.model.Source;
import org.folg.gedcom.model.SourceCitation;
import org.folg.gedcom.model.Submitter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import app.familygem.dettaglio.Archivio;
import app.familygem.dettaglio.ArchivioRef;
import app.familygem.dettaglio.Autore;
import app.familygem.dettaglio.Cambiamenti;
import app.familygem.dettaglio.CitazioneFonte;
import app.familygem.dettaglio.ExtensionDetail;
import app.familygem.dettaglio.Evento;
import app.familygem.dettaglio.Famiglia;
import app.familygem.dettaglio.Fonte;
import app.familygem.dettaglio.Immagine;
import app.familygem.dettaglio.Indirizzo;
import app.familygem.dettaglio.Nome;
import app.familygem.dettaglio.Nota;

public class Memoria {

	static Map<Class, Class> classes = new HashMap<>();
	private static final Memoria memory = new Memoria();
	List<StepStack> list = new ArrayList<>();

	Memoria() {
		classes.put(Person.class, Individuo.class);
		classes.put(Repository.class, Archivio.class);
		classes.put(RepositoryRef.class, ArchivioRef.class);
		classes.put(Submitter.class, Autore.class);
		classes.put(Change.class, Cambiamenti.class);
		classes.put(SourceCitation.class, CitazioneFonte.class);
		classes.put(GedcomTag.class, ExtensionDetail.class);
		classes.put(EventFact.class, Evento.class);
		classes.put(Family.class, Famiglia.class);
		classes.put(Source.class, Fonte.class);
		classes.put(Media.class, Immagine.class);
		classes.put(Address.class, Indirizzo.class);
		classes.put(Name.class, Nome.class);
		classes.put(Note.class, Nota.class);
	}

	// Restituisce l'ultima pila creata se ce n'è almeno una
	// oppure ne restituisce una vuota giusto per non restituire null
	static StepStack getStack() {
		if (memory.list.size() > 0)
			return memory.list.get(memory.list.size() - 1);
		else
			return new StepStack(); // una pila vuota che non viene aggiunta alla lista
	}

	public static StepStack addStack() {
		StepStack stack = new StepStack();
		memory.list.add(stack);
		return stack;
	}

	// Aggiunge il primo oggetto in una nuova pila
	public static void setFirst(Object object) {
		setFirst(object, null);
	}

	public static void setFirst(Object object, String tag) {
		addStack();
		Step step = add(object);
		if (tag != null)
			step.tag = tag;
		else if (object instanceof Person)
			step.tag = "INDI";
		// print("setFirst");
	}

	// Aggiunge un oggetto alla fine dell'ultima pila esistente
	public static Step add(Object object) {
		Step step = new Step();
		step.object = object;
		getStack().add(step);
		// print("add");
		return step;
	}

	// Mette il primo oggetto se non ci sono pile oppure sostituisce il primo
	// oggetto nell'ultima pila esistente
	// In altre parole mette il primo oggetto senza aggiungere ulteriori pile
	public static void replaceFirst(Object object) {
		String tag = object instanceof Family ? "FAM" : "INDI";
		if (memory.list.size() == 0) {
			setFirst(object, tag);
		} else {
			getStack().clear();
			Step step = add(object);
			step.tag = tag;
		}
		// print("replaceFirst");
	}

	// L'oggetto contenuto nel primo passo della pila
	public static Object getFirstObject() {
		if (getStack().size() > 0)
			return getStack().firstElement().object;
		else
			return null;
	}

	// L'oggetto nel passo precedente all'ultimo
	public static Object getObjectContainer() {
		if (getStack().size() > 1)
			return getStack().get(getStack().size() - 2).object;
		else
			return null;
	}

	// L'oggetto nell'ultimo passo
	public static Object getObject() {
		if (getStack().size() == 0)
			return null;
		else
			return getStack().peek().object;
	}

	static void goBack() {
		while (getStack().size() > 0 && getStack().lastElement().filotto)
			getStack().pop();
		if (getStack().size() > 0)
			getStack().pop();
		if (getStack().isEmpty())
			memory.list.remove(getStack());
		// print("goBack");
	}

	// Quando un oggetto viene eliminato, lo rende null in tutti i passi,
	// e anche gli oggetti negli eventuali passi seguenti vengono annullati.
	public static void invalidateInstances(Object object) {
		for (StepStack stack : memory.list) {
			boolean next = false;
			for (Step step : stack) {
				if (step.object != null && (step.object.equals(object) || next)) {
					step.object = null;
					next = true;
				}
			}
		}
	}

	public static void print(String intro) {
		if (intro != null)
			s.l(intro);
		for (StepStack stack : memory.list) {
			for (Step step : stack) {
				String filotto = step.filotto ? "< " : "";
				if (step.tag != null)
					s.p(filotto + step.tag + " ");
				else if (step.object != null)
					s.p(filotto + step.object.getClass().getSimpleName() + " ");
				else
					s.p(filotto + "Null"); // capita in rarissimi casi
			}
			s.l("");
		}
		s.l("- - - -");
	}

	static class StepStack extends Stack<Step> {
	}

	public static class Step {
		public Object object;
		public String tag;
		public boolean filotto; // FindStack lo setta true quindi onBackPressed la pila va eliminata in blocco
	}
}