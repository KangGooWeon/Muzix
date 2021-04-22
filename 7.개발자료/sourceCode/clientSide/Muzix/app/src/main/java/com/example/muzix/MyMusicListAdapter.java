package com.example.muzix;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by arden on 2019-03-18.
 */

public class MyMusicListAdapter extends RecyclerView.Adapter<MyMusicListViewHolder>{

    private List<MidiFile>musicList;
    private Context context;
    private Activity activity;
    private int type;
    private List<Boolean> checkedList;
    private int location = 0;

    public MyMusicListAdapter(List<MidiFile>musicList, Context context, int type){
        this.musicList = musicList;
        this.context = context;
        this.type = type; // if type ==2 show detail of music List
        checkedList = new ArrayList<>(); // this is for checked delete
        int size = musicList.size();
        for(int i = 0; i<size; i++)
            checkedList.add(false);
    }

    @Override
    public MyMusicListViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.vh_my_music_list,null);
        return new MyMusicListViewHolder(v);
    }
    public void setActivity(Activity activity){
        this.activity =activity;
    }
    @Override
    public void onBindViewHolder(MyMusicListViewHolder holder, int position) {
        MidiFile midi = musicList.get(position);

        if(midi.getAlbumCoverUri()!=null)
            Glide.with(context).load(midi.getAlbumCoverUri()).into(holder.music_art);

        if(type == 2){ //상세 나타내기
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.scope_layout.setVisibility(View.VISIBLE);
            holder.scope_layout.setVisibility(View.VISIBLE);

            holder.my_music_list_item.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    showModifyDialog(position);
                    return true;
                }
            });
        }

        holder.music_title.setText(midi.getFileName());
        holder.music_genre.setText(midi.getGenre());
        holder.modify_date_txt.setText(midi.getModify_date());
        holder.scopeSwitch.setChecked(musicList.get(position).getScope());

        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                checkedList.set(position,isChecked);
            }
        });

        holder.my_music_list_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, SheetMusicActivity.class);
                MidiFile midifile = musicList.get(position);
                intent.putExtra("title",midifile.getFileName());
                intent.putExtra("music_id",midifile.getMusic_id());
                intent.putExtra("writer",midifile.getWriter());
                intent.putExtra("genre",midifile.getGenre());
                intent.putExtra("scope",midifile.getScope());
                intent.putExtra("cover_img",midifile.getAlbumCoverUri());
                intent.putExtra("melody_csv",midifile.getMelody_csv());
                intent.putExtra("chord_txt",midifile.getChord_txt());
                intent.putExtra("modify_date",midifile.getModify_date());
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            }
        });

        holder.scopeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                new JSPServerConnector().execute(context.getString(R.string.update_music),
                        "update_type","change_scope",
                        "music_id",musicList.get(position).getMusic_id(),
                        "scope",Boolean.toString(isChecked));
            }
        });
    }

    private void showModifyDialog(int position){
        final Dialog longclick_dialog = new Dialog(context);
        longclick_dialog.setContentView(R.layout.musicitem_longclick_dialog);
        ((TextView)longclick_dialog.findViewById(R.id.longclick_title)).setText(musicList.get(position).getFileName());
        String[] arr = {"제목 수정","장르 수정","커버 사진 선택","삭제"};
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1,arr);
        ListView listView = (ListView)longclick_dialog.findViewById(R.id.ac_longclick_dialog);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i){
                    case 0:
                        longclick_dialog.dismiss();
                        final Dialog mTitledialog = new Dialog(context);
                        mTitledialog.setContentView(R.layout.modify_music_title_dialog);
                        ((EditText)mTitledialog.findViewById(R.id.edit_music_title)).setText(musicList.get(position).getFileName());
                        ((Button)mTitledialog.findViewById(R.id.confirm_modify_music_title)).setOnClickListener(new Button.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                String title = ((EditText)mTitledialog.findViewById(R.id.edit_music_title)).getText().toString();
                                if(title.replace(" ","").equals("")){
                                    errorHandle("제목을 입력하지 않았습니다.");
                                    return;
                                }
                                try {
                                    JSONObject obj = new JSPServerConnector().execute(context.getString(R.string.update_music),"update_type","change_title"
                                            ,"music_id",musicList.get(position).getMusic_id(),"title",title).get();
                                    if(obj.getString("result").equals("success")){
                                        musicList.get(position).setFilename(title);
                                        notifyDataSetChanged();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                mTitledialog.dismiss();
                            }
                        });

                        ((Button)mTitledialog.findViewById(R.id.modify_music_title_cancel)).setOnClickListener(new Button.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mTitledialog.dismiss();
                            }
                        });
                        mTitledialog.show();
                        break;
                    case 1:
                        longclick_dialog.dismiss();
                        final Dialog mGenreDialog = new Dialog(context);
                        mGenreDialog.setContentView(R.layout.modify_genre_dialog);
                        Spinner genreSpinner = (Spinner)mGenreDialog.findViewById(R.id.music_genre_spinner);

                        String[] genres = {"발라드","재즈","RnB","록","클래식"}; //이거 한글로 수정 할 방법 찾아야함
                        ArrayAdapter<String> genreAdapter = new ArrayAdapter<String>(context,R.layout.spinner_item,genres);
                        genreAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
                        genreSpinner.setAdapter(genreAdapter);
                        for(int j =0; j<genres.length; j++){
                            if(genres[j].equals(musicList.get(position).getGenre())){
                                genreSpinner.setSelection(j);
                                break;
                            }
                        }
                        ((Button)mGenreDialog.findViewById(R.id.confirm_modify_music_genre)).setOnClickListener(new Button.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                try {
                                    JSONObject obj = new JSPServerConnector().execute(context.getString(R.string.update_music),"update_type","change_genre"
                                            ,"music_id",musicList.get(position).getMusic_id(),"genre",genres[genreSpinner.getSelectedItemPosition()]).get();
                                    if(obj.getString("result").equals("success")){
                                        musicList.get(position).setGenre(genres[genreSpinner.getSelectedItemPosition()]);
                                        notifyDataSetChanged();
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                mGenreDialog.dismiss();
                            }
                        });

                        ((Button)mGenreDialog.findViewById(R.id.modify_music_genre_cancel)).setOnClickListener(new Button.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                mGenreDialog.dismiss();
                            }
                        });
                        mGenreDialog.show();
                        break;
                    case 2:
                        location = position;
                        Intent intent = new Intent();
                        intent.setType("image/*");
                        intent.setAction(Intent.ACTION_GET_CONTENT);
                        activity.startActivityForResult(intent, 1);
                        break;
                    case 3:
                        longclick_dialog.dismiss();
                        new AlertDialog.Builder(context)
                                .setTitle("노래 삭제")
                                .setMessage("노래를 삭제합니다.\n노래에 대한 정보가 모두 사라집니다. 삭제하시겠습니까?")

                                // set three option buttons
                                .setPositiveButton("삭제",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog,int whichButton) {
                                                checkedList.set(position,true);
                                                musicList = deleteItem();
                                                notifyDataSetChanged();
                                                dialog.dismiss();
                                            }
                                        })// setPositiveButton

                                .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dialog.dismiss();
                                    }
                                })// setNegativeButton
                                .create().show();
                        break;
                }
            }
        });
        longclick_dialog.show();
    }

    public int getLocation(){
        return location;
    }
    private void errorHandle(String msg){
        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(context);
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
    /*삭제 후 남은 값들을 return*/
    public List<MidiFile> deleteItem(){
        int size = checkedList.size();
        List<MidiFile> tmpList = musicList;
        boolean isChecked = false;
        for(int i =size-1; i>=0; i--) {
            if(checkedList.get(i)==true) {
                try {
                    isChecked= true;
                    JSONObject obj =new JSPServerConnector().execute(context.getString(R.string.delete_music), "music_id", musicList.get(i).getMusic_id()).get();
                    if(obj.getString("result").equals("success")){
                            tmpList.remove(i);
                    }
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        if(!isChecked){
            return null;
        }

        size = tmpList.size();
        for(int i = 0; i<size; i++)
            checkedList.add(false);
        return tmpList;
    }
    @Override
    public int getItemCount() {
        return musicList.size();
    }

    private class JSPServerConnector extends AsyncTask<String, Void, JSONObject>{

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
                    Log.e("TAG-JSPServer-error", "Type : "+type+" Connection Error!");
                }

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                con.disconnect();
            }
            return null;
        }
    }
}
