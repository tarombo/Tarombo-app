package app.familygem;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.NoteContainer;
import org.folg.gedcom.model.NoteRef;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import app.familygem.dettaglio.Nota;
import app.familygem.visita.ListaNote;
import app.familygem.visita.RiferimentiNota;
import app.familygem.visita.TrovaPila;
import static app.familygem.Global.gc;
import app.familygem.R;
public class Quaderno extends Fragment {
	
	@Override
	public View onCreateView( LayoutInflater inflater, ViewGroup container, Bundle bandolo ) {
		boolean scegliNota = getActivity().getIntent().getBooleanExtra("quadernoScegliNota",false );
		View vista = inflater.inflate( R.layout.magazzino, container, false );
		if( gc != null ) {
			LinearLayout scatola = vista.findViewById( R.id.magazzino_scatola );
			ProgressBar progressBar = vista.findViewById( R.id.magazzino_circolo );
			TextView progressText = vista.findViewById( R.id.magazzino_progresso_testo );
			View scrollView = vista.findViewById( R.id.magazzino_scroll );
			
			// Show progress bar and hide content
			progressBar.setVisibility( View.VISIBLE );
			progressText.setVisibility( View.VISIBLE );
			scrollView.setVisibility( View.GONE );
			
			// Load notes in background thread
			ExecutorService executor = Executors.newSingleThreadExecutor();
			Handler handler = new Handler(Looper.getMainLooper());
			
			executor.execute(() -> {
				// This runs in background thread
				List<Note> listaNoteCondivise = gc.getNotes();
				ListaNote visitaNote = new ListaNote();
				if( !scegliNota ) {
					gc.accept( visitaNote );
				}
				final int totaleNote = listaNoteCondivise.size() + visitaNote.listaNote.size();
				
				// Update UI on main thread in batches to prevent ANR
				handler.post(() -> {
					// Update title first
					((AppCompatActivity)getActivity()).getSupportActionBar().setTitle( totaleNote + " "
							+ getString(totaleNote==1 ? R.string.note : R.string.notes).toLowerCase() );
					
					// Initialize progress text
					progressText.setText("0 / " + totaleNote);
					
					// Add shared notes header if needed
					if( !listaNoteCondivise.isEmpty() && !scegliNota ) {
						View tit = inflater.inflate( R.layout.titoletto, scatola, true );
						String txt = listaNoteCondivise.size() +" "+
								getString(listaNoteCondivise.size()==1 ? R.string.shared_note : R.string.shared_notes).toLowerCase();
						((TextView)tit.findViewById(R.id.titolo_testo)).setText( txt );
					}
					
					// Add notes in batches
					final int BATCH_SIZE = 50;
					final int[] currentIndex = {0};
					final int[] loadedCount = {0};
					
					// Process shared notes in batches
					Runnable addSharedNotesBatch = new Runnable() {
						@Override
						public void run() {
							int endIndex = Math.min(currentIndex[0] + BATCH_SIZE, listaNoteCondivise.size());
							for(int i = currentIndex[0]; i < endIndex; i++) {
								registerForContextMenu( addNote(scatola, listaNoteCondivise.get(i)) );
								loadedCount[0]++;
							}
							currentIndex[0] = endIndex;
							
							// Update progress text
							progressText.setText(loadedCount[0] + " / " + totaleNote);
							
							if(currentIndex[0] < listaNoteCondivise.size()) {
								handler.postDelayed(this, 10); // Small delay between batches
							} else {
								// Shared notes done, now add inline notes header and start inline notes
								if( !scegliNota && !visitaNote.listaNote.isEmpty() ) {
									View tit = inflater.inflate( R.layout.titoletto, scatola, false );
									String txt = visitaNote.listaNote.size() +" "+
											getString(visitaNote.listaNote.size()==1 ? R.string.simple_note : R.string.simple_notes).toLowerCase();
									((TextView)tit.findViewById(R.id.titolo_testo)).setText( txt );
									scatola.addView( tit );
								}
								
								// Process inline notes in batches
								currentIndex[0] = 0;
								Runnable addInlineNotesBatch = new Runnable() {
									@Override
									public void run() {
										int endIndex = Math.min(currentIndex[0] + BATCH_SIZE, visitaNote.listaNote.size());
										for(int i = currentIndex[0]; i < endIndex; i++) {
											if(!scegliNota) {
												registerForContextMenu( addNote(scatola, (Note)visitaNote.listaNote.get(i)) );
												loadedCount[0]++;
											}
										}
										currentIndex[0] = endIndex;
										
										// Update progress text
										progressText.setText(loadedCount[0] + " / " + totaleNote);
										
										if(currentIndex[0] < visitaNote.listaNote.size()) {
											handler.postDelayed(this, 10); // Small delay between batches
										} else {
											// All notes added, hide progress bar
											progressBar.setVisibility( View.GONE );
											progressText.setVisibility( View.GONE );
											scrollView.setVisibility( View.VISIBLE );
										}
									}
								};
								
								if(!scegliNota && !visitaNote.listaNote.isEmpty()) {
									handler.post(addInlineNotesBatch);
								} else {
									// No inline notes, hide progress bar now
									progressBar.setVisibility( View.GONE );
									progressText.setVisibility( View.GONE );
									scrollView.setVisibility( View.VISIBLE );
								}
							}
						}
					};
					
					if(!listaNoteCondivise.isEmpty()) {
						handler.post(addSharedNotesBatch);
					} else {
						// No shared notes, check inline notes
						if( !scegliNota && !visitaNote.listaNote.isEmpty() ) {
							View tit = inflater.inflate( R.layout.titoletto, scatola, false );
							String txt = visitaNote.listaNote.size() +" "+
									getString(visitaNote.listaNote.size()==1 ? R.string.simple_note : R.string.simple_notes).toLowerCase();
							((TextView)tit.findViewById(R.id.titolo_testo)).setText( txt );
							scatola.addView( tit );
							
							// Process inline notes in batches
							currentIndex[0] = 0;
							Runnable addInlineNotesBatch = new Runnable() {
								@Override
								public void run() {
									int endIndex = Math.min(currentIndex[0] + BATCH_SIZE, visitaNote.listaNote.size());
									for(int i = currentIndex[0]; i < endIndex; i++) {
										registerForContextMenu( addNote(scatola, (Note)visitaNote.listaNote.get(i)) );
										loadedCount[0]++;
									}
									currentIndex[0] = endIndex;
									
									// Update progress text
									progressText.setText(loadedCount[0] + " / " + totaleNote);
									
									if(currentIndex[0] < visitaNote.listaNote.size()) {
										handler.postDelayed(this, 10); // Small delay between batches
									} else {
										// All notes added, hide progress bar
										progressBar.setVisibility( View.GONE );
										progressText.setVisibility( View.GONE );
										scrollView.setVisibility( View.VISIBLE );
									}
								}
							};
							handler.post(addInlineNotesBatch);
						} else {
							// No notes at all, hide progress bar
							progressBar.setVisibility( View.GONE );
							progressText.setVisibility( View.GONE );
							scrollView.setVisibility( View.VISIBLE );
						}
					}
				});
			});
			
			vista.findViewById( R.id.fab ).setOnClickListener( v -> nuovaNota( getContext(), null ) );
		}
		return vista;
	}

	// Andandosene dall'attività senza aver scelto una nota condivisa resetta l'extra
	@Override
	public void onPause() {
		super.onPause();
		getActivity().getIntent().removeExtra("quadernoScegliNota");
	}

	View addNote( final LinearLayout scatola, final Note nota ) {
		View vistaNota = LayoutInflater.from(scatola.getContext()).inflate( R.layout.quaderno_pezzo, scatola, false );
		scatola.addView( vistaNota );
		String testo = nota.getValue();
		((TextView)vistaNota.findViewById( R.id.nota_testo )).setText( testo );
		TextView vistaCita = vistaNota.findViewById( R.id.nota_citazioni );
		if( nota.getId() == null )
			vistaCita.setVisibility( View.GONE );
		else {
			RiferimentiNota contaUso = new RiferimentiNota( gc, nota.getId(), false );
			vistaCita.setText( String.valueOf(contaUso.tot) );
		}
		vistaNota.setOnClickListener( v -> {
			// Restituisce l'id di una nota a Individuo e Dettaglio
			if( getActivity().getIntent().getBooleanExtra("quadernoScegliNota",false) ) {
				Intent intento = new Intent();
				intento.putExtra( "idNota", nota.getId() );
				getActivity().setResult( AppCompatActivity.RESULT_OK, intento );
				getActivity().finish();
			} else { // Apre il dettaglio della nota
				Intent intento = new Intent( scatola.getContext(), Nota.class );
				if( nota.getId() != null ) { // Nota condivisa
					Memoria.setPrimo( nota );
				} else { // Nota semplice
					new TrovaPila( gc, nota );
					intento.putExtra( "daQuaderno", true );
				}
				scatola.getContext().startActivity( intento );
			}
		});
		vistaNota.setTag( nota );	// per il menu contestuale Elimina
		return vistaNota;
	}

	// Crea una nuova nota condivisa, attaccata a un contenitore oppure slegata
	static void nuovaNota( Context contesto, Object contenitore ){
		Note notaNova = new Note();
		String id = U.newId( gc, Note.class );
		notaNova.setId( id );
		notaNova.setValue( "" );
		gc.addNote( notaNova );
		if( contenitore != null ) {
			NoteRef refNota = new NoteRef();
			refNota.setRef( id );
			((NoteContainer)contenitore).addNoteRef( refNota );
		}
		U.saveJson( true, notaNova );
		Memoria.setPrimo( notaNova );
		contesto.startActivity( new Intent( contesto, Nota.class ) );
	}

	private Note nota;
	@Override
	public void onCreateContextMenu( ContextMenu menu, View vista, ContextMenu.ContextMenuInfo info ) {
		nota = (Note) vista.getTag();
		menu.add(0, 0, 0, R.string.delete );
	}
	@Override
	public boolean onContextItemSelected( MenuItem item ) {
		if( item.getItemId() == 0 ) { // Elimina
			Object[] capi = U.deleteNote( nota, null );
			U.saveJson( false, capi );
			getActivity().recreate();
		} else {
			return false;
		}
		return true;
	}
}