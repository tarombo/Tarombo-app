package app.familygem;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import org.folg.gedcom.model.Address;
import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.GedcomTag;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.NoteContainer;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.SourceCitation;
import org.folg.gedcom.model.SourceCitationContainer;
import org.folg.gedcom.model.SpouseRef;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import app.familygem.constants.Gender;
import app.familygem.dettaglio.Evento;
import app.familygem.dettaglio.Nome;
import static app.familygem.Global.gc;
import app.familygem.R;
public class IndividuoEventi extends Fragment {

	Person uno;
	private View vistaCambi;
	boolean hasDeat = false;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View vistaEventi = inflater.inflate(R.layout.individuo_scheda, container, false);
		if( gc != null ) {
			LinearLayout scatola = vistaEventi.findViewById(R.id.contenuto_scheda);
			uno = gc.getPerson(Global.indi);
			if( uno != null ) {
				for( Name nome : uno.getNames()) {
					String tit = getString(R.string.name);
					if( nome.getType() != null && !nome.getType().isEmpty() ) {
						tit += " (" + TypeView.getTranslatedType(nome.getType(), TypeView.Combo.NAME) + ")";
					}
					piazzaEvento(scatola, tit, U.getFullName(nome), nome);
				}

				makeNotNull(uno, "BIRT");

				Settings.Tree tree = Global.settings.getCurrentTree();
				boolean isOnline = tree != null && tree.githubRepoFullName != null && !tree.isForked;

				for (EventFact fatto : uno.getEventsFacts() ) {
					String txt = "";
					String tag = fatto.getTag();
					if(tag == null)
						continue;

					// skip private tag
					if(tag.equals(U.PRIVATE_TAG))
						continue;

					if(tag.equals("DEAT"))
						hasDeat = true;

					if( fatto.getValue() != null ) {
						if( fatto.getValue().equals("Y") && tag!=null && ( tag.equals("BIRT") || tag.equals("CHR") || tag.equals("DEAT") ) )
							txt = getString(R.string.yes);
						else txt = fatto.getValue();
						txt += "\n";
					}
					//if( fatto.getType() != null ) txt += fatto.getType() + "\n"; // Included in event title
					if( fatto.getDate() != null ) txt += new Datatore(fatto.getDate()).writeDateLong() + "\n";
					if( fatto.getPlace() != null ) txt += fatto.getPlace() + "\n";
					Address indirizzo = fatto.getAddress();
					if( indirizzo != null ) txt += Dettaglio.writeAddress(indirizzo, true) + "\n";
					if( fatto.getCause() != null )	txt += fatto.getCause();
					if( txt.endsWith("\n") ) txt = txt.substring(0, txt.length() - 1); // Rimuove l'ultimo acapo
					piazzaEvento( scatola, writeEventTitle(fatto), txt, fatto );
				}

				if(!hasDeat){
					EventFact eventFact = new EventFact();
					eventFact.setTag("DEAT");
					eventFact.setDate("");
					eventFact.setPlace("");
					piazzaEvento( scatola, writeEventTitle(eventFact), "", eventFact);
				}

				// Show private tag here
				if (isOnline) {
					showPrivateSwitch(scatola);
				}

				for( Extension est : U.findExtensions( uno ) ) {
					piazzaEvento( scatola, est.name, est.text, est.gedcomTag );
				}

				U.addNotes( scatola, uno, true );
				U.citeSources( scatola, uno );
				vistaCambi = U.cambiamenti( scatola, uno.getChange() );
			}
		}
		return vistaEventi;
	}

	private void makeNotNull(Person person, String tag){
		List<EventFact> eventFacts = person.getEventsFacts();
		Optional<EventFact> optional = eventFacts.stream().filter(x -> x.getTag().equals(tag)).findFirst();
		if(optional.isPresent()){
			EventFact fact = optional.get();
			if(fact.getPlace() == null)
				fact.setPlace("");

			if(fact.getDate() == null)
				fact.setDate("");
		}
		else{
			EventFact fact = new EventFact();
			fact.setTag(tag);
			fact.setPlace("");
			fact.setDate("");
			person.addEventFact(fact);
		}
	}

	// Scopre se è un nome con name pieces o un suffisso nel value
	boolean nomeComplesso( Name n ) {
		// Name pieces
		boolean ricco = n.getGiven() != null || n.getSurname() != null
				|| n.getPrefix() != null || n.getSurnamePrefix() != null || n.getSuffix() != null
				|| n.getFone() != null || n.getRomn() != null;
		// Qualcosa dopo il cognome
		String nome = n.getValue();
		boolean suffisso = false;
		if( nome != null ) {
			nome = nome.trim();
			if( nome.lastIndexOf('/') < nome.length()-1 )
				suffisso = true;
		}
		return ricco || suffisso;
	}

	// Compose the title of an event of the person
	public static String writeEventTitle(EventFact event) {
		int str = 0;
		switch( event.getTag() ) {
			case "SEX": str = R.string.sex; break;
			case "BIRT": str = R.string.birth; break;
			case "BAPM": str = R.string.baptism; break;
			case "BURI": str = R.string.burial; break;
			case "DEAT": str = R.string.death; break;
			case "EVEN": str = R.string.event; break;
			case "OCCU": str = R.string.occupation; break;
			case "RESI": str = R.string.residence;
		}
		String txt;
		if( str != 0 )
			txt = Global.context.getString(str);
		else
			txt = event.getDisplayType();
		if( event.getType() != null )
			txt += " (" + event.getType() + ")";
		return txt;
	}

	private int sessoCapitato;
	private void piazzaEvento(LinearLayout scatola, String titolo, String testo, Object oggetto) {
		View vistaFatto = LayoutInflater.from(scatola.getContext()).inflate( R.layout.individuo_eventi_pezzo, scatola, false);
		scatola.addView( vistaFatto );

		TextView tvTitolo = vistaFatto.findViewById( R.id.evento_titolo );
		tvTitolo.setText( titolo );

		TextView vistaTesto = vistaFatto.findViewById( R.id.evento_text );
		if( testo.isEmpty() ) vistaTesto.setVisibility( View.GONE );
		else vistaTesto.setText( testo );

		if( oggetto instanceof SourceCitationContainer ) {
			List<SourceCitation> citaFonti = ((SourceCitationContainer)oggetto).getSourceCitations();
			TextView vistaCitaFonti = vistaFatto.findViewById( R.id.evento_fonti );
			if( !citaFonti.isEmpty() ) {
				vistaCitaFonti.setText( String.valueOf(citaFonti.size()) );
				vistaCitaFonti.setVisibility( View.VISIBLE );
			}
		}
		LinearLayout scatolaAltro = vistaFatto.findViewById( R.id.evento_altro );
		if( oggetto instanceof NoteContainer )
			U.addNotes( scatolaAltro, oggetto, false );
		vistaFatto.setTag( R.id.tag_object, oggetto );
		registerForContextMenu( vistaFatto );
		if( oggetto instanceof Name ) {
			U.addMedia( scatolaAltro, oggetto, false );
			vistaFatto.setOnClickListener( v -> {
				Memoria.add( oggetto );
				startActivity( new Intent(getContext(), Nome.class) );
			});
		} else if( oggetto instanceof EventFact ) {
			EventFact eventFact = (EventFact)oggetto;
			String tag = eventFact.getTag();

			boolean editable = true;

			if(tag.equals("DEAT")){
				SwitchCompat swDead = vistaFatto.findViewById(R.id.sw_dead);
				swDead.setVisibility(View.VISIBLE);
				boolean isDead = Objects.equals(eventFact.getValue(), "Y") || !(Objects.equals(eventFact.getDate(), ""));
				swDead.setChecked(isDead);

				CompoundButton.OnCheckedChangeListener listener = (btn, value) -> {
					tvTitolo.setVisibility(value ? View.VISIBLE : View.GONE);
					vistaTesto.setVisibility(value ? View.VISIBLE : View.GONE);

					List<EventFact> eventFacts = uno.getEventsFacts();
					if(!value){
						eventFact.setDate("");
						eventFact.setPlace("");
						vistaTesto.setText("");
						eventFacts.remove(eventFact);
					}
					else{
						if(!eventFacts.contains(eventFact)){
							String date = eventFact.getDate();
							if(date == null || date.isEmpty()){
								eventFact.setValue("Y");
							}
							uno.addEventFact(eventFact);
						}
					}
				};

				swDead.setOnCheckedChangeListener((btn, value) -> {
					listener.onCheckedChanged(btn, value);
					U.saveJson( true, uno );
				});

				listener.onCheckedChanged(swDead, isDead);
			}
			else if(tag.equals("SEX") ) {
				Map<String,String> sessi = new LinkedHashMap<>();
				sessi.put( "M", getString(R.string.male) );
				sessi.put( "F", getString(R.string.female) );
				sessi.put( "U", getString(R.string.unknown) );
				vistaTesto.setText( testo );
				sessoCapitato = 0;
				for( Map.Entry<String,String> sex : sessi.entrySet() ) {
					if( testo.equals( sex.getKey() ) ) {
						vistaTesto.setText( sex.getValue() );
						break;
					}
					sessoCapitato++;
				}
				if( sessoCapitato > 2 ) sessoCapitato = -1;
				vistaFatto.setOnClickListener( vista -> new AlertDialog.Builder( vista.getContext() )
					.setSingleChoiceItems( sessi.values().toArray(new String[0]), sessoCapitato, (dialog, item) -> {
						eventFact.setValue( new ArrayList<>(sessi.keySet()).get(item) );
						aggiornaRuoliConiugali(uno);
						dialog.dismiss();
						refresh(1);
						U.saveJson( true, uno );
					}).show() );

				editable = false;
			}

			if(editable){
				U.addMedia(scatolaAltro, oggetto, false);
				vistaFatto.setOnClickListener( v -> {
					Memoria.add(oggetto);
					startActivity(new Intent(getContext(), Evento.class));
				});
			}
		} else if( oggetto instanceof GedcomTag ) {
			vistaFatto.setOnClickListener( v -> {
				Memoria.add( oggetto );
				startActivity( new Intent( getContext(), app.familygem.dettaglio.ExtensionDetail.class ) );
			});
		}
	}

	private void showPrivateSwitch(LinearLayout scatola) {
		View vistaFatto = LayoutInflater.from(scatola.getContext()).inflate( R.layout.individuo_eventi_pezzo, scatola, false);
		scatola.addView( vistaFatto );

		TextView tvTitolo = vistaFatto.findViewById( R.id.evento_titolo );
		tvTitolo.setVisibility(View.GONE);

		TextView vistaTesto = vistaFatto.findViewById( R.id.evento_text );
		vistaTesto.setVisibility(View.GONE);

		SwitchCompat swPrivate = vistaFatto.findViewById(R.id.sw_private);
		swPrivate.setVisibility(View.VISIBLE);

		swPrivate.setChecked(U.isPrivate(uno));
		swPrivate.setOnCheckedChangeListener( (coso, attivo) -> {
			if (attivo)
				U.setPrivate(uno);
			else
				U.setNonPrivate(uno);
		});
	}

	// In tutte le famiglie coniugali rimuove gli spouse ref di 'person' e ne aggiunge uno corrispondente al sesso
	// Serve soprattutto in caso di esportazione del Gedcom per avere allineati gli HUSB e WIFE con il sesso
	static void aggiornaRuoliConiugali(Person person) {
		SpouseRef spouseRef = new SpouseRef();
		spouseRef.setRef(person.getId());
		boolean removed = false;
		for( Family fam : person.getSpouseFamilies(gc) ) {
			if( Gender.isFemale(person) ) { // Female 'person' will become a wife
				Iterator<SpouseRef> husbandRefs = fam.getHusbandRefs().iterator();
				while( husbandRefs.hasNext() ) {
					String hr = husbandRefs.next().getRef();
					if( hr != null && hr.equals(person.getId()) ) {
						husbandRefs.remove();
						removed = true;
					}
				}
				if( removed ) {
					fam.addWife(spouseRef);
					removed = false;
				}
			} else { // For all other sexs 'person' will become husband
				Iterator<SpouseRef> wifeRefs = fam.getWifeRefs().iterator();
				while( wifeRefs.hasNext() ) {
					String wr = wifeRefs.next().getRef();
					if( wr != null && wr.equals(person.getId()) ) {
						wifeRefs.remove();
						removed = true;
					}
				}
				if( removed ) {
					fam.addHusband(spouseRef);
					removed = false;
				}
			}
		}
	}

	// Menu contestuale
	View vistaPezzo;
	Object oggettoPezzo;
	@Override
	public void onCreateContextMenu( ContextMenu menu, View vista, ContextMenu.ContextMenuInfo info ) {
		// menuInfo come al solito è null
		vistaPezzo = vista;
		oggettoPezzo = vista.getTag( R.id.tag_object );
		if( oggettoPezzo instanceof Name ) {
			menu.add( 0, 200, 0, R.string.copy );
			if( uno.getNames().indexOf(oggettoPezzo) > 0 )
				menu.add( 0, 201, 0, R.string.move_up );
			if( uno.getNames().indexOf(oggettoPezzo) < uno.getNames().size()-1 )
				menu.add( 0, 202, 0, R.string.move_down );
			menu.add( 0, 203, 0, R.string.delete );
		} else if( oggettoPezzo instanceof EventFact ) {
			menu.add( 0, 210, 0, R.string.copy );
			if( uno.getEventsFacts().indexOf(oggettoPezzo) > 0 )
				menu.add( 0, 211, 0, R.string.move_up );
			if( uno.getEventsFacts().indexOf(oggettoPezzo) < uno.getEventsFacts().size()-1 )
				menu.add( 0, 212, 0, R.string.move_down );
			menu.add( 0, 213, 0, R.string.delete );
		} else if( oggettoPezzo instanceof GedcomTag ) {
			menu.add( 0, 220, 0, R.string.copy );
			menu.add( 0, 221, 0, R.string.delete );
		} else if( oggettoPezzo instanceof Note ) {
			menu.add( 0, 225, 0, R.string.copy );
			if( ((Note)oggettoPezzo).getId() != null )
				menu.add( 0, 226, 0, R.string.unlink );
			menu.add( 0, 227, 0, R.string.delete );
		} else if( oggettoPezzo instanceof SourceCitation ) {
			menu.add( 0, 230, 0, R.string.copy );
			menu.add( 0, 231, 0, R.string.delete );
		}
	}
	@Override
	public boolean onContextItemSelected( MenuItem item ) {
		List<Name> nomi = uno.getNames();
		List<EventFact> fatti = uno.getEventsFacts();
		int cosa = 0; // cosa aggiornare dopo la modifica
		switch( item.getItemId() ) {
			// Nome
			case 200: // Copia nome
			case 210: // Copia evento
			case 220: // Copia estensione
				U.copyToClipboard(((TextView)vistaPezzo.findViewById(R.id.evento_titolo)).getText(),
						((TextView)vistaPezzo.findViewById(R.id.evento_text)).getText());
				return true;
			case 201: // Sposta su
				nomi.add(nomi.indexOf(oggettoPezzo) - 1, (Name)oggettoPezzo);
				nomi.remove(nomi.lastIndexOf(oggettoPezzo));
				cosa = 2;
				break;
			case 202: // Sposta giù
				nomi.add(nomi.indexOf(oggettoPezzo) + 2, (Name)oggettoPezzo);
				nomi.remove(nomi.indexOf(oggettoPezzo));
				cosa = 2;
				break;
			case 203: // Elimina
				if( U.preserva(oggettoPezzo) ) return false;
				uno.getNames().remove(oggettoPezzo);
				Memoria.invalidateInstances(oggettoPezzo);
				vistaPezzo.setVisibility(View.GONE);
				cosa = 2;
				break;
			// Evento generico
			case 211: // Sposta su
				fatti.add(fatti.indexOf(oggettoPezzo) - 1, (EventFact)oggettoPezzo);
				fatti.remove(fatti.lastIndexOf(oggettoPezzo));
				cosa = 1;
				break;
			case 212: // Sposta giu
				fatti.add(fatti.indexOf(oggettoPezzo) + 2, (EventFact)oggettoPezzo);
				fatti.remove(fatti.indexOf(oggettoPezzo));
				cosa = 1;
				break;
			case 213:
				// todo Confirmation elimina
				uno.getEventsFacts().remove(oggettoPezzo);
				Memoria.invalidateInstances(oggettoPezzo);
				vistaPezzo.setVisibility(View.GONE);
				break;
			// Extension
			case 221: // Elimina
				U.removeExtension((GedcomTag)oggettoPezzo, uno, vistaPezzo);
				break;
			// Nota
			case 225: // Copia
				U.copyToClipboard(getText(R.string.note), ((TextView)vistaPezzo.findViewById(R.id.nota_text)).getText());
				return true;
			case 226: // Scollega
				U.unlinkNote((Note)oggettoPezzo, uno, vistaPezzo);
				break;
			case 227:
				Object[] capi = U.deleteNote((Note)oggettoPezzo, vistaPezzo);
				U.saveJson(true, capi);
				refresh(0);
				return true;
			// Citazione fonte
			case 230: // Copia
				U.copyToClipboard(getText(R.string.source_citation),
						((TextView)vistaPezzo.findViewById(R.id.source_text)).getText() + "\n"
								+ ((TextView)vistaPezzo.findViewById(R.id.citation_text)).getText());
				return true;
			case 231: // Elimina
				// todo conferma : Vuoi eliminare questa citazione della fonte? La fonte continuerà ad esistere.
				uno.getSourceCitations().remove(oggettoPezzo);
				Memoria.invalidateInstances(oggettoPezzo);
				vistaPezzo.setVisibility(View.GONE);
				break;
			default:
				return false;
		}
		U.saveJson(true, uno);
		refresh(cosa);
		return true;
	}

	// Rinfresca il contenuto del frammento Eventi
	void refresh(int what) {
		if( what == 0 ) { // sostituisce solo la data di cambiamento
			LinearLayout scatola = getActivity().findViewById(R.id.contenuto_scheda);
			if( vistaCambi != null )
				scatola.removeView(vistaCambi);
			vistaCambi = U.cambiamenti(scatola, uno.getChange());
		} else { // ricarica il fragment
			FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
			fragmentManager.beginTransaction().detach(this).commit();
			fragmentManager.beginTransaction().attach(this).commit();
			if( what == 2 ) { // aggiorna anche il titolo dell'activity
				CollapsingToolbarLayout barraCollasso = requireActivity().findViewById(R.id.toolbar_layout);
				barraCollasso.setTitle(U.getPrincipalName(uno));
			}
		}
	}
}
