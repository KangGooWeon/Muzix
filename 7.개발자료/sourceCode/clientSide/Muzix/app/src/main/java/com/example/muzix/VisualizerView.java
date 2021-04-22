package com.example.muzix;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/*@Class VisualizerView
* if you recording you can see Visualizer
* it draws with realtime short data*/
public class VisualizerView extends View {
    private Paint graphPaint;
    private List<Integer> graphData;
    private static final int VALUE_TO_HEIGHT_SCALE = 100;
    public VisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        graphPaint = new Paint();
        graphPaint.setColor(Color.RED);
        graphPaint.setStrokeWidth(5);
        graphData = new ArrayList<>();
    }

    void addValuetoGraph(int value){
        value /= VALUE_TO_HEIGHT_SCALE;
        graphData.add(value);
        if(graphData.size() >= getWidth()) graphData.remove(0);
    }

    public void resetCanvas(){
        graphData.clear();
        invalidate();
    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int graphHeight = getHeight()/2;
        canvas.drawLine(0, graphHeight, getWidth(), graphHeight, graphPaint);
        if(!graphData.isEmpty()) {
            for (int i = 0; i < graphData.size() - 1; i++) {
                canvas.drawLine(
                        getWidth() - graphData.size() + i,
                        graphHeight - graphData.get(i),
                        getWidth() - graphData.size() + i,
                        graphHeight + graphData.get(i),
                        graphPaint);
            }
        }
    }
}