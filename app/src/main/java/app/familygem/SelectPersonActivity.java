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

import com.familygem.utility.Helper;
import com.familygem.utility.PrivatePerson;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.folg.gedcom.model.Gedcom;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;
import org.folg.gedcom.parser.JsonParser;
import org.folg.gedcom.parser.ModelParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

import app.familygem.Anagrafe;
import app.familygem.Global;
import app.familygem.R;
import app.familygem.Settings;
import app.familygem.U;

public class SelectPersonActivity extends AppCompatActivity {

    public static final String EXTRA_TREE_ID  = "TREE_ID";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        int treeId = intent.getIntExtra(EXTRA_TREE_ID, -1);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_select_person);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        apriGedcom(treeId, true);

        FragmentManager fm = getSupportFragmentManager();
        SelectPersonFragment fragment = new SelectPersonFragment(new SelectPersonFragment.Callback() {
            @Override
            public void onClick(Person person) {
                selectRelation(person);
            }
        });

        fm.beginTransaction().replace( R.id.content_fragment, fragment ).commit();
    }

    private void selectRelation(Person person){
        CharSequence[] parenti = {getText(R.string.parent), getText(R.string.sibling),
                getText(R.string.partner), getText(R.string.child)};

        String name = U.epiteto(person);

        new AlertDialog.Builder(this).setItems(parenti, (dialog, index) -> {
            importaGedcom();
        }).show();
    }

    static boolean apriGedcom(int idAlbero, boolean salvaPreferenze) {
        Global.gc = leggiJson(idAlbero);
        if( Global.gc == null )
            return false;
        if( salvaPreferenze ) {
            Global.settings.openTree = idAlbero;
            Global.settings.save();
        }
        Global.indi = Global.settings.getCurrentTree().root;
        Global.familyNum = 0; // eventualmente lo resetta se era > 0
        Global.daSalvare = false; // eventualmente lo resetta se era true
        return true;
    }

    static Gedcom leggiJson(int treeId) {
        Gedcom gedcom;
        File file = new File(Global.context.getFilesDir(), treeId + ".json");
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
//		json = updateLanguage(json);
        gedcom = new JsonParser().fromJson(json);
        if( gedcom == null ) {
            Toast.makeText(Global.context, R.string.no_useful_data, Toast.LENGTH_LONG).show();
            return null;
        }

        // handle privacy
        Settings.Tree tree = Global.settings.getTree(treeId);
        if (tree != null && !tree.isForked && tree.githubRepoFullName != null) {
            gedcom.createIndexes();
            List<PrivatePerson> privatePersons = U.getPrivatePersons(treeId);
            for (PrivatePerson priv : privatePersons) {
                Person p = gedcom.getPerson(priv.personId);
                if (p != null) {
                    p.setMedia(priv.mediaList);
                    p.setEventsFacts(priv.eventFacts);
                }
            }
        }
        return gedcom;
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
                    Gedcom gc = new ModelParser().parseGedcom( input );
                    if( gc.getHeader() == null ) {
                        Toast.makeText( this, R.string.invalid_gedcom, Toast.LENGTH_LONG ).show();
                        return;
                    }

                    Helper.makeGuidGedcom(gc);

                    // TODO select person 2
                } catch( Exception e ) {
                    Toast.makeText( this, e.getLocalizedMessage(), Toast.LENGTH_LONG ).show();
                }
            });
}