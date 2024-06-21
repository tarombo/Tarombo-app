package app.familygem.importnode;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.ParentFamilyRef;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.model.SpouseFamilyRef;
import org.folg.gedcom.model.SpouseRef;
import org.folg.gedcom.parser.ModelParser;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import app.familygem.Alberi;
import app.familygem.EditaIndividuo;
import app.familygem.Global;
import app.familygem.R;
import app.familygem.U;

public class SelectPersonActivity extends AppCompatActivity {
    public static final String EXTRA_TREE_ID  = "TREE_ID";
    private int tree1Id;
    private SelectPersonViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_select_person);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        tree1Id = intent.getIntExtra(EXTRA_TREE_ID, -1);

        viewModel = new ViewModelProvider(this).get(SelectPersonViewModel.class);
        viewModel.getState().observe(this, state ->{
            switch (state){
                case SELECT_PERSON_1:
                    selectPerson1(tree1Id);
                    break;
                case SELECT_RELATION:
                    selectRelation();
                    break;
                case SELECT_PERSON_2:
                    showPerson2List();
                    break;
                case DONE:
                    onPerson2Selected();
                    break;
                default:
                    break;
            }
        });
    }

    private void selectPerson1(int treeId){
        openGedcom(treeId, true);

        FragmentManager fm = getSupportFragmentManager();
        SelectPersonFragment fragment = new SelectPersonFragment();

        fm.beginTransaction().replace( R.id.content_fragment, fragment ).commit();
    }

    private void selectRelation(){
        new AlertDialog.Builder(this).setItems(getRelationTitles(), (dialog, index) -> {
            int relationIndex = index + 1;
            viewModel.setRelationIndex(relationIndex);
            String pesonId1 = viewModel.getPersonId1();
            U.controllaMultiMatrimoni2( pesonId1, relationIndex, this, (familyId, placement) -> {
                viewModel.setFamilyId(familyId);
                viewModel.setPlacement(placement);
                importaGedcom();
            });
        }).show();
    }

    private boolean openGedcom(int idAlbero, boolean salvaPreferenze) {
        return  Alberi.apriGedcom(idAlbero, salvaPreferenze);
    }

    void importaGedcom() {
        Intent intent = new Intent( Intent.ACTION_GET_CONTENT );
        intent.setType( "application/*" );
        importGedcomActivityResultLauncher.launch(intent);
    }

    ActivityResultLauncher<Intent> importGedcomActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                try {
                    Intent intent = result.getData();
                    Uri uri = intent.getData();
                    InputStream input = getContentResolver().openInputStream(uri);
                    Global.gc2 = new ModelParser().parseGedcom( input );
                    if( Global.gc2.getHeader() == null ) {
                        Toast.makeText( this, R.string.invalid_gedcom, Toast.LENGTH_LONG ).show();
                        return;
                    }

                    Global.gc2.createIndexes();
                    Global.gc = Global.gc2;

                    viewModel.setState(SelectPersonViewModel.State.SELECT_PERSON_2);
                } catch( Exception e ) {
                    Toast.makeText( this, e.getLocalizedMessage(), Toast.LENGTH_LONG ).show();
                }
            });

    void showPerson2List(){
        FragmentManager fm = getSupportFragmentManager();
        SelectPersonFragment fragment = new SelectPersonFragment();
        fm.beginTransaction().replace( R.id.content_fragment, fragment ).commit();
    }

    void onPerson2Selected(){
        String name1 = viewModel.getPersonName1();
        String name2 = viewModel.getPersonName2();
        int relationIndex = viewModel.getRelationIndex();
        String relationName = getRelationTitles()[relationIndex - 1].toString();

        String template = getString(R.string.confirm_import_gedcom_to_node)
                .replace("[person1]", name1)
                .replace("[person2]", name2)
                .replace("[relation]", relationName);

        new AlertDialog.Builder(this)
                .setMessage(template)
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    finish();
                })
                .setPositiveButton(R.string.OK, (dialog, which) -> {
                    linkToNode();
                })
                .show();
    }

    void linkToNode(){
        openGedcom(tree1Id, true);
        Gedcom gc1 = Global.gc;
        Gedcom gc2 = Global.gc2;

        // refresh person1 reference
        String person1Id = viewModel.getPersonId1();
        String person2Id = viewModel.getPersonId2();

        // Import person, family etc from gc2 to gc1
        List<Person> people2 = gc2.getPeople();
        for(Person person: people2){
            String newId = U.nuovoId(gc1, Person.class);

            if(Objects.equals(person.getId(), person2Id)){
                person2Id = newId;
            }

            U.changePersonId(person, newId, gc2);
            gc1.addPerson(person);
        }

        List<Family> family2 = gc2.getFamilies();
        for (Family family: family2) {
            String newId = U.nuovoId(gc1, Family.class);
            U.changeFamilyId(family, newId, gc2);
            gc1.addFamily(family);
        }

        String familyId = viewModel.getFamilyId();
        int relationIndex = viewModel.getRelationIndex();
        String placement = viewModel.getPlacement();
        Object[] modificati = EditaIndividuo.addRelative(person1Id, person2Id, familyId, relationIndex, placement);
        U.salvaJson(true, modificati);

        finish();
    }

    @Override
    protected  void onPause() {
        if(isFinishing()){
            Global.gc2 = null;
        }
        super.onPause();
    }

    private CharSequence[] getRelationTitles(){
        return new CharSequence[] {getText(R.string.parent), getText(R.string.sibling), getText(R.string.partner), getText(R.string.child)};
    }
}