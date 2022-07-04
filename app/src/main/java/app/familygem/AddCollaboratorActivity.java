package app.familygem;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;

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
        // Generate sample data
        String[] animalNameList = new String[]{"Lion", "Tiger", "Dog",
                "Cat", "Tortoise", "Rat", "Elephant", "Fox",
                "Cow","Donkey","Monkey"};

        // Locate the ListView in listview_main.xml
        list = (ListView) findViewById(R.id.listview);

        for (int i = 0; i < animalNameList.length; i++) {
            GithubUser GithubUser = new GithubUser(animalNameList[i]);
            // Binds all strings into an array
            arraylist.add(GithubUser);
        }

        // Pass results to ListViewAdapter Class
        adapter = new UsersListViewAdapter(this, arraylist, this);

        // Binds the Adapter to the ListView
        list.setAdapter(adapter);

        // Locate the EditText in listview_main.xml
        editsearch = (SearchView) findViewById(R.id.search);
        editsearch.setOnQueryTextListener(this);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {

        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        String text = newText;
        adapter.filter(text);
        return false;
    }

    @Override
    public void onItemClick(final GithubUser user) {
        addSelectedUser(user);
        Log.d("collaborator", "user:" + user.getName() + " totals:" + selectedUsers.size());
    }

    private void removeSelectedUser(final GithubUser user) {
        selectedUsers.remove(user);
    }

    private void addSelectedUser(final GithubUser user) {
        for (GithubUser selectedUser : selectedUsers) {
            if (selectedUser.equals(user)) {
                return;
            }
        }
        View view = getLayoutInflater().inflate(R.layout.item_selected_user,
                selectedUsersChipGroup, false);
        Chip selectedUserChip = view.findViewById(R.id.chips_item_filter);
        selectedUserChip.setText(user.getName());
        selectedUserChip.setTag(user.getName());
        selectedUsersChipGroup.addView(selectedUserChip);
        selectedUsers.add(user);
        selectedUserChip.setOnCloseIconClickListener((v) -> {
            selectedUsersChipGroup.removeView(v);
            removeSelectedUser(user);
        });
    }
}