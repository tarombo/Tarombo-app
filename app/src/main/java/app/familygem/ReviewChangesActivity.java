package app.familygem;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatTextView;

import org.folg.gedcom.model.Gedcom;

import java.util.Iterator;
import java.util.Map;

import kotlin.Pair;

public class ReviewChangesActivity extends AppCompatActivity {
    public enum CallbackAction {
        CLOSE
    }
    private Map<String, CompareDiffTree.DiffPeople> diffPeopleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_changes);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        Intent intent = getIntent();
        diffPeopleMap = (Map<String, CompareDiffTree.DiffPeople>) intent.getSerializableExtra("diffPeopleMap");
        diffPeopleMap.forEach((key, value) -> System.out.println(key + " -> " + value.toString()));

        TextView textualDiffText = findViewById(R.id.text_diff_info);
        textualDiffText.setMovementMethod(new ScrollingMovementMethod());
        showTextualDiff(textualDiffText);

        findViewById(R.id.btn_approve).setOnClickListener(v -> {
            // TODO approve process
            finish();
        });

        findViewById(R.id.btn_reject).setOnClickListener(v -> {
            // TODO reject process
        });

        findViewById(R.id.btn_close).setOnClickListener(v -> {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("action", CallbackAction.CLOSE);
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        });
    }

    private void showTextualDiff(TextView textualDiffText) {
        StringBuilder stringBuilder = new StringBuilder();
        Iterator<Map.Entry<String, CompareDiffTree.DiffPeople>> iteratorLeft = diffPeopleMap.entrySet().iterator();
        while (iteratorLeft.hasNext()) {
            Map.Entry<String, CompareDiffTree.DiffPeople> entry = iteratorLeft.next();
            CompareDiffTree.DiffPeople diffPeople = entry.getValue();
            if (diffPeople.changeType == CompareDiffTree.ChangeType.ADDED) {
                stringBuilder.append("- ");
                stringBuilder.append(getString(R.string.text_add_person, "\"" + diffPeople.personName + "\""));
                stringBuilder.append("\n\n");
            } else if (diffPeople.changeType == CompareDiffTree.ChangeType.REMOVED) {
                stringBuilder.append("- ");
                stringBuilder.append(getString(R.string.text_remove_person, "\"" + diffPeople.personName + "\""));
                stringBuilder.append("\n\n");
            } else if (diffPeople.changeType == CompareDiffTree.ChangeType.MODIFIED) {
                for (CompareDiffTree.ChangeItem changeItem : CompareDiffTree.ChangeItem.values()) {
                    Pair<String, String> changes = diffPeople.properties.get(changeItem);
                    if (changes == null)
                        continue;
                    if (changeItem == CompareDiffTree.ChangeItem.NAME) {
                        stringBuilder.append("- ");
                        stringBuilder.append(getModifiedName(changes));
                        stringBuilder.append("\n");
                    } else if (changeItem == CompareDiffTree.ChangeItem.SEX) {
                        stringBuilder.append("- ");
                        stringBuilder.append(getModifiedSex(changes, "\"" + diffPeople.personName + "\""));
                        stringBuilder.append("\n");
                    } else if (changeItem == CompareDiffTree.ChangeItem.DEATH) {
                        stringBuilder.append("- ");
                        stringBuilder.append(getModifiedDeath(changes, "\"" + diffPeople.personName + "\""));
                        stringBuilder.append("\n");
                    } else if (changeItem == CompareDiffTree.ChangeItem.BIRTH_DATE) {
                        stringBuilder.append("- ");
                        stringBuilder.append(getModifiedBirthDate(changes, "\"" + diffPeople.personName + "\""));
                        stringBuilder.append("\n");
                    } else if (changeItem == CompareDiffTree.ChangeItem.BIRTH_PLACE) {
                        stringBuilder.append("- ");
                        stringBuilder.append(getModifiedBirthPlace(changes, "\"" + diffPeople.personName + "\""));
                        stringBuilder.append("\n");
                    }
                }
                stringBuilder.append("\n");
            }
        }
        textualDiffText.setText(stringBuilder.toString());
    }

    private String getModifiedBirthPlace(Pair<String, String> changes, String personName) {
        String birthPlace1 = changes.component1();
        if (birthPlace1 == null)
            birthPlace1 = "";
        String birthPlace2 = changes.component2();
        if (birthPlace2 == null)
            birthPlace2 = "";
        return getString(R.string.text_modify_person_birthplace, personName, "\"" + birthPlace1 + "\"", "\"" + birthPlace2 + "\"");
    }

    private String getModifiedBirthDate(Pair<String, String> changes, String personName) {
        String birthDate1 = changes.component1();
        if (birthDate1 == null)
            birthDate1 = "";
        String birthDate2 = changes.component2();
        if (birthDate2 == null)
            birthDate2 = "";
        return getString(R.string.text_modify_person_birthdate, personName, "\"" + birthDate1 + "\"", "\"" + birthDate2 + "\"");
    }
    private String getModifiedDeath(Pair<String, String> changes, String personName) {
        String death1 = changes.component1();
        if (death1 == null)
            death1 = "";
        String death2 = changes.component2();
        if (death2 == null)
            death2 = "";
        return getString(R.string.text_modify_person_death, personName, "\"" + death1 + "\"", "\"" + death2 + "\"");
    }

    private String getModifiedSex(Pair<String, String> changes, String personName) {
        String sex1 = "";
        if ("M".equals(changes.component1()))
            sex1 = getString(R.string.male);
        else if ("F".equals(changes.component1()))
            sex1 = getString(R.string.female);
        else if ("U".equals(changes.component1()))
            sex1 = getString(R.string.unknown);
        String sex2 = "";
        if ("M".equals(changes.component2()))
            sex2 = getString(R.string.male);
        else if ("F".equals(changes.component2()))
            sex2 = getString(R.string.female);
        else if ("U".equals(changes.component2()))
            sex2 = getString(R.string.unknown);
        return getString(R.string.text_modify_person_sex, personName, "\"" + sex1 + "\"", "\"" + sex2 + "\"");
    }
    private String getModifiedName(Pair<String, String> names) {
        String changes1 = names.component1();
        if (changes1 == null)
            changes1 = "";
        else
            changes1 = changes1.replace("/", "");
        String changes2 = names.component2();
        if (changes2 == null)
            changes2 = "";
        else
            changes2 = changes2.replace("/", "");

        return getString(R.string.text_modify_person_name
                , "\"" + changes1 + "\""
                , "\"" + changes2 + "\"");
    }
}
