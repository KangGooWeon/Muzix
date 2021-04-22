package com.example.muzix;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.muzix.csvutil.CSVWrite;
import com.example.muzix.meta.Tempo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/*@Class ModifyNavigation
* this class for update music*/
public class ModifyNavigation extends RelativeLayout {

    private Activity activity;
    private MidiFile midiFile;
    private ImageButton btn_confrim;
    private TextView title;
    private String music_id;
    public ModifyNavigation(Activity activity,MidiFile midiFile,String title,String music_id) {
        super(activity);
        inflate(activity, R.layout.nav_modify_sheet, this);
        btn_confrim = findViewById(R.id.btn_confirm);
        this.title = findViewById(R.id.musicTitle);
        btn_confrim.setOnClickListener(v->confirmModify());

        this.activity = activity;
        this.midiFile = midiFile;
        this.music_id = music_id;
        this.title.setText(title);
    }

    private void confirmModify() {
        /*멜로디를 CSV로 만들어서 */
        midiFile = ((ModifySheetMusicActivity) ModifySheetMusicActivity.modifyContext).getMidifile();
        ArrayList<MidiTrack> tracks = midiFile.getTracks();
        //"",key_sig,tempo,pitch,velocity,start_time,duration
        List<String[]> csv_data = new ArrayList<String[]>();
        csv_data.add(new String[] {"","key_sig","tempo","pitch","velocity","start_time","duration"});
        TimeSignature ts = midiFile.getTime();

        int ts_tempo = ts.getTempo();
        while(true) {
            if(0<=ts_tempo&& ts_tempo<=300)
                break;
            ts_tempo = 60000000 / ts_tempo;
        }
        String tempo = Integer.toString(ts_tempo);
        String key_sig = ts.getNumerator()+"/"+ts.getDenominator();
        for(int i =0; i< tracks.size(); i++){
            ArrayList<MidiNote> notes = tracks.get(i).getNotes();
            for(int j=0;j<notes.size(); j++){
                csv_data.add(new String[]{Integer.toString(j),key_sig,tempo,
                        Integer.toString(notes.get(j).getNumber()),"100",
                        Integer.toString(notes.get(j).getStartTime()),
                        Integer.toString(notes.get(j).getDuration())});
            }
        }
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+music_id+".csv";
        File f = new File(path);
        if(f.exists())
            f.delete();
        CSVWrite csvWrite = new CSVWrite(path);
        csvWrite.writeCsv(csv_data);
        f= new File(path);
        new FileUploader(f).execute(activity.getString(R.string.update_file));
        if(((ModifySheetMusicActivity)ModifySheetMusicActivity.modifyContext).IsChangedChord()){
            path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/chord.txt";
            String chord_path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+music_id+".txt";
            FileInputStream fis = null;
            FileOutputStream fos = null;

            try {
                fis = new FileInputStream(path);                             // 원본파일
                fos = new FileOutputStream(chord_path);   // 복사위치

                byte[] buffer = new byte[1024];
                int readcount = 0;

                while((readcount=fis.read(buffer)) != -1) {
                    fos.write(buffer, 0, readcount);    // 파일 복사
                }
                fis.close();
                fos.close();
            } catch(Exception e) {
                e.printStackTrace();
            }

            f = new File(chord_path);
            new FileUploader(f).execute(activity.getString(R.string.update_file));
        }
    }

    private void errorHandle(String msg) {
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(activity);
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

    private class FileUploader extends AsyncTask<String, Void, JSONObject> {
        File file;
        String writer_id;
        ProgressDialog asyncDialog = new ProgressDialog(
                activity);

        public FileUploader(File file) {
            this.file = file;
            SharedPreferences pref = activity.getSharedPreferences("MuzixAutoLogin", Activity.MODE_PRIVATE);
            writer_id = pref.getString("MUZIXID", null);
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
                writer.append(writer_id).append(LINE_FEED);
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
                    Intent intent = new Intent();
                    intent.putExtra("title",title.getText().toString());
                    intent.putExtra("melody_csv","http://54.180.95.158:8080/server/filestorage/"+writer_id+"/"+music_id+".csv");
                    intent.putExtra("chord_txt","http://54.180.95.158:8080/server/filestorage/"+writer_id+"/"+music_id+".txt");
                    intent.putExtra("music_id",music_id);
                    intent.putExtra("writer",writer_id);
                    activity.setResult(Activity.RESULT_OK,intent);
                    activity.finish();
                } else {
                    errorHandle("네트워크 오류");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
