// Set ordinato dei media
// Quasi sempre può sostituire MediaContainerList

package app.familygem.visitors;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.MediaContainer;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.Source;
import org.folg.gedcom.model.SourceCitation;
import org.folg.gedcom.model.Visitor;
import java.util.LinkedHashSet;
import java.util.Set;
import app.familygem.F;
import app.familygem.Global;

public class MediaList extends Visitor {

	public Set<Media> list = new LinkedHashSet<>();
	private Gedcom gc;
	/*
	 * 0 tutti i media
	 * 1 solo gli oggetti media condivisi (per tutto il Gedcom)
	 * 2 solo i locali (non serve gc)
	 * 3 condivisi e locali ma solo immagini e video anteprimabili (per il menu
	 * principale)
	 */
	private int mode;

	public MediaList(Gedcom gc, int mode) {
		this.gc = gc;
		this.mode = mode;
	}

	private boolean visitObject(Object object) {
		if (object instanceof MediaContainer) {
			MediaContainer container = (MediaContainer) object;
			if (mode == 0)
				list.addAll(container.getAllMedia(gc)); // aggiunge media condivisi e locali
			else if (mode == 2)
				list.addAll(container.getMedia()); // solo i media locali
			else if (mode == 3)
				for (Media med : container.getAllMedia(gc))
					filter(med);
		}
		return true;
	}

	// Aggiunge solo quelli presunti bellini con anteprima
	private void filter(Media media) {
		String file = F.getMediaPath(Global.settings.openTree, media); // todo e le immagini dagli URI?
		if (file != null && file.lastIndexOf('.') > 0) {
			String extension = file.substring(file.lastIndexOf('.') + 1);
			switch (extension) {
				case "jpg":
				case "jpeg":
				case "png":
				case "gif":
				case "bmp":
				case "webp": // ok
				case "heic": // ok todo l'immagine può risultare ruotata di 90° o 180°
				case "heif": // sinonimo di .heic
				case "mp4":
				case "3gp": // ok
				case "webm": // ok
				case "mkv": // ok
					list.add(media);
			}
		}
	}

	@Override
	public boolean visit(Gedcom gc) {
		if (mode < 2)
			list.addAll(gc.getMedia()); // rastrella tutti gli oggetti media condivisi del Gedcom
		else if (mode == 3)
			for (Media med : gc.getMedia())
				filter(med);
		return true;
	}

	@Override
	public boolean visit(Person p) {
		return visitObject(p);
	}

	@Override
	public boolean visit(Family f) {
		return visitObject(f);
	}

	@Override
	public boolean visit(EventFact e) {
		return visitObject(e);
	}

	@Override
	public boolean visit(Name n) {
		return visitObject(n);
	}

	@Override
	public boolean visit(SourceCitation c) {
		return visitObject(c);
	}

	@Override
	public boolean visit(Source s) {
		return visitObject(s);
	}
}
