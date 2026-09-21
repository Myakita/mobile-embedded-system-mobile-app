package com.example.mobile_embedded_system.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.mobile_embedded_system.R;
import com.example.mobile_embedded_system.domain.TacticalStatusEvaluator;

/**
 * Тактический глиф статуса бойца по ТЗ (Part D §6.6).
 * Геометрические фигуры:
 * ● OK — круг (зеленый)
 * ▲ WARNING — треугольник (амбра)
 * ■ CRITICAL — квадрат (красный)
 * ○ OFFLINE / STALE — контурный круг (серый)
 */
public class StatusGlyphView extends View {

    public enum GlyphType {
        OK,
        WARNING,
        CRITICAL,
        OFFLINE
    }

    private GlyphType glyphType = GlyphType.OK;
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path trianglePath = new Path();

    public StatusGlyphView(@NonNull Context context) {
        super(context);
        init();
    }

    public StatusGlyphView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public StatusGlyphView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        fillPaint.setStyle(Paint.Style.FILL);
        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeWidth(dpToPx(1.5f));
        updatePaints();
    }

    public void setGlyphType(GlyphType type) {
        this.glyphType = type != null ? type : GlyphType.OK;
        updatePaints();
        invalidate();
    }

    public void setFromTacticalStatus(TacticalStatusEvaluator.Status status, boolean isStale) {
        if (isStale) {
            setGlyphType(GlyphType.OFFLINE);
        } else if (status == TacticalStatusEvaluator.Status.CRITICAL) {
            setGlyphType(GlyphType.CRITICAL);
        } else if (status == TacticalStatusEvaluator.Status.WARNING) {
            setGlyphType(GlyphType.WARNING);
        } else {
            setGlyphType(GlyphType.OK);
        }
    }

    private void updatePaints() {
        int color;
        switch (glyphType) {
            case OK:
                color = resolveColor(R.attr.appStatusOk);
                fillPaint.setColor(color);
                break;
            case WARNING:
                color = resolveColor(R.attr.appStatusWarning);
                fillPaint.setColor(color);
                break;
            case CRITICAL:
                color = resolveColor(R.attr.appStatusCritical);
                fillPaint.setColor(color);
                break;
            case OFFLINE:
            default:
                color = resolveColor(R.attr.appInk2);
                strokePaint.setColor(color);
                break;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int defaultSize = (int) dpToPx(14);
        int width = resolveSize(defaultSize, widthMeasureSpec);
        int height = resolveSize(defaultSize, heightMeasureSpec);
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float radius = Math.min(w, h) / 2f - dpToPx(1);

        switch (glyphType) {
            case OK:
                // ● Заполненный круг
                canvas.drawCircle(cx, cy, radius, fillPaint);
                break;

            case WARNING:
                // ▲ Заполненный треугольник
                trianglePath.reset();
                trianglePath.moveTo(cx, cy - radius);
                trianglePath.lineTo(cx + radius, cy + radius);
                trianglePath.lineTo(cx - radius, cy + radius);
                trianglePath.close();
                canvas.drawPath(trianglePath, fillPaint);
                break;

            case CRITICAL:
                // ■ Заполненный квадрат (0dp скругления)
                canvas.drawRect(cx - radius, cy - radius, cx + radius, cy + radius, fillPaint);
                break;

            case OFFLINE:
                // ○ Контурный круг
                canvas.drawCircle(cx, cy, radius - dpToPx(0.5f), strokePaint);
                break;
        }
    }

    private int resolveColor(int attrResId) {
        TypedValue typedValue = new TypedValue();
        if (getContext().getTheme().resolveAttribute(attrResId, typedValue, true)) {
            return typedValue.data;
        }
        return 0xFF888888;
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()
        );
    }
}
