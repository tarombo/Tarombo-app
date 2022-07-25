package app.familygem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.familygem.action.GetColloratorsTask;
import com.familygem.action.RemoveCollaboratorTask;
import com.familygem.utility.GithubUser;

import java.util.ArrayList;

public class ContributorsActivity extends AppCompatActivity implements UsersListViewAdapter.ItemCallback {
    // Declare Variables
    ListView list;
    UsersListViewAdapter adapter;
    ArrayList<GithubUser> arraylist = new ArrayList<>();
    private String repoFullName;
    ProgressBar pc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contributors);

        // Locate the ListView in listview_main.xml
        list = findViewById(R.id.listview);
        Bundle bundle = getIntent().getExtras();
        repoFullName = bundle.getString("repoFullName");
//        repoFullName = "putrastotest/tarombo-putrastotest-20220723213446";

        // Pass results to ListViewAdapter Class
        adapter = new UsersListViewAdapter(this, arraylist, this, true);

        // Binds the Adapter to the ListView
        list.setAdapter(adapter);

        pc = findViewById(R.id.progress_circular);

        // Barra
        ActionBar barra = getSupportActionBar();
        View barraAzione = getLayoutInflater().inflate( R.layout.barra_edita, new LinearLayout(getApplicationContext()), false);
        barraAzione.findViewById( R.id.edita_annulla ).setOnClickListener( v -> finish());
        Button btnAdd = barraAzione.findViewById(R.id.edita_salva);
        btnAdd.setOnClickListener( v -> {
            // show  add contributor screen
            Intent intent = new Intent(ContributorsActivity.this, AddCollaboratorActivity.class);
            intent.putExtra("repoFullName", repoFullName);
            startActivity(intent);
        } );
        btnAdd.setText(R.string.add);
        barra.setCustomView( barraAzione );
        barra.setDisplayShowCustomEnabled( true );

    }

    @Override
    protected void onResume() {
        super.onResume();
        getData();
    }

    @Override
    public void onItemClick(final GithubUser user) {
        Log.d("collaborator", "user:" + user.getName());
        // we are using onItemClick to trigger delete in context menu of each row in listview
        removeSelectedUser(user);

    }

    private void removeSelectedUser(final GithubUser user) {
        // remove from github server
        pc.setVisibility(View.VISIBLE);
        RemoveCollaboratorTask.execute(ContributorsActivity.this, repoFullName, user, () -> {
            pc.setVisibility(View.GONE);
            adapter.removeUser(user);
            adapter.notifyDataSetChanged();
        },error -> {
            pc.setVisibility(View.GONE);
            // show error message
            new AlertDialog.Builder(ContributorsActivity.this)
                    .setTitle(R.string.find_errors)
                    .setMessage(error)
                    .setCancelable(false)
                    .setPositiveButton(R.string.OK, (eDialog, which) -> eDialog.dismiss())
                    .show();
        });

    }

    private void getData() {
        pc.setVisibility(View.VISIBLE);
        GetColloratorsTask.execute(ContributorsActivity.this, repoFullName, users -> {
            adapter.clearData();
            adapter.setUserList(users);
            adapter.notifyDataSetChanged();
            pc.setVisibility(View.GONE);
        }, error -> {
            pc.setVisibility(View.GONE);
            // show error message
            new AlertDialog.Builder(ContributorsActivity.this)
                    .setTitle(R.string.find_errors)
                    .setMessage(error)
                    .setCancelable(false)
                    .setPositiveButton(R.string.OK, (eDialog, which) -> eDialog.dismiss())
                    .show();
        });
    }




}