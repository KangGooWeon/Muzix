package com.example.muzix;
import android.content.Context;
import android.content.Intent;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.List;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class AutoScrollAdapter extends PagerAdapter {

    Context context;
    List<MidiFile> musicList;

    public AutoScrollAdapter(Context context, List<MidiFile> musicList) {
        this.context = context;
        this.musicList = musicList;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {

        MidiFile music = musicList.get(position);
        //뷰페이지 슬라이딩 할 레이아웃 인플레이션
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.vh_recent_music,null);
        TextView title = (TextView)v.findViewById(R.id.album_title);
        TextView writer = (TextView)v.findViewById(R.id.music_writer);
        title.setText(music.getFileName());
        writer.setText("-"+music.getWriter()+"-");
        ImageView album_cover = (ImageView) v.findViewById(R.id.album_cover);

        if(music.getAlbumCoverUri()!=null)
            Glide.with(context).load(music.getAlbumCoverUri()).into(album_cover);
        LinearLayout music_item = (LinearLayout)v.findViewById(R.id.music_item);

        music_item.setOnClickListener(new View.OnClickListener() {
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
        container.addView(v);
        return v;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {

        container.removeView((View)object);

    }

    @Override
    public int getCount() {
        return musicList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }
}
