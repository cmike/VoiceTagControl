package com.muu.voicetagctrl;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class VoiceDataHolder extends Thread {
	Handler notify_handler = null;
	
	final static int FFT_SIZE = 2048;
	final static int N_FRAMES_IN_HW_BUFFER = 5;
    private float averageAbsVolume = 0.0f;
	private int wave_chunk_ms = 120;
	private AudioRecord audioRecord;
	private boolean isRecording;
	private boolean is_valid = false;
	private int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private int nChannels = 1; //  AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
	private int bSamples = 16; // AudioFormat.ENCODING_PCM_16BIT;
	private int sampleRate = 44100; // Use 8000; while in Debugger
	private int frameByteSize = FFT_SIZE; // for 1024 fft size (16bit sample size)
	int framePeriod = 0; // The number of oscillations during 'wave_chunk_ms'
	int recBufSize =  0; 
	byte[] buffer;
	private Object syncer = null; 
	
	public VoiceDataHolder(Handler volume_level_handler){
		if (android.os.Build.MODEL.contains("sdk")) 
			sampleRate = 8000;  // Emulator
		else
			sampleRate = 44100;
		
		framePeriod = sampleRate * wave_chunk_ms / 1000; // The number of oscillations during 'wave_chunk_ms'
		int bufferSize = framePeriod * bSamples * nChannels / 8;
		
		if (bufferSize % 1024 != 0) {
		  int n1024 = bufferSize / 1024 + 1;
		
		  bufferSize = n1024 * 1024;
		} 
		
		
		frameByteSize = bufferSize;
			
		Log.d("Init 1", "Frame period " +
		Integer.toString(framePeriod)   +
		" Frame Buffer (bytes) "        +
		Integer.toString(frameByteSize));
		// need to be larger than size of a frame
		
		recBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfiguration, audioEncoding); 
		if (recBufSize > 0) {
			
			// We want AudioRecord buffer to be at least N_FRAMES_IN_HW_BUFFER times larger than one frame's buffer (frameByteSize)
			if (frameByteSize * N_FRAMES_IN_HW_BUFFER > recBufSize) {
				recBufSize = frameByteSize * N_FRAMES_IN_HW_BUFFER;
			}
			
			Log.d ("Init 2", "AudioRecord Buffer (bytes) " +
			Integer.toString(recBufSize));
			if (recorderInit()) {
				buffer = new byte[frameByteSize];
				is_valid = true;
				syncer = new Object();
				Log.d ("VoiceDataHolder", "Initialized OK");
			}
		} else
			is_valid = false;
		
		notify_handler = volume_level_handler;
		setName("VoiceDataHolder");
	}
	
	private boolean recorderInit () {
		boolean ret = false;

		if (audioRecord != null) {
			if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED)
				ret = true;
			else {
				audioRecord.release();
				audioRecord = null;
			}
		}

		if (!ret) {
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 
					sampleRate, channelConfiguration, audioEncoding, recBufSize);
			if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
				audioRecord.setRecordPositionUpdateListener(updateListener);
				audioRecord.setPositionNotificationPeriod(framePeriod);
				ret = true;
			}
		}

		return (ret);
	}
	private void VolumeLevelNotify () {
		int		level = (int) averageAbsVolume;
		if (notify_handler != null) {
			Message msg = notify_handler.obtainMessage(level);
			
			notify_handler.sendMessage(msg);
		}
	}
	
	public AudioRecord getAudioRecord(){
		return audioRecord;
	}
	
	public boolean isValid () { return (is_valid); }
	public boolean isRecording(){
		return this.isAlive() && isRecording;
	}
	
	public void startRecording(){
		if (!recorderInit ()) {
			Log.e("VoiceDataHolder", "Recorder Init. failure");
			return;
		}
		try{
			audioRecord.startRecording();
			isRecording = true;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void stopRecording(){
		try{
			audioRecord.stop();
			//audioRecord.release();
			isRecording = false;
			synchronized (syncer) {
				   syncer.notify();
				}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private AudioRecord.OnRecordPositionUpdateListener updateListener = new AudioRecord.OnRecordPositionUpdateListener()
	{
		public void onPeriodicNotification(AudioRecord recorder)
		{
			if (getFrameBytes() != null) {
				VolumeLevelNotify();
			}
		}
		public void onMarkerReached(AudioRecord recorder)
		{
			// NOT USED
		}
	};
	
	public byte[] getFrameBytes(){
		int n_read = audioRecord.read(buffer, 0, frameByteSize);
		
		Log.d("getFrameBytes", 
				"Read " + Integer.toString(n_read) + 
				" out of " + Integer.toString (frameByteSize));
		
		// analyze sound
		int totalAbsValue = 0;
        short sample = 0; 
        
        for (int i = 1; i < n_read; i += 2) {
            sample = (short)((buffer[i-1]) | buffer[i] << 8);
            totalAbsValue += Math.abs(sample);
        }
        
        if (n_read > 0)
          averageAbsVolume = totalAbsValue / n_read / 2;
        else
          averageAbsVolume = 0;

        Log.d("getFrameBytes", 
        		"Average Volume " + Float.toString(averageAbsVolume));
        
        // no input
        if (averageAbsVolume < 30){
        	return null;
        }
        
		return buffer;
	}
	
	public void run() {
		startRecording();
		if (getFrameBytes() != null) {
			VolumeLevelNotify();
		}
		try {
			synchronized (syncer) {
				
				syncer.wait();
			}
			
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		audioRecord.setRecordPositionUpdateListener(null);

		if (audioRecord.getState() == AudioRecord.RECORDSTATE_STOPPED)
			Log.d("VoiceDataHolderRun", "Alredy Stopped");
	}
	

}
