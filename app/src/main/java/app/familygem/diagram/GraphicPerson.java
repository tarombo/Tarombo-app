package app.familygem.diagram;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.folg.gedcom.model.Person;

import app.familygem.Diagram;
import app.familygem.F;
import app.familygem.constants.Gender;
import app.familygem.Global;
import app.familygem.Individuo;
import app.familygem.Memoria;
import app.familygem.R;
import app.familygem.Settings;
import app.familygem.U;
import graph.gedcom.PersonNode;

public class GraphicPerson extends GraphicMetric {
    ImageView background;
    boolean printPDF;
    PersonNode personNode;
    Diagram diagramFragment; // Reference to parent fragment for access control methods and callbacks

    public GraphicPerson(Context context, PersonNode personNode, Diagram diagramFragment) {
        super(context, personNode);
        this.personNode = personNode;
        this.diagramFragment = diagramFragment;
        Person person = personNode.person;

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.diagram_card, this, true);
        View border = view.findViewById(R.id.card_border);

        if (Gender.isMale(person))
            border.setBackgroundResource(R.drawable.casella_bordo_maschio);
        else if (Gender.isFemale(person))
            border.setBackgroundResource(R.drawable.casella_bordo_femmina);

        background = view.findViewById(R.id.card_background);
        if (personNode.isFulcrumNode()) {
            background.setBackgroundResource(R.drawable.casella_sfondo_evidente);
        } else if (personNode.acquired) {
            background.setBackgroundResource(R.drawable.casella_sfondo_sposo);
        }

        F.showPrimaryPhoto(Global.gc, person, view.findViewById(R.id.card_photo));

        TextView vistaNome = view.findViewById(R.id.card_name);
        String nome = U.getPrincipalName(person);
        if (nome.isEmpty() && view.findViewById(R.id.card_photo).getVisibility() == View.VISIBLE)
            vistaNome.setVisibility(View.GONE);
        else
            vistaNome.setText(nome);

        TextView vistaTitolo = view.findViewById(R.id.card_title);
        String titolo = U.getTitle(person);
        if (titolo.isEmpty())
            vistaTitolo.setVisibility(View.GONE);
        else
            vistaTitolo.setText(titolo);

        TextView vistaDati = view.findViewById(R.id.card_data);
        String dati = U.twoDates(person, true);
        if (dati.isEmpty())
            vistaDati.setVisibility(View.GONE);
        else
            vistaDati.setText(dati);

        if (!U.isDead(person))
            view.findViewById(R.id.card_mourn).setVisibility(View.GONE);

        // Context menu needs the activity/fragment registering it.
        // Ideally we pass that responsibility back or register here if View allows.
        // View.registerForContextMenu is available? Yes but usually used in
        // Activity/Fragment.
        // But Views can show context menus.
        diagramFragment.registerForContextMenu(this);

        setOnClickListener(v -> {
            if (person.getId().equals(Global.indi)) {
                Settings.Tree tree = Global.settings.getCurrentTree();
                Log.d("DiagramClick",
                        "Tapped on current focus: " + U.getPrincipalName(person) + " (ID: " + person.getId() + ")");

                diagramFragment.enforcePrivateAccess(tree, person, () -> {
                    Log.d("DiagramClick", "Opening Individuo screen for: " + U.getPrincipalName(person));
                    Memoria.setFirst(person);
                    context.startActivity(new Intent(context, Individuo.class));
                });
            } else {
                diagramFragment.clickCard(person);
            }
        });
    }

    public void setPrintPDF(boolean printPDF) {
        this.printPDF = printPDF;
        invalidate();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        // Change background color for PDF export
        if (printPDF && personNode.acquired && background != null) {
            background.setBackgroundResource(R.drawable.casella_sfondo_sposo_stampa);
        }
    }
}
