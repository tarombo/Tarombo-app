package app.familygem.dettaglio;

import org.folg.gedcom.model.GedcomTag;
import app.familygem.Dettaglio;
import app.familygem.Memoria;
import app.familygem.R;
import app.familygem.U;

public class Estensione extends Dettaglio {

	GedcomTag e;

	@Override
	public void impagina() {
		setTitle( getString( R.string.extension ) );
		e = (GedcomTag) casta( GedcomTag.class );
		mettiBava( e.getTag() );
		addItem( getString(R.string.id), "Id", false, false );
		addItem( getString(R.string.value), "Value", true, true );
		addItem( "Ref", "Ref", false, false );
		addItem( "ParentTagName", "ParentTagName", false, false ); // non ho capito se viene usato o no
		for( GedcomTag figlio : e.getChildren() ) {
			String testo = U.digExtension(figlio,0);
			if( testo.endsWith("\n") )
				testo = testo.substring( 0, testo.length()-1 );
			creaPezzo( figlio.getTag(), testo, figlio, true );
		}
	}

	@Override
	public void elimina() {
		U.removeExtension( e, Memoria.oggettoContenitore(), null );
		U.updateDate( Memoria.oggettoCapo() );
	}
}
