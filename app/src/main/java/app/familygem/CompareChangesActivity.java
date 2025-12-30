package app.familygem;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.google.android.material.button.MaterialButtonToggleGroup;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.parser.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import app.familygem.R;

public class CompareChangesActivity extends AppCompatActivity {
    public enum CompareType {
        SubmitChanges,
        GetChanges,
        MergePullRequest
    }
    private DiagramCompareFragment fragmentBefore;
    private DiagramCompareFragment fragmentAfter;

    private Gedcom gedcomBefore = null;
    private Gedcom gedcomAfter = null;

    private Map<String, CompareDiffTree.DiffPeople> diffPeopleMap = new HashMap<>();
    private CompareType compareType;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare_changes);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        Intent intent = getIntent();
        compareType = (CompareType) intent.getSerializableExtra("compareType");
        String jsonFileNameBefore = intent.getStringExtra("jsonFileNameBefore");
        if (jsonFileNameBefore != null) {
            File jsonFileBefore = new File(Global.context.getFilesDir(), jsonFileNameBefore);
            if (jsonFileBefore.exists())
                gedcomBefore = readJson(jsonFileBefore);
        }
        String jsonFileNameAfter = intent.getStringExtra("jsonFileNameAfter");
        if (jsonFileNameAfter != null) {
            File jsonFileAfter = new File(Global.context.getFilesDir(), jsonFileNameAfter);
            if (jsonFileAfter.exists())
                gedcomAfter = readJson(jsonFileAfter);
        }

        if (gedcomBefore != null && gedcomAfter != null) {
            List<CompareDiffTree.DiffPeople> diffPeopleList = CompareDiffTree.compare(gedcomBefore, gedcomAfter);
            diffPeopleMap = new HashMap<>();
            for (CompareDiffTree.DiffPeople diffPeople : diffPeopleList) {
                diffPeopleMap.put(diffPeople.personId, diffPeople);
            }
        }


        // fragment for before
        fragmentBefore = new DiagramCompareFragment(gedcomBefore, diffPeopleMap, compareType);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_before, fragmentBefore).commit();

        fragmentAfter = new DiagramCompareFragment(gedcomAfter, diffPeopleMap, compareType);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_after, fragmentAfter).commit();

        MaterialButtonToggleGroup buttonToggleGroup = findViewById(R.id.toggle_before_after);
        buttonToggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_before) {
                    findViewById(R.id.fragment_before).setVisibility(View.VISIBLE);
                    findViewById(R.id.fragment_after).setVisibility(View.INVISIBLE);
                } else {
                    findViewById(R.id.fragment_before).setVisibility(View.INVISIBLE);
                    findViewById(R.id.fragment_after).setVisibility(View.VISIBLE);
                }
            }
        });
    }

    // Legge il Json e restituisce un Gedcom
    static Gedcom readJson(File file) {
        Gedcom gedcom;
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while( (line = br.readLine()) != null ) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch( Exception | Error e ) {
            String message = e instanceof OutOfMemoryError ? Global.context.getString(R.string.not_memory_tree) : e.getLocalizedMessage();
            Toast.makeText(Global.context, message, Toast.LENGTH_LONG).show();
            return null;
        }
        String json = text.toString();
        gedcom = new JsonParser().fromJson(json);
        if( gedcom == null ) {
            Toast.makeText(Global.context, R.string.no_useful_data, Toast.LENGTH_LONG).show();
            return null;
        }
        return gedcom;
    }

}