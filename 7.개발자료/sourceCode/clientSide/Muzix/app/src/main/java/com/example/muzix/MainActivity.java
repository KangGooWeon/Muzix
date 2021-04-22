package com.example.muzix;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cn.trinea.android.view.autoscrollviewpager.AutoScrollViewPager;

/*==================================================================================================*/
/*==================================================================================================*/
/*                                                                                                  */
/*                            Muzix application Android Source Code                                 */
/*                            created by KKH                                                        */
/*                            Last Modify Date : 2019.05.26                                         */
/*                            Version 0.0.1                                                         */
/*                            email : ardente6320@naver.com                                         */
/*                                                                                                  */
/*        when you use the app you can record your own Melody and can make your own lead sheets     */
/*        you can use your voice(not low voice, middle or high) and piano, guitar                   */
/*        electronic bass, electronic guitar if you find anther instrument you can make             */
/*        Please contact to me and I will updated                                                   */
/*==================================================================================================*/
/*==================================================================================================*/

/*MainActivity Class
* when you login this activity will be main home View
* you can use toolbar to search other user's music(only title)
* you can see latest modified music
* you can see your own music list*/

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private Toolbar toolbar;    /*Toolbar menu*/
    private AutoScrollViewPager home_recent_music_list; /*AutoScrolling the latest modified music*/
    private RecyclerView search_music_list,home_my_music_list; /*searching list and your music_list View*/
    private TextView nicknameView,idView,no_recent_music_txt,no_my_music_txt;

    /*==================================================================================================*/
    /*if you start search this view will be gone and if you stop search it will be visible*/
    private TextView my_music_list_txtview, recent_music_txtview;
    private View view_line;
    /*==================================================================================================*/

    private FloatingActionButton add_music_fab;
    private ImageView title_logo;
    private SearchView searchView;

    private List<MidiFile> recent_music_list,my_music_list,search_list;
    private MyMusicListAdapter myMusicListAdapter;
    private NavigationView navigationView;
    private String id,nickname;
    private boolean isKakao= false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*=============================================*/
        /*          검색시 숨겨지거나 다시 나타나야할 것들 */
        /*=============================================*/
        recent_music_txtview = (TextView)findViewById(R.id.recent_music_txtview);
        my_music_list_txtview = (TextView)findViewById(R.id.my_music_list_txtview);
        view_line = (View)findViewById(R.id.view1);
        title_logo = (ImageView)findViewById(R.id.title_logo);

        /*========HomeView의 리스트 내역 초기화===========*/
        search_list = new ArrayList<MidiFile>();
        recent_music_list = new ArrayList<MidiFile>();
        my_music_list = new ArrayList<MidiFile>();
        /*===========================================*/

        search_music_list = (RecyclerView)findViewById(R.id.search_list);
        home_recent_music_list = (AutoScrollViewPager) findViewById(R.id.home_recent_music_list);
        home_my_music_list = (RecyclerView)findViewById(R.id.home_my_music_list);
        no_my_music_txt = (TextView)findViewById(R.id.no_my_music_txt);
        no_recent_music_txt = (TextView)findViewById(R.id.no_recent_music_txt);

        /*================RecyclerView Setting==========*/
        /*My_Music_List -> Vertical                     */
        LinearLayoutManager search_list_layoutmanager = new LinearLayoutManager(this);
        search_list_layoutmanager.setOrientation(LinearLayoutManager.VERTICAL);
        search_music_list.setHasFixedSize(true);
        search_music_list.setLayoutManager(search_list_layoutmanager);

        LinearLayoutManager my_music_list_layoutmanager = new LinearLayoutManager(this);
        my_music_list_layoutmanager.setOrientation(LinearLayoutManager.VERTICAL);
        home_my_music_list.setHasFixedSize(true);
        home_my_music_list.setLayoutManager(my_music_list_layoutmanager);
        home_my_music_list.setNestedScrollingEnabled(false);



        /*==============================================*/

        /*FloatingActionButton -> 노래 추가 버튼 */
        add_music_fab = (FloatingActionButton) findViewById(R.id.fab_add_music);
        add_music_fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this,RecordActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                MainActivity.this.startActivity(intent);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        /*===========================================================================*/
        /*                  set navigation                                           */
        /*===========================================================================*/
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        /*===========================================================================*/
    }
   /*============================================================================================*/
    @Override
    protected void onResume() {
        super.onResume();
        /*==============ID, nikckname 값 가져오기============================*/
        SharedPreferences pref = this.getSharedPreferences("MuzixAutoLogin", Activity.MODE_PRIVATE);
        id = pref.getString("MUZIXID", null);
        nickname = pref.getString("NICKNAME",null);
        isKakao = pref.getBoolean("ISKAKAO",false);

        View headerView = navigationView.getHeaderView(0);
        idView = (TextView)headerView.findViewById(R.id.idView);
        nicknameView = (TextView)headerView.findViewById(R.id.nicknameView);
        idView.setText(id);
        nicknameView.setText(nickname);
        if(isKakao) {
            Menu menu = navigationView.getMenu();
            MenuItem modify_menu = (MenuItem) menu.findItem(R.id.nav_member_info);
            modify_menu.setVisible(false);
        }

        new JSPServerConnector(1).execute(getString(R.string.select_music),"member_id",id);
        new JSPServerConnector(2).execute(getString(R.string.search_music),"member_id",id,"keyword","N");
        /*=====================================================================*/
    }

    /*if you click back button this  function will be start*/
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (!searchView.isIconified()) {
            title_logo.setVisibility(View.VISIBLE);
            add_music_fab.show();
            recent_music_txtview.setVisibility(View.VISIBLE);
            my_music_list_txtview.setVisibility(View.VISIBLE);
            view_line.setVisibility(View.VISIBLE);
            if(my_music_list.size()==0)
                no_my_music_txt.setVisibility(View.VISIBLE);
            else
                home_my_music_list.setVisibility(View.VISIBLE);
            if(recent_music_list.size()==0)
                no_recent_music_txt.setVisibility(View.VISIBLE);
            else
                home_recent_music_list.setVisibility(View.VISIBLE);

            search_list.clear();
            search_music_list.setVisibility(View.GONE);
            searchView.onActionViewCollapsed();
        }else {
            super.onBackPressed();
        }
    }

    /*this function is created for menu*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        searchView = (SearchView)toolbar.getMenu().findItem(R.id.menu_search).getActionView();
        searchView.setQueryHint("노래 제목...");

        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() { //if click 'X' button this will be start
                title_logo.setVisibility(View.VISIBLE);
                add_music_fab.show();
                recent_music_txtview.setVisibility(View.VISIBLE);
                my_music_list_txtview.setVisibility(View.VISIBLE);
                view_line.setVisibility(View.VISIBLE);
                if(my_music_list.size()==0)
                    no_my_music_txt.setVisibility(View.VISIBLE);
                else
                    home_my_music_list.setVisibility(View.VISIBLE);
                if(recent_music_list.size()==0)
                    no_recent_music_txt.setVisibility(View.VISIBLE);
                else
                    home_recent_music_list.setVisibility(View.VISIBLE);

                search_list.clear();
                search_music_list.setVisibility(View.GONE);

                if (!searchView.isIconified()) {
                    searchView.onActionViewCollapsed();
                }
                searchView.clearFocus();
                return true;
            }
        });
        searchView.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { //if you start searching
                title_logo.setVisibility(View.GONE);
                add_music_fab.hide();
                recent_music_txtview.setVisibility(View.GONE);
                my_music_list_txtview.setVisibility(View.GONE);
                view_line.setVisibility(View.GONE);
                if(my_music_list.size()==0)
                    no_my_music_txt.setVisibility(View.GONE);
                else
                    home_my_music_list.setVisibility(View.GONE);
                if(recent_music_list.size()==0)
                    no_recent_music_txt.setVisibility(View.GONE);
                else
                    home_recent_music_list.setVisibility(View.GONE);

                search_list.clear();
                search_music_list.setVisibility(View.VISIBLE);
                new JSPServerConnector(3).execute(getString(R.string.search_music),"member_id",id,"keyword","N");
            }
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                new JSPServerConnector(3).execute(getString(R.string.search_music),"member_id",id,"keyword",s);
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_music_list) {
            Intent intent = new Intent(this, MyMusicListActivity.class);
            intent.putExtra("musicList", (Serializable) my_music_list);
            startActivity(intent);
        } else if (id == R.id.nav_member_info) {
            Intent intent = new Intent(this, ModifyMemberActivity.class);
            startActivity(intent);

        } else if (id == R.id.nav_logout) {
            SharedPreferences preferences =getSharedPreferences("MuzixAutoLogin", Activity.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.clear();
            editor.commit();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /*=====================Connect to JSPServer===========================================*/
    /*                     you will get Database data with JSON format                    */
    /*====================================================================================*/
    class JSPServerConnector extends AsyncTask<String, Void,JSONObject>{

        private int type; //0번이면 -> recent_music_list, 1번이면-> my_music_list

        public JSPServerConnector(int type){
            this.type = type;
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

                for(int i =1; i<strings.length;i+=2){ //post parameter
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

        @Override
        protected void onPostExecute(JSONObject jsonObject) {
            super.onPostExecute(jsonObject);
            JSONObject json_data = jsonObject;
            try {
                if (type == 1) {
                    if (json_data.getString("result").equals("success")) {
                        JSONArray json_arr = (JSONArray) json_data.get("list");
                        int size = json_arr.length();

                        if(size ==0){
                            no_my_music_txt.setVisibility(View.VISIBLE);
                        }
                        my_music_list.clear();
                        for(int i =0; i<size; i++){
                            JSONObject obj = json_arr.getJSONObject(i);
                            String music_id = obj.getString("music_id");
                            String title = obj.getString("title");
                            String writer = obj.getString("writer");
                            String genre = obj.getString("genre");
                            String cover_img_url = obj.getString("cover_img_url");
                            if(cover_img_url.equals("null"))
                                cover_img_url = null;
                            String melody_csv = obj.getString("melody_csv");
                            String chord_txt = obj.getString("chord_txt");
                            boolean scope = obj.getBoolean("scope");
                            String modify_date = obj.getString("modify_date");
                            my_music_list.add(new MidiFile(music_id,writer,title,genre,cover_img_url,melody_csv,chord_txt,scope,modify_date));
                        }
                    }
                    myMusicListAdapter = new MyMusicListAdapter(my_music_list,MainActivity.this,1);
                    home_my_music_list.setAdapter(myMusicListAdapter);
                } else if (type == 2) {
                    if (json_data.getString("result").equals("success")) {
                        JSONArray json_arr = (JSONArray) json_data.get("list");
                        int size = json_arr.length();

                        if(size ==0){
                            no_recent_music_txt.setVisibility(View.VISIBLE);
                        }
                        recent_music_list.clear();
                        for(int i =0; i<size; i++){
                            JSONObject obj = json_arr.getJSONObject(i);
                            String music_id = obj.getString("music_id");
                            String title = obj.getString("title");
                            String writer = obj.getString("writer");
                            String genre = obj.getString("genre");
                            String cover_img_url = obj.getString("cover_img");
                            if(cover_img_url.equals("null"))
                                cover_img_url = null;
                            String melody_csv = obj.getString("melody_csv");
                            String chord_txt = obj.getString("chord_txt");
                            boolean scope = obj.getBoolean("scope");
                            String modify_date = obj.getString("modify_date");
                            recent_music_list.add(new MidiFile(music_id,writer,title,genre,cover_img_url,melody_csv,chord_txt,scope,modify_date));
                        }
                    }
                    AutoScrollAdapter scrollAdapter = new AutoScrollAdapter(MainActivity.this, recent_music_list);
                    home_recent_music_list.setAdapter(scrollAdapter); //Auto Viewpager에 Adapter 장착
                    home_recent_music_list.setInterval(5000); // 페이지 넘어갈 시간 간격 설정
                    home_recent_music_list.startAutoScroll(); //Auto Scroll 시작
                }
                else if(type==3){
                    if (json_data.getString("result").equals("success")) {
                        JSONArray json_arr = (JSONArray) json_data.get("list");
                        int size = json_arr.length();

                        search_list.clear();
                        for(int i =0; i<size; i++){
                            JSONObject obj = json_arr.getJSONObject(i);
                            String music_id = obj.getString("music_id");
                            String title = obj.getString("title");
                            String writer = obj.getString("writer");
                            String genre = obj.getString("genre");
                            String cover_img_url = obj.getString("cover_img");
                            if(cover_img_url.equals("null"))
                                cover_img_url = null;
                            String melody_csv = obj.getString("melody_csv");
                            String chord_txt = obj.getString("chord_txt");
                            boolean scope = obj.getBoolean("scope");
                            String modify_date = obj.getString("modify_date");
                            search_list.add(new MidiFile(music_id,writer,title,genre,cover_img_url,melody_csv,chord_txt,scope,modify_date));
                        }
                    }
                    SearchListAdapter searchListAdapter= new SearchListAdapter(MainActivity.this,search_list);
                    search_music_list.setAdapter(searchListAdapter); //Auto Viewpager에 Adapter 장착
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
    /*=====================================================================================*/
}
