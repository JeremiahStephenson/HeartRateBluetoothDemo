package com.rain.android.heartratemonitor.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.rain.android.heartratemonitor.R;
import com.jjoe64.graphview.GraphViewDataInterface;
import com.jjoe64.graphview.GraphViewStyle;

import java.util.ArrayList;

/**
 * Created by jeremiahstephenson on 10/18/13.
 */
public class BpmGraphView extends SmoothLineGraphView {
    public BpmGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BpmGraphView(Context context, AttributeSet attrs, boolean verticalLabelsOnRight) {
        super(context, attrs, verticalLabelsOnRight);
    }

    public BpmGraphView(Context context, String title) {
        super(context, title);
    }

    public BpmGraphView(Context context, String title, boolean verticalLabelsOnRight) {
        super(context, title, verticalLabelsOnRight);
    }

    public BpmGraphView(Context context, String title, GraphViewStyle style, boolean verticalLabelsOnRight) {
        super(context, title, style, verticalLabelsOnRight);
    }

    public BpmGraphView(Context context, String title, GraphViewStyle style, boolean verticalLabelsOnRight, boolean showSideImages) {
        super(context, title, style, verticalLabelsOnRight, showSideImages);
    }

    @Override
    protected void drawShadedBackground(Canvas canvas, GraphViewDataInterface[] values, float graphwidth, float graphheight, float border, double minX, double minY, double diffX, double diffY, float horstart) {

        if (getDrawBackground()) {

            final DisplayMetrics metrics = getResources().getDisplayMetrics();
            final int px = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5, metrics);
            final int hbMin = getResources().getInteger(R.integer.heart_beat_minimum);

            double differ = 0;
            if (hbMin > minY && hbMin < minY + diffY) {
                final double diff = (hbMin - minY) / diffY;
                differ = graphheight - (diff * graphheight);
            }

            if (minY < hbMin) {

                ArrayList<Integer> ys = new ArrayList<Integer>();
                for (int y = px + (int)border + (int)differ; y <= graphheight + border + px; y+=px) {
                    ys.add(y);
                }

                ArrayList<Integer> xs = new ArrayList<Integer>();
                for (int x = px; x <= graphwidth + px; x+=px) {
                    xs.add(x);
                }

                final int longer = (xs.size() > ys.size()) ? xs.size() : ys.size();
                final int shorter = (xs.size() < ys.size()) ? xs.size() : ys.size();

                for (int i = 0; i < shorter; i++) {
                    canvas.drawLine(xs.get(i), border + (int)differ, 0, ys.get(i), paintBackground);
                }

                if (xs.size() < ys.size()) {
                    for (int i = shorter; i < longer; i++) {
                        canvas.drawLine(0, ys.get(i), xs.get(xs.size() - 1), ys.get(i - xs.size()), paintBackground);
                    }
                } else if (xs.size() > ys.size()) {
                    for (int i = shorter; i < longer; i++) {
                        canvas.drawLine(xs.get(i), border + (int)differ, xs.get(i - shorter), ys.get(ys.size() - 1), paintBackground);
                    }
                }

                for (int i = 0; i < shorter - 1; i++) {
                    canvas.drawLine(xs.get(xs.size() - 1) - xs.get(i), ys.get(ys.size() - 1), xs.get(xs.size() - 1), (ys.get(ys.size() - 1) - ys.get(i)) + border + (int)differ, paintBackground);
                }
            }
        }
    }

    @Override
    protected void drawShadedBackground(Canvas canvas, GraphViewDataInterface[] values, Path path, float graphwidth, float graphheight, float border, double minX, double minY, double diffX, double diffY, float horstart) {
        drawShadedBackground(canvas, values, graphwidth, graphheight, border, minX, minY, diffX, diffY, horstart);
    }

    @Override
    protected void setPaintBackground() {
        paintBackground = new Paint();
        paintBackground.setColor(getResources().getColor(R.color.heart_beat_red));
        paintBackground.setStrokeWidth(4);
    }
}
