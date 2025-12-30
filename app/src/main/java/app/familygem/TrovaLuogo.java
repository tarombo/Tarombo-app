// Classe di servizio per suggerire i nomi dei luoghi formattati in stile Gedcom grazie a GeoNames

package app.familygem;

import android.content.Context;
import android.text.InputType;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import app.familygem.BuildConfig;

public class TrovaLuogo extends AppCompatAutoCompleteTextView {
	// GeoNames settings
	private final app.familygem.geonames.GeoNamesService service;

	public TrovaLuogo(Context contesto, AttributeSet as) {
		super(contesto, as);
		AdattatoreLista adattatoreLista = new AdattatoreLista(contesto, android.R.layout.simple_spinner_dropdown_item);
		setAdapter(adattatoreLista);
		setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);

		retrofit2.Retrofit retrofit = new retrofit2.Retrofit.Builder().baseUrl("http://api.geonames.org/")
				.addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create()).build();
		service = retrofit.create(app.familygem.geonames.GeoNamesService.class);
	}

	class AdattatoreLista extends ArrayAdapter<String> implements Filterable {
		List<String> places;

		AdattatoreLista(Context contesto, int pezzo) {
			super(contesto, pezzo);
			places = new ArrayList<>();
		}

		@Override
		public int getCount() {
			return places.size();
		}

		@Override
		public String getItem(int index) {
			if (!places.isEmpty() && index < places.size()) // Evita IndexOutOfBoundsException
				return places.get(index);
			return "";
		}

		@Override
		public Filter getFilter() {
			return new Filter() {
				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					FilterResults filterResults = new FilterResults();
					if (constraint != null) {
						try {
							retrofit2.Call<app.familygem.geonames.GeoNamesResponse> call = service.search(
									constraint.toString(),
									3,
									"FULL",
									Locale.getDefault().getLanguage(), // en, es, it...
									BuildConfig.geoNamesUsername);
							app.familygem.geonames.GeoNamesResponse response = call.execute().body();

							places.clear();
							if (response != null && response.geonames != null) {
								for (app.familygem.geonames.GeoNamesToponym topo : response.geonames) {
									String str = topo.name; // Toponimo
									if (topo.adminName4 != null && !topo.adminName4.equals(str))
										str += ", " + topo.adminName4; // Paese
									if (topo.adminName3 != null && !str.contains(topo.adminName3))
										str += ", " + topo.adminName3; // Comune
									if (topo.adminName2 != null && !topo.adminName2.isEmpty()
											&& !str.contains(topo.adminName2))
										str += ", " + topo.adminName2; // Provincia
									if (topo.adminName1 != null && !str.contains(topo.adminName1))
										str += ", " + topo.adminName1; // Regione
									if (topo.countryName != null && !str.contains(topo.countryName))
										str += ", " + topo.countryName; // Nazione
									if (str != null && !places.contains(str)) // Avoid null and duplicates
										places.add(str);
								}
							}
							filterResults.values = places;
							filterResults.count = places.size();
						} catch (Exception e) {
						}
					}
					return filterResults;
				}

				@Override
				protected void publishResults(CharSequence constraint, FilterResults results) {
					if (results != null && results.count > 0) {
						notifyDataSetChanged();
					} else {
						notifyDataSetInvalidated();
					}
				}
			};
		}
	}
}
