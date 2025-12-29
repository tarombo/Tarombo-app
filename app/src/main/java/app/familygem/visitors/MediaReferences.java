/*
Visitatore che rispetto a un media condiviso ha una triplice funzione:
- Contare i riferimenti al media in tutti i MediaContainer
- Oppure elimina gli stessi riferimenti al media
- Nel frattempo elenca gli oggetti capostipite delle pile che contengono il media
*/

package app.familygem.visitors;

import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.MediaContainer;
import org.folg.gedcom.model.MediaRef;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class MediaReferences extends TotalVisitor {

	private Media media; // il media condiviso
	private boolean delete; // eliminare i Ref o no
	private Object rootObject; // il capostipite della pila
	public int count = 0; // il conto dei riferimenti a un Media
	public Set<Object> rootObjects = new LinkedHashSet<>(); // l'elenco degli oggetti capostipiti contenti un Media

	public MediaReferences(Gedcom gc, Media media, boolean delete) {
		this.media = media;
		this.delete = delete;
		gc.accept(this);
	}

	@Override
	boolean visitObject(Object object, boolean isRoot) {
		if (isRoot)
			rootObject = object;
		if (object instanceof MediaContainer) {
			MediaContainer container = (MediaContainer) object;
			Iterator<MediaRef> mediaRefIterator = container.getMediaRefs().iterator();
			while (mediaRefIterator.hasNext())
				if (mediaRefIterator.next().getRef().equals(media.getId())) {
					rootObjects.add(rootObject);
					if (delete)
						mediaRefIterator.remove();
					else
						count++;
				}
			if (container.getMediaRefs().isEmpty())
				container.setMediaRefs(null);
		}
		return true;
	}
}