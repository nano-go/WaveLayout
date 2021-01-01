package com.nano.wavedemo;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.nano.wavelayout.WaveLayout;
import android.widget.Toast;
import android.animation.ValueAnimator;

public class MainActivity extends AppCompatActivity {
    
	
	private WaveLayout mWaveLayout ;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
		mWaveLayout = findViewById(R.id.wave_layout) ;
		ValueAnimator animator = ValueAnimator.ofFloat(mWaveLayout.getMin(), mWaveLayout.getMax()) ;
		animator.setDuration(13000) ;
		animator.setRepeatCount(1) ;
		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener(){
			@Override
			public void onAnimationUpdate(ValueAnimator va) {
				mWaveLayout.setProgress((float)va.getAnimatedValue()) ;
			}
		});
		animator.start() ;
    }
    
}
