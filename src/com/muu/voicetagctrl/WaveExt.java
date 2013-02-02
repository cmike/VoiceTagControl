package com.muu.voicetagctrl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.os.Environment;



public class WaveExt {
	private static final String WAVE_EXT_FOLDER = "WaveExt";
	private static final String WAVE_FILE_EXT   = ".wav";
	// Output file path
	private String          filePath = null;
	
	// File writer
	private RandomAccessFile randomAccessWriter;
	private boolean this_is_tmp_file = true; 
	private boolean keep_going       = true; // File operations are performed properly.
		       
	// Number of channels, sample rate, sample size(size in bits), buffer size, audio source, sample size(see AudioFormat)
	private short                    nChannels = 1; // AudioFormat.CHANNEL_CONFIGURATION_MONO
	private int                      sRate;			// {44100, 22050, 11025, 8000}; oscialtion per sec.
	private short                    bSamples = 16; // AudioFormat.ENCODING_PCM_16BIT

	// Number of bytes written to file after header
	// after stop() is called, this size is written to the header/data chunk in the wave file
	private int                      payloadSize = 0;
	
	public WaveExt (int SampleRate) {
		nChannels = 1;
		sRate     = SampleRate;
		bSamples = 16;
		payloadSize = 0;
	}
	
	private static String InitTemporaryStorage () {
		String fullPath      = null;
		String pathToStorage = Environment.getExternalStorageDirectory().getPath();
		File file            = new File(pathToStorage, WAVE_EXT_FOLDER);
		boolean ret			 = false;

		if (!file.exists()) {
			ret = file.mkdirs();
		}
		else
			ret = true;

		if (ret)
			fullPath = file.getAbsolutePath() + "/" + System.currentTimeMillis() + WAVE_FILE_EXT;

		return (fullPath);
	}
	
	private void WaveHeaderWrite () {
		if (randomAccessWriter != null) {

			try {
				randomAccessWriter.setLength(0); // Set file length to 0, to prevent unexpected behavior in case the file already existed
				randomAccessWriter.writeBytes("RIFF");
				randomAccessWriter.writeInt(0); // Final file size not known yet, write 0 
				randomAccessWriter.writeBytes("WAVE");
				randomAccessWriter.writeBytes("fmt ");
				randomAccessWriter.writeInt(Integer.reverseBytes(16)); // Sub-chunk size, 16 for PCM
				randomAccessWriter.writeShort(Short.reverseBytes((short) 1)); // AudioFormat, 1 for PCM
				randomAccessWriter.writeShort(Short.reverseBytes(nChannels));// Number of channels, 1 for mono, 2 for stereo
				randomAccessWriter.writeInt(Integer.reverseBytes(sRate)); // Sample rate
				randomAccessWriter.writeInt(Integer.reverseBytes(sRate*bSamples*nChannels/8)); // Byte rate, SampleRate*NumberOfChannels*BitsPerSample/8
				randomAccessWriter.writeShort(Short.reverseBytes((short)(nChannels*bSamples/8))); // Block align, NumberOfChannels*BitsPerSample/8
				randomAccessWriter.writeShort(Short.reverseBytes(bSamples)); // Bits per sample
				randomAccessWriter.writeBytes("data");
				randomAccessWriter.writeInt(0); // Data chunk size not known yet, write 0
			} catch (Exception e) {
				e.printStackTrace();
				keep_going = false;
			}
		}
	}
	
	public boolean Close () {
		boolean isOK = true;
		
		try
		{
			randomAccessWriter.seek(4); // Write size to RIFF header
			randomAccessWriter.writeInt(Integer.reverseBytes(36+payloadSize));
		
			randomAccessWriter.seek(40); // Write size to Subchunk2Size field
			randomAccessWriter.writeInt(Integer.reverseBytes(payloadSize));
		
			randomAccessWriter.close();
		}
		catch(IOException e)
		{
			e.printStackTrace();
			isOK = false;
		}
		
		return (isOK);
	}
	
	public boolean Close (String new_name) {
		boolean ret = false;
		if (new_name != null && !this_is_tmp_file)
			ret = false;
		else if (!Close ())
			ret = false;
		else {
			// NOT Implemented Rename
			ret  = false;
		}
		
		return (ret);
	}
	
	public void DataWrite (byte[] buffer) {
		
		if (!keep_going)
			return;
		
		try
		{ 
			randomAccessWriter.write(buffer); // Write buffer to file
			payloadSize += buffer.length;
		} catch (Exception e) {
			e.printStackTrace();
			keep_going = false;
		}
	}
	static WaveExt Init (int SampleRate, String fullFileName) {
		WaveExt 		 obj         = null;
		String  		 use_path    = null;
		RandomAccessFile fileHandler = null;
		boolean			 all_done    = false;
		boolean			 its_tmp     = false;

		if (fullFileName == null) {
			use_path = InitTemporaryStorage ();
			its_tmp  = true;
		}
		else
			use_path = fullFileName;

		//randomAccessWriter = fileHandler;

		if (use_path != null) {
			all_done = true;
			try {
				fileHandler = new RandomAccessFile(use_path, "rw");
			} catch (Exception e) {
				all_done = false;
				e.printStackTrace();
			}
		}

		if (all_done) {
			obj = new WaveExt(SampleRate);

			obj.filePath = use_path;
			obj.randomAccessWriter = fileHandler;
			obj.this_is_tmp_file   = its_tmp;

			obj.WaveHeaderWrite();
		}
		return (obj);
	}
}
