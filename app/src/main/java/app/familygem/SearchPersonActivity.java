package app.familygem;

import static app.familygem.Global.gc;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import org.folg.gedcom.model.Person;

import java.util.ArrayList;
import java.util.List;

import app.familygem.constants.Gender;

public class SearchPersonActivity extends AppCompatActivity {

    private List<Person> people;
    private PersonSearchAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_person);

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.find_person);

        // Setup RecyclerView
        recyclerView = findViewById(R.id.recycler_view);
        if (gc != null) {
            people = new ArrayList<>(gc.getPeople());
            adapter = new PersonSearchAdapter();
            recyclerView.setAdapter(adapter);
        }
    }

        @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.cerca, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.ricerca).getActionView();
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchView.clearFocus();
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    if (adapter != null) {
                        adapter.getFilter().filter(newText);
                    }
                    return true;
                }
            });
        }
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private class PersonSearchAdapter extends RecyclerView.Adapter<PersonSearchAdapter.PersonViewHolder> implements Filterable {
        private List<Person> filteredPeople;

        public PersonSearchAdapter() {
            this.filteredPeople = new ArrayList<>(people);
        }

        @Override
        public PersonViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pezzo_individuo, parent, false);
            return new PersonViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PersonViewHolder holder, int position) {
            Person person = filteredPeople.get(position);
            holder.bind(person);
        }

        @Override
        public int getItemCount() {
            return filteredPeople.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    String query = charSequence.toString();
                    List<Person> filtered = new ArrayList<>();

                    if (query.isEmpty()) {
                        filtered.addAll(people);
                    } else {
                        for (Person person : people) {
                            if (U.epiteto(person).toLowerCase().contains(query.toLowerCase())) {
                                filtered.add(person);
                            }
                        }
                    }

                    FilterResults filterResults = new FilterResults();
                    filterResults.values = filtered;
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    filteredPeople = (List<Person>) filterResults.values;
                    notifyDataSetChanged();
                }
            };
        }

        class PersonViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            View itemView;

            PersonViewHolder(View itemView) {
                super(itemView);
                this.itemView = itemView;
                itemView.setOnClickListener(this);
            }

            void bind(Person person) {
                // Set person name
                TextView nameView = itemView.findViewById(R.id.indi_nome);
                nameView.setText(U.epiteto(person));

                // Set gender border
                int borderResource;
                Gender gender = Gender.getGender(person);
                switch (gender) {
                    case MALE:
                        borderResource = R.drawable.casella_bordo_maschio;
                        break;
                    case FEMALE:
                        borderResource = R.drawable.casella_bordo_femmina;
                        break;
                    default:
                        borderResource = R.drawable.casella_bordo_neutro;
                }
                itemView.findViewById(R.id.indi_bordo).setBackgroundResource(borderResource);

                // Set details
                U.details(person, itemView.findViewById(R.id.indi_dettagli));

                // Set photo
                F.showPrimaryPhoto(gc, person, itemView.findViewById(R.id.indi_foto));

                // Set death indicator
                itemView.findViewById(R.id.indi_lutto).setVisibility(U.isDead(person) ? View.VISIBLE : View.GONE);

                // Store person ID in tag
                itemView.setTag(person.getId());
            }

            @Override
            public void onClick(View view) {
                String personId = (String) view.getTag();
                Intent resultIntent = new Intent();
                resultIntent.putExtra("selectedPersonId", personId);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        }
    }
}