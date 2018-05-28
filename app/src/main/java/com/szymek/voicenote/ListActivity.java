package com.szymek.voicenote;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.SequenceInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ListActivity extends AppCompatActivity {

    private static final String AUDIO_RECORDER_FOLDER = "VoiceRecorder";

    private MediaPlayer mediaPlayer;

    private ListView list;
    private List<File> wavFileList;
    List<Integer> checkedIndexes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        initialize();
        getFileList();

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    private void initialize(){
        ((Button) findViewById(R.id.btnPlaySelected)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnDeleteSelected)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnMergeSelected)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnRecord)).setOnClickListener(btnClick);
        ((Button) findViewById(R.id.btnStop)).setOnClickListener(btnClick);
        list = (ListView) findViewById(R.id.list);

        ((Button) findViewById(R.id.btnPlaySelected)).setEnabled(false);
        ((Button) findViewById(R.id.btnMergeSelected)).setEnabled(false);
        ((Button) findViewById(R.id.btnDeleteSelected)).setEnabled(false);
    }

    private void getFileList(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);
        if (!file.exists()) {
            file.mkdirs();
        }

        File[] files = file.listFiles();
        wavFileList = new ArrayList<>();
        for(int i=0; i<files.length; i++){
            if(files[i].getName().substring(files[i].getName().length()-3,files[i].getName().length()).equals("wav")){
                wavFileList.add(files[i]);
            }
        }

        final ListItem[] listItems = new ListItem[wavFileList.size()];
        for(int i = 0; i < listItems.length; i++){
            listItems[i] = getFileDescription(wavFileList.get(i).getAbsolutePath().substring(0,wavFileList.get(i).getAbsolutePath().length()-3) + "txt");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_multiple_choice){

            private LayoutInflater inflater = null;

            @Override
            public int getCount() {
                return listItems.length;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                View mv;
                inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                if(view == null){
                    view = inflater.inflate(R.layout.list_item, null);
                }
                mv = view;



                TextView author = (TextView) mv.findViewById(R.id.item_author);
                TextView title = (TextView) mv.findViewById(R.id.item_title);
                TextView description = (TextView) mv.findViewById(R.id.item_description);
                TextView dateAndDuration = (TextView) mv.findViewById(R.id.item_date_and_duration);
                author.setText(listItems[i].getAuthor());
                title.setText(listItems[i].getTitle());
                description.setText(listItems[i].getDescription());
                dateAndDuration.setText(listItems[i].getDateAndDuration());
                return mv;
            }
        };

        list.setAdapter(adapter);

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView adapterView, View view, int position, long id) {
                SparseBooleanArray checked = list.getCheckedItemPositions();
                checkedIndexes = new ArrayList<>();
                for(int i=0; i<checked.size(); i++){
                    if(checked.valueAt(i)){
                        checkedIndexes.add(checked.keyAt(i));
                        (adapterView.getChildAt(checked.keyAt(i))).setBackgroundColor(Color.LTGRAY);
                    }else{
                        (adapterView.getChildAt(checked.keyAt(i))).setBackgroundColor(Color.WHITE);
                    }
                }
                enableButtons(checkedIndexes.size());
            }
        });
    }

    private void enableButtons(int checkedListSize){
        if(checkedListSize == 0){
            ((Button) findViewById(R.id.btnPlaySelected)).setEnabled(false);
            ((Button) findViewById(R.id.btnMergeSelected)).setEnabled(false);
            ((Button) findViewById(R.id.btnDeleteSelected)).setEnabled(false);
        }else if(checkedListSize == 1){
            ((Button) findViewById(R.id.btnPlaySelected)).setEnabled(true);
            ((Button) findViewById(R.id.btnMergeSelected)).setEnabled(false);
            ((Button) findViewById(R.id.btnDeleteSelected)).setEnabled(true);
        }else if(checkedListSize == 2){
            ((Button) findViewById(R.id.btnPlaySelected)).setEnabled(false);
            ((Button) findViewById(R.id.btnMergeSelected)).setEnabled(true);
            ((Button) findViewById(R.id.btnDeleteSelected)).setEnabled(true);
        } else if(checkedListSize > 2){
            ((Button) findViewById(R.id.btnPlaySelected)).setEnabled(false);
            ((Button) findViewById(R.id.btnMergeSelected)).setEnabled(false);
            ((Button) findViewById(R.id.btnDeleteSelected)).setEnabled(true);
        }
    }

    private ListItem getFileDescription(String filename){
        File file = new File(filename);
        ListItem item = new ListItem();
        try {
            FileInputStream fis = new FileInputStream(file);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            item.setAuthor(br.readLine());
            item.setTitle(br.readLine());
            item.setDescription(br.readLine());
            item.setDateAndDuration(br.readLine());
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return item;
    }

    private void playSelected(){
        if(mediaPlayer != null) {

            mediaPlayer.release();

        }

        Uri uri = Uri.fromFile(wavFileList.get(checkedIndexes.get(0)));
        mediaPlayer = MediaPlayer.create(this, uri);

        mediaPlayer.start();
    }

    private void stopPlaying(){
        if(mediaPlayer != null) {

            mediaPlayer.release();

        }
    }

    private void deleteSelected(){
        for(int i=0; i<checkedIndexes.size(); i++){
            File file = wavFileList.get(checkedIndexes.get(i));
            File txtFile = new File(file.getAbsolutePath().substring(0, file.getAbsolutePath().length()-3) + "txt");
            file.delete();
            txtFile.delete();
        }
        getFileList();
    }

    private void mergeSelected(){
        File wavFile1 = wavFileList.get(checkedIndexes.get(0));
        File wavFile2 = wavFileList.get(checkedIndexes.get(1));
        File txtFile = new File(wavFile2.getAbsolutePath().substring(0, wavFile2.getAbsolutePath().length()-3) + "txt");
        txtFile.delete();

        String newName = wavFile1.getAbsolutePath();
        newName = newName.substring(0,newName.length()-4)+"merged.wav";
        String oldName = wavFile1.getAbsolutePath();

        FileInputStream in1 = null, in2 = null;
        final int RECORDER_BPP=16; //8,16,32..etc
        int RECORDER_SAMPLERATE=44100; //8000,11025,16000,32000,48000,96000,44100..et
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;  //mono=1,stereo=2
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

        int bufferSize=512;
        byte[] data = new byte[bufferSize];


        try {
            in1 = new FileInputStream(wavFile1);
            in2 = new FileInputStream(wavFile2);


            out = new FileOutputStream(newName);

            totalAudioLen = in1.getChannel().size() + in2.getChannel().size();



            totalDataLen = totalAudioLen + 36;

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate,RECORDER_BPP);

            while (in1.read(data) != -1)
            {

                out.write(data);

            }
            while (in2.read(data) != -1)
            {

                out.write(data);
            }

            in1.close();
            in2.close();
            out.close();
            out.flush();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        wavFile1.delete();
        wavFile2.delete();

        File file = new File(newName);
        file.renameTo(new File(oldName));

        File newFile = new File(oldName);
        File txtFileToUpdate = new File(newFile.getAbsolutePath().substring(0, newFile.getAbsolutePath().length()-3) + "txt");
        ListItem item = new ListItem();
        try {
            FileInputStream fis = new FileInputStream(txtFileToUpdate);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            item.setAuthor(br.readLine());
            item.setTitle(br.readLine());
            item.setDescription(br.readLine());
            item.setDateAndDuration(br.readLine());
            br.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Uri uri = Uri.fromFile(newFile);
        MediaPlayer mp = MediaPlayer.create(getApplicationContext(), uri);
        int newDuration = mp.getDuration()/1000;
        String time = String.format("%02d:%02d:%02d",newDuration/3600,(newDuration%3600)/60,newDuration%60);
        String oldTime = item.getDateAndDuration();
        oldTime = time + oldTime.substring(8,oldTime.length());
        item.setDateAndDuration(oldTime);

        try {
            FileOutputStream fOut = new FileOutputStream(txtFileToUpdate);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.write(item.getAuthor()+"\n");
            myOutWriter.write(item.getTitle()+"\n");
            myOutWriter.write(item.getDescription()+"\n");
            myOutWriter.write(item.getDateAndDuration());
            myOutWriter.close();
            fOut.close();
        } catch (Exception e){}


        getFileList();
    }


    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate, int RECORDER_BPP)
            throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte)(totalDataLen & 0xff);
        header[5] = (byte)((totalDataLen >> 8) & 0xff);
        header[6] = (byte)((totalDataLen >> 16) & 0xff);
        header[7] = (byte)((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte)(longSampleRate & 0xff);
        header[25] = (byte)((longSampleRate >> 8) & 0xff);
        header[26] = (byte)((longSampleRate >> 16) & 0xff);
        header[27] = (byte)((longSampleRate >> 24) & 0xff);
        header[28] = (byte)(byteRate & 0xff);
        header[29] = (byte)((byteRate >> 8) & 0xff);
        header[30] = (byte)((byteRate >> 16) & 0xff);
        header[31] = (byte)((byteRate >> 24) & 0xff);
        header[32] = (byte)(2 * 16 / 8);
        header[33] = 0;
        header[34] = (byte) RECORDER_BPP;
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte)(totalAudioLen & 0xff);
        header[41] = (byte)((totalAudioLen >> 8) & 0xff);
        header[42] = (byte)((totalAudioLen >> 16) & 0xff);
        header[43] = (byte)((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btnPlaySelected: {
                    playSelected();
                    break;
                }
                case R.id.btnDeleteSelected: {
                    deleteSelected();
                    break;
                }
                case R.id.btnMergeSelected: {
                    mergeSelected();
                    break;
                }
                case R.id.btnRecord: {
                    Intent intent = new Intent(getApplicationContext(), RecordActivity.class);
                    startActivity(intent);
                    break;
                }
                case R.id.btnStop: {
                    stopPlaying();
                    break;
                }
            }
        }
    };
}
