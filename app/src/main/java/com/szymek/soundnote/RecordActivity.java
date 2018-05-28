package com.szymek.soundnote;

import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStreamWriter;
import java.util.Date;

import android.app.Activity;
import android.os.Environment;
import android.widget.ProgressBar;
import android.widget.TextView;

public class RecordActivity extends Activity {
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FILE_EXT_TXT = ".txt";
    private static final String AUDIO_RECORDER_FOLDER = "VoiceRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "temp_record.raw";
    private static final int RECORDER_BPP = 16;
    private static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MIN_SUM_TO_SAVE_DATA = 20000;
    private static final int MAX_VOLUME_PROGRESS = 64*512;
    private static final int MAX_SILENCE_TIME = 400;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private boolean isPause = false;
    private int timer = 0;

    private ProgressBar volume;
    private EditText authorInput;
    private EditText titleInput;
    private EditText descriptionInput;
    private TextView dateAndLengthLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        startTimer();
        initialze();
        enableButtons(false);

        bufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    }

    private void initialze() {
        ((Button) findViewById(R.id.btnStart)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnStop)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnDelete)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnSave)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnList)).setOnClickListener(btnClick);

        volume = (ProgressBar) findViewById(R.id.volume_bar);
        volume.setMax(MAX_VOLUME_PROGRESS-MIN_SUM_TO_SAVE_DATA);

        authorInput = (EditText) findViewById(R.id.author_input);
        titleInput = (EditText) findViewById(R.id.title_input);
        descriptionInput = (EditText) findViewById(R.id.description_input);
        dateAndLengthLabel = (TextView) findViewById(R.id.date_and_length_label);
    }

    private void enableButton(int id, boolean isEnable) {
        ((Button) findViewById(id)).setEnabled(isEnable);
    }

    private void enableButtons(boolean isRecording) {
        enableButton(R.id.btnStart, !isRecording);
        enableButton(R.id.btnStop, isRecording);
        enableButton(R.id.btnDelete, !isRecording);
        enableButton(R.id.btnSave, !isRecording);
    }

    private String getFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);
        if (!file.exists()) {
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getTempFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);
        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);
        if (tempFile.exists())
            tempFile.delete();
        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private void writeAudioDataToFile() {
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int silenceData = 0;

        int read;
        int sum;
        if (null != os) {
            while (isRecording) {
                if(!isPause){
                    read = recorder.read(data, 0, bufferSize);

                    sum=0;
                    for(int i=0; i<bufferSize; i++) sum+=(Math.abs(data[i]));
                    volume.setProgress(sum-MIN_SUM_TO_SAVE_DATA);
                    if(sum < MIN_SUM_TO_SAVE_DATA){
                        silenceData++;
                    }else{
                        silenceData = 0;
                    }

                    if(silenceData<MAX_SILENCE_TIME){
                        if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                            try {
                                os.write(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void createTextFileToNote(String filename){
        File file = new File(filename);
        Uri uri = Uri.fromFile(file);
        MediaPlayer mp = MediaPlayer.create(this, uri);
        int duration = mp.getDuration();
        duration /= 1000;
        String time = String.format("%02d:%02d:%02d",duration/3600,(duration%3600)/60,duration%60);

        StringBuilder textFileContext = new StringBuilder();
        textFileContext.append(authorInput.getText());
        textFileContext.append("\n");
        textFileContext.append(titleInput.getText());
        textFileContext.append("\n");
        textFileContext.append(descriptionInput.getText());
        textFileContext.append("\n");
        String dateAndTime = time + dateAndLengthLabel.getText().subSequence(8,dateAndLengthLabel.getText().length());
        textFileContext.append(dateAndTime);

        String name = filename.substring(0,filename.length()-4) + AUDIO_RECORDER_FILE_EXT_TXT;
        try {
            FileOutputStream fOut = new FileOutputStream(name);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.write(textFileContext.toString());
            myOutWriter.close();
            fOut.close();
        } catch (Exception e){}
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;
        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    private void startTimer(){
        final Handler timerHandler = new Handler();
        timerHandler.post(new Runnable() {
            @Override
            public void run() {
                Date today = new Date();
                String time = String.format("%02d:%02d:%02d",timer/3600,(timer%3600)/60,timer%60);
                String date = String.format("%02d/%02d/%4d", today.getDate(),today.getMonth()+1,today.getYear()+1900);
                dateAndLengthLabel.setText(time + " - " + date);

                if(!isPause && isRecording){
                    timer++;
                }else if(isPause || !isRecording){
                    volume.setProgress(0);
                }

                timerHandler.postDelayed(this, 1000);
            }
        });
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnStart: {
                    actionStart();
                    break;
                }
                case R.id.btnStop: {
                    actionStop();
                    break;
                }
                case R.id.btnDelete: {
                    actionDelete();
                    break;
                }
                case R.id.btnSave: {
                    actionSave();
                    break;
                }
                case R.id.btnList: {
                    Intent intent = new Intent(getApplicationContext(), ListActivity.class);
                    startActivity(intent);
                }
            }
        }
    };

    private void actionStart(){
        if(!isPause){
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLERATE, RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);

            int i = recorder.getState();
            if (i == 1){
                enableButtons(true);
                recorder.startRecording();
                isRecording = true;
                timer = 0;
                recordingThread = new Thread(new Runnable() {

                    @Override
                    public void run() {
                        writeAudioDataToFile();
                    }
                });
                recordingThread.start();
            }
        }else{
            isPause = false;
            enableButtons(true);
        }
    }

    private void actionStop(){
        if(isRecording){
            enableButtons(false);
            isPause = true;
        }
        volume.setProgress(0);
    }

    private void actionDelete(){
        if (null != recorder) {
            isRecording = false;
            isPause = false;

            int i = recorder.getState();
            if (i == 1)
                recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
            timer = 0;
            enableButton(R.id.btnSave, false);
            enableButton(R.id.btnDelete, false);
        }

        deleteTempFile();
        authorInput.setText("");
        titleInput.setText("");
        descriptionInput.setText("");
        volume.setProgress(0);
    }

    private void actionSave(){
        if (null != recorder) {
            isRecording = false;
            isPause = false;

            int i = recorder.getState();
            if (i == 1)
                recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
            timer = 0;
            enableButton(R.id.btnSave, false);
            enableButton(R.id.btnDelete, false);
        }

        String fileName = getFilename();
        copyWaveFile(getTempFilename(), fileName);
        deleteTempFile();
        createTextFileToNote(fileName);
        authorInput.setText("");
        titleInput.setText("");
        descriptionInput.setText("");
        volume.setProgress(0);
    }
}