package com.muu.voicetagctrl;


import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class VoiceTagControlActivity extends Activity {
    VoiceDataHolder		mVoiceData = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
		setButtonHandlers();
		enableButtons(false);
    }
    
	private void setButtonHandlers() {
		((Button) findViewById(R.id.buttonRecord)).setOnClickListener(btnClick);
		((Button) findViewById(R.id.buttonStop)).setOnClickListener(btnClick);
	}

	private void enableButton(int id, boolean isEnable) {
		((Button) findViewById(id)).setEnabled(isEnable);
	}

	private void enableButtons(boolean isRecording) {
		enableButton(R.id.buttonRecord, !isRecording);
		enableButton(R.id.buttonStop, isRecording);
	}
	
	private void startRecording () {
		if (mVoiceData == null)
			mVoiceData = new VoiceDataHolder (upd_handler);
		
		if (mVoiceData.isValid()) {
			UpdateVolume(0);
			mVoiceData.start();
		}
		else {
			Toast.makeText(getApplicationContext(), "Unable Start",
					Toast.LENGTH_SHORT).show();			
		}
	}
	
	private void stopRecording () {
		if (mVoiceData !=null && mVoiceData.isRecording())
			mVoiceData.startRecording();
	}
	private View.OnClickListener btnClick = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			switch (v.getId()) {
			case R.id.buttonRecord: {
				Toast.makeText(getApplicationContext(), "Start Recording",
						Toast.LENGTH_SHORT).show();

				enableButtons(true);
				startRecording();

				break;
			}
			case R.id.buttonStop: {
				Toast.makeText(getApplicationContext(), "Stop Recording",
						Toast.LENGTH_SHORT).show();
				enableButtons(false);
				stopRecording();

				break;
			}
			}
		}
	};
	
	private void UpdateVolume (int volume_value) {
		TextView indicator = (TextView) findViewById(R.id.textVolume);
		
		indicator.setText(Integer.toString(volume_value));
	}
    private Handler upd_handler = new Handler(){
		
		@Override
		public void handleMessage(Message msg) {
			UpdateVolume(msg.what);
		}

	};
}