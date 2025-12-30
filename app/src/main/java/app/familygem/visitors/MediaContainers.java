/* Visitatore un po' complementare a MediaReferences, avente una funzione:
- modifica il ref che punta alla nota
- potrebbe produrre una lista dei contenitori che includono un certo Media condiviso
*/

package app.familygem.visitors;

import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.MediaContainer;
import org.folg.gedcom.model.MediaRef;

public class MediaContainers extends TotalVisitor {

	// public Set<MediaContainer> contenitori = new LinkedHashSet<>();
	private Media media;
	private String newId;

	public MediaContainers(Gedcom gc, Media media, String newId) {
		this.media = media;
		this.newId = newId;
		gc.accept(this);
	}

	@Override
	boolean visitObject(Object object, boolean isRoot) {
		if (object instanceof MediaContainer) {
			for (MediaRef mr : ((MediaContainer) object).getMediaRefs())
				if (mr.getRef().equals(media.getId())) {
					// if( newId != null )
					mr.setRef(newId);
					// else
					// contenitori.add( (MediaContainer) object );
				}
		}
		return true;
	}
}
