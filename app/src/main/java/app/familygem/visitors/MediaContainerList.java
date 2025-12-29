// Mappa ordinata dei media ciascuno col suo obj container
// Il container serve praticamente solo a scollegaMedia in IndividuoMedia

package app.familygem.visitors;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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

public class MediaContainerList extends Visitor {

	public List<MediaHolder> mediaList = new ArrayList<>();
	private Gedcom gc;
	private boolean wantAll;	// Elencare tutti i media (anche i locali) o solo gli oggetti media condivisi

	public MediaContainerList( Gedcom gc, boolean wantAll ) {
		this.gc = gc;
		this.wantAll = wantAll;
	}

	private boolean visitObject( Object obj ) {
		if( wantAll && obj instanceof MediaContainer ) {
			//for( MediaRef r : p.getMediaRefs() ) mediaList.put( r.getMedia(gc), p );	// elenca i ref a vuoto => media null
			MediaContainer container = (MediaContainer) obj;
			for( Media med : container.getAllMedia( gc ) ) { // Oggetti media e media locali di ciascun record
				MediaHolder medCont = new MediaHolder(med, obj);
				if( !mediaList.contains(medCont) )
					mediaList.add( medCont );
			}
		}
		return true;
	}

	@Override
	public boolean visit( Gedcom gc ) {
		for( Media med : gc.getMedia() )
			mediaList.add( new MediaHolder(med, gc) );	// rastrella gli oggetti media
		return true;
	}
	@Override
	public boolean visit( Person p ) {
		return visitObject( p );
	}
	@Override
	public boolean visit( Family f ) {
		return visitObject( f );
	}
	@Override
	public boolean visit( EventFact e ) {
		return visitObject( e );
	}
	@Override
	public boolean visit( Name n ) {
		return visitObject( n );
	}
	@Override
	public boolean visit( SourceCitation c ) {
		return visitObject( c );
	}
	@Override
	public boolean visit( Source s ) {
		return visitObject( s );
	}

	// Classe che rappresenta un Media con il suo obj container
	static public class MediaHolder {
		public Media media;
		public Object container;
		public MediaHolder( Media media, Object container ) {
			this.media = media;
			this.container = container;
		}
		@Override
		public boolean equals( Object o ) {
			return media.equals( ((MediaHolder)o).media);
		}
		@Override
		public int hashCode() {
			return Objects.hash( media );
		}
	}
}