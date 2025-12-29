package app.familygem;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.familygem.action.DeleteMediaFileTask;
import com.familygem.utility.Helper;
import com.canhub.cropper.CropImage;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.MediaContainer;
import org.folg.gedcom.model.MediaRef;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import app.familygem.visitors.MediaContainerList;
import app.familygem.visitors.MediaReferences;
import app.familygem.visitors.FindStack;
import static app.familygem.Global.gc;
import app.familygem.R;

public class Galleria extends Fragment {

	MediaContainerList visitaMedia;
	AdattatoreGalleriaMedia adattatore;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle bandolo) {
		setHasOptionsMenu(true);
		View vista = inflater.inflate(R.layout.galleria, container, false);
		RecyclerView griglia = vista.findViewById(R.id.galleria);
		ProgressBar progressBar = vista.findViewById(R.id.galleria_circolo);
		TextView progressText = vista.findViewById(R.id.galleria_progress_text);

		griglia.setHasFixedSize(true);
		if (gc != null) {
			// Show progress bar and hide content
			progressBar.setVisibility(View.VISIBLE);
			progressText.setVisibility(View.VISIBLE);
			griglia.setVisibility(View.GONE);

			// Load media in background thread
			ExecutorService executor = Executors.newSingleThreadExecutor();
			Handler handler = new Handler(Looper.getMainLooper());

			executor.execute(() -> {
				visitaMedia = new MediaContainerList(gc,
						!getActivity().getIntent().getBooleanExtra("galleriaScegliMedia", false));
				gc.accept(visitaMedia);
				final int totaleMedia = visitaMedia.mediaList.size();

				// Update UI on main thread
				handler.post(() -> {
					// Update title
					((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(totaleMedia
							+ " " + getString(R.string.media).toLowerCase());

					// Initialize progress text
					progressText.setText("0 / " + totaleMedia);

					// Setup RecyclerView
					RecyclerView.LayoutManager gestoreLayout = new GridLayoutManager(getContext(), 2);
					griglia.setLayoutManager(gestoreLayout);
					adattatore = new AdattatoreGalleriaMedia(visitaMedia.mediaList, true);
					griglia.setAdapter(adattatore);

					// Batch notify adapter
					final int BATCH_SIZE = 50;
					final int[] currentIndex = { 0 };

					Runnable notifyBatch = new Runnable() {
						@Override
						public void run() {
							int endIndex = Math.min(currentIndex[0] + BATCH_SIZE, totaleMedia);
							if (endIndex > currentIndex[0]) {
								adattatore.notifyItemRangeInserted(currentIndex[0], endIndex - currentIndex[0]);
								currentIndex[0] = endIndex;

								// Update progress text
								progressText.setText(currentIndex[0] + " / " + totaleMedia);
							}

							if (currentIndex[0] < totaleMedia) {
								handler.postDelayed(this, 10);
							} else {
								// All media loaded, hide progress bar
								progressBar.setVisibility(View.GONE);
								progressText.setVisibility(View.GONE);
								griglia.setVisibility(View.VISIBLE);
							}
						}
					};

					if (totaleMedia > 0) {
						handler.post(notifyBatch);
					} else {
						// No media, hide progress bar
						progressBar.setVisibility(View.GONE);
						progressText.setVisibility(View.GONE);
						griglia.setVisibility(View.VISIBLE);
					}
				});
			});

			vista.findViewById(R.id.fab)
					.setOnClickListener(v -> F.openImagePicker(getContext(), Galleria.this, 4546, null));
		}
		return vista;
	}

	// Andandosene dall'attività resetta l'extra se non è stato scelto un media
	// condiviso
	@Override
	public void onPause() {
		super.onPause();
		getActivity().getIntent().removeExtra("galleriaScegliMedia");
	}

	// Scrive il titolo nella barra
	void arredaBarra() {
		((AppCompatActivity) getActivity()).getSupportActionBar().setTitle(visitaMedia.mediaList.size()
				+ " " + getString(R.string.media).toLowerCase());
	}

	// Aggiorna i contenuti della galleria
	void ricrea() {
		visitaMedia.mediaList.clear();
		gc.accept(visitaMedia);
		adattatore.notifyDataSetChanged();
		arredaBarra();
	}

	// todo bypassabile?
	static int popolarita(Media med) {
		MediaReferences riferiMedia = new MediaReferences(gc, med, false);
		return riferiMedia.count;
	}

	static Media nuovoMedia(Object contenitore) {
		Media media = new Media();
		media.setId(U.newId(gc, Media.class));
		media.setFileTag("FILE"); // Necessario per poi esportare il Gedcom
		gc.addMedia(media);
		if (contenitore != null) {
			MediaRef rifMed = new MediaRef();
			rifMed.setRef(media.getId());
			((MediaContainer) contenitore).addMediaRef(rifMed);
		}
		return media;
	}

	// Scollega da un contenitore un media condiviso
	static void scollegaMedia(String mediaId, MediaContainer container) {
		Iterator<MediaRef> refs = container.getMediaRefs().iterator();
		while (refs.hasNext()) {
			MediaRef ref = refs.next();
			if (ref.getMedia(Global.gc) == null // Eventuale ref a un media inesistente
					|| ref.getRef().equals(mediaId))
				refs.remove();
		}
		if (container.getMediaRefs().isEmpty())
			container.setMediaRefs(null);
	}

	// Elimina un media condiviso o locale e rimuove i riferimenti nei contenitori
	// Restituisce un array con i capostipiti modificati
	public static Object[] eliminaMedia(Media media, View vista, Context context) {
		Set<Object> capi;
		if (media.getId() != null) { // media OBJECT
			gc.getMedia().remove(media);
			deleteMediaFileOnGithub(context, media);
			// Elimina i riferimenti in tutti i contenitori
			MediaReferences eliminaMedia = new MediaReferences(gc, media, true);
			capi = eliminaMedia.rootObjects;
		} else { // media LOCALE
			new FindStack(gc, media); // trova temporaneamente la pila del media per individuare il container
			MediaContainer container = (MediaContainer) Memoria.getObjectContainer();
			container.getMedia().remove(media);
			// delete file media from github
			deleteMediaFileOnGithub(context, media);
			if (container.getMedia().isEmpty())
				container.setMedia(null);
			capi = new HashSet<>(); // set con un solo Object capostipite
			capi.add(Memoria.getFirstObject());
			Memoria.goBack(); // elimina la pila appena creata
		}
		Memoria.invalidateInstances(media);
		if (vista != null)
			vista.setVisibility(View.GONE);
		return capi.toArray(new Object[0]);
	}

	public static void deleteMediaFileOnGithub(Context context, Media media) {
		Settings.Tree currentTree = Global.settings.getCurrentTree();
		if (currentTree != null && currentTree.githubRepoFullName != null
				&& !currentTree.githubRepoFullName.isEmpty()) {
			// delete file media from github
			Helper.requireEmail(context,
					context.getString(R.string.set_email_for_commit),
					context.getString(R.string.OK), context.getString(R.string.cancel), email -> {
						DeleteMediaFileTask.execute(context, currentTree.githubRepoFullName, email, currentTree.id,
								media);
					});
		}
	}

	// Il file pescato dal file manager diventa media condiviso
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			if (requestCode == 4546) { // File preso da app fornitrice viene salvato in Media ed eventualmente
										// ritagliato
				Media media = nuovoMedia(null);
				if (F.proposeCrop(getContext(), this, data, media)) { // se è un'immagine (quindi ritagliabile)
					U.saveJson(false, media);
					// Non deve scattare onRestart() + recreate() perché poi il fragment di arrivo
					// non è più lo stesso
					return;
				}
			} else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
				F.finishImageCrop(data, getActivity());
			}
			U.saveJson(true, Global.mediaCroppato);
		} else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) // se clic su freccia indietro in Crop
																				// Image
			Global.edited = true;
	}

	// Menu contestuale
	private Media media;

	@Override
	public void onCreateContextMenu(ContextMenu menu, View vista, ContextMenu.ContextMenuInfo info) {
		media = (Media) vista.getTag(R.id.tag_object);
		menu.add(0, 0, 0, R.string.delete);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (item.getItemId() == 0) {
			Object[] modificati = eliminaMedia(media, null, getActivity());
			ricrea();
			U.saveJson(false, modificati);
			return true;
		}
		return false;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.add(0, 0, 0, R.string.media_folders);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == 0) {
			startActivity(new Intent(getContext(), CartelleMedia.class)
					.putExtra("idAlbero", Global.settings.openTree));
			return true;
		}
		return false;
	}

	@Override
	public void onRequestPermissionsResult(int codice, String[] permessi, int[] accordi) {
		F.permissionsResult(getContext(), this, codice, permessi, accordi, null);
	}
}
