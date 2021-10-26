package com.example.audiotracktest;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    ExampleAudioManager audioManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        audioManager = new ExampleAudioManager(this.getApplicationContext());
    }

    public void buttonPressed(View view) {
        if (audioManager.isRunning()) {
            audioManager.stop();
        } else {
            audioManager.start();
        }
    }
}

class ExampleAudioManager {
    private static final int INT16_MAX = Short.MAX_VALUE;

    private final AudioTrack audioTrack;

    int sampleSizeInFrames = 44100 / 20;

    Thread m_audioThread;
    private boolean stopFlag;

    AudioManager.OnAudioFocusChangeListener focusListener;
    Context appContext;

    public ExampleAudioManager(Context context) {
        this.appContext = context;
        int sampleRate = 44100;
        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
				AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
				sampleSizeInFrames * 4, AudioTrack.MODE_STREAM);
    }

    public boolean isRunning() {
        if (m_audioThread == null) return false;
        return m_audioThread.isAlive();
    }

    Runnable m_AudioThreadRunnable = () -> {
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
        play();
    };

    public void start() {
        Log.d("AudioTrackDebug","Start...");
//        audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
//                AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT,
//                sampleSizeInFrames * 4, AudioTrack.MODE_STREAM);

        if (focusListener == null) {
            focusListener = focusChange -> {
                Log.i("audio","Audio focus change");
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                    // Pause playback
                    ExampleAudioManager.this.stop();
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    // Resume playback
                    if (!ExampleAudioManager.this.isRunning()) {
                        ExampleAudioManager.this.start();
                    }
                } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                    ExampleAudioManager.this.stop();
                    // Stop playback
                }
            };
        }

        AudioManager am = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
        int result = am.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            stopFlag = false;
            m_audioThread = new Thread(m_AudioThreadRunnable);
            m_audioThread.start();
        } else {
            Log.e("AudioFocus","Focus not granted: " + result);
        }
    }

    public void stop() {
        AudioManager am = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
        am.abandonAudioFocus(focusListener);

        stopFlag = true;
        audioTrack.stop();
    }

    public void cleanUp() {
        audioTrack.release();
    }

    private void play() {
        audioTrack.setPlaybackHeadPosition(0);
        audioTrack.play();

        int sampleSizeInBytes = sampleSizeInFrames * 4;
        int sampleSizeInShorts = sampleSizeInBytes / 2;

        short[] frameBuffer = new short[sampleSizeInBytes];
        long currentTimeInFrames;

        int inNumberFrames = sampleSizeInFrames;
        currentTimeInFrames = -inNumberFrames;

        while (!stopFlag) {

            currentTimeInFrames += inNumberFrames;

            for (int i = 0; i < sampleSizeInShorts; i++) {
                if ((currentTimeInFrames + i) % 22050 < 2000) {
                    frameBuffer[i] = (short) (Math.random() * INT16_MAX);
                } else {
                    frameBuffer[i] = 0;
                }
            }

            audioTrack.write(frameBuffer, 0, sampleSizeInShorts);

        }
        stopFlag = false;
    }

}