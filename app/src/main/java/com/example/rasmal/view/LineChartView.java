package com.example.rasmal.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.rasmal.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Canvas-drawn line chart for the dashboard's portfolio performance card.
 * Draws a smoothed line with a gradient fill under it, coloured green/red
 * depending on whether the series ends up or down versus its first point.
 * Every point plotted here comes from real data (the trade ledger + live
 * quotes) — this view has no knowledge of where the numbers came from.
 */
public class LineChartView extends View {

    /** One plotted point: x is a 0..1 position along the timeline, y is the SAR value. */
    public static class Point {
        public final float x;
        public final double value;
        public Point(float x, double value) { this.x = x; this.value = value; }
    }

    private List<Point> points = new ArrayList<>();
    private boolean up = true;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    public LineChartView(Context context) { super(context); init(); }
    public LineChartView(Context context, @Nullable AttributeSet attrs) { super(context, attrs); init(); }
    public LineChartView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle); init();
    }

    private void init() {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(dp(2.5f));
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint.setStyle(Paint.Style.FILL);

        dotPaint.setStyle(Paint.Style.FILL);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(dp(1f));
        gridPaint.setColor(ContextCompat.getColor(getContext(), R.color.border_subtle));
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }

    /** Sets the series to plot. Points should already be sorted by x ascending. */
    public void setPoints(List<Point> newPoints) {
        this.points = newPoints != null ? newPoints : new ArrayList<>();
        this.up = this.points.size() < 2
                || this.points.get(this.points.size() - 1).value >= this.points.get(0).value;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0 || points.size() < 2) return;

        float padTop = dp(12f), padBottom = dp(12f);
        float chartTop = padTop, chartBottom = h - padBottom;
        float chartHeight = chartBottom - chartTop;

        double min = points.get(0).value, max = points.get(0).value;
        for (Point p : points) {
            if (p.value < min) min = p.value;
            if (p.value > max) max = p.value;
        }
        // Avoid a perfectly flat line hugging one edge when all values are equal.
        if (max - min < 0.01) { max += 1; min -= 1; }
        double range = max - min;

        int lineColor = ContextCompat.getColor(getContext(), up ? R.color.up_green : R.color.down_red);
        int fillTop = up ? ContextCompat.getColor(getContext(), R.color.chart_fill_top)
                         : Color.argb(0x33, 0xFF, 0x5B, 0x5B);
        int fillBottom = up ? ContextCompat.getColor(getContext(), R.color.chart_fill_bottom)
                            : Color.argb(0x00, 0xFF, 0x5B, 0x5B);

        linePaint.setColor(lineColor);
        dotPaint.setColor(lineColor);
        fillPaint.setShader(new LinearGradient(0, chartTop, 0, chartBottom,
                fillTop, fillBottom, Shader.TileMode.CLAMP));

        // Faint horizontal midline for scale reference.
        float midY = chartTop + chartHeight / 2f;
        canvas.drawLine(0, midY, w, midY, gridPaint);

        List<PointF> screen = new ArrayList<>(points.size());
        for (Point p : points) {
            float sx = p.x * w;
            float norm = (float) ((p.value - min) / range);
            float sy = chartBottom - norm * chartHeight;
            screen.add(new PointF(sx, sy));
        }

        linePath.reset();
        fillPath.reset();
        buildSmoothPath(screen, linePath);

        fillPath.set(linePath);
        fillPath.lineTo(screen.get(screen.size() - 1).x, chartBottom);
        fillPath.lineTo(screen.get(0).x, chartBottom);
        fillPath.close();

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        PointF last = screen.get(screen.size() - 1);
        canvas.drawCircle(last.x, last.y, dp(4f), dotPaint);
        Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(dp(2f));
        ring.setColor(ContextCompat.getColor(getContext(), R.color.surface));
        canvas.drawCircle(last.x, last.y, dp(4f), ring);
    }

    /** Catmull-Rom-ish smoothing via cubic Beziers between successive points. */
    private void buildSmoothPath(List<PointF> pts, Path path) {
        path.moveTo(pts.get(0).x, pts.get(0).y);
        for (int i = 0; i < pts.size() - 1; i++) {
            PointF p0 = pts.get(i);
            PointF p1 = pts.get(i + 1);
            float midX = (p0.x + p1.x) / 2f;
            path.cubicTo(midX, p0.y, midX, p1.y, p1.x, p1.y);
        }
    }
}
