package com.brentandjody.speakslow;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;


/**
 * Listens to live audio from the microphone, and creates a stream
 */
public class AudioListenerService extends Service {

    public static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    public static final int SAMPLE_RATE = 8000;
    public static final int CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO;
    public static final int CHANNEL_OUT = AudioFormat.CHANNEL_OUT_MONO;
    public static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_IN, ENCODING);

    private final String TAG = this.getClass().getSimpleName();
    private boolean mRecord = false;
    private final IBinder mBinder = new LocalBinder();
    private AudioRecord audioRecorder;
    private LinkedBlockingQueue stream;

    @Override
    public IBinder onBind(Intent intent) {
        audioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_IN, ENCODING, BUFFER_SIZE);
        stream = new LinkedBlockingQueue();
        startRecording();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        boolean result = super.onUnbind(intent);
        stopRecording();
        stream.clear();
        return result;
    }

    public static Intent makeIntent(Context context) {
        Intent intent = new Intent(context, AudioListenerService.class);
        return intent;
    }

    public void startRecording() {
        Log.d(TAG, "Begin recording...");
        mRecord = true;

        audioRecorder.startRecording();
        Thread background = new Thread() {
            @Override
            public void run() {
                try {
                    while (mRecord) {
                        byte[] buffer = new byte[BUFFER_SIZE];
                        audioRecorder.read(buffer, 0, buffer.length);
                        stream.put(buffer);
                    }
                } catch(InterruptedException e) {
                    Log.i(TAG, "...Recording interrupted");
                }
                audioRecorder.stop();
                audioRecorder.release();
            }
        };
        background.start();
    }

    public void stopRecording() {
        if (mRecord) {
            Log.d(TAG, "...ending recording");
            mRecord = false;
            audioRecorder.stop();
        }
    }

    public LinkedBlockingQueue getAudioStream() {
        return stream;
    }

    public class LocalBinder extends Binder {
        AudioListenerService getService() {
            return AudioListenerService.this;
        }
    }

}
