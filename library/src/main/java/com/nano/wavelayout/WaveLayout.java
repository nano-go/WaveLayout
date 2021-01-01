package com.nano.wavelayout;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

public class WaveLayout extends FrameLayout {

	private float currentWaveX ;
	private int mWaveLength ;
	private int mWaveHeight ;
	private Paint mPaint ;
	private Path mWavePath ;

	private float mMax ;
	private boolean mMaxInitialized ;
	private float mMin ;
	private boolean mMinInitialized ;
	private float mProgress ;
	
	private boolean mWaveAnimationStarted ;
	private boolean mIsAttched ;
	private boolean mAggregatedIsVisible ;

	private final long mUiThreadId ;

	private ValueAnimator mWaveAnimator ;
	private int mDuration ;
	private TimeInterpolator mInterpolator ;
	
	private AnimatorUpdateListener mUpdateListener = new AnimatorUpdateListener(){
		@Override
		public void onAnimationUpdate(ValueAnimator va) {
			currentWaveX = (float)va.getAnimatedValue() ;
			postInvalidateOnAnimation() ;
		}
	} ;

	public WaveLayout(Context context) {
		this(context, null) ;
	}

	public WaveLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0) ;
	}

	public WaveLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr) ;

		setLayerType(View.LAYER_TYPE_SOFTWARE, null) ;

		this.mUiThreadId = Thread.currentThread().getId() ;

		this.mPaint = new Paint(Paint.ANTI_ALIAS_FLAG) ;
		this.mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN)) ;
		this.mPaint.setColor(Color.WHITE) ;
		this.mPaint.setStyle(Paint.Style.FILL) ;
		this.mWavePath = new Path() ;	

		initWaveView() ;
		initAttrs(attrs,defStyleAttr) ;
		initOrResetAnimator() ;
	}

	private void initWaveView() {
		this.mMin = 0 ;
		this.mMax = 100 ;
		this.mWaveHeight = 12 ;
		this.mWaveAnimationStarted = true ;
		this.mDuration = 1000 ;
		this.mInterpolator = new LinearInterpolator() ;
	}
	
	private void initAttrs(AttributeSet attrs, int defStyleAttr) {
		TypedArray ta = getContext().obtainStyledAttributes(attrs,R.styleable.WaveLayout,defStyleAttr,0) ;
		if(ta == null){
			return ;
		}
		
		mWaveLength = ta.getDimensionPixelSize(R.styleable.WaveLayout_wave_length,mWaveLength) ;
		mWaveHeight = ta.getDimensionPixelSize(R.styleable.WaveLayout_wave_height,mWaveHeight) ;
		setWaveColor(ta.getColor(R.styleable.WaveLayout_wave_color,mPaint.getColor())) ;
		setMax(ta.getFloat(R.styleable.WaveLayout_max,mMax)) ;
		setMin(ta.getFloat(R.styleable.WaveLayout_min,mMin)) ;
		setProgress(mMin) ;
		
		int duration = ta.getInteger(R.styleable.WaveLayout_wave_duration,mDuration) ;
		if(duration > 0){
			this.mDuration = duration ;
		}
		ta.recycle() ;
	}

	private void initOrResetAnimator() {
		if(this.mWaveAnimator == null){
			this.mWaveAnimator = ValueAnimator.ofFloat(0,mWaveLength) ;
		}else{
			mWaveAnimator.setFloatValues(0,mWaveLength) ;
		}
		this.mWaveAnimator.setDuration(mDuration) ;
		this.mWaveAnimator.setRepeatCount(ValueAnimator.INFINITE) ;
		this.mWaveAnimator.setRepeatMode(ValueAnimator.INFINITE) ;
		this.mWaveAnimator.setInterpolator(mInterpolator) ;
		this.mWaveAnimator.addUpdateListener(mUpdateListener) ;
	}


	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setWaveLength((int)(getMeasuredWidth() / 2.5f)) ;
	}

	@Override
	protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
		if (mWaveLength <= 0) {
			return super.drawChild(canvas, child, drawingTime);
		}

		final int w = getWidth() ;
		final int h = getHeight() ;
		final int left = getPaddingLeft() ;
		final int top = getPaddingTop() ;
		final int right = w - getPaddingRight() ;
		final int bottom = h - getPaddingBottom() ;

		int layerCount = canvas.saveLayer(
		    left, top,right, bottom,
			null, Canvas.ALL_SAVE_FLAG
		) ;

		boolean superResult = super.drawChild(canvas, child, drawingTime);
		
		float range = mMax - mMin ;
		float scale = 0f ;
		if(range > 0){
			scale = (mProgress - mMin) / range ;
		}
		
		float currentY = h * (1 - scale) + top;
		if(currentY > bottom){
			currentY = bottom ;
		}
		
		final float startX = -mWaveLength + left + currentWaveX ;
		final float haflWaveLen = mWaveLength / 2f ;
		final float haflWaveHeight = mWaveHeight / 2f ;
		mWavePath.reset() ;
		mWavePath.moveTo(startX, currentY + haflWaveHeight) ;
		for (float y = startX; y < right; y += mWaveLength) {
			mWavePath.rQuadTo(haflWaveLen / 2, -haflWaveHeight, haflWaveLen, 0) ;
			mWavePath.rQuadTo(haflWaveLen / 2, haflWaveHeight, haflWaveLen, 0) ;
		}
		mWavePath.lineTo(right, bottom) ;
		mWavePath.lineTo(left, bottom) ;
		mWavePath.lineTo(left, currentY + haflWaveHeight) ; 
		mWavePath.close() ;

		canvas.drawPath(mWavePath, mPaint) ;

		canvas.restoreToCount(layerCount) ;
		return superResult ;
	}

	@Override
	public void onVisibilityAggregated(boolean isVisible) {
		super.onVisibilityAggregated(isVisible);
		if (mAggregatedIsVisible != isVisible) {
			mAggregatedIsVisible = isVisible ;
			if (isVisible) {
				if (mWaveAnimationStarted) {
					startWaveAnimation(false) ;
				}
			} else {
				pauseWaveAnimation(false) ;
			}
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		this.mIsAttched = true ;
		if (mWaveAnimationStarted) {
			startWaveAnimation(false) ;
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		pauseWaveAnimation(false) ;
		this.mIsAttched = false ;
	}

	private void autoInvalidate() {
		if (mUiThreadId == Thread.currentThread().getId()) {
			invalidate() ;
		} else {
			postInvalidate() ;
		}
	}
	
	public void startWaveAnimation() {
		startWaveAnimation(true) ;
	}
	
	private void startWaveAnimation(boolean fromUser) {
		if (fromUser) {
			// When the view is attached to the window or is visible, 
			// the animator will continue to run if the WaveAnimatorStarted is true.
			this.mWaveAnimationStarted = true ;
		}
		
		if (!canRunAnimator()) {
			return ;
		}
		
		if(mWaveAnimator.isRunning()) {
			mWaveAnimator.cancel() ;
		}
		
		mWaveAnimator.start() ;
	}
	
	private boolean canRunAnimator() {
		return mWaveLength > 0 && mIsAttched && getVisibility() == VISIBLE && 
			getWindowVisibility() == VISIBLE ;
	}

	public void pauseWaveAnimation() {
		pauseWaveAnimation(true) ;
	}

	private void pauseWaveAnimation(boolean fromUser) {
		if (fromUser) {
			this.mWaveAnimationStarted = false ;
		}
		if (mWaveAnimator.isRunning()) {
		    this.mWaveAnimator.pause() ;
		}
	}
	
	public void setInterpolator(TimeInterpolator interpolator){
		this.mInterpolator = interpolator ;
		mWaveAnimator.setInterpolator(interpolator) ;
	}

	public TimeInterpolator getInterpolator(){
		return mInterpolator ;
	}

	public void setDuration(int duration){
		mDuration = duration ;
		if(mWaveAnimator != null){
		    mWaveAnimator.setDuration(duration) ;
		}
	}

	public int getDuration(){
		return mDuration ;
	}
	
	public void incrementProgressBy(float diff) {
		setProgress(mProgress + diff) ;
	}
	
	public synchronized void setProgress(float progress) {
		if (mMaxInitialized && progress > mMax) {
			progress = mMax ;
		}
		if (mMinInitialized && progress < mMin) {
			progress = mMin ;
		}
		if (progress == mProgress) {
			return ;
		}
		
		mProgress = progress ;
		autoInvalidate() ;
	}

	public synchronized float getMax() {
		return mMax ;
	}

	public synchronized void setMax(float max) {
		if (mMinInitialized) {
			if (max < mMin) {
				max = mMin ;
			}
		}
		mMaxInitialized = true ;
		if (mMinInitialized && max != mMax) {
			mMax = max ;
			if (mProgress > mMax) {
				mProgress = mMax ;
			}
			autoInvalidate() ;
		} else {
			mMax = max ;
		}
	}

	public synchronized float getMin() {
		return mMin ;
	}

	public synchronized void setMin(float min) {
		if (mMaxInitialized) {
			if (min > mMax) {
				min = mMax ;
			}
		}

		mMinInitialized = true ;
		if (mMaxInitialized && min != mMin) {
			mMin = min ;
			if (mProgress < mMin) {
				mProgress = mMin ;
			}
			autoInvalidate() ;
		} else {
			mMin = min; 
		}
	}

	public void setWaveLength(int waveLength) {
		if (waveLength <= 0) {
			waveLength = 0 ;
		}

		if (mWaveLength == waveLength) {
			return ;
		}
		
		int oldWaveLength = mWaveLength ;
		mWaveLength = waveLength ;
		
		if(mWaveAnimator != null){
		    mWaveAnimator.setFloatValues(0, mWaveLength) ;
		}
		if(mWaveLength > 0 && mWaveAnimationStarted){
			startWaveAnimation(false) ;
		}else if(oldWaveLength <= 0){
			pauseWaveAnimation(false) ;
		}
	}

	public void setWaveColor(int color) {
		if (color != mPaint.getColor()) {
		    this.mPaint.setColor(color) ;
		    autoInvalidate() ;
		}
	}

}
