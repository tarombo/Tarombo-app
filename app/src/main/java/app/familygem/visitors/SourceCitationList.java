// Partendo dall'id di una fonte genera una lista di triplette: capostipite / contenitore / citazioni della fonte
// Usato da Biblioteca, da Fonte e da Confirmation

package app.familygem.visitors;

import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.SourceCitation;
import org.folg.gedcom.model.SourceCitationContainer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SourceCitationList extends TotalVisitor {

	public List<Triplet> list = new ArrayList<>();
	private String sourceId; // id della fonte
	private Object rootObject;

	public SourceCitationList(Gedcom gc, String id) {
		this.sourceId = id;
		gc.accept(this);
	}

	@Override
	boolean visitObject(Object object, boolean isRoot) {
		if (isRoot)
			rootObject = object;
		if (object instanceof SourceCitationContainer) {
			analyze(object, ((SourceCitationContainer) object).getSourceCitations());
		} // Note non estende SourceCitationContainer, ma implementa i suoi propri metodi
		else if (object instanceof Note) {
			analyze(object, ((Note) object).getSourceCitations());
		}
		return true;
	}

	private void analyze(Object container, List<SourceCitation> citations) {
		for (SourceCitation cit : citations)
			// Le fonti-note non hanno Ref ad una fonte
			if (cit.getRef() != null && cit.getRef().equals(sourceId)) {
				Triplet triplet = new Triplet();
				triplet.rootObject = rootObject;
				triplet.container = container;
				triplet.citation = cit;
				list.add(triplet);
			}
	}

	public Object[] getRoots() {
		Set<Object> roots = new LinkedHashSet<>(); // unifica i duplicati
		for (Triplet tri : list) {
			roots.add(tri.rootObject);
		}
		return roots.toArray();
	}

	// Classe per stoccare insieme i tre elementi capostipite - contenitore -
	// citazione
	public class Triplet {
		public Object rootObject;
		public Object container; // Sarebbe un SourceCitationContainer ma Note fa eccezione
		public SourceCitation citation;
	}
}
