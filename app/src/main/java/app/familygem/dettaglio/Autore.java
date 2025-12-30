package app.familygem.dettaglio;

import org.folg.gedcom.model.Submitter;
import app.familygem.Dettaglio;
import app.familygem.Podio;
import app.familygem.R;
import app.familygem.U;

public class Autore extends Dettaglio {

	Submitter a;

	@Override
	public void impagina() {
		setTitle( R.string.submitter );
		a = (Submitter) casta( Submitter.class );
		mettiBava( "SUBM", a.getId() );
		addItem( getString(R.string.value), "Value", false, true );   // Value de che?
		addItem( getString(R.string.name), "Name" );
		addItem( getString(R.string.address), a.getAddress() );
		addItem( getString(R.string.www), "Www" );
		addItem( getString(R.string.email), "Email" );
		addItem( getString(R.string.telephone), "Phone" );
		addItem( getString(R.string.fax), "Fax" );
		addItem( getString(R.string.language), "Language" );
		addItem( getString(R.string.rin), "Rin", false, false );
		mettiEstensioni( a );
		U.cambiamenti( box, a.getChange() );
	}

	@Override
	public void elimina() {
		// Ricordiamo che almeno un autore deve essere specificato
		// non aggiorna la data di nessun record
		Podio.eliminaAutore( a );
	}
}
