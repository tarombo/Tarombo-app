package app.familygem.dettaglio;

import org.folg.gedcom.model.Repository;
import org.folg.gedcom.model.Source;
import java.util.ArrayList;
import java.util.List;
import app.familygem.Dettaglio;
import app.familygem.Global;
import app.familygem.Magazzino;
import app.familygem.R;
import app.familygem.U;

public class Archivio extends Dettaglio {

	Repository a;

	@Override
	public void impagina() {
		setTitle( R.string.repository );
		a = (Repository) casta( Repository.class );
		mettiBava( "REPO", a.getId() );
		addItem( getString(R.string.value), "Value", false, true );	// Non molto Gedcom standard
		addItem( getString(R.string.name), "Name" );
		addItem( getString(R.string.address), a.getAddress() );
		addItem( getString(R.string.www), "Www" );
		addItem( getString(R.string.email), "Email" );
		addItem( getString(R.string.telephone), "Phone" );
		addItem( getString(R.string.fax), "Fax" );
		addItem( getString(R.string.rin), "Rin", false, false );
		mettiEstensioni( a );
		U.addNotes( box, a, true );
		U.cambiamenti( box, a.getChange() );

		// Raccoglie e mostra le fonti che citano questo Repository
		List<Source> fontiCitanti = new ArrayList<>();
		for( Source fonte : Global.gc.getSources() )
			if( fonte.getRepositoryRef() != null && fonte.getRepositoryRef().getRef() != null
					&& fonte.getRepositoryRef().getRef().equals(a.getId()) )
				fontiCitanti.add( fonte );
		if( !fontiCitanti.isEmpty() )
			U.addCard( box, fontiCitanti.toArray(), R.string.sources );
		a.putExtension( "fonti", fontiCitanti.size() );
	}

	@Override
	public void elimina() {
		U.updateDate( (Object[]) Magazzino.elimina( a ) );
	}
}
