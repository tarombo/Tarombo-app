// Contatore delle citazioni di una fonte
// Servirebbe per sostituire U.countCitazioni(Source) in Biblioteca
// è più preciso nella conta cioè non gli sfugge nulla, ma è quattro volte più lento

package app.familygem.visitors;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.SourceCitation;
import org.folg.gedcom.model.Visitor;

public class SourceCitationCount extends Visitor {

	public int count = 0;
	String id;

	SourceCitationCount(String id) {
		this.id = id;
	}

	@Override
	public boolean visit(Person p) {
		for (SourceCitation c : p.getSourceCitations())
			if (c.getRef() != null) // necessario perché le note-fonti non hanno Ref alla fonte
				if (c.getRef().equals(id))
					count++;
		return true;
	}

	@Override
	public boolean visit(Family f) {
		for (SourceCitation c : f.getSourceCitations())
			if (c.getRef() != null)
				if (c.getRef().equals(id))
					count++;
		return true;
	}

	@Override
	public boolean visit(Name n) {
		for (SourceCitation c : n.getSourceCitations())
			if (c.getRef() != null)
				if (c.getRef().equals(id))
					count++;
		return true;
	}

	@Override
	public boolean visit(EventFact e) {
		for (SourceCitation c : e.getSourceCitations())
			if (c.getRef() != null)
				if (c.getRef().equals(id))
					count++;
		return true;
	}

	@Override
	public boolean visit(Note n) {
		for (SourceCitation c : n.getSourceCitations())
			if (c.getRef() != null)
				if (c.getRef().equals(id))
					count++;
		return true;
	}
}
