package app.familygem;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SearchView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.familygem.action.InviteCollaboratorsTask;
import com.familygem.action.SearchUsersTask;
import com.familygem.utility.GithubUser;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class AddCollaboratorActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, UsersListViewAdapter.ItemCallback {
    // Declare Variables
    ListView list;
    UsersListViewAdapter adapter;
    SearchView editsearch;
    ArrayList<GithubUser> arraylist = new ArrayList<>();
    ChipGroup selectedUsersChipGroup;
    ArrayList<GithubUser> selectedUsers = new ArrayList<>();
    private Timer timer;
    private String repoFullName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_collaborator);
        selectedUsersChipGroup = findViewById(R.id.selected_users);
        // Locate the ListView in listview_main.xml
        list = (ListView) findViewById(R.id.listview);
        Bundle bundle = getIntent().getExtras();
        repoFullName = bundle.getString("repoFullName"); //"putrastotest/tarombo-putrastotest-20220703144809"

        // Pass results to ListViewAdapter Class
        adapter = new UsersListViewAdapter(this, arraylist, this, false);

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

        timer = new Timer();
    }

    private void addCollaborators() {
        if (selectedUsers.size() == 0)
            return;

        final ProgressDialog pd = new ProgressDialog(this);
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setMessage(getString(R.string.invite_collaborators));
        pd.show();
        InviteCollaboratorsTask.execute(AddCollaboratorActivity.this, repoFullName, selectedUsers, () -> {
            pd.dismiss();
            finish();
        }, error -> {
            pd.dismiss();
            // show error message
            new AlertDialog.Builder(AddCollaboratorActivity.this)
                    .setTitle(R.string.find_errors)
                    .setMessage(error)
                    .setCancelable(false)
                    .setPositiveButton(R.string.OK, (eDialog, which) -> eDialog.dismiss())
                    .show();
        });
    }

    @Override
    public boolean onQueryTextSubmit(String query) {

        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        //        adapter.filter(text);
        timer.cancel();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                SearchUsersTask.execute(AddCollaboratorActivity.this, newText, users -> {
                    adapter.clearData();
                    adapter.setUserList(users);
                    adapter.notifyDataSetChanged();
                }, error -> {
                    // show error message
                    new AlertDialog.Builder(AddCollaboratorActivity.this)
                            .setTitle(R.string.find_errors)
                            .setMessage(error)
                            .setCancelable(false)
                            .setPositiveButton(R.string.OK, (eDialog, which) -> eDialog.dismiss())
                            .show();
                });
            }
        }, 300);

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

    @Override
    protected void onPause() {
        super.onPause();
        if (timer != null)
            timer.cancel();
    }
}