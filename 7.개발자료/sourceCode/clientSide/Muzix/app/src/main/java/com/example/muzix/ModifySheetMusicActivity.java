package com.example.muzix;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.muzix.csvutil.CSVRead;
import com.example.muzix.meta.Tempo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.zip.CRC32;

/*@Class ModifySheetMusicActivity
* you can modify the sheet */
public class ModifySheetMusicActivity extends Activity {
    public static Context modifyContext;
    public static final String MidiTitleID = "MidiTitleID";
    public static final int settingsRequestCode = 1;

    private ModifyNavigation nav; /* The Navigation */
    private ModifyTool mtool;   /* The modify toolbar */
    private Piano piano;         /* The piano at the top */
    private SheetMusic sheet;    /* The sheet music */
    private LinearLayout layout; /* THe layout */
    private MidiFile midifile;   /* The midi file to play */
    private MidiOptions options; /* The options for sheet music and sound */
    private long midiCRC;      /* CRC of the midi bytes */
    private String title;
    private String music_id;
    private boolean isChangedChord = false;


    /** Create this SheetMusicActivity.
     * The Intent should have two parameters:
     * - data: The uri of the midi file to open.
     * - MidiTitleID: The title of the song (String)
     */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Hide the navigation bar before the views are laid out
        //hideSystemUI();
        modifyContext = this;
        ClefSymbol.LoadImages(this);
        TimeSigSymbol.LoadImages(this);

        // Parse the MidiFile from the raw bytes
        isChangedChord = false;

        title = this.getIntent().getStringExtra(MidiTitleID);

        this.setTitle("MidiSheetMusic: " + title);
        byte[] data = null;
        Intent intent = getIntent();
        try {
            try {

                String targetDir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";

                List<String[]> csv_data = new ArrayList<String[]>();

                CSVRead read = new CSVRead(targetDir + "melody.csv");
                csv_data = read.readCsv();

                Iterator<String[]> it = csv_data.iterator();

                MidiTrack noteTrack = new MidiTrack();
                TimeSignature ts = null;

                Tempo tempo = null;

                int channel = 0;
                int pitch = 0;
                int velocity = 0; //강세
                long start_time = 0;
                long duration = 0;

                while (it.hasNext()) {
                    String[] array = (String[]) it.next();
                    if (array.length == 1)
                        continue;
                    if (array[1].equals("key_sig"))
                        continue;
                    if (ts == null) {
                        ts = new TimeSignature();
                        String[] keySigs = array[1].split("/");
                        int numerator = Integer.parseInt(keySigs[0]);
                        int denominator = Integer.parseInt(keySigs[1]);
                        ts.setTimeSignature(numerator, denominator, TimeSignature.DEFAULT_METER, TimeSignature.DEFAULT_DIVISION);
                    }
                    if (tempo == null) {
                        tempo = new Tempo();
                        tempo.setBpm((int) (Float.parseFloat(array[2])));
                        noteTrack.insertEvent(ts);
                        noteTrack.insertEvent(tempo);
                    }
                    pitch = (int) Float.parseFloat(array[3]);

                    if (pitch == -1)
                        continue;

                    velocity = (int) Float.parseFloat(array[4]);
                    start_time = (long) Float.parseFloat(array[5]);
                    duration = (long) Float.parseFloat(array[6]);
                    noteTrack.insertNote(channel, pitch, velocity, start_time, duration);
                }
                for (int i = 1; i <= 128; i++)
                    noteTrack.insertEvent(new ProgramChange(0, start_time + duration, channel, i));
                List<MidiTrack> new_tracks = new ArrayList<MidiTrack>();
                new_tracks.add(noteTrack);

                MidiFile midi = new MidiFile(MidiFile.DEFAULT_RESOLUTION, new_tracks);
                midi.setActivity(this);
                // 4. Write the MIDI data to a file
                String mPath = targetDir + "/temp" + "/temp.mid";

                try {
                    File f = new File(mPath);
                    if (f.exists())
                        f.delete();

                    midi.writeToFile(mPath);

                    MidiFile temp = null;

                    Uri uri = Uri.parse(mPath);
                    if (uri == null) {
                        this.finish();
                    }

                    FileUri file = new FileUri(uri, "temp");

                    try {
                        data = file.getData(this);
                        temp = new MidiFile(data, title);
                    } catch (MidiFileException e) {
                        this.finish();
                        return;
                    }

                    temp.setGenre(intent.getExtras().getString("genre"));
                    temp.setAlbumCoverUri(intent.getExtras().getString("cover_img"));
                    temp.setWriter(intent.getExtras().getString("writer"));
                    temp.setModify_date(intent.getExtras().getString("modify_date"));
                    temp.setScope(intent.getExtras().getBoolean("scope"));

                    music_id = intent.getExtras().getString("music_id");
                    temp.setMusic_id(music_id);

                    boolean isTxtCreated = new FileDownloadAsynkTask().execute(intent.getExtras().getString("chord_txt"),"chord","txt").get();
                    ArrayList<MeasureChordSymbol> chords = new ArrayList<MeasureChordSymbol>();
                    int measure = temp.getTime().getMeasure(); // 마디별 시간

                    MeasureChordSymbol postChord = null;
                    if(isTxtCreated){
                        try{
                            InputStream is = new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath()+"/chord.txt");
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                            String line="";
                            while((line=reader.readLine())!=null){
                                String[] chord_datas = line.split(" ");

                                float chord_time = Float.parseFloat(chord_datas[0]);
                                String chord = chord_datas[1];
                                if(postChord==null){
                                    postChord = new MeasureChordSymbol((int)chord_time,chord);
                                    chords.add(postChord);
                                }
                                else {
                                    if(postChord.getStartTime()+measure<chord_time){
                                        while(postChord.getStartTime()+measure<chord_time) {
                                            postChord = new MeasureChordSymbol(postChord.getStartTime() + measure, chord);
                                            chords.add(postChord);
                                        }
                                    }
                                    else{
                                        if(chord.equals(postChord.getText()))
                                            continue;
                                        postChord = new MeasureChordSymbol((int)chord_time,chord);
                                        chords.add(postChord);
                                    }
                                }
                            }

                            reader.close();
                            is.close();
                            temp.setChords(chords);
                        }catch (IOException e){
                            e.printStackTrace();
                        }
                    }
                    if (temp != null) {
                        midifile = temp;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        catch (MidiFileException e) {
            this.finish();
            return;
        }

        // Initialize the settings (MidiOptions).
        // If previous settings have been saved, used those
        options = new MidiOptions(midifile);
        CRC32 crc = new CRC32();
        crc.update(data);
        midiCRC = crc.getValue();
        SharedPreferences settings = getPreferences(0);
        options.scrollVert = settings.getBoolean("scrollVert", false);
        options.shade1Color = settings.getInt("shade1Color", options.shade1Color);
        options.shade2Color = settings.getInt("shade2Color", options.shade2Color);
        //options.showPiano = settings.getBoolean("showPiano", true);
        String json = settings.getString("" + midiCRC, null);
        MidiOptions savedOptions = MidiOptions.fromJson(json);
        if (savedOptions != null) {
            options.merge(savedOptions);
        }
        createView();
        mtool.setSheetUpdateRequestListener(() -> createSheetMusic(options));
        createSheetMusic(options);
    }

    /* Create the MidiPlayer and Piano views */
    void createView() {
        setContentView(R.layout.sheet_music_layout);
        LinearLayout parent_layout = (LinearLayout) findViewById(R.id.my_layout);
        parent_layout.setOrientation(LinearLayout.VERTICAL);
        layout = new LinearLayout(this);

        nav = new ModifyNavigation(this,midifile,title,music_id);
        mtool = new ModifyTool(this);
        piano = new Piano(this);

        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1900);
        LinearLayout.LayoutParams player_param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0,1f);
        parent_layout.addView(nav,0);
        parent_layout.addView(layout,1,param);
        parent_layout.addView(mtool,2,player_param);
        mtool.SetPiano(piano);
        layout.requestLayout();
        parent_layout.requestLayout();
    }

    /** Create the SheetMusic view with the given options */
    private void
    createSheetMusic(MidiOptions options) {
        if (sheet != null) {
            layout.removeView(sheet);
        }

        //piano.setVisibility(options.showPiano ? View.VISIBLE : View.GONE);
        sheet = new SheetMusic(this);
        sheet.setNote_location(0);
        sheet.init(midifile, options);
        sheet.setModifyTool(mtool);
        layout.addView(sheet);
        piano.SetMidiFile(midifile, options, mtool);
        piano.SetShadeColors(options.shade1Color, options.shade2Color);

        mtool.SetMidiFile(midifile, options, sheet);
        layout.requestLayout();
        mtool.updateToolbarButtons();
        sheet.callOnDraw();
    }

    public MidiFile getMidifile(){
        return midifile;
    }

    public void setMidiTracks(ArrayList<MidiTrack> tracks){
        midifile.setTracks(tracks);
    }

    public boolean IsChangedChord(){
        return isChangedChord;
    }
    /** Always display this activity in landscape mode. */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    public void setChords(String url){
        try {
            boolean isTxtCreated = new FileDownloadAsynkTask().execute(url,"chord","txt").get();
            ArrayList<MeasureChordSymbol> chords = new ArrayList<MeasureChordSymbol>();
            int measure = midifile.getTime().getMeasure(); // 마디별 시간

            MeasureChordSymbol postChord = null;
            if (isTxtCreated) {
                try {
                    InputStream is = new FileInputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/chord.txt");
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String line = "";
                    while ((line = reader.readLine()) != null) {
                        String[] chord_datas = line.split(" ");

                        float chord_time = Float.parseFloat(chord_datas[0]);
                        String chord = chord_datas[1];
                        if (postChord == null) {
                            postChord = new MeasureChordSymbol((int) chord_time, chord);
                            chords.add(postChord);
                        } else {
                            if (postChord.getStartTime() + measure < chord_time) {
                                while (postChord.getStartTime() + measure < chord_time) {
                                    postChord = new MeasureChordSymbol(postChord.getStartTime() + measure, chord);
                                    chords.add(postChord);
                                }
                            } else {
                                if (chord.equals(postChord.getText()))
                                    continue;
                                postChord = new MeasureChordSymbol((int) chord_time, chord);
                                chords.add(postChord);
                            }
                        }
                    }

                    reader.close();
                    is.close();
                    midifile.setChords(chords);

                    isChangedChord = true;

                    createView();
                    createSheetMusic(options);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        setResult(Activity.RESULT_CANCELED);
        finish();
    }

    /** When the menu button is pressed, initialize the menus. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mtool != null) {
            mtool.Pause();
        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sheet_menu, menu);
        return true;
    }

    public MidiFile changeMidiFile(String file_location){
        Uri uri = Uri.parse(file_location);
        if (uri == null) {
            this.finish();
            return null;
        }
        title = this.getIntent().getStringExtra(MidiTitleID);
        if (title == null) {
            title = uri.getLastPathSegment();
        }
        FileUri file = new FileUri(uri, title);
        this.setTitle("MidiSheetMusic: " + title);
        byte[] data;
        try {
            data = file.getData(this);
            midifile = new MidiFile(data, title);
        }
        catch (MidiFileException e) {
            this.finish();
            return null;
        }
        return midifile;
    }

    /** This is the callback when the SettingsActivity is finished.
     *  Get the modified MidiOptions (passed as a parameter in the Intent).
     *  Save the MidiOptions.  The key is the CRC checksum of the midi data,
     *  and the value is a JSON dump of the MidiOptions.
     *  Finally, re-create the SheetMusic View with the new options.
     */
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent intent) {
        if (requestCode != settingsRequestCode) {
            return;
        }
        options = (MidiOptions)
                intent.getSerializableExtra(SettingsActivity.settingsID);

        // Check whether the default instruments have changed.
        for (int i = 0; i < options.instruments.length; i++) {
            if (options.instruments[i] !=
                    midifile.getTracks().get(i).getInstrument()) {
                options.useDefaultInstruments = false;
            }
        }
        // Save the options.
        SharedPreferences.Editor editor = getPreferences(0).edit();
        editor.putBoolean("scrollVert", options.scrollVert);
        editor.putInt("shade1Color", options.shade1Color);
        editor.putInt("shade2Color", options.shade2Color);
        editor.putBoolean("showPiano", options.showPiano);
        for (int i = 0; i < options.noteColors.length; i++) {
            editor.putInt("noteColor" + i, options.noteColors[i]);
        }
        String json = options.toJson();
        if (json != null) {
            editor.putString("" + midiCRC, json);
        }
        editor.apply();

        // Recreate the sheet music with the new options
        createSheetMusic(options);
    }

    /** When this activity resumes, redraw all the views */
    @Override
    protected void onResume() {
        super.onResume();
        layout.requestLayout();
        mtool.invalidate();
        piano.invalidate();
        if (sheet != null) {
            sheet.invalidate();
        }
        layout.requestLayout();
    }

    /** When this activity pauses, stop the music */
    @Override
    protected void onPause() {
        if (mtool != null) {
            mtool.Pause();
        }
        super.onPause();
    }

    /************************** Hide navigation buttons **************************/

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables sticky immersive mode.
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    class FileDownloadAsynkTask extends AsyncTask<String, Integer, Boolean> {

        private String fileName;
        private String type;
        String savePath = Environment.getExternalStorageDirectory() + File.separator;

        @Override
        protected void onPreExecute() {

            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... params) {

            File dir = new File(savePath);

            boolean isCreated = false;
            //상위 디렉토리가 존재하지 않을 경우 생성
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String fileUrl = params[0];
            fileName = params[1];
            type = params[2];
            String localPath = savePath+fileName;
            if(type.equals("csv"))
                localPath += ".csv";
            else if(type.equals("txt"))
                localPath+=".txt";

            try {
                URL imgUrl = new URL(fileUrl);
                //서버와 접속하는 클라이언트 객체 생성
                HttpURLConnection conn = (HttpURLConnection) imgUrl.openConnection();
                int response = conn.getResponseCode();

                if(200<=response&&response<=300) {
                    isCreated = true;
                    File file = new File(localPath);

                    InputStream is = conn.getInputStream();
                    OutputStream outStream = new FileOutputStream(file);

                    byte[] buf = new byte[1024];
                    int len = 0;

                    while ((len = is.read(buf)) > 0) {
                        outStream.write(buf, 0, len);
                    }

                    outStream.close();
                    is.close();
                }
                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return isCreated;
        }

        @Override
        protected void onPostExecute(Boolean obj) {
            super.onPostExecute(obj);
        }
    }
}