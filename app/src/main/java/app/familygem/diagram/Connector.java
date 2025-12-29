package app.familygem.diagram;

import android.content.Context;
import android.view.LayoutInflater;

import app.familygem.Diagram;
import app.familygem.R;
import graph.gedcom.PersonNode;

public class Connector extends GraphicMetric {
    public Connector(Context context, PersonNode personNode, Diagram diagramFragment) {
        super(context, personNode);
        LayoutInflater.from(context).inflate(R.layout.diagram_connector, this, true);
        setOnClickListener(v -> {
            // open subtree diagram
            diagramFragment.openSubtree(personNode.person);
        });
    }
}
