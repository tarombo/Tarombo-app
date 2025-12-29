package app.familygem.diagram;

import android.content.Context;
import android.widget.RelativeLayout;

import graph.gedcom.Metric;

public abstract class GraphicMetric extends RelativeLayout {
    public Metric metric;

    public GraphicMetric(Context context, Metric metric) {
        super(context);
        this.metric = metric;
        setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }
}
