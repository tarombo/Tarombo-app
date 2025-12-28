package app.familygem.dettaglio;

import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.SourceCitation;
import org.folg.gedcom.model.SourceCitationContainer;
import app.familygem.Dettaglio;
import app.familygem.Memoria;
import app.familygem.R;
import app.familygem.U;
import static app.familygem.Global.gc;

public class CitazioneFonte extends Dettaglio {

	SourceCitation c;

	@Override
	public void impagina() {
		mettiBava( "SOUR" );
		c = (SourceCitation) casta( SourceCitation.class );
		if( c.getSource(gc) != null ) {  // source CITATION valida
			setTitle( R.string.source_citation );
			U.addSource( box, c.getSource(gc), true );
		} else if( c.getRef() != null ) {  // source CITATION di una fonte inesistente (magari eliminata)
			setTitle( R.string.inexistent_source_citation );
		} else {	// source NOTE
			setTitle( R.string.source_note );
			addItem( getString(R.string.value), "Value", true, true );
		}
		addItem( getString(R.string.page), "Page", true, true );
		addItem( getString(R.string.date), "Date" );
		addItem( getString(R.string.text), "Text", true, true );	// vale sia per sourceNote che per sourceCitation
		//c.getTextOrValue();	praticamente inutile
		//if( c.getDataTagContents() != null )
		//	U.addItem( box, "Data Tag Contents", c.getDataTagContents().toString() );	// COMBINED DATA TEXT
		addItem( getString(R.string.certainty), "Quality" );	// un numero da 0 a 3
		//addItem( "Ref", "Ref", false, false ); // l'id della fonte
		mettiEstensioni( c );
		U.addNotes( box, c, true );
		U.addMedia( box, c, true );
	}

	@Override
	public void elimina() {
		Object contenitore = Memoria.oggettoContenitore();
		if( contenitore instanceof Note )	// Note non extende SourceCitationContainer
			((Note)contenitore).getSourceCitations().remove( c );
		else
			((SourceCitationContainer)contenitore).getSourceCitations().remove( c );
		U.updateDate( Memoria.oggettoCapo() );
		Memoria.annullaIstanze(c);
	}
}
