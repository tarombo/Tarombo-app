package app.familygem;

import static app.familygem.Global.gc;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.familygem.action.SaveInfoFileTask;
import com.familygem.utility.FamilyGemTreeInfoModel;
import com.familygem.utility.Helper;
import com.google.android.material.textfield.TextInputEditText;

import org.folg.gedcom.model.EventFact;
import org.folg.gedcom.model.Name;
import org.folg.gedcom.model.Person;

import java.util.ArrayList;
import java.util.List;


public class EditConnectorActivity extends AppCompatActivity {

    Person p;
    String idIndi;
    int relazione = 1;
    TextInputEditText subRepoUrlTextInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_connector);
        Bundle bundle = getIntent().getExtras();
        idIndi = bundle.getString("idIndividuo");
        final int relazioneTemp = bundle.getInt("relazione", 0 );
        subRepoUrlTextInput = findViewById(R.id.sub_repo_url);
        if (relazioneTemp > 0) {
            p = new Person();
            relazione = relazioneTemp;
        } else{
            p = gc.getPerson(idIndi);
            // show sub tree url
            subRepoUrlTextInput.setText(U.getSubTreeUrl(p));
            // TODO: show relation to root sub tree
            findViewById(R.id.radioGroup).setVisibility(View.GONE);
            findViewById(R.id.root_family_relation_label).setVisibility(View.GONE);
        }

        // Barra
        ActionBar barra = getSupportActionBar();
        View barraAzione = getLayoutInflater().inflate( R.layout.barra_edita, new LinearLayout(getApplicationContext()), false);
        barraAzione.findViewById( R.id.edita_annulla ).setOnClickListener( v -> onBackPressed() );
        barraAzione.findViewById(R.id.edita_salva).setOnClickListener( v -> salva(relazioneTemp == 0) );
        barra.setCustomView( barraAzione );
        barra.setDisplayShowCustomEnabled( true );
    }

    void salva(boolean onlySaveUrl) {
        String subRepoUrl = subRepoUrlTextInput.getText().toString();
        if (subRepoUrl == null || subRepoUrl.isEmpty()) {
            new AlertDialog.Builder(EditConnectorActivity.this)
                    .setTitle(R.string.find_errors)
                    .setMessage(getString(R.string.sub_tree_url_is_required))
                    .setCancelable(false)
                    .setPositiveButton(R.string.OK, (eDialog, which) -> eDialog.dismiss())
                    .show();
            return;
        }

        U.gedcomSicuro(gc); // È capitato un crash perché qui gc era null

        if (onlySaveUrl) {
            for( EventFact fatto : p.getEventsFacts() ) {
                if (fatto.getTag() != null && fatto.getTag().equals(U.CONNECTOR_TAG)) {
                    fatto.setValue(subRepoUrl);
                    break;
                }
            }
            U.salvaJson(false, null);
        } else {

            // Nome
            Name name = new Name();
            name.setValue("connector");
            List<Name> nomi = new ArrayList<>();
            nomi.add(name);
            p.setNames(nomi);

            // save URL of the sub repo (sub tree)
            EventFact connector = new EventFact();
            connector.setTag(U.CONNECTOR_TAG);
            connector.setValue(subRepoUrl);
            p.addEventFact(connector);

            if (((RadioButton) findViewById(R.id.parent)).isChecked())
                relazione = 1;
            else if (((RadioButton) findViewById(R.id.sibling)).isChecked())
                relazione = 2;
            else if (((RadioButton) findViewById(R.id.partner)).isChecked())
                relazione = 3;
            else if (((RadioButton) findViewById(R.id.child)).isChecked())
                relazione = 4;

            // Finalizzazione individuo nuovo
            Object[] modificati = {p, null}; // il null serve per accogliere una eventuale Family
            String nuovoId = U.nuovoId(gc, Person.class);
            p.setId(nuovoId);
            gc.addPerson(p);
            if (Global.settings.getCurrentTree().root == null)
                Global.settings.getCurrentTree().root = nuovoId;
            Global.settings.save();
            Settings.Tree tree = Global.settings.getCurrentTree();
            if (tree.githubRepoFullName != null)
                Helper.requireEmail(Global.context, Global.context.getString(R.string.set_email_for_commit),
                        Global.context.getString(R.string.OK), Global.context.getString(R.string.cancel), email -> {
                            FamilyGemTreeInfoModel infoModel = new FamilyGemTreeInfoModel(
                                    tree.title,
                                    tree.persons,
                                    tree.generations,
                                    tree.media,
                                    tree.root,
                                    tree.grade,
                                    tree.createdAt,
                                    tree.updatedAt
                            );
                            SaveInfoFileTask.execute(Global.context, tree.githubRepoFullName, email, tree.id, infoModel, () -> {
                            }, () -> {
                            }, error -> {
                                Toast.makeText(Global.context, error, Toast.LENGTH_LONG).show();
                            });
                        }
                );

            String idFamiglia = null;
            modificati = EditaIndividuo.addRelative(idIndi, nuovoId, idFamiglia, relazione, getIntent().getStringExtra("collocazione"));

            U.salvaJson(true, modificati);
        }
        onBackPressed();
    }
}