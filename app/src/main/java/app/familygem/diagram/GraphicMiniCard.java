package app.familygem.diagram;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import app.familygem.Diagram;
import app.familygem.constants.Gender;
import app.familygem.R;
import graph.gedcom.PersonNode;

public class GraphicMiniCard extends GraphicMetric {
    RelativeLayout layout;
    boolean printPDF;
    Diagram diagramFragment;

    public GraphicMiniCard(Context context, PersonNode personNode, Diagram diagramFragment) {
        super(context, personNode);
        this.diagramFragment = diagramFragment;

        LayoutInflater inflater = LayoutInflater.from(context);
        View miniCard = inflater.inflate(R.layout.diagram_minicard, this, true);
        TextView miniCardText = miniCard.findViewById(R.id.minicard_text);
        miniCardText.setText(personNode.amount > 100 ? "100+" : String.valueOf(personNode.amount));
        Gender sex = Gender.getGender(personNode.person);
        if (sex == Gender.MALE)
            miniCardText.setBackgroundResource(R.drawable.casella_bordo_maschio);
        else if (sex == Gender.FEMALE)
            miniCardText.setBackgroundResource(R.drawable.casella_bordo_femmina);
        if (personNode.acquired) {
            layout = miniCard.findViewById(R.id.minicard);
            layout.setBackgroundResource(R.drawable.casella_sfondo_sposo);
        }
        miniCard.setOnClickListener(view -> diagramFragment.clickCard(personNode.person));
    }

    public void setPrintPDF(boolean printPDF) {
        this.printPDF = printPDF;
        invalidate();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (printPDF && layout != null) {
            layout.setBackgroundResource(R.drawable.casella_sfondo_sposo_stampa);
        }
    }
}
