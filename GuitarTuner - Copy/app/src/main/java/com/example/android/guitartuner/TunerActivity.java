package com.example.android.guitartuner;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.res.Resources;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.CountDownTimer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import org.jtransforms.fft.FloatFFT_1D;

public class TunerActivity extends AppCompatActivity {

    private static final String TAG = "Tuner Activity";

    private final static int SAMPLE_FREQ = 16000;
    private final static double REC_LENGTH_S = 0.512; // FFT performs best if SAMPLE*LENGTH is a power of 2
    private final static short AMPLITUDE_DETECTOR = 700;
    private final static float MAX_SLIDER_FREQ_OFFSET_HZ = 30.0f;

    private short[] mRecordedData;
    private int mSliderY = 0;

    private boolean mIsListening;
    private boolean mIsRecording;
    private boolean mIsProcessing;

    private boolean mHelpTextVisible = true;

    private FloatFFT_1D fft;

    TextView currentFreqTextView;
    TextView stringFreqTextView;
    TextView stringNameTextView;
    TextView helpTextView;
    RelativeLayout slider;
    LinearLayout sliderLinear;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tuner);

        currentFreqTextView = (TextView) findViewById(R.id.current_frequency);
        stringFreqTextView = (TextView) findViewById(R.id.string_freq);
        stringNameTextView = (TextView) findViewById(R.id.string_name);
        helpTextView = (TextView) findViewById(R.id.help_text);
        sliderLinear = (LinearLayout) findViewById(R.id.slider_linear);
        slider = (RelativeLayout) sliderLinear.findViewById(R.id.slider);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Thread listenerThread = new Thread(new Runnable() {

            @Override
            public void run() {
                mIsListening = true;
                mIsRecording = false;
                startListening();
            }

        });

        listenerThread.start();

    }

    @Override
    public void onPause() {
        super.onPause();

        mIsListening = false;

    }

    private void startListening(){

        int recordedDataSize = (int) (SAMPLE_FREQ * REC_LENGTH_S);
        fft = new FloatFFT_1D(recordedDataSize);
        mRecordedData = new short[recordedDataSize];

        int bufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_FREQ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        short[] readAudioData = new short[bufferSize];

        AudioRecord audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_FREQ,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        audioRecord.startRecording();

        int indexInRecordedData = 0;
        while(mIsListening){
            int readDataSize = audioRecord.read(readAudioData, 0, bufferSize);
            for(int i = 0; i < readDataSize; i++){

                if(readAudioData[i] >= AMPLITUDE_DETECTOR && !mIsProcessing) mIsRecording = true;

                if(mIsRecording == true && indexInRecordedData < recordedDataSize) {
                    mRecordedData[indexInRecordedData++] = readAudioData[i];
                } else if(indexInRecordedData >= recordedDataSize){
                    mIsRecording = false;
                    indexInRecordedData = 0;
                    processRecordedData();
                }
            }
        }

        audioRecord.stop();
        audioRecord.release();

    }

    private void processRecordedData() {
        mIsProcessing = true;

        float[] recordedDataFFT;
        recordedDataFFT = FrequencyDetector.convertToFloats(mRecordedData);
        fft.realForward(recordedDataFFT);

        float[] halfAbsFFT;
        halfAbsFFT = FrequencyDetector.absAndHalve(recordedDataFFT);

        int[] peaks;
        peaks = FrequencyDetector.identifyPeaksAndRemove(halfAbsFFT);

        String peakString = "";
        for(int e : peaks) peakString += e + " ";
        Log.d(TAG, peakString);

        int frequency = FrequencyDetector.detectFrequency(peaks);
        if(frequency != -1) this.updateTuner(frequency);

        mIsProcessing = false;
    }

    private void updateTuner(final int frequency) {

        final int stringFreq = FrequencyDetector.detectStringFreq(frequency);
        final String stringName = FrequencyDetector.getStringName(stringFreq);

        float dFromCenter = stringFreq - frequency;

        float percentage = (dFromCenter / MAX_SLIDER_FREQ_OFFSET_HZ);

        percentage = 50 + (50*percentage);

        if(percentage < 0) percentage = 0;
        else if(percentage > 100) percentage = 100;

        final float weight = percentage;

        Resources r = getResources();
        float dpTopx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 80, r.getDisplayMetrics());

        final int sliderWidth = (int)dpTopx;

        Runnable uiChanges = new Runnable() {
            @Override
            public void run() {
                currentFreqTextView.setText(frequency + "");
                stringFreqTextView.setText(stringFreq + " Hz");
                stringNameTextView.setText(stringName + "");
                slider.setLayoutParams(new
                    LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, sliderWidth, weight)
                );

                if(mHelpTextVisible) {
                    hideHelpTextView();
                    showStringName();
                }

                synchronized (this) {
                    this.notify();
                }
            }
        };

        // Runs uiChanges on UI Thread, and waits for it to finish
        synchronized (uiChanges) {
            runOnUiThread(uiChanges);

            try {
                uiChanges.wait();
            }
            catch (InterruptedException e){
                e.printStackTrace();
            }

        }
    }

    private void hideHelpTextView() {
        helpTextView.animate()
                .alpha(0.0f)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        helpTextView.setVisibility(View.GONE);
                        mHelpTextVisible = false;
                    }
                });
    }

    private void showStringName() {
        // prepare string name text view
        float hideHeight = stringNameTextView.getY();
        stringNameTextView.setTranslationY(hideHeight);
        stringNameTextView.setVisibility(View.VISIBLE);
        stringNameTextView.setAlpha(0.0f);

        // slide and fade in string name text view
        stringNameTextView.animate()
                .translationY(0)
                .alpha(1.0f);
    }

}