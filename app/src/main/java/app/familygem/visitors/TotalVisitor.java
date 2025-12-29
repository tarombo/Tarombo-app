// Modello di Visitor che visita tutti i possibili contenitori del Gedcom distinguendo i capostipiti

package app.familygem.visitors;

import org.folg.gedcom.model.Change;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
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
import org.folg.gedcom.model.Visitor;

public class TotalVisitor extends Visitor {

	private boolean visitObject(Object object) {
		return visitObject(object, false);
	}

	boolean visitObject(Object object, boolean isRoot) {
		return true;
	}

	@Override
	public boolean visit(Header h) {
		return visitObject(h, true);
	}

	@Override
	public boolean visit(Person p) {
		return visitObject(p, true);
	}

	@Override
	public boolean visit(Family f) {
		return visitObject(f, true);
	}

	@Override
	public boolean visit(Source s) {
		return visitObject(s, true);
	}

	@Override
	public boolean visit(Repository r) {
		return visitObject(r, true);
	}

	@Override
	public boolean visit(Submitter s) {
		return visitObject(s, true);
	}

	@Override
	public boolean visit(Media m) {
		return visitObject(m, m.getId() != null);
	}

	@Override
	public boolean visit(Note n) {
		return visitObject(n, n.getId() != null);
	}

	@Override
	public boolean visit(Name n) {
		return visitObject(n);
	}

	@Override
	public boolean visit(EventFact e) {
		return visitObject(e);
	}

	@Override
	public boolean visit(SourceCitation s) {
		return visitObject(s);
	}

	@Override
	public boolean visit(RepositoryRef r) {
		return visitObject(r);
	}

	@Override
	public boolean visit(Change c) {
		return visitObject(c);
	}
}
