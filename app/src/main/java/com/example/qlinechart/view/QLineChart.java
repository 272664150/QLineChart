package com.example.qlinechart.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import java.util.List;

public class QLineChart extends View {

    private static final float VALUE_FLOAT_DELTA = 0.0000001f;

    private int mViewWidth;
    private int mViewHeight;

    private float mPaddingLeft;
    private float mPaddingTop;
    private float mPaddingRight;
    private float mPaddingBottom;

    private int mLabelCount;
    private float mMinYAxis;
    private float mMaxYAxis;

    private int mGridDashedLineColor;
    private float mGridDashedLineWidth;
    private Paint mGridDashedLinePaint;

    private int mBrokenLineColor;
    private float mBrokenLineWidth;
    private Paint mBrokenLinePaint;

    private int mHighlightingColor;
    private float mHighlightingWidth;
    private Paint mHighlightingPaint;
    private Paint mHighlightingCirclePaint;

    private int mTouchXIndex;
    private float mTouchXAxis;
    private float mLastEventX;
    private float mLastEventY;

    private float mXAxisRatio;
    private List<Entry> mLinePoints;
    private OnHighlightingListener mListener;

    public QLineChart(@NonNull Context context) {
        this(context, null);
    }

    public QLineChart(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public QLineChart(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        initParams();
    }

    private void initParams() {
        mTouchXIndex = Integer.MIN_VALUE;
        mTouchXAxis = Float.MIN_VALUE;
        mXAxisRatio = VALUE_FLOAT_DELTA;

        mGridDashedLinePaint = new Paint();
        mGridDashedLinePaint.setAntiAlias(true);
        mGridDashedLinePaint.setStyle(Paint.Style.STROKE);
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        float dashGap = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, metrics);
        float dashWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, metrics);
        mGridDashedLinePaint.setPathEffect(new DashPathEffect(new float[]{dashWidth, dashGap}, 0));

        mBrokenLinePaint = new Paint();
        mBrokenLinePaint.setAntiAlias(true);
        mBrokenLinePaint.setStyle(Paint.Style.STROKE);

        mHighlightingPaint = new Paint();
        mHighlightingPaint.setAntiAlias(true);
        mHighlightingPaint.setStyle(Paint.Style.STROKE);

        mHighlightingCirclePaint = new Paint();
        mHighlightingCirclePaint.setAntiAlias(true);
        mHighlightingCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) {
            mViewWidth = w;
            mViewHeight = h;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                getParent().requestDisallowInterceptTouchEvent(true);
                break;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(event.getX() - mLastEventX) < Math.abs(event.getY() - mLastEventY)
                        && Math.abs(event.getY() - mLastEventY) > ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }

        mLastEventX = event.getX();
        mLastEventY = event.getY();
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                mTouchXAxis = event.getX();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchXAxis = Float.MIN_VALUE;
                break;
            default:
                break;
        }

        invalidate();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawGridDashedLine(canvas);
        drawBrokenLine(canvas);

        if (Math.abs(mTouchXAxis - Float.MIN_VALUE) > VALUE_FLOAT_DELTA
                && ((mTouchXAxis - mPaddingLeft > VALUE_FLOAT_DELTA) || Math.abs(mTouchXAxis - mPaddingLeft) < VALUE_FLOAT_DELTA)
                && ((mViewWidth - mPaddingRight - mTouchXAxis > VALUE_FLOAT_DELTA) || Math.abs(mViewWidth - mPaddingRight - mTouchXAxis) < VALUE_FLOAT_DELTA)) {
            drawHighlighting(canvas);
            notifyHighlightingChange();
        }
    }

    private void drawGridDashedLine(Canvas canvas) {
        canvas.drawLine(mPaddingLeft, mPaddingTop, mViewWidth - mPaddingRight, mPaddingTop, mGridDashedLinePaint);
        for (int i = 1; i < mLabelCount; i++) {
            canvas.drawLine(mPaddingLeft, mPaddingTop + (mViewHeight - mPaddingBottom - mPaddingTop) / mLabelCount * i, mViewWidth - mPaddingRight, mPaddingTop + (mViewHeight - mPaddingBottom - mPaddingTop) / mLabelCount * i, mGridDashedLinePaint);
        }
        canvas.drawLine(mPaddingLeft, mViewHeight - mPaddingBottom, mViewWidth - mPaddingRight, mViewHeight - mPaddingBottom, mGridDashedLinePaint);
    }

    private void drawBrokenLine(Canvas canvas) {
        if (mLinePoints == null || mLinePoints.isEmpty()) {
            return;
        }

        int index = 1;
        Path path = new Path();
        mXAxisRatio = (mViewWidth - mPaddingLeft - mPaddingRight) / mLinePoints.size();

        path.moveTo(mLinePoints.get(index - 1).x * mXAxisRatio + mPaddingLeft, calcYAxisValue(mLinePoints.get(index - 1).y));
        for (; index < mLinePoints.size(); index++) {
            path.lineTo(mLinePoints.get(index).x * mXAxisRatio + mPaddingLeft, calcYAxisValue(mLinePoints.get(index - 1).y));
            path.lineTo(mLinePoints.get(index).x * mXAxisRatio + mPaddingLeft, calcYAxisValue(mLinePoints.get(index).y));
        }
        path.lineTo(mViewWidth - mPaddingRight, calcYAxisValue(mLinePoints.get(index - 1).y));
        canvas.drawPath(path, mBrokenLinePaint);
    }

    private void drawHighlighting(Canvas canvas) {
        if (mLinePoints == null || mLinePoints.isEmpty()) {
            return;
        }

        canvas.drawLine(mTouchXAxis, 0, mTouchXAxis, mViewHeight, mHighlightingPaint);

        //防止除数为0的情况
        if (mXAxisRatio < VALUE_FLOAT_DELTA) {
            mXAxisRatio = VALUE_FLOAT_DELTA;
        }
        mTouchXIndex = (int) Math.floor((mTouchXAxis - mPaddingLeft) / (mXAxisRatio));
        //当处于最右端时，mTouchXIndex可能计算为mLinePoints.size()，引起崩溃
        if (mTouchXIndex == mLinePoints.size()) {
            mTouchXIndex--;
        }

        canvas.drawCircle(mTouchXAxis, calcYAxisValue(mLinePoints.get(mTouchXIndex).y), mHighlightingWidth * 4, mHighlightingCirclePaint);
        mHighlightingCirclePaint.setColor(Color.WHITE);
        mHighlightingCirclePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawCircle(mTouchXAxis, calcYAxisValue(mLinePoints.get(mTouchXIndex).y), mHighlightingWidth * 2, mHighlightingCirclePaint);
    }

    private float calcYAxisValue(float y) {
        if (mMaxYAxis < mMinYAxis || mMaxYAxis < y || mMinYAxis > y) {
            return y;
        }

        //mMaxYAxis和mMinYAxis相等，在坐标轴底部画线
        if (mMaxYAxis - mMinYAxis < VALUE_FLOAT_DELTA) {
            return mViewHeight - mPaddingBottom;
        }
        return mViewHeight - (y - mMinYAxis) / (mMaxYAxis - mMinYAxis) * (mViewHeight - mPaddingBottom - mPaddingTop) - mPaddingBottom;
    }

    private void notifyHighlightingChange() {
        if (mListener != null
                && mLinePoints != null
                && !mLinePoints.isEmpty()) {
            mListener.onValue(mLinePoints.get(mTouchXIndex).x, mLinePoints.get(mTouchXIndex).y);
        }
    }

    public void setAxisPadding(float left, float top, float right, float bottom) {
        this.mPaddingLeft = left;
        this.mPaddingTop = top;
        this.mPaddingRight = right;
        this.mPaddingBottom = bottom;
    }

    public void setLabelCount(int labelCount) {
        this.mLabelCount = labelCount;
    }

    public void setMinAndMaxYAxis(float minYAxis, float maxYAxis) {
        this.mMinYAxis = minYAxis;
        this.mMaxYAxis = maxYAxis;
    }

    public void setGridDashedLineColor(int gridDashedLineColor) {
        this.mGridDashedLineColor = gridDashedLineColor;
        mGridDashedLinePaint.setColor(mGridDashedLineColor);
    }

    public void setGridDashedLineWidth(float gridDashedLineWidth) {
        this.mGridDashedLineWidth = gridDashedLineWidth;
        mGridDashedLinePaint.setStrokeWidth(mGridDashedLineWidth);
    }

    public void setBrokenLineColor(int brokenLineColor) {
        this.mBrokenLineColor = brokenLineColor;
        mBrokenLinePaint.setColor(mBrokenLineColor);
    }

    public void setBrokenLineWidth(float brokenLineWidth) {
        this.mBrokenLineWidth = brokenLineWidth;
        mBrokenLinePaint.setStrokeWidth(mBrokenLineWidth);
    }

    public void setHighlightingColor(int highlightingColor) {
        this.mHighlightingColor = highlightingColor;
        mHighlightingPaint.setColor(mHighlightingColor);
        mHighlightingCirclePaint.setColor(mHighlightingColor);
    }

    public void setHighlightingWidth(float highlightingWidth) {
        this.mHighlightingWidth = highlightingWidth;
        mHighlightingPaint.setStrokeWidth(mHighlightingWidth);
        mHighlightingCirclePaint.setStrokeWidth(mHighlightingWidth * 2);
    }

    public void setBrokenLineData(List<Entry> points) {
        mLinePoints = points;
    }

    public static class Entry {
        private float x;
        private float y;

        public Entry(float x, float y) {
            this.x = x;
            this.y = y;
        }

        public float getX() {
            return x;
        }

        public float getY() {
            return y;
        }
    }

    public void setHighlightingListener(OnHighlightingListener listener) {
        this.mListener = listener;
    }

    public interface OnHighlightingListener {
        void onValue(float x, float y);
    }
}