package app.familygem;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.familygem.action.GetOpenPRTask;
import com.familygem.restapi.models.Pull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChangeProposalActivity extends AppCompatActivity {
    int treeId;
    String repoFullName;
    ListView listView;
    List<Map<String, String>> pullList;
    SimpleAdapter adapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_proposal);
        treeId = getIntent().getIntExtra("treeId", 0);
        repoFullName = getIntent().getStringExtra("repoFullName");
        listView = findViewById(R.id.list);
        GetOpenPRTask.execute(ChangeProposalActivity.this, repoFullName, treeId, pulls -> {
            pullList = new ArrayList<>();
            for (Pull pull : pulls ) {
                Map<String, String> dato = new HashMap<>(3);
                dato.put("pullNo", String.valueOf(pull.number));
                if (pull.user.name != null)
                    dato.put("proposer", pull.user.login + "(" + pull.user.name + ")");
                else
                    dato.put("proposer", pull.user.login);
                // TODO convert to UTC and then convert to local date & time
                dato.put("datetime", pull.createdAt);
                pullList.add(dato);
            }
            adapter = new SimpleAdapter(ChangeProposalActivity.this, this.pullList,
                    R.layout.proposed_change_view,
                    new String[] {"proposer", "datetime"},
                    new int[] {R.id.proposer, R.id.date_time}) {
                @Override
                public View getView(final int posiz, View convertView, ViewGroup parent) {
                    View vistaAlbero = super.getView( posiz, convertView, parent );
//                    ((TextView)vistaAlbero.findViewById(R.id.proposer)).setText();
                    return vistaAlbero;
                }
            };
            listView.setAdapter(adapter);
            // adapter.notifyDataSetChanged();
            findViewById(R.id.progress_circular).setVisibility(View.GONE);
        },
                error -> {
            findViewById(R.id.progress_circular).setVisibility(View.GONE);
                    new AlertDialog.Builder(ChangeProposalActivity.this)
                            .setTitle(R.string.find_errors)
                            .setMessage(error)
                            .setCancelable(false)
                            .setPositiveButton(R.string.OK, (gDialog, gwhich) -> gDialog.dismiss())
                            .show();
        });
    }


}