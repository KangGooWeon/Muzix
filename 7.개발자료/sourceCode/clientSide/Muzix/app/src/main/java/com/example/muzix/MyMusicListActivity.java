package com.example.muzix;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


/*show detail of user music list */
public class MyMusicListActivity extends AppCompatActivity{
    private ImageButton returnBtn;
    private LinearLayout addItembtn;
    private ImageButton deleteBtn;
    private TextView no_item_txt;
    private RecyclerView myMusicListView;

    MyMusicListAdapter myMusicListAdapter = null;
    List<MidiFile> myMusicList = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_musiclist_layout);
        Intent intent = getIntent();
        myMusicList = (List<MidiFile>) intent.getExtras().getSerializable("musicList");
        returnBtn = (ImageButton)findViewById(R.id.btn_return);
        deleteBtn = (ImageButton)findViewById(R.id.deleteBtn);
        addItembtn = (LinearLayout)findViewById(R.id.addView);
        no_item_txt = (TextView)findViewById(R.id.no_item_txt);
        myMusicListView = (RecyclerView)findViewById(R.id.my_music_list);


        if(myMusicList.size()!=0)
            no_item_txt.setVisibility(View.GONE);

        returnBtn.setOnClickListener(v->doReturn());
        deleteBtn.setOnClickListener(v->doDeleteItem());
        addItembtn.setOnClickListener(v->doAddItem());

        LinearLayoutManager my_music_layoutmanager = new LinearLayoutManager(this);
        my_music_layoutmanager.setOrientation(LinearLayoutManager.VERTICAL);
        myMusicListView.setHasFixedSize(true);
        myMusicListView.setLayoutManager(my_music_layoutmanager);

        myMusicListAdapter = new MyMusicListAdapter(myMusicList,this,2);
        myMusicListAdapter.setActivity(this);
        myMusicListView.setAdapter(myMusicListAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                try {
                    Uri fileUri = data.getData();

                    File file = new File(getRealPathFromURI(fileUri));
                    new JSPServerConnector(file).execute(getString(R.string.update_cover_img),
                            myMusicList.get(myMusicListAdapter.getLocation()).getMusic_id(),
                            myMusicList.get(myMusicListAdapter.getLocation()).getWriter());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doReturn(){
        finish();
    }

    private void doAddItem(){
        Intent intent = new Intent(this,RecordActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }

    private void doDeleteItem(){
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(this);
        builder.setTitle("노래 삭제");
        builder.setMessage("선택한 노래를 삭제합니다.\n삭제된 노래는 복구가 되지 않습니다.\n정말로 삭제하시겠습니까?");
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                List<MidiFile>tmpList = myMusicListAdapter.deleteItem();
                if(tmpList==null)
                    errorHandle("삭제할 음악이 없습니다.");
                else {
                    myMusicList = tmpList;
                    myMusicListAdapter.notifyDataSetChanged();
                }
                dialogInterface.dismiss();
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    public void errorHandle(String msg){
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

    private String getRealPathFromURI(Uri contentUri) {
        if (contentUri.getPath().startsWith("/storage")) {
            return contentUri.getPath();
        }
        String path_id = DocumentsContract.getDocumentId(contentUri).split(":")[1];
        String[] columns = { MediaStore.Files.FileColumns.DATA };
        String selection = MediaStore.Files.FileColumns._ID + " = " + path_id;
        Cursor cursor = getContentResolver().query(MediaStore.Files.getContentUri("external"), columns, selection, null, null);
        try {
            int columnIndex = cursor.getColumnIndex(columns[0]);
            if (cursor.moveToFirst()) {
                return cursor.getString(columnIndex);
            }
        } finally {
            cursor.close();
        } return null;
    }


    private class JSPServerConnector extends AsyncTask<String, Void, JSONObject> {
        File file;
        public JSPServerConnector(File file){
            this.file= file;
        }
        @Override
        protected JSONObject doInBackground(String... strings) {
            String boundary = "^-----^";
            String LINE_FEED = "\r\n";
            String charset = "UTF-8";
            OutputStream outputStream;
            PrintWriter writer;

            JSONObject result = null;
            try{

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
                writer.append("Content-Disposition: form-data; name=\"music_id\"").append(LINE_FEED);
                writer.append("Content-Type: text/plain; charset=" + charset).append(LINE_FEED);
                writer.append(LINE_FEED);
                writer.append(strings[1]).append(LINE_FEED);
                writer.flush();
                writer.append("--" + boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"writer_id\"").append(LINE_FEED);
                writer.append("Content-Type: text/plain; charset=" + charset).append(LINE_FEED);
                writer.append(LINE_FEED);
                writer.append(strings[2]).append(LINE_FEED);
                writer.flush();

                /** 파일 데이터를 넣는 부분**/
                writer.append("--" + boundary).append(LINE_FEED);
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"").append(LINE_FEED);
                writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(file.getName())).append(LINE_FEED);
                writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
                writer.append(LINE_FEED);
                writer.flush();

                FileInputStream inputStream = new FileInputStream(file);
                byte[] buffer = new byte[(int)file.length()];
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


            } catch (Exception e){
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
            try {
                JSONObject json_data = jsonObject;
                if(json_data.getString("result").equals("success")) {
                    myMusicList.get(myMusicListAdapter.getLocation()).setAlbumCoverUri(json_data.getString("cover_img"));
                    myMusicListAdapter.notifyDataSetChanged();
                }else{
                    errorHandle("네트워크 오류");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }
}
