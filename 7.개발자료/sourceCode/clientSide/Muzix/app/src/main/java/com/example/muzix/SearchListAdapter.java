package com.example.muzix;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.util.List;

public class SearchListAdapter extends RecyclerView.Adapter<SearchListViewHolder>{
    private List<MidiFile> musicList;
    private Context context;

    public SearchListAdapter(Context context, List<MidiFile> musicList){
        this.context = context;
        this.musicList = musicList;
    }

    @NonNull
    @Override
    public SearchListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.vh_search_list,null);
        return new SearchListViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SearchListViewHolder holder, int position) {
        MidiFile music = musicList.get(position);

        holder.title.setText(music.getFileName());
        holder.genre.setText(music.getGenre());
        holder.writer.setText(music.getWriter());
        if(music.getAlbumCoverUri()!=null)
            Glide.with(context).load(music.getAlbumCoverUri()).into(holder.album_cover);

        holder.search_item.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, SheetMusicActivity.class);
                intent.putExtra("title",music.getFileName());
                intent.putExtra("music_id",music.getMusic_id());
                intent.putExtra("writer",music.getWriter());
                intent.putExtra("genre",music.getGenre());
                intent.putExtra("scope",music.getScope());
                intent.putExtra("cover_img",music.getAlbumCoverUri());
                intent.putExtra("melody_csv",music.getMelody_csv());
                intent.putExtra("chord_txt",music.getChord_txt());
                intent.putExtra("modify_date",music.getModify_date());
                intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return musicList.size();
    }
}
