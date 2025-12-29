package app.familygem.dettaglio;

import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;
import app.familygem.Dettaglio;
import app.familygem.Global;
import app.familygem.Memoria;
import app.familygem.R;
import app.familygem.U;
import static app.familygem.Global.gc;

public class Nome extends Dettaglio {

	Name n;

	@Override
	public void impagina() {
		setTitle(R.string.name);
		mettiBava("NAME", null);
		n = (Name)casta(Name.class);

		String nome = "";
		String cognome = "";
		String epiteto = n.getValue();
		if( epiteto != null ) {
			nome = epiteto.replaceAll("/.*?/", "").trim(); // Rimuove il cognome
			if( epiteto.indexOf('/') < epiteto.lastIndexOf('/') )
				cognome = epiteto.substring(epiteto.indexOf('/') + 1, epiteto.lastIndexOf('/')).trim();
		}
		creaPezzo(getString(R.string.given), nome, 4043, false);
		creaPezzo(getString(R.string.surname), cognome, 6064, false);

		addItem(getString(R.string.nickname), "Nickname");
		addItem(getString(R.string.type), "Type", true, false); // _TYPE in GEDCOM 5.5, TYPE in GEDCOM 5.5.1
		addItem(getString(R.string.prefix), "Prefix", Global.settings.expert, false);
		addItem(getString(R.string.given), "Given", Global.settings.expert, false);
		addItem(getString(R.string.surname_prefix), "SurnamePrefix", Global.settings.expert, false);
		addItem(getString(R.string.surname), "Surname", Global.settings.expert, false);
		addItem(getString(R.string.suffix), "Suffix", Global.settings.expert, false);
		addItem(getString(R.string.married_name), "MarriedName", false, false); // _marrnm
		addItem(getString(R.string.aka), "Aka", false, false); // _aka
		addItem(getString(R.string.romanized), "Romn", Global.settings.expert, false);
		addItem(getString(R.string.phonetic), "Fone", Global.settings.expert, false);
		mettiEstensioni(n);
		U.addNotes(box, n, true);
		U.addMedia(box, n, true); // Mi sembra strano che un Name abbia Media.. comunque..
		U.citeSources(box, n);
	}

	@Override
	public void elimina() {
		Person costui = gc.getPerson(Global.indi);
		costui.getNames().remove(n);
		U.updateDate(costui);
		Memoria.invalidateInstances(n);
	}
}
