package app.familygem.diagram;

import android.content.Context;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import app.familygem.Datatore;
import app.familygem.dettaglio.Famiglia;
import app.familygem.Memoria;
import app.familygem.R;
import graph.gedcom.Bond;
import graph.gedcom.FamilyNode;
import static graph.gedcom.Util.HEARTH_DIAMETER;
import static graph.gedcom.Util.MINI_HEARTH_DIAMETER;
import static graph.gedcom.Util.MARRIAGE_HEIGHT;

public class GraphicBond extends GraphicMetric {
    View hearth;
    boolean printPDF;

    public GraphicBond(Context context, Bond bond) {
        super(context, bond);
        RelativeLayout bondLayout = new RelativeLayout(context);
        // bondLayout.setBackgroundColor(0x44ff00ff);
        addView(bondLayout, new LayoutParams(toPx(bond.width), toPx(bond.height)));
        FamilyNode familyNode = bond.familyNode;
        if (bond.marriageDate == null) {
            hearth = new View(context);
            hearth.setBackgroundResource(R.drawable.diagram_hearth);
            int diameter = toPx(familyNode.mini ? MINI_HEARTH_DIAMETER : HEARTH_DIAMETER);
            LayoutParams hearthParams = new LayoutParams(diameter, diameter);
            hearthParams.topMargin = toPx(familyNode.centerRelY()) - diameter / 2;
            hearthParams.addRule(CENTER_HORIZONTAL);
            bondLayout.addView(hearth, hearthParams);
        } else {
            TextView year = new TextView(context);
            year.setBackgroundResource(R.drawable.diagram_year_oval);
            year.setGravity(Gravity.CENTER);
            year.setText(new Datatore(bond.marriageDate).writeDate(true));
            year.setTextSize(13f);
            LayoutParams yearParams = new LayoutParams(LayoutParams.MATCH_PARENT, toPx(MARRIAGE_HEIGHT));
            yearParams.topMargin = toPx(bond.centerRelY() - MARRIAGE_HEIGHT / 2);
            bondLayout.addView(year, yearParams);
        }
        setOnClickListener(view -> {
            Memoria.setFirst(familyNode.spouseFamily);
            context.startActivity(new Intent(context, Famiglia.class));
        });
    }

    private int toPx(float dips) {
        return (int) (dips * getResources().getDisplayMetrics().density + 0.5f);
    }

    public void setPrintPDF(boolean printPDF) {
        this.printPDF = printPDF;
        invalidate();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        if (printPDF && hearth != null) {
            hearth.setBackgroundResource(R.drawable.diagram_hearth_print);
        }
    }
}
