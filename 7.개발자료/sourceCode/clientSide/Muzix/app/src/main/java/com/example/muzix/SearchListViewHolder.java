package com.example.muzix;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SearchListViewHolder extends RecyclerView.ViewHolder {
    ImageView album_cover;
    TextView title,genre,writer;
    RelativeLayout search_item;
    public SearchListViewHolder(@NonNull View itemView) {
        super(itemView);
        search_item = (RelativeLayout)itemView.findViewById(R.id.search_item);
        album_cover = (ImageView)itemView.findViewById(R.id.album_cover);
        title = (TextView)itemView.findViewById(R.id.title);
        genre = (TextView)itemView.findViewById(R.id.genre);
        writer = (TextView)itemView.findViewById(R.id.writer);
    }
}
