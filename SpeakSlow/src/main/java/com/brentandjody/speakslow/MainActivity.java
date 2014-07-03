package com.brentandjody.speakslow;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.concurrent.LinkedBlockingQueue;


public class MainActivity extends Activity {

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(AudioListenerService.makeIntent(this), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBound) {
            mAudioStream = null;
            mService.stopRecording();
            unbindService(mConnection);
            mBound=false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
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
        final int SLOWDOWN = 3;
        if (mBound) {
            Log.d(TAG, "Starting playback...");
            mPlayback = true;
            mTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, mService.SAMPLE_RATE,
                    mService.CHANNEL_OUT, mService.ENCODING, mService.BUFFER_SIZE, AudioTrack.MODE_STREAM);
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
                                    mTrack.write(buffer, 0, buffer.length);
                                    if (i >= SLOWDOWN) {
                                        mTrack.write(buffer, 0, buffer.length);
                                        i = 0;
                                    }
                                }
                            }
                        } catch (InterruptedException e) {
                            Log.i(TAG, "...playback interrupted");
                        }
                    } else {
                        Log.e(TAG, "No audio stream");
                    }
                    mTrack.stop();
                    mTrack.release();
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
