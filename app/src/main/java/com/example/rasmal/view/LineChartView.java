package com.example.rasmal.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.rasmal.R;

/**
 * A lightweight upward line/area chart drawn from normalized (0..1) mock points.
 * No external chart library — just a polyline plus a filled gradient area.
 */
public class LineChartView extends View {

    private float[] points = {0.1f, 0.3f, 0.25f, 0.5f, 0.45f, 0.7f, 0.85f};

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    private final float density;

    public LineChartView(Context context) {
        this(context, null);
    }

    public LineChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;

        int green = ContextCompat.getColor(context, R.color.primary_strong);

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3f * density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(green);

        fillPaint.setStyle(Paint.Style.FILL);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(green);
    }

    public void setPoints(float[] points) {
        if (points != null && points.length >= 2) {
            this.points = points;
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        fillPaint.setShader(new LinearGradient(
                0, 0, 0, h,
                Color.argb(64, 0, 196, 106),   // ~#4000C46A
                Color.argb(0, 0, 196, 106),    // transparent
                Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int n = points.length;
        if (n < 2) return;

        float padV = 8f * density;
        float w = getWidth();
        float h = getHeight();
        float usableH = h - padV * 2;
        float stepX = w / (n - 1);

        linePath.reset();
        fillPath.reset();

        float firstX = 0f;
        float firstY = padV + (1f - points[0]) * usableH;
        linePath.moveTo(firstX, firstY);
        fillPath.moveTo(firstX, h);
        fillPath.lineTo(firstX, firstY);

        for (int i = 1; i < n; i++) {
            float x = stepX * i;
            float y = padV + (1f - points[i]) * usableH;
            // smooth-ish curve using quadratic midpoints
            float prevX = stepX * (i - 1);
            float prevY = padV + (1f - points[i - 1]) * usableH;
            float midX = (prevX + x) / 2f;
            linePath.quadTo(prevX, prevY, midX, (prevY + y) / 2f);
            fillPath.quadTo(prevX, prevY, midX, (prevY + y) / 2f);
            if (i == n - 1) {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }
        }

        fillPath.lineTo(w, h);
        fillPath.close();

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);

        // endpoint dot
        float lastX = w - linePaint.getStrokeWidth() / 2f;
        float lastY = padV + (1f - points[n - 1]) * usableH;
        canvas.drawCircle(lastX, lastY, 4f * density, dotPaint);
    }
}
