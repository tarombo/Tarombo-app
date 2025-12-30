// Visitatore che produce in Memoria la pila gerarchica degli oggetti tra il record capostipite e un oggetto dato
// ad es. Person > Media semplice
// oppure Family > Note > SourceCitation > Note semplice

package app.familygem.visitors;

import org.folg.gedcom.model.Change;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Header;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.Repository;
import org.folg.gedcom.model.RepositoryRef;
import org.folg.gedcom.model.Source;
import org.folg.gedcom.model.SourceCitation;
import org.folg.gedcom.model.Submitter;
import org.folg.gedcom.model.Visitable;
import org.folg.gedcom.model.Visitor;
import java.util.Iterator;
import java.util.List;
import app.familygem.Memoria;

public class FindStack extends Visitor {

	private List<Memoria.Step> stack;
	private Object target;
	private boolean found;

	public FindStack(Gedcom gc, Object target) {
		stack = Memoria.addStack(); // in una nuova pila apposta
		this.target = target;
		gc.accept(this);
	}

	private boolean process(Object object, String tag, boolean isRoot) {
		if (!found) {
			if (isRoot)
				stack.clear(); // ogni capostipite fa ricominciare da capo una pila
			Memoria.Step step = new Memoria.Step();
			step.object = object;
			step.tag = tag;
			if (!isRoot)
				step.filotto = true; // li marchia per eliminarli poi in blocco onBackPressed
			stack.add(step);
		}
		if (object.equals(target)) {
			Iterator<Memoria.Step> steps = stack.iterator();
			while (steps.hasNext()) {
				CleanStack cleaner = new CleanStack(target);
				((Visitable) steps.next().object).accept(cleaner);
				if (cleaner.toDelete)
					steps.remove();
			}
			found = true;
			// Memoria.print("FindStack");
		}
		return true;
	}

	@Override
	public boolean visit(Header step) {
		return process(step, "HEAD", true);
	}

	@Override
	public boolean visit(Person step) {
		return process(step, "INDI", true);
	}

	@Override
	public boolean visit(Family step) {
		return process(step, "FAM", true);
	}

	@Override
	public boolean visit(Source step) {
		return process(step, "SOUR", true);
	}

	@Override
	public boolean visit(Repository step) {
		return process(step, "REPO", true);
	}

	@Override
	public boolean visit(Submitter step) {
		return process(step, "SUBM", true);
	}

	@Override
	public boolean visit(Media step) {
		return process(step, "OBJE", step.getId() != null);
	}

	@Override
	public boolean visit(Note step) {
		return process(step, "NOTE", step.getId() != null);
	}

	@Override
	public boolean visit(Name step) {
		return process(step, "NAME", false);
	}

	@Override
	public boolean visit(EventFact step) {
		return process(step, step.getTag(), false);
	}

	@Override
	public boolean visit(SourceCitation step) {
		return process(step, "SOUR", false);
	}

	@Override
	public boolean visit(RepositoryRef step) {
		return process(step, "REPO", false);
	}

	@Override
	public boolean visit(Change step) {
		return process(step, "CHAN", false);
	}
	/*
	 * ok ma poi tanto GedcomTag non è Visitable e quindi non prosegue la visita
	 * 
	 * @Override
	 * public boolean visit( String chiave, Object estensioni ) {
	 * if( chiave.equals("folg.more_tags") ) {
	 * for( GedcomTag est : (List<GedcomTag>)estensioni ) {
	 * //s.l(est.getClass().getName()+" "+est.getTag());
	 * opera( est, est.getTag(), false );
	 * }
	 * }
	 * return true;
	 * }
	 */
}