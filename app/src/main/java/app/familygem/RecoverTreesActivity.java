package app.familygem;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.familygem.action.GetMyReposTask;
import com.familygem.action.RedownloadRepoTask;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.familygem.R;

public class RecoverTreesActivity extends AppCompatActivity {
    ListView listView;
    List<Map<String, String>> repoList;
    SimpleAdapter adapter;
    ProgressBar pc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recover_trees);
        listView = findViewById(R.id.list);
        pc = findViewById(R.id.progress_circular);
        getData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recover_all, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.recover_all) {
            // User chose the "Settings" item, show the app settings UI...
            recoverAll();
            return true;
        } else {
            // If we got here, the user's action was not recognized.
            // Invoke the superclass to handle it.
            return super.onOptionsItemSelected(item);
        }
    }

    private void recoverAll() {
        if (repoList.size() > 0) {
            String currentRepoFullName = repoList.get(0).get("repoFullName");
            recoverTree(currentRepoFullName, this::recoverAll);
        }
    }

    private void removeFromList(String repoFullName) {
        for (Map<String, String> item : repoList) {
            String name = item.get("repoFullName");
            if (name.equals(repoFullName)) {
                repoList.remove(item);
                adapter.notifyDataSetChanged();
                return;
            }
        }
    }

    private void getData() {
        pc.setVisibility(View.VISIBLE);
        List<String> repoFullNames = U.getListOfCurrentRepoFullNames();
        GetMyReposTask.execute(RecoverTreesActivity.this, repoFullNames, treeInfos -> {
            repoList = new ArrayList<>();
            for (FamilyGemTreeInfoModel treeInfo : treeInfos) {
                Settings.Tree tree = new Settings.Tree(-1,
                        treeInfo.title,
                        treeInfo.filePath,
                        treeInfo.persons,
                        treeInfo.generations,
                        treeInfo.root,
                        null,
                        treeInfo.grade,
                        treeInfo.githubRepoFullName,
                        treeInfo.createdAt,
                        treeInfo.updatedAt);
                Map<String, String> dato = new HashMap<>(3);
                dato.put("repoFullName", treeInfo.githubRepoFullName);
                dato.put("dati", Trees.writeInfo(RecoverTreesActivity.this, tree));
                dato.put("titolo", treeInfo.title);
                repoList.add(dato);
            }
            adapter = new SimpleAdapter(RecoverTreesActivity.this, this.repoList,
                    R.layout.pezzo_albero,
                    new String[] { "titolo", "dati" },
                    new int[] { R.id.albero_titolo, R.id.albero_dati }) {
                @Override
                public View getView(final int posiz, View convertView, ViewGroup parent) {
                    View vistaAlbero = super.getView(posiz, convertView, parent);
                    final String repoFullName = repoList.get(posiz).get("repoFullName");
                    vistaAlbero.findViewById(R.id.albero_menu).setOnClickListener(vista -> {
                        PopupMenu popup = new PopupMenu(RecoverTreesActivity.this, vista);
                        Menu menu = popup.getMenu();
                        menu.add(0, 0, 0, R.string.recover_tree);
                        popup.show();
                        popup.setOnMenuItemClickListener(item -> {
                            int id = item.getItemId();
                            if (id == 0) {
                                recoverTree(repoFullName, () -> {
                                });
                            } else {
                                return false;
                            }
                            return true;
                        });
                    });
                    return vistaAlbero;
                }
            };
            listView.setAdapter(adapter);
            pc.setVisibility(View.GONE);
        }, error -> new AlertDialog.Builder(RecoverTreesActivity.this)
                .setTitle(R.string.find_errors)
                .setMessage(error)
                .setCancelable(false)
                .setPositiveButton(R.string.OK, (gDialog, gwhich) -> gDialog.dismiss())
                .show());
    }

    private void recoverTree(final String repoFullName, Runnable next) {
        pc.setVisibility(View.VISIBLE);
        // download repo
        int nextTreeId = Global.settings.max() + 1;
        RedownloadRepoTask.execute(RecoverTreesActivity.this, repoFullName, nextTreeId, infoModel -> {
            // add tree info and save settings.json
            Settings.Tree tree = new Settings.Tree(nextTreeId,
                    infoModel.title,
                    infoModel.filePath,
                    infoModel.persons,
                    infoModel.generations,
                    infoModel.root,
                    null,
                    infoModel.grade,
                    infoModel.githubRepoFullName,
                    infoModel.createdAt,
                    infoModel.updatedAt);
            File dirMedia = Helper.getDirMedia(this, nextTreeId);
            tree.dirs.add(dirMedia.getPath());
            tree.isForked = infoModel.isForked;
            Global.settings.addTree(tree);
            Global.settings.save();

            if (RecoverTreesActivity.this.isFinishing())
                return;

            removeFromList(repoFullName);
            pc.setVisibility(View.GONE);
            listView.post(next);
        }, error -> {
            if (RecoverTreesActivity.this.isFinishing())
                return;
            pc.setVisibility(View.GONE);
            new AlertDialog.Builder(RecoverTreesActivity.this)
                    .setTitle(R.string.find_errors)
                    .setMessage(error)
                    .setCancelable(false)
                    .setPositiveButton(R.string.OK, (dialog, which) -> {
                        dialog.dismiss();
                        finish();
                    }).show();
        });
    }
}