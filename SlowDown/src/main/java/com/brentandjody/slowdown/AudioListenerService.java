package com.brentandjody.slowdown;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


/**
 * Listens to live audio from the microphone, and creates a stream
 */
public class AudioListenerService extends Service {

    private final String TAG = this.getClass().getSimpleName();
    private final IBinder mBinder = new LocalBinder();
    private AudioRecord audioStream;

    @Override
    public IBinder onBind(Intent intent) {
        int minBufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioStream = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000, AudioFormat.CHANNEL_IN_MONO,AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
        startRecording();
        return mBinder;
    }

    public static Intent makeIntent(Context context) {
        Intent intent = new Intent(context, AudioListenerService.class);
        return intent;
    }

    public void startRecording() {
        audioStream.startRecording();
    }

    public void stopRecording() {
        audioStream.stop();
    }

    private AudioRecord getAudioStream() {
        return audioStream;
    }

    public class LocalBinder extends Binder {
        AudioListenerService getService() {
            return AudioListenerService.this;
        }
    }

}
