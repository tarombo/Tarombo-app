// Singleton che gestisce gli oggetti dei 2 Gedcom durante l'importazione degli aggiornamenti

package app.familygem;

import android.app.Activity;
import java.util.ArrayList;
import java.util.List;

public class Comparison {

	private static final Comparison comparison = new Comparison();
	private List<DiffItem> list = new ArrayList<>();
	boolean autoProceed; // stabilisce se accettare automaticamente tutti gli aggiornamenti
	int totalChoices; // Scelte totali in caso di autoProsegui
	int choicesMade; // Posizione in caso di autoProsegui

	static Comparison get() {
		return comparison;
	}

	public static List<DiffItem> getList() {
		return get().list;
	}

	static DiffItem addItem(Object object, Object object2, int type) {
		DiffItem item = new DiffItem();
		item.object = object;
		item.object2 = object2;
		item.type = type;
		getList().add(item);
		return item;
	}

	// Restituisce il fronte attualmente attivo
	static DiffItem getItem(Activity activity) {
		return getList().get(activity.getIntent().getIntExtra("posizione", 0) - 1);
	}

	// Da chiamare quando si esce dal processo di confronto
	static void reset() {
		getList().clear();
		get().autoProceed = false;
	}

	static class DiffItem {
		Object object;
		Object object2;
		int type; // numero da 1 a 7 che definisce il tipo: 1 Nota -> 7 Famiglia
		boolean dualOption; // ha la possibilità di aggiungi + sostituisci
		/*
		 * che fare di questa coppia di oggetti:
		 * 0 niente
		 * 1 oggetto2 viene aggiunto ad albero
		 * 2 oggetto2 sostituisce oggetto
		 * 3 oggetto viene eliminato
		 */
		int action;
	}
}