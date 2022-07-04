package app.familygem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;

import com.familygem.action.SearchUsersTask;
import com.familygem.utility.GithubUser;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;

public class AddCollaboratorActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, UsersListViewAdapter.ItemCallback {
    // Declare Variables
    ListView list;
    UsersListViewAdapter adapter;
    SearchView editsearch;
    ArrayList<GithubUser> arraylist = new ArrayList<>();
    ChipGroup selectedUsersChipGroup;
    ArrayList<GithubUser> selectedUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_collaborator);
        selectedUsersChipGroup = findViewById(R.id.selected_users);
        // Locate the ListView in listview_main.xml
        list = (ListView) findViewById(R.id.listview);

        // Pass results to ListViewAdapter Class
        adapter = new UsersListViewAdapter(this, arraylist, this);

        // Binds the Adapter to the ListView
        list.setAdapter(adapter);

        // Locate the EditText in listview_main.xml
        editsearch = (SearchView) findViewById(R.id.search);
        editsearch.setOnQueryTextListener(this);

        // Barra
        ActionBar barra = getSupportActionBar();
        View barraAzione = getLayoutInflater().inflate( R.layout.barra_edita, new LinearLayout(getApplicationContext()), false);
        barraAzione.findViewById( R.id.edita_annulla ).setOnClickListener( v -> finish());
        Button btnAdd = barraAzione.findViewById(R.id.edita_salva);
        btnAdd.setOnClickListener( v -> addCollaborators() );
        btnAdd.setText(R.string.assign_to_collaborators);
        barra.setCustomView( barraAzione );
        barra.setDisplayShowCustomEnabled( true );
    }

    private void addCollaborators() {
        if (selectedUsers.size() == 0)
            return;

        finish();
    }

    @Override
    public boolean onQueryTextSubmit(String query) {

        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        //        adapter.filter(text);
        SearchUsersTask.execute(this, newText, users -> {
            adapter.clearData();
            adapter.setUserList(users);
            adapter.notifyDataSetChanged();
        }, error -> {

        });
        return false;
    }

    @Override
    public void onItemClick(final GithubUser user) {
        addSelectedUser(user);
        Log.d("collaborator", "user:" + user.getName() + " totals:" + selectedUsers.size());
    }

    private void removeSelectedUser(final GithubUser user) {
        for (GithubUser selectedUser : selectedUsers) {
            if (selectedUser.getUserName().equals(user.getUserName())) {
                selectedUsers.remove(selectedUser);
                return;
            }
        }
    }

    private void addSelectedUser(final GithubUser user) {
        for (GithubUser selectedUser : selectedUsers) {
            if (selectedUser.getUserName().equals(user.getUserName())) {
                return;
            }
        }
        View view = getLayoutInflater().inflate(R.layout.item_selected_user,
                selectedUsersChipGroup, false);
        Chip selectedUserChip = view.findViewById(R.id.chips_item_filter);
        selectedUserChip.setText(user.getName());
        selectedUserChip.setTag(user.getUserName());
        selectedUsersChipGroup.addView(selectedUserChip);
        selectedUsers.add(user);
        selectedUserChip.setOnCloseIconClickListener((v) -> {
            selectedUsersChipGroup.removeView(v);
            removeSelectedUser(user);
        });
    }
}