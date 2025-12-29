package app.familygem.dettaglio;

import android.app.Activity;
import org.folg.gedcom.model.Note;
import app.familygem.Dettaglio;
import app.familygem.Global;
import app.familygem.Memoria;
import app.familygem.R;
import app.familygem.U;
import app.familygem.visitors.NoteReferences;

public class Nota extends Dettaglio {

	Note n;

	@Override
	public void impagina() {
		n = (Note) casta(Note.class);
		if (n.getId() == null) {
			setTitle(R.string.note);
			mettiBava("NOTE");
		} else {
			setTitle(R.string.shared_note);
			mettiBava("NOTE", n.getId());
		}
		addItem(getString(R.string.text), "Value", true, true);
		addItem(getString(R.string.rin), "Rin", false, false);
		mettiEstensioni(n);
		U.citeSources(box, n);
		U.cambiamenti(box, n.getChange());
		if (n.getId() != null) {
			NoteReferences rifNota = new NoteReferences(Global.gc, n.getId(), false);
			if (rifNota.count > 0)
				U.addCard(box, rifNota.rootObjects.toArray(), R.string.shared_by);
		} else if (((Activity) box.getContext()).getIntent().getBooleanExtra("daQuaderno", false)) {
			U.addCard(box, Memoria.getFirstObject(), R.string.written_in);
		}
	}

	@Override
	public void elimina() {
		U.updateDate(U.deleteNote(n, null));
	}
}
