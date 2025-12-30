package app.familygem.diagram;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;

import app.familygem.Diagram;
import app.familygem.Individuo;
import app.familygem.Memoria;
import app.familygem.R;
import graph.gedcom.PersonNode;

public class Asterisk extends GraphicMetric {
    public Asterisk(Context context, PersonNode personNode, Diagram diagramFragment) {
        super(context, personNode);
        LayoutInflater.from(context).inflate(R.layout.diagram_asterisk, this, true);
        diagramFragment.registerForContextMenu(this);
        setOnClickListener(v -> {
            Memoria.setFirst(personNode.person);
            context.startActivity(new Intent(context, Individuo.class));
        });
    }
}
