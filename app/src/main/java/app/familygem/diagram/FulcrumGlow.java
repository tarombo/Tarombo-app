package app.familygem.diagram;

import android.content.Context;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

import app.familygem.Global;
import app.familygem.R;
import graph.gedcom.Metric;

public class FulcrumGlow extends View {
    public static final int GLOW_SPACE = 20; // Default value, sync with Diagram if needed
    Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    BlurMaskFilter bmf = new BlurMaskFilter(25f * getResources().getDisplayMetrics().density,
            BlurMaskFilter.Blur.NORMAL);
    int extend = 5; // draw a rectangle a little bigger
    Metric targetMetric;
    boolean printPDF;

    public FulcrumGlow(Context context, Metric metric) {
        super(context == null ? Global.context : context);
        this.targetMetric = metric;
    }

    public void setPrintPDF(boolean printPDF) {
        this.printPDF = printPDF;
        invalidate();
    }

    public void setTargetMetric(Metric metric) {
        this.targetMetric = metric;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (targetMetric == null)
            return;
        paint.setColor(getResources().getColor(R.color.evidenzia));
        paint.setMaskFilter(bmf);
        setLayerType(View.LAYER_TYPE_SOFTWARE, paint);

        float density = getResources().getDisplayMetrics().density;
        int glowSpacePx = (int) (GLOW_SPACE * density);
        int extendPx = (int) (extend * density);

        // This is tricky because original code used 'toPx' which depends on context
        // density from Diagram...
        // But here we are in a View so we have context.
        // Also original code accessed 'fulcrumView.metric'. passing metric in
        // constructor is better.

        // However, 'toPx' in Diagram does: (int) (dips *
        // Global.context.getResources().getDisplayMetrics().density + 0.5f);

        canvas.drawRect(toPx(GLOW_SPACE - extend), toPx(GLOW_SPACE - extend),
                toPx(targetMetric.width + GLOW_SPACE + extend),
                toPx(targetMetric.height + GLOW_SPACE + extend), paint);
    }

    private int toPx(float dips) {
        return (int) (dips * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void invalidate() {
        if (printPDF) {
            setVisibility(GONE);
        } else {
            super.invalidate();
        }
    }
}
