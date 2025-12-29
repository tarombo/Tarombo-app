package app.familygem.dettaglio;

import org.folg.gedcom.model.Address;
import app.familygem.Dettaglio;
import app.familygem.Memoria;
import app.familygem.R;
import app.familygem.U;

public class Indirizzo extends Dettaglio {

	Address a;

	@Override
	public void impagina() {
		setTitle( R.string.address );
		mettiBava( "ADDR" );
		a = (Address) casta( Address.class );
		addItem( getString(R.string.value), "Value", false, true );	// Fortemente deprecato in favore dell'indirizzo frammentato
		addItem( getString(R.string.name), "Name", false, false );	// _name non standard
		addItem( getString(R.string.line_1), "AddressLine1" );
		addItem( getString(R.string.line_2), "AddressLine2" );
		addItem( getString(R.string.line_3), "AddressLine3" );
		addItem( getString(R.string.postal_code), "PostalCode" );
		addItem( getString(R.string.city), "City" );
		addItem( getString(R.string.state), "State" );
		addItem( getString(R.string.country), "Country" );
		mettiEstensioni( a );
	}

	@Override
	public void elimina() {
		eliminaIndirizzo( Memoria.getObjectContainer() );
		U.updateDate( Memoria.getFirstObject() );
		Memoria.invalidateInstances(a);
	}
}
