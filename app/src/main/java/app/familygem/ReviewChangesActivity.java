package app.familygem;



import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import com.google.android.material.button.MaterialButtonToggleGroup;


public class ReviewChangesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_changes);
        if (getSupportActionBar() != null)
            getSupportActionBar().hide();

        // fragment for before
        Bundle bundleBefore = new Bundle();
        bundleBefore.putInt("treeId", 2);
        DiagramCompareFragment fragmentBefore = new DiagramCompareFragment();
        fragmentBefore.setArguments(bundleBefore);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_before, fragmentBefore)
                .addToBackStack("before")
                .commit();
        DiagramCompareFragment fragmentAfter = new DiagramCompareFragment();
        Bundle bundleAfter = new Bundle();
        bundleAfter.putInt("treeId", 6);
        fragmentAfter.setArguments(bundleAfter);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_after, fragmentAfter)
                .addToBackStack("before")
                .commit();

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

        // initial state: show before and hide after
//        findViewById(R.id.fragment_before).setVisibility(View.VISIBLE);
//        findViewById(R.id.fragment_after).setVisibility(View.INVISIBLE);
    }

}