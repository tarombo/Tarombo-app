/* Visitatore per nota condivisa con una triplice funzione:
 - Contare in tutti gli elementi del Gedcom i riferimenti alla nota condivisa
 - Eliminare i riferimenti alla nota
 - Nel frattempo raccogliere tutti i capostipiti
*/

package app.familygem.visitors;

import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.NoteContainer;
import org.folg.gedcom.model.NoteRef;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class NoteReferences extends TotalVisitor {

	private String noteId; // Id della nota condivisa
	private boolean delete; // bandierina per eliminare i ref alla nota piuttosto che contarli
	private Object rootObject;
	public int count = 0; // i riferimenti alla nota condivisa
	public Set<Object> rootObjects = new LinkedHashSet<>();

	public NoteReferences(Gedcom gc, String id, boolean delete) {
		this.noteId = id;
		this.delete = delete;
		gc.accept(this);
	}

	@Override
	boolean visitObject(Object object, boolean isRoot) {
		if (isRoot)
			rootObject = object;
		if (object instanceof NoteContainer) {
			NoteContainer blocco = (NoteContainer) object;
			Iterator<NoteRef> refi = blocco.getNoteRefs().iterator();
			while (refi.hasNext()) {
				NoteRef nr = refi.next();
				if (nr.getRef().equals(noteId)) {
					rootObjects.add(rootObject);
					if (delete)
						refi.remove();
					else
						count++;
				}
			}
			// Only set to null when deleting to avoid ConcurrentModificationException
			// during traversal
			if (delete && blocco.getNoteRefs().isEmpty())
				blocco.setNoteRefs(null);
		}
		return true;
	}
}