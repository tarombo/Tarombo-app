package app.familygem;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import org.folg.gedcom.model.Person;

import java.util.Iterator;
import java.util.Map;

import kotlin.Pair;

public class ReviewChangesActivity extends AppCompatActivity {
    private Map<String, CompareDiffTree.DiffPeople> diffPeopleMap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_changes);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        Intent intent = getIntent();
        diffPeopleMap = (Map<String, CompareDiffTree.DiffPeople>) intent.getSerializableExtra("diffPeopleMap");
//        diffPeopleMap.forEach((key, value) -> System.out.println(key + " -> " + value.toString()));

        TextView textualDiffText = findViewById(R.id.text_diff_info);
        textualDiffText.setMovementMethod(new ScrollingMovementMethod());
        showTextualDiff(textualDiffText);
    }

    private void showTextualDiff(TextView textualDiffText) {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<Map.Entry<String, CompareDiffTree.DiffPeople>> iteratorLeft = diffPeopleMap.entrySet().iterator();
        while (iteratorLeft.hasNext()) {
            Map.Entry<String, CompareDiffTree.DiffPeople> entry = iteratorLeft.next();
            CompareDiffTree.DiffPeople diffPeople = entry.getValue();
            if (diffPeople.changeType == CompareDiffTree.ChangeType.ADDED) {
                stringBuilder.append("- ");
                stringBuilder.append(getString(R.string.text_add_person, "\"" + getName(diffPeople) + "\""));
                stringBuilder.append("\n\n");
            } else if (diffPeople.changeType == CompareDiffTree.ChangeType.REMOVED) {
                stringBuilder.append("- ");
                stringBuilder.append(getString(R.string.text_remove_person, "\"" + getName(diffPeople) + "\""));
                stringBuilder.append("\n\n");
            } else if (diffPeople.changeType == CompareDiffTree.ChangeType.MODIFIED) {
                for (Map.Entry<CompareDiffTree.ChangeItem, Pair<String, String>> mapElement : diffPeople.properties.entrySet()) {
                    Pair<String, String> changes = mapElement.getValue();
                    stringBuilder.append("- ");
                    stringBuilder.append(getString(R.string.text_modify_person, "\"" + changes.component1() + "\"", "\"" + changes.component2() + "\""));
                    stringBuilder.append("\n");
                }
                stringBuilder.append("\n");
            }
        }
        textualDiffText.setText(stringBuilder.toString());
    }



    private String getName(CompareDiffTree.DiffPeople diffPeople) {
        Pair<String, String> names = diffPeople.properties.get(CompareDiffTree.ChangeItem.NAME);
        return names.component2().replace("/", "");
    }
}
