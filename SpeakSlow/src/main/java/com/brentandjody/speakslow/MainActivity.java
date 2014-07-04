package com.brentandjody.speakslow;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.concurrent.LinkedBlockingQueue;


public class MainActivity extends Activity {

    private static final int SLOWDOWN = 2;
    private static final float SILENCE_THRESHOLD = 150f;

    private final String TAG = this.getClass().getSimpleName();
    private boolean mBound = false;
    private boolean mPlayback = false;
    private AudioListenerService mService;
    private LinkedBlockingQueue mAudioStream;
    private AudioTrack mTrack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bindService(AudioListenerService.makeIntent(this), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            mAudioStream = null;
            mService.stopRecording();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAudioStream = null;
        if (mService!=null) mService.stopRecording();
        if (mConnection!=null) unbindService(mConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mPlayback)
            startPlayback();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mPlayback)
            stopPlayback();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startPlayback() {
        if (mBound) {
            Log.d(TAG, "Starting playback...");
            mPlayback = true;
            mTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, AudioListenerService.SAMPLE_RATE,
                    AudioListenerService.CHANNEL_OUT, AudioListenerService.ENCODING, AudioListenerService.BUFFER_SIZE, AudioTrack.MODE_STREAM);
            mTrack.play();
            Thread background = new Thread() {
                @Override
                public void run() {
                    if (mAudioStream != null) {
                        try {
                            int i = 0;
                            while (mPlayback) {
                                if (!mAudioStream.isEmpty()) {
                                    i++;
                                    byte[] buffer = (byte[]) mAudioStream.take();
                                    if (soundLevel(buffer) > SILENCE_THRESHOLD) {
                                        mTrack.write(buffer, 0, buffer.length);
                                        if (i >= SLOWDOWN) {
                                            mTrack.write(buffer, 0, buffer.length);
                                            i = 0;
                                        }
                                    }
                                }
                            }
                            Log.d(TAG, "Playback thread is ending");
                            mTrack.stop();
                            mTrack.release();
                        } catch (InterruptedException e) {
                            Log.i(TAG, "...playback interrupted");
                        }
                    } else {
                        Log.e(TAG, "No audio stream");
                    }
                }
            };
            background.start();
        } else {
            Log.d(TAG, "could not start playback");
        }
    }

    private void stopPlayback() {
        Log.d(TAG, "...stopping playback");
        mPlayback=false;
    }

    private float soundLevel(byte[] buffer) {
        float total = 0.0f;
        short sample;
        for (int i=0; i < AudioListenerService.BUFFER_SIZE; i+=2) {
            sample = (short)((buffer[i]) | buffer[i+1] << 8);
            total += Math.abs(sample) / (AudioListenerService.BUFFER_SIZE/2);
        }
        return total;
    }

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            AudioListenerService.LocalBinder binder = (AudioListenerService.LocalBinder) iBinder;
            mService = binder.getService();
            mAudioStream = mService.getAudioStream();
            mBound = true;
            startPlayback();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            mAudioStream = null;
            mBound = false;
        }
    };




}
