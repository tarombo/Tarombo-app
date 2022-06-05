package app.familygem;

import androidx.appcompat.app.AppCompatActivity;
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


public class CompareChangesActivity extends AppCompatActivity {
    private DiagramCompareFragment fragmentBefore;
    private DiagramCompareFragment fragmentAfter;

    private Gedcom gedcomBefore;
    private Gedcom gedcomAfter;

    private Map<String, CompareDiffTree.DiffPeople> diffPeopleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare_changes);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        File jsonFileBefore = new File(Global.context.getFilesDir(),"treeA_1.json");
        gedcomBefore = leggiJson(jsonFileBefore);
        File jsonFileAfter = new File(Global.context.getFilesDir(),"treeD_2.json");
        gedcomAfter = leggiJson(jsonFileAfter);
        List<CompareDiffTree.DiffPeople> diffPeopleList = CompareDiffTree.compare(gedcomBefore, gedcomAfter);
        diffPeopleMap = new HashMap<>();
        for (CompareDiffTree.DiffPeople diffPeople : diffPeopleList) {
            diffPeopleMap.put(diffPeople.personId, diffPeople);
        }

        // fragment for before
        fragmentBefore = new DiagramCompareFragment(gedcomBefore, diffPeopleMap);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_before, fragmentBefore).commit();

        fragmentAfter = new DiagramCompareFragment(gedcomAfter, diffPeopleMap);
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
    static Gedcom leggiJson(File file) {
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