package com.example.muzix;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/*@Class EmphasisSwitch
* this is created for Metronome
* this view is only one circle
* if 4/4
* 0 0 0 0 -> this is 4 EmphasisSwitches
* 0 is EmphasisSwitch */
public class EmphasisSwitch extends View {

    private Paint paint;


    private float checked;
    private boolean isChecked;

    private final int margin = ConversionUtils.getPixelsFromDp(8);

    public EmphasisSwitch(Context context) {
        this(context, null);
    }

    public EmphasisSwitch(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmphasisSwitch(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setAntiAlias(true);
    }

    public void setPaint(Paint paint){
        this.paint = paint;
        invalidate();
    }

    public void setChecked(boolean isChecked) {
        if (isChecked != this.isChecked) {
            this.isChecked = isChecked;

            ValueAnimator animator = ValueAnimator.ofFloat(isChecked ? 0 : 1, isChecked ? 1 : 0);
            animator.setInterpolator(new DecelerateInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    checked = (float) valueAnimator.getAnimatedValue();
                    invalidate();
                }
            });
            animator.start();
        }
    }

    public boolean isChecked() {
        return isChecked;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int size = ConversionUtils.getPixelsFromDp(10);
        canvas.drawCircle(canvas.getWidth() / 2, canvas.getHeight() / 2, size, paint);
    }



    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int size = Math.min(getMeasuredWidth(), getMeasuredHeight());
        setMeasuredDimension(size, size);
    }
}

class ConversionUtils {

    public static int getPixelsFromDp(float dp) {
        return (int) (Resources.getSystem().getDisplayMetrics().density * dp);
    }

    public static float getDpFromPixels(int pixels) {
        return pixels / Resources.getSystem().getDisplayMetrics().density;
    }

}