package com.example.mobile_embedded_system.ui.widget;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

import com.example.mobile_embedded_system.R;

/**
 * Тактическая кнопка с удержанием для подтверждения критических приказов (ТЗ Part D §6.12).
 * Нулевой радиус скруглений (0dp), отсутствие ripple, прогресс заполнения полосой.
 */
public class HoldToConfirmButton extends AppCompatTextView {

    public interface OnConfirmedListener {
        void onConfirmed(HoldToConfirmButton button);
    }

    private static final long DEFAULT_HOLD_DURATION_MS = 1200L;

    private long holdDurationMs = DEFAULT_HOLD_DURATION_MS;
    private float progress = 0.0f; // 0.0 to 1.0
    private ValueAnimator animator;
    private OnConfirmedListener onConfirmedListener;

    private Paint progressPaint;
    private Paint borderPaint;

    public HoldToConfirmButton(@NonNull Context context) {
        super(context);
        init();
    }

    public HoldToConfirmButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HoldToConfirmButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setGravity(android.view.Gravity.CENTER);
        setTextAppearance(getContext(), R.style.Text_Label);

        progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressPaint.setStyle(Paint.Style.FILL);
        progressPaint.setColor(resolveColor(R.attr.appStatusWarning));

        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(dpToPx(1));
        borderPaint.setColor(resolveColor(R.attr.appHairline));

        setBackgroundColor(resolveColor(R.attr.appSurface));
        setTextColor(resolveColor(R.attr.appInk));
    }

    public void setHoldDurationMs(long durationMs) {
        this.holdDurationMs = durationMs;
    }

    public void setOnConfirmedListener(OnConfirmedListener listener) {
        this.onConfirmedListener = listener;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return super.onTouchEvent(event);
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                startHoldAnimation();
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                cancelHoldAnimation();
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void startHoldAnimation() {
        cancelHoldAnimation();

        animator = ValueAnimator.ofFloat(0.0f, 1.0f);
        animator.setDuration(holdDurationMs);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            progress = (float) animation.getAnimatedValue();
            invalidate();
            if (progress >= 1.0f) {
                onHoldCompleted();
            }
        });
        animator.start();
    }

    private void cancelHoldAnimation() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        progress = 0.0f;
        invalidate();
    }

    private void onHoldCompleted() {
        vibrateFeedback();
        cancelHoldAnimation();
        if (onConfirmedListener != null) {
            onConfirmedListener.onConfirmed(this);
        }
    }

    private void vibrateFeedback() {
        try {
            Vibrator vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(100);
                }
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();

        // 1. Полоса прогресса удержания снизу кнопки
        if (progress > 0.0f) {
            float progressWidth = w * progress;
            float progressHeight = dpToPx(4);
            canvas.drawRect(0, h - progressHeight, progressWidth, h, progressPaint);
        }

        // 2. Окантовка 0dp (Part D hairline)
        canvas.drawRect(0, 0, w, h, borderPaint);

        super.onDraw(canvas);
    }

    private int resolveColor(int attrResId) {
        TypedValue typedValue = new TypedValue();
        if (getContext().getTheme().resolveAttribute(attrResId, typedValue, true)) {
            return typedValue.data;
        }
        return 0xFF888888;
    }

    private float dpToPx(int dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()
        );
    }
}
