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
	byte[] buffer;
	
	public VoiceDataHolder(Handler volume_level_handler){
		if (android.os.Build.MODEL.contains("sdk")) 
			sampleRate = 8000;  // Emulator
		else
			sampleRate = 44100;
		
		int framePeriod = sampleRate * wave_chunk_ms / 1000; // The number of oscillations during 'wave_chunk_ms'
		int bufferSize = framePeriod * 2 * bSamples * nChannels / 8;
		
		frameByteSize = bufferSize;
			
		// need to be larger than size of a frame
		int recBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfiguration, audioEncoding); 
		if (recBufSize > 0) {
			
			// We want AudioRecord buffer to be at least N_FRAMES_IN_HW_BUFFER times larger than one frame's buffer (frameByteSize)
			if (frameByteSize * N_FRAMES_IN_HW_BUFFER > recBufSize) {
				recBufSize = frameByteSize * N_FRAMES_IN_HW_BUFFER;
			}
			
			audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 
					sampleRate, channelConfiguration, audioEncoding, recBufSize);
			if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
			audioRecord.setRecordPositionUpdateListener(updateListener);
			audioRecord.setPositionNotificationPeriod(framePeriod);
			buffer = new byte[frameByteSize];
			is_valid = true;
			Log.d ("VoiceDataHolder", "Initialized OK");
			}
		} else
			is_valid = false;
		
		notify_handler = volume_level_handler;
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
			isRecording = false;
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
		audioRecord.read(buffer, 0, frameByteSize);
		
		// analyze sound
		int totalAbsValue = 0;
        short sample = 0; 
        
        for (int i = 0; i < frameByteSize; i += 2) {
            sample = (short)((buffer[i]) | buffer[i + 1] << 8);
            totalAbsValue += Math.abs(sample);
        }
        averageAbsVolume = totalAbsValue / frameByteSize / 2;

        //System.out.println(averageAbsValue);
        
        // no input
        if (averageAbsVolume < 30){
        	return null;
        }
        
		return buffer;
	}
	
	public void run() {
		startRecording();
	}
	

}
