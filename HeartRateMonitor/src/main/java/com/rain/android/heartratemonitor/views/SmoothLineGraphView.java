package com.rain.android.heartratemonitor.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;

import com.jjoe64.graphview.GraphViewDataInterface;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewStyle;
import com.jjoe64.graphview.LineGraphView;

/**
 * Created by jeremiahstephenson on 10/25/13.
 */
public class SmoothLineGraphView extends LineGraphView {

    protected Path mLine;

    public SmoothLineGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SmoothLineGraphView(Context context, AttributeSet attrs, boolean verticalLabelsOnRight) {
        super(context, attrs, verticalLabelsOnRight);
    }

    public SmoothLineGraphView(Context context, String title) {
        super(context, title);
    }

    public SmoothLineGraphView(Context context, String title, boolean verticalLabelsOnRight) {
        super(context, title, verticalLabelsOnRight);
    }

    public SmoothLineGraphView(Context context, String title, GraphViewStyle style, boolean verticalLabelsOnRight) {
        super(context, title, style, verticalLabelsOnRight);
    }

    public SmoothLineGraphView(Context context, String title, GraphViewStyle style, boolean verticalLabelsOnRight, boolean showSideImages) {
        super(context, title, style, verticalLabelsOnRight, showSideImages);
    }

    @Override
    public void drawSeries(Canvas canvas, GraphViewDataInterface[] values, float graphwidth, float graphheight, float border, double minX, double minY, double diffX, double diffY, float horstart, GraphViewSeries.GraphViewSeriesStyle style, int[] colors) {

        final Shader temp = paint.getShader();

        // draw data
        paint.setStrokeWidth(style.thickness);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setDither(true);                    // set the dither to true
        paint.setStyle(Paint.Style.STROKE);       // set to STOKE
        paint.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
        paint.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
        paint.setPathEffect(new CornerPathEffect(50));

        paint.setAntiAlias(true);
        if (colors != null) {
            paint.setShader(new LinearGradient(0, 0, 0, getHeight(), colors, null, Shader.TileMode.MIRROR));
        } else {
            paint.setColor(style.color);
        }

        if (mLine == null) {
            mLine = new Path();
        }

        mLine.reset();

        float x;
        float y;

        for (int i = 0; i < values.length; i++) {

            x = convertX(values[i].getX(), minX, diffX, graphwidth, horstart);
            y = convertY(values[i].getY(), minY, diffY, graphheight, border);

            if (i == 0) {
                mLine.moveTo(x, y);
            } else {
                mLine.lineTo(x, y);
            }
        }

        drawShadedBackground(canvas, values, mLine, graphwidth, graphheight, border, minX, minY, diffX, diffY, horstart);

        canvas.drawPath(mLine, paint);

        paint.setShader(temp);
    }

    private float convertX(double xValue, double minX, double diffX, float graphwidth, float horstart) {

        final double valX = xValue - minX;
        final double ratX = valX / diffX;
        final double x = graphwidth * ratX;

        return (float)(x + (horstart + 1));
    }

    private float convertY(double yValue, double minY, double diffY, float graphheight, float border) {

        final double valX = yValue - minY;
        final double ratX = valX / diffY;
        final double y = graphheight * ratX;

        return (float)((border - y) + graphheight);
    }

    @Override
    protected void setPaintBackground() {
        super.setPaintBackground();

        paintBackground.setStrokeCap(Paint.Cap.ROUND);
        paintBackground.setDither(true);                    // set the dither to true
        paintBackground.setStrokeJoin(Paint.Join.ROUND);    // set the join to round you want
        paintBackground.setStrokeCap(Paint.Cap.ROUND);      // set the paint cap to round too
        paintBackground.setPathEffect(new CornerPathEffect(50));   // set the path effect when they join.
    }
}
