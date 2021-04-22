package com.example.muzix;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Created by arden on 2019-03-18.
 */

public class MyMusicListViewHolder extends RecyclerView.ViewHolder {
    RelativeLayout my_music_list_item;
    CheckBox checkBox;
    ImageView music_art;
    TextView music_title,music_genre,modify_date_txt;
    LinearLayout scope_layout;
    Switch scopeSwitch;
    public MyMusicListViewHolder(View itemView) {
        super(itemView);
        my_music_list_item = (RelativeLayout)itemView.findViewById(R.id.my_music_list_item);
        checkBox = (CheckBox)itemView.findViewById(R.id.checkbox);
        music_art = (ImageView)itemView.findViewById(R.id.music_art);
        music_title = (TextView)itemView.findViewById(R.id.music_title);
        music_genre = (TextView)itemView.findViewById(R.id.music_genre);
        modify_date_txt = (TextView)itemView.findViewById(R.id.modify_date_txt);
        scope_layout = (LinearLayout)itemView.findViewById(R.id.scope_layout);
        scopeSwitch = (Switch)itemView.findViewById(R.id.scope_switch);

    }
}
