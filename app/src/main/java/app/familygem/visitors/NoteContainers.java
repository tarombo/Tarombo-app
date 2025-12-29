/* Visitatore un po' complementare a NoteReferences, avente una doppia funzione:
- produce una lista dei contenitori che includono una certa Nota condivisa
- modifica il ref che punta alla nota
*/

package app.familygem.visitors;

import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.NoteContainer;
import org.folg.gedcom.model.NoteRef;
import java.util.LinkedHashSet;
import java.util.Set;

public class NoteContainers extends TotalVisitor {

	public Set<NoteContainer> containers = new LinkedHashSet<>();
	private Note note; // la nota condivisa da cercare
	private String newId; // il nuovo id da mettere nei ref

	public NoteContainers(Gedcom gc, Note note, String newId) {
		this.note = note;
		this.newId = newId;
		gc.accept(this);
	}

	@Override
	boolean visitObject(Object object, boolean isRoot) {
		if (object instanceof NoteContainer) {
			for (NoteRef nr : ((NoteContainer) object).getNoteRefs())
				if (nr.getRef().equals(note.getId())) {
					if (newId != null)
						nr.setRef(newId);
					else
						containers.add((NoteContainer) object);
				}
		}
		return true;
	}
}
