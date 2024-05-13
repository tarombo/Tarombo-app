package app.familygem;

import static app.familygem.Global.gc;

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

import com.familygem.action.SaveInfoFileTask;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;
import com.familygem.utility.PrivatePerson;

import org.folg.gedcom.model.Family;
import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Media;
import org.folg.gedcom.model.Note;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.util.List;

import app.familygem.dettaglio.Famiglia;

public class SelectPersonActivity extends AppCompatActivity {

    public static final String EXTRA_TREE_ID  = "TREE_ID";
    private Person person1;
    private Person person2;
    private String relationName;
    private int relationIndex;
    private int tree1Id;
    private Gedcom gc1;
    private Gedcom gc2;

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
        selectPerson1(tree1Id);
    }

    private void selectPerson1(int treeId){
        openGedcom(treeId, true);

        FragmentManager fm = getSupportFragmentManager();
        SelectPersonFragment fragment = new SelectPersonFragment(this::onPerson1Selected);

        fm.beginTransaction().replace( R.id.content_fragment, fragment ).commit();
    }

    private void onPerson1Selected(Person person){
        person1 = person;
        selectRelation();
    }

    private void selectRelation(){
        CharSequence[] parenti = {getText(R.string.parent), getText(R.string.sibling),
                getText(R.string.partner), getText(R.string.child)};

        new AlertDialog.Builder(this).setItems(parenti, (dialog, index) -> {
            this.relationIndex = index + 1;
            this.relationName = parenti[index].toString();
            importaGedcom();
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
                    gc2 = new ModelParser().parseGedcom( input );
                    if( gc2.getHeader() == null ) {
                        Toast.makeText( this, R.string.invalid_gedcom, Toast.LENGTH_LONG ).show();
                        return;
                    }

                    gc2.createIndexes();
                    Global.gc = gc2;

                    showPerson2List();
                } catch( Exception e ) {
                    Toast.makeText( this, e.getLocalizedMessage(), Toast.LENGTH_LONG ).show();
                }
            });

    void showPerson2List(){
        FragmentManager fm = getSupportFragmentManager();
        SelectPersonFragment fragment = new SelectPersonFragment(this::onPerson2Selected);
        fm.beginTransaction().replace( R.id.content_fragment, fragment ).commit();
    }

    void onPerson2Selected(Person person){
        person2 = person;
        String name1 = U.epiteto(this.person1);
        String name2 = U.epiteto(this.person2);

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
        // TODO merge tree
        openGedcom(tree1Id, true);
        gc1 = Global.gc;

        // refresh person1 reference
        String person1Id = person1.getId();
        person1 = gc1.getPerson(person1Id);

        // TODO import person, family etc from gc2 to gc1
        List<Family> family2 = gc2.getFamilies();
        for (Family family: family2) {
            gc1.addFamily(family);
        }

        List<Media> media2 = gc2.getMedia();
        for (Media media: media2) {
            gc1.addMedia(media);
        }

        List<Note> note2 =  gc2.getNotes();
        for(Note note: note2){
            gc1.addNote(note);
        }

        List<Person> people2 = gc2.getPeople();
        for(Person person: people2){
            gc1.addPerson(person);
        }

        // refresh person2 reference from gc1
        String person2Id = person2.getId();
        person2 = gc1.getPerson(person2Id);

        // TODO link. Verify link
        EditaIndividuo.addRelative(person1Id, person2Id, null, relationIndex, null);

        List<Person> debugPerson = gc1.getPeople();

        // TODO Save tree

        // Finalizzazione individuo nuovo. Finalization of new individual.
        Object[] modificati = { person1, person2 }; // il null serve per accogliere una eventuale Family. the null is used to accommodate a possible Family

        U.salvaJson(true, modificati);

        finish();
    }

    @Override
    protected  void onPause() {
        if(isFinishing()){
            // Clear gc on back
            Global.gc = null;
        }
        super.onPause();
    }
}