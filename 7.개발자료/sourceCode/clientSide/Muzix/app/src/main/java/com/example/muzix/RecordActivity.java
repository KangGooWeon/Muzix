package com.example.muzix;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by arden on 2019-03-19.
 */

public class RecordActivity  extends AppCompatActivity {
    private VisualizerView visualizerView;
    private ImageButton mRecordBtn, returnBtn, saveBtn, refreshBtn, mPlayBtn;
    private Button keysigBtn, tempoBtn;
    private TextView timeTxt;
    private LinearLayout metronome_layout;

    private final int mBufferSize = 1024;//2048
    private final int mBytesPerElement = 2;
    // 설정할 수 있는 sampleRate, AudioFormat, channelConfig 값들을 정의
    private final int[] mSampleRates = new int[]{44100, 22050, 11025, 8000};
    private final short[] mAudioFormats = new short[]{AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT};
    private final short[] mChannelConfigs = new short[]{AudioFormat.CHANNEL_IN_STEREO, AudioFormat.CHANNEL_IN_MONO};

    // 위의 값들 중 실제 녹음 및 재생 시 선택된 설정값들을 저장
    private int mSampleRate;
    private short mAudioFormat;
    private short mChannelConfig;
    private AudioRecord mRecorder = null;
    private Thread mRecordingThread = null;
    private boolean mIsRecording = false;   // 녹음 중인지에 대한 상태값
    private boolean isPlaying = false;
    private String mPath = "";  // 녹음한 파일을 저장할 경로

    private Handler mainHandler;

    private long startingTime;
    private long elapsedTime;
    private Runnable timerDisplay;
    private Runnable graphUpdate;
    private AudioProcessingTools.MaxAmplitude maxAmplitude;
    private static final String LOG = "Loremar_Logs";

    private int[] keyarr = {2, 3, 4, 6};
    private String[] keySigArr = {"2/4", "3/4", "4/4", "6/8", "12/8"};
    private int keysigindex = 0;
    private int[] tempoArr = {60, 90, 120, 180};
    private int emphasisindex = 0;
    private MetronomeDisplay metronomeDisPlay = null;

    private int interval = 1000;
    List<Boolean> emphasisList;
    private Bitmap album_cover = null;
    private String title;
    private String genre;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_music);

        requestRecordAudioPermission();
        requestWriteStoragePermission();
        requestReadStoragePermission();

        visualizerView = (VisualizerView) findViewById(R.id.visualizerView);
        mRecordBtn = (ImageButton) findViewById(R.id.start);
        returnBtn = (ImageButton) findViewById(R.id.btn_return);
        mPlayBtn = (ImageButton) findViewById(R.id.playBtn);
        timeTxt = (TextView) findViewById(R.id.timer_view);
        metronome_layout = (LinearLayout) findViewById(R.id.metronome_layout);
        keysigBtn = (Button) findViewById(R.id.keysigBtn);
        tempoBtn = (Button) findViewById(R.id.tempoBtn);
        refreshBtn = (ImageButton) findViewById(R.id.resetBtn);
        saveBtn = (ImageButton) findViewById(R.id.btn_create);
        keysigBtn.setText(keySigArr[keysigindex]);
        tempoBtn.setText(Integer.toString(tempoArr[0]));

        returnBtn.setOnClickListener(v -> doReturn());
        mRecordBtn.setOnClickListener(v -> doRecord());
        mPlayBtn.setOnClickListener(v -> doPlay());
        refreshBtn.setOnClickListener(v -> doReset());
        saveBtn.setOnClickListener(v -> showConfirmDialog());
        keysigBtn.setOnClickListener(v -> changeKeySignature());
        tempoBtn.setOnClickListener(v -> changeTempo());
        setEhmpaysis();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /*================================녹음 권한 받아오기================================================*/
    private void requestRecordAudioPermission() {
        //check API version, do nothing if API version < 23!
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
            }
        }
    }
    /*===============================================================================================*/

    /*===========================외부 저장소에 쓰고 읽기 권한 받아오기 ========================================*/
    private void requestWriteStoragePermission() {
        //check API version, do nothing if API version < 23!
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                }
            }
        }
    }

    private void requestReadStoragePermission() {
        //check API version, do nothing if API version < 23!
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                }
            }
        }
    }
    /*==================================================================================================================*/

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d("Activity", "Granted!");
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("Activity", "Denied!");
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void doReturn() {
        if (mIsRecording) {
            stopRecording();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mRecordBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.record_icon));
                maxAmplitude.value = 0;
            } else {
                mRecordBtn.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.record_icon));
                maxAmplitude.value = 0;
            }
            keysigBtn.setEnabled(true);
            keysigBtn.setClickable(true);
            tempoBtn.setEnabled(true);
            tempoBtn.setClickable(true);
            saveBtn.setEnabled(true);
            saveBtn.setClickable(true);
        }
        finish();
    }

    private void changeKeySignature() {
        final Dialog d = new Dialog(this);
        d.setContentView(R.layout.picker_dialog);
        d.getWindow().setGravity(Gravity.BOTTOM);
        WindowManager.LayoutParams params = d.getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        d.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        Button set = (Button) d.findViewById(R.id.setBtn);
        Button cancel = (Button) d.findViewById(R.id.cancelBtn);
        TextView num_txt = (TextView) d.findViewById(R.id.picker_txt);
        num_txt.setText("조표 변경");
        final NumberPicker nopicker = (NumberPicker) d.findViewById(R.id.picker);

        NumberPicker.Formatter formatter = new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return keySigArr[value];
            }
        };
        nopicker.setMaxValue(4);
        nopicker.setMinValue(0);
        nopicker.setFormatter(formatter);
        nopicker.setWrapSelectorWheel(false);
        nopicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                keysigindex = nopicker.getValue();
                keysigBtn.setText(keySigArr[keysigindex]);
                setEhmpaysis();
                d.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.show();
    }

    private void changeTempo() {
        final Dialog d = new Dialog(this);
        d.setContentView(R.layout.picker_dialog);
        d.getWindow().setGravity(Gravity.BOTTOM);
        WindowManager.LayoutParams params = d.getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        d.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);


        Button set = (Button) d.findViewById(R.id.setBtn);
        Button cancel = (Button) d.findViewById(R.id.cancelBtn);
        TextView num_txt = (TextView) d.findViewById(R.id.picker_txt);
        num_txt.setText("템포 변경");
        final NumberPicker nopicker = (NumberPicker) d.findViewById(R.id.picker);

        NumberPicker.Formatter formatter = new NumberPicker.Formatter() {
            @Override
            public String format(int value) {
                return Integer.toString(tempoArr[value]);
            }
        };
        nopicker.setMaxValue(3);
        nopicker.setMinValue(0);
        nopicker.setFormatter(formatter);
        nopicker.setWrapSelectorWheel(false);
        nopicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                interval = toInterval(tempoArr[nopicker.getValue()]);
                tempoBtn.setText(Integer.toString(tempoArr[nopicker.getValue()]));
                d.dismiss();
            }
        });
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.show();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (mIsRecording) {
            stopRecording();
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsRecording) {
            stopRecording();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                try {
                    // 선택한 이미지에서 비트맵 생성
                    InputStream in = getContentResolver().openInputStream(data.getData());
                    album_cover = BitmapFactory.decodeStream(in);
                    in.close();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // 녹음을 수행할 Thread를 생성하여 녹음을 수행하는 함수
    private void startRecording() {
        mRecorder = findAudioRecord();
        requestRecordAudioPermission();
        mRecorder.startRecording();
        mIsRecording = true;
        mRecordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                startingTime = SystemClock.elapsedRealtime();
                timerDisplay = new Runnable() {
                    @Override
                    public void run() {
                        elapsedTime = SystemClock.elapsedRealtime() - startingTime;
                        long elapsedTimeHours = TimeUnit.MILLISECONDS.toHours(elapsedTime);
                        long elapsedTimeMins = TimeUnit.MILLISECONDS.toMinutes(elapsedTime) - TimeUnit.HOURS.toMinutes(elapsedTimeHours);
                        long elapsedTimeSecs = TimeUnit.MILLISECONDS.toSeconds(elapsedTime) - TimeUnit.MINUTES.toSeconds(elapsedTimeMins);
                        timeTxt.setText(elapsedTimeHours + " : " + elapsedTimeMins + " : " + elapsedTimeSecs);
                        mainHandler.postDelayed(this, 1000);
                    }
                };

                mainHandler.post(timerDisplay);

                maxAmplitude = new AudioProcessingTools.MaxAmplitude(0);
                graphUpdate = new Runnable() {
                    @Override
                    public void run() {
                        visualizerView.addValuetoGraph(maxAmplitude.value);
                        maxAmplitude.value = 0;
                        visualizerView.postInvalidate();
                        mainHandler.postDelayed(this, 30);
                    }
                };
                mainHandler.post(graphUpdate);
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        mRecordingThread.start();
    }


    // 녹음을 하기 위한 sampleRate, audioFormat, channelConfig 값들을 설정
    private AudioRecord findAudioRecord() {
        for (int rate : mSampleRates) {
            for (short format : mAudioFormats) {
                for (short channel : mChannelConfigs) {
                    try {
                        int bufferSize = AudioRecord.getMinBufferSize(rate, channel, format);
                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {
                            mSampleRate = rate;
                            mAudioFormat = format;
                            mChannelConfig = channel;
                            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, mSampleRate, mChannelConfig, mAudioFormat, bufferSize);
                            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                                return recorder;    // 적당한 설정값들로 생성된 Recorder 반환
                            } else
                                recorder.release();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return null;                     // 적당한 설정값들을 찾지 못한 경우 Recorder를 찾지 못하고 null 반환
    }

    // 실제 녹음한 data를 file에 쓰는 함수
    private void writeAudioDataToFile() {
        String sd = Environment.getExternalStorageDirectory().getAbsolutePath();
        mPath = sd + "/record_audiorecord.pcm";
        short sData[] = new short[mBufferSize];
        int gain = 20;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mPath);
            while (mIsRecording) {
                int len = mRecorder.read(sData, 0, mBufferSize);
                AudioProcessingTools.readAudioApplyGain2(sData, len, mRecorder, mBufferSize, 20, maxAmplitude);
                byte bData[] = short2byte(sData);
                fos.write(bData, 0, mBufferSize * mBytesPerElement);
            }
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // short array형태의 data를 byte array형태로 변환하여 반환하는 함수
    private byte[] short2byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    /*==============================================================================================*/
    private void rawToWave(final File rawFile, final File waveFile) throws IOException {

        byte[] rawData = new byte[(int) rawFile.length()];
        DataInputStream input = null;
        try {
            input = new DataInputStream(new FileInputStream(rawFile));
            input.read(rawData);
        } finally {
            if (input != null) {
                input.close();
            }
        }

        DataOutputStream output = null;
        try {
            output = new DataOutputStream(new FileOutputStream(waveFile));
            // WAVE header
            // see http://ccrma.stanford.edu/courses/422/projects/WaveFormat/
            writeString(output, "RIFF"); // chunk id
            writeInt(output, 36 + rawData.length); // chunk size
            writeString(output, "WAVE"); // format
            writeString(output, "fmt "); // subchunk 1 id
            writeInt(output, 16); // subchunk 1 size
            writeShort(output, (short) 1); // audio format (1 = PCM)
            writeShort(output, (short) 2); // number of channels
            writeInt(output, 44100); // sample rate
            writeInt(output, mSampleRate * 2); // byte rate
            writeShort(output, (short) 2); // block align
            writeShort(output, (short) 16); // bits per sample
            writeString(output, "data"); // subchunk 2 id
            writeInt(output, rawData.length); // subchunk 2 size
            // Audio data (conversion big endian -> little endian)
            short[] shorts = new short[rawData.length / 2];
            ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
            ByteBuffer bytes = ByteBuffer.allocate(shorts.length * 2);
            for (short s : shorts) {
                bytes.putShort(s);
            }

            output.write(fullyReadFileToBytes(rawFile));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    byte[] fullyReadFileToBytes(File f) throws IOException {
        int size = (int) f.length();
        byte bytes[] = new byte[size];
        byte tmpBuff[] = new byte[size];
        FileInputStream fis = new FileInputStream(f);
        try {

            int read = fis.read(bytes, 0, size);
            if (read < size) {
                int remain = size - read;
                while (remain > 0) {
                    read = fis.read(tmpBuff, 0, remain);
                    System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
                    remain -= read;
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            fis.close();
        }

        return bytes;
    }

    private void writeInt(final DataOutputStream output, final int value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
        output.write(value >> 16);
        output.write(value >> 24);
    }

    private void writeShort(final DataOutputStream output, final short value) throws IOException {
        output.write(value >> 0);
        output.write(value >> 8);
    }

    private void writeString(final DataOutputStream output, final String value) throws IOException {
        for (int i = 0; i < value.length(); i++) {
            output.write(value.charAt(i));
        }
    }

    /*==============================================================================================*/
    // 녹음을 중지하는 함수
    private void stopRecording() {
        if (mRecorder != null) {
            mIsRecording = false;
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
            mainHandler.removeCallbacks(timerDisplay);
            mainHandler.removeCallbacks(metronomeDisPlay);
            mainHandler.removeCallbacks(graphUpdate);
            keysigBtn.setEnabled(true);
            keysigBtn.setClickable(true);
            tempoBtn.setEnabled(true);
            tempoBtn.setClickable(true);
            saveBtn.setEnabled(true);
            saveBtn.setClickable(true);
            mRecordingThread = null;
            String sd = Environment.getExternalStorageDirectory().getAbsolutePath();
            File f1 = new File(sd + "/" + "record_audiorecord.pcm"); // The location of your PCM file
            File f2 = new File(sd + "/" + "record_audiorecord.wav"); // The location where you want your WAV file
            try {
                rawToWave(f1, f2);
            } catch (IOException e) {
                e.printStackTrace();
            }
            f1.delete();
        }
    }

    private void doPlay() {
        // 녹음 파일이 없는 상태에서 재생 버튼 클릭 시, 우선 녹음부터 하도록 Toast 표시
        if (mPath.length() == 0 || mIsRecording) {
            Toast.makeText(RecordActivity.this, "Please record, first.", Toast.LENGTH_SHORT).show();
            return;
        }
// 녹음된 파일이 있는 경우 해당 파일 재생
        playWaveFile();
    }

    private void doReset() {
        if (mIsRecording) {
            stopRecording();
        }
        emphasisindex = 0;
        timeTxt.setText("0 : 0 : 0");
        refreshBtn.setVisibility(View.GONE);
        mPlayBtn.setVisibility(View.GONE);
        mainHandler.removeCallbacks(timerDisplay);
        mainHandler.removeCallbacks(metronomeDisPlay);
        mainHandler.removeCallbacks(graphUpdate);
        visualizerView.resetCanvas();
        setEhmpaysis();
        emphasisindex = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mRecordBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.record_icon));
            maxAmplitude.value = 0;
        } else {
            mRecordBtn.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.record_icon));
            maxAmplitude.value = 0;
        }
    }

    // 녹음할 때 설정했던 값과 동일한 설정값들로 해당 파일을 재생하는 함수
    private void playWaveFile() {
        int minBufferSize = AudioTrack.getMinBufferSize(mSampleRate, mChannelConfig, mAudioFormat);
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, mSampleRate, mChannelConfig, mAudioFormat, minBufferSize, AudioTrack.MODE_STREAM);
        int count = 0;
        byte[] data = new byte[mBufferSize];

        try {
            FileInputStream fis = new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/record_audiorecord.wav");
            DataInputStream dis = new DataInputStream(fis);
            audioTrack.play();

            while ((count = dis.read(data, 0, mBufferSize)) > -1) {
                audioTrack.write(data, 0, count);
            }

            audioTrack.stop();
            audioTrack.release();
            dis.close();
            fis.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void setEhmpaysis() {
        metronome_layout.removeAllViews();
        emphasisList = new ArrayList<>();
        int nominator = Integer.parseInt(keySigArr[keysigindex].split("/")[0]);
        for (int i = 0; i < nominator; i++) {
            EmphasisSwitch emphasisSwitch = new EmphasisSwitch(this);
            emphasisSwitch.setChecked(false);
            emphasisList.add(false);
            emphasisSwitch.setLayoutParams(new LinearLayout.LayoutParams(ConversionUtils.getPixelsFromDp(30), ConversionUtils.getPixelsFromDp(30)));
            metronome_layout.addView(emphasisSwitch);
        }
    }

    private int toInterval(long bpm) {
        return (int) (60000 / bpm);
    }

    private void doRecord() {
        if (mIsRecording == false) {
            keysigBtn.setEnabled(false);
            keysigBtn.setClickable(false);
            tempoBtn.setEnabled(false);
            tempoBtn.setClickable(false);
            saveBtn.setEnabled(false);
            saveBtn.setClickable(false);
            startRecording();
            mIsRecording = true;
            String[] keySig = keySigArr[keysigindex].split("/");

            metronomeDisPlay = new MetronomeDisplay(Integer.parseInt(keySig[0]), Integer.parseInt(keySig[1]));
            emphasisindex = 0;
            mainHandler.post(metronomeDisPlay);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mRecordBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.stop_icon));
            } else {
                mRecordBtn.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.stop_icon));
            }
        } else {
            stopRecording();
            mIsRecording = false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mRecordBtn.setBackground(ContextCompat.getDrawable(this, R.drawable.record_icon));
                maxAmplitude.value = 0;
            } else {
                mRecordBtn.setBackgroundDrawable(ContextCompat.getDrawable(this, R.drawable.record_icon));
                maxAmplitude.value = 0;
            }
            mPlayBtn.setVisibility(View.VISIBLE);
        }
        refreshBtn.setVisibility(View.VISIBLE);
    }

    private void errorHandle(String msg) {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle("오류");
        builder.setMessage(msg);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        builder.show();
    }

    private void showConfirmDialog() {
        LayoutInflater inflator = LayoutInflater.from(this);
        final View dialogView = inflator.inflate(R.layout.save_music_dialog, null);
        final EditText filenameView = (EditText) dialogView.findViewById(R.id.save_music_filename);
        final Spinner genre_spinner = (Spinner)dialogView.findViewById(R.id.genre_spinner);

        String[] genres = {"발라드","재즈","RnB","록","클래식"};
        ArrayAdapter<String> genreAdapter = new ArrayAdapter<String>(this,R.layout.spinner_item,genres);
        genreAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
        genre_spinner.setAdapter(genreAdapter);

        genre_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                genre = genres[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        //builder.setTitle("노래 생성");
        builder.setView(dialogView);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface builder, int whichButton) {
                String fname = filenameView.getText().toString();
                if (fname.replace(" ", "").equals(""))
                    errorHandle("제목을 설정해주세요.");
                else {
                    title = fname;
                    SharedPreferences pref = getSharedPreferences("MuzixAutoLogin", Activity.MODE_PRIVATE);
                    String writer_id = pref.getString("MUZIXID", null);
                    File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+"record_audiorecord.wav");
                    new FileUploader(file).execute("http://54.180.95.158:8080/server/fileupload.jsp", writer_id);
                    builder.dismiss();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface builder, int whichButton) {
                builder.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    class MetronomeDisplay implements Runnable {

        private int numerator;
        private int accent;
        private int denominator;
        private Paint accentPaint;
        private Paint normalPaint;
        private Paint paint;

        public MetronomeDisplay(int numerator, int denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
            interval = toInterval(Integer.parseInt(tempoBtn.getText().toString()));
            if (denominator == 8) {
                interval /= 2;
                accent = 3;
            } else if (denominator == 4) {
                accent = numerator;
            }

            accentPaint = new Paint();
            accentPaint.setColor(Color.RED);
            accentPaint.setStyle(Paint.Style.FILL);
            accentPaint.setStrokeWidth(2);
            accentPaint.setAntiAlias(true);

            normalPaint = new Paint();
            normalPaint.setColor(Color.RED);
            normalPaint.setStyle(Paint.Style.STROKE);
            normalPaint.setStrokeWidth(2);
            normalPaint.setAntiAlias(true);

            paint = new Paint();
            paint.setColor(Color.BLACK);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2);
            paint.setAntiAlias(true);
        }

        @Override
        public void run() {
            if (mIsRecording) {
                mainHandler.postDelayed(this, interval);

                if (emphasisindex >= emphasisList.size())
                    emphasisindex = 0;
                boolean isEmphasis = emphasisList.get(emphasisindex);

                for (int i = 0; i < metronome_layout.getChildCount(); i++) {
                    if (i == emphasisindex) {
                        if (emphasisindex % accent == 0) {
                            ((EmphasisSwitch) metronome_layout.getChildAt(i)).setPaint(accentPaint);
                        } else {
                            ((EmphasisSwitch) metronome_layout.getChildAt(i)).setPaint(normalPaint);
                        }
                        emphasisList.set(i, !isEmphasis);
                    } else
                        ((EmphasisSwitch) metronome_layout.getChildAt(i)).setPaint(paint);
                }
                emphasisindex++;
            }
        }
    }

    class PythonServerConnector extends AsyncTask<Void, Void, String> {
        private String serverIP = "54.180.95.158";

        private int serverPort = 8011;

        private String sd;
        private String filename;
        private InetAddress serverAddr;
        private Socket sock;
        private String keysig;
        private String tempo;
        private String member_id;
        private PrintWriter printwriter;
        private BufferedReader bufferedReader;

        ProgressDialog asyncDialog = new ProgressDialog(
                RecordActivity.this);

        private int type = 0;

        public PythonServerConnector(String keysig, String tempo, String member_id) {
            filename = "record_audiorecord";
            this.keysig = keysig;
            this.tempo = tempo;
            this.member_id = member_id;
            sd = Environment.getExternalStorageDirectory().getAbsolutePath();
        }

        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("코드를 추출중입니다...");

            // show dialog
            asyncDialog.show();
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                sock = new Socket(serverIP, serverPort); // Creating the server socket.


                if (sock != null) {

                    //자동 flushing 기능이 있는 PrintWriter 객체를 생성한다.

                    //client.getOutputStream() 서버에 출력하기 위한 스트림을 얻는다.

                    printwriter = new PrintWriter(sock.getOutputStream(), true);

                    InputStreamReader isr = new InputStreamReader(sock.getInputStream());

                    printwriter.write("type" + "\n" + "create" + "\n");
                    printwriter.flush();

                    //입력 스트림 inputStreamReader에 대해 기본 크기의 버퍼를 갖는 객체를 생성한다.

                    bufferedReader = new BufferedReader(isr);

                    while (true) {
                        try {
                            //스트림으로부터 읽어올수 있으면 true를 반환한다.
                            if (bufferedReader.ready()) {
                                //'\n', '\r'을 만날 때까지 읽어온다.(한줄을 읽어온다.)
                                String message = bufferedReader.readLine();
                                if(message.equals("getType")){
                                    printwriter.write("member_id" + "\n" + member_id + "\n");
                                    printwriter.flush();
                                    publishProgress(null);
                                    continue;
                                }
                                else if (message.equals("getMemberId")) {
                                    printwriter.write("key_sig" + "\n" + keysig + "\n");
                                    printwriter.flush();
                                    publishProgress(null);
                                    continue;
                                } else if (message.equals("getKeySig")) {
                                    printwriter.write("tempo" + "\n" + tempo + "\n");
                                    printwriter.flush();
                                    publishProgress(null);
                                    continue;
                                } else if (message.equals("getTempo")) {
                                    printwriter.write("filename" + "\n" + filename + "\n");
                                    printwriter.flush();
                                    publishProgress(null);
                                    continue;
                                } else if (message.equals("getFilename")) {
                                    /*DataOutputStream dos = new
                                            DataOutputStream(sock.getOutputStream());
                                    filesend(dos, filename);*/
                                    continue;
                                }else if(message.equals("generated")){
                                    bufferedReader.close();
                                    isr.close();
                                    printwriter.close();
                                    sock.close();
                                    return "success";
                                }
                            }
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    errorHandle("서버 연결을 실패했습니다.");
                }
            } catch (UnknownHostException e) {
                errorHandle("서버 연결을 실패했습니다.");
                e.printStackTrace();
            } catch (IOException e) {
                errorHandle("서버 연결을 실패했습니다.");
                e.printStackTrace();

            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s.equals("success")) {
                new JSPServerConnector().execute(getString(R.string.insert_music),"member_id",member_id,"title",title,"genre",genre);
            } else
                errorHandle("파일 불러오기를 실패했습니다.");
        }
    }

    private class FileUploader extends AsyncTask<String, Void, JSONObject> {
        File file;
        ProgressDialog asyncDialog = new ProgressDialog(
                RecordActivity.this);

        public FileUploader(File file) {
            this.file = file;
        }

        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("파일을 보내는 중입니다...");

            // show dialog
            asyncDialog.show();
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(String... strings) {
            String boundary = "^-----^";
            String LINE_FEED = "\r\n";
            String charset = "UTF-8";
            OutputStream outputStream;
            PrintWriter writer;

            JSONObject result = null;
            try {

                URL url = new URL(strings[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestProperty("Content-Type", "multipart/form-data;charset=utf-8;boundary=" + boundary);
                connection.setRequestMethod("POST");
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setConnectTimeout(15000);

                outputStream = connection.getOutputStream();
                writer = new PrintWriter(new OutputStreamWriter(outputStream, charset), true);

                /** Body에 데이터를 넣어줘야 할경우 없으면 Pass **/
                writer.append("--" + boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"writer_id\"").append(LINE_FEED);
                writer.append("Content-Type: text/plain; charset=" + charset).append(LINE_FEED);
                writer.append(LINE_FEED);
                writer.append(strings[1]).append(LINE_FEED);
                writer.flush();

                /** 파일 데이터를 넣는 부분**/
                writer.append("--" + boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"").append(LINE_FEED);
                writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(file.getName())).append(LINE_FEED);
                writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
                writer.append(LINE_FEED);
                writer.flush();

                FileInputStream inputStream = new FileInputStream(file);
                byte[] buffer = new byte[(int) file.length()];
                int bytesRead = -1;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                inputStream.close();
                writer.append(LINE_FEED);
                writer.flush();

                writer.append("--" + boundary + "--").append(LINE_FEED);
                writer.close();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    try {
                        result = new JSONObject(response.toString());

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    String inputLine;
                    StringBuffer response = new StringBuffer();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    result = new JSONObject(response.toString());
                }

            } catch (ConnectException e) {
                Log.e("Error : ", "ConnectException");
                e.printStackTrace();


            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
            try {
                JSONObject json_data = jsonObject;
                if (json_data.getString("result").equals("success")) {
                    SharedPreferences pref = getSharedPreferences("MuzixAutoLogin", Activity.MODE_PRIVATE);
                    String writer_id = pref.getString("MUZIXID", null);
                    new PythonServerConnector(keySigArr[keysigindex], tempoBtn.getText().toString(), writer_id).execute();
                } else {
                    errorHandle("네트워크 오류");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
    class JSPServerConnector extends AsyncTask<String, Void,JSONObject>{

        ProgressDialog asyncDialog = new ProgressDialog(
                RecordActivity.this);
        @Override
        protected void onPreExecute() {
            asyncDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            asyncDialog.setMessage("파일을 불러오는 중입니다...");

            asyncDialog.show();
            super.onPreExecute();
        }

        @Override
        protected JSONObject doInBackground(String... strings) {
            HttpURLConnection con = null;
            try {
                URL myurl = new URL(strings[0]);
                con = (HttpURLConnection) myurl.openConnection();
                con.setDefaultUseCaches(false);
                con.setDoInput(true);                         // 서버에서 읽기 모드 지정
                con.setDoOutput(true);                       // 서버로 쓰기 모드 지정
                con.setRequestMethod("POST");
                con.setRequestProperty("content-type", "application/x-www-form-urlencoded; charset=UTF-8");
                StringBuffer buffer = new StringBuffer();

                for(int i =1; i<strings.length;i+=2){
                    buffer.append(strings[i]).append("=").append(strings[i+1]);
                    if(i < strings.length-2)
                        buffer.append("&");
                }

                PrintWriter pw = new PrintWriter(new OutputStreamWriter(con.getOutputStream(), "UTF-8"));
                pw.write(buffer.toString());
                pw.flush();

                int response = con.getResponseCode();
                if (response >=200 && response <=300) {
                    StringBuilder builder = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(con.getInputStream(),"UTF-8"))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            builder.append(line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return new JSONObject(builder.toString());
                } else {
                    errorHandle("서버 연결을 실패했습니다.");
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                con.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
            JSONObject json_data = jsonObject;
            try {
                if (json_data.getString("result").equals("success")) {
                    Intent intent = new Intent(RecordActivity.this, SheetMusicActivity.class);
                    intent.putExtra("title", json_data.getString("title"));
                    intent.putExtra("music_id", json_data.getString("music_id"));
                    intent.putExtra("writer", json_data.getString("writer_id"));
                    intent.putExtra("genre", json_data.getString("genre"));
                    intent.putExtra("scope", json_data.getBoolean("scope"));
                    intent.putExtra("cover_img", json_data.getString("cover_img"));
                    intent.putExtra("melody_csv", json_data.getString("melody_csv"));
                    intent.putExtra("chord_txt", json_data.getString("chord_txt"));
                    intent.putExtra("modify_date", json_data.getString("modify_date"));
                    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    RecordActivity.this.startActivity(intent);
                    RecordActivity.this.finish();
                }
            }catch (Exception e){
                errorHandle("노래 생성을 실패했습니다.");
                e.printStackTrace();
            }
        }
    }
}
