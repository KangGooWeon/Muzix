/*
 * Copyright (c) 2011-2012 Madhav Vaidyanathan
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */

package com.example.muzix;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.muzix.csvutil.CSVRead;
import com.example.muzix.meta.InstrumentName;
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

/**
 * SheetMusicActivity is the main activity. The main components are:
 * <ul>
 *  <li> MidiPlayer : The buttons and speed bar at the top.
 *  <li> Piano : For highlighting the piano notes during playback.
 *  <li> SheetMusic : For highlighting the sheet music notes during playback.
 */
public class SheetMusicActivity extends Activity {

    public static final String MidiTitleID = "MidiTitleID";
    public static final int settingsRequestCode = 1;

    private MidiPlayer player;   /* The play/stop/rewind toolbar */
    private Piano piano;         /* The piano at the top */
    private SheetMusic sheet;    /* The sheet music */
    private LinearLayout layout; /* THe layout */
    private MidiFile midifile;   /* The midi file to play */
    private MidiOptions options; /* The options for sheet music and sound */
    private long midiCRC;      /* CRC of the midi bytes */
    private FileUri fileUri;

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

        ClefSymbol.LoadImages(this);
        TimeSigSymbol.LoadImages(this);

        // Parse the MidiFile from the raw bytes

        Intent intent = getIntent();
        String title = intent.getExtras().getString("title");

        this.setTitle("MidiSheetMusic: " + title);
        byte[] data= null;
        try {
            try {
                boolean isCreated = new FileDownloadAsynkTask().execute(intent.getExtras().getString("melody_csv"),"melody","csv").get();
                if(isCreated) {
                    String targetDir = Environment.getExternalStorageDirectory().toString();

                    List<String[]> csv_data = new ArrayList<String[]>();

                    CSVRead read = new CSVRead(targetDir + "/" + "melody" + ".csv");
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
                            tempo.setBpm((int)(Float.parseFloat(array[2])));
                            noteTrack.insertEvent(ts);
                            noteTrack.insertEvent(tempo);
                        }
                        pitch =  (int) Float.parseFloat(array[3]);

                        if (pitch == -1)
                            continue;

                        velocity =  (int) Float.parseFloat(array[4]);
                        start_time =  (long) Float.parseFloat(array[5]);
                        duration = (long) Float.parseFloat(array[6]);
                        noteTrack.insertNote(channel, pitch, velocity, start_time, duration);
                    }
                    for(int i =1;i<=128;i++)
                        noteTrack.insertEvent(new ProgramChange(0,start_time+duration,channel,i));
                    List<MidiTrack> new_tracks = new ArrayList<MidiTrack>();
                    new_tracks.add(noteTrack);

                    MidiFile midi = new MidiFile(MidiFile.DEFAULT_RESOLUTION, new_tracks);
                    midi.setActivity(this);
                    // 4. Write the MIDI data to a file
                    String mPath = targetDir + "/temp"+"/temp.mid";

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
                        temp.setMusic_id(intent.getExtras().getString("music_id"));
                        temp.setChord_txt(intent.getExtras().getString("chord_txt"));
                        temp.setMelody_csv(intent.getExtras().getString("melody_csv"));
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
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }
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
        player.setSheetUpdateRequestListener(() -> createSheetMusic(options));
        createSheetMusic(options);
    }

    private void reBuildView(Intent intent){
        String title = intent.getExtras().getString("title");

        this.setTitle("MidiSheetMusic: " + title);
        byte[] data= null;
        try {
            try {
                boolean isCreated = new FileDownloadAsynkTask().execute(intent.getExtras().getString("melody_csv"),"melody","csv").get();
                if(isCreated) {
                    String targetDir = Environment.getExternalStorageDirectory().toString();

                    List<String[]> csv_data = new ArrayList<String[]>();

                    CSVRead read = new CSVRead(targetDir + "/" + "melody" + ".csv");
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
                            tempo.setBpm((int)(Float.parseFloat(array[2])));
                            noteTrack.insertEvent(ts);
                            noteTrack.insertEvent(tempo);
                        }
                        pitch =  (int) Float.parseFloat(array[3]);

                        if (pitch == -1)
                            continue;

                        velocity =  (int) Float.parseFloat(array[4]);
                        start_time =  (long) Float.parseFloat(array[5]);
                        duration = (long) Float.parseFloat(array[6]);
                        noteTrack.insertNote(channel, pitch, velocity, start_time, duration);
                    }
                    for(int i =1;i<=128;i++)
                        noteTrack.insertEvent(new ProgramChange(0,start_time+duration,channel,i));
                    List<MidiTrack> new_tracks = new ArrayList<MidiTrack>();
                    new_tracks.add(noteTrack);

                    MidiFile midi = new MidiFile(MidiFile.DEFAULT_RESOLUTION, new_tracks);
                    midi.setActivity(this);
                    // 4. Write the MIDI data to a file
                    String mPath = targetDir + "/temp"+"/temp.mid";

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

                        temp.setWriter(intent.getExtras().getString("writer"));
                        temp.setMusic_id(intent.getExtras().getString("music_id"));

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
                    }
                    catch(IOException e)
                    {
                        e.printStackTrace();
                    }
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
        player.setSheetUpdateRequestListener(() -> createSheetMusic(options));
        createSheetMusic(options);
    }

    /* Create the MidiPlayer and Piano views */
    void createView() {
        setContentView(R.layout.sheet_music_layout);
        LinearLayout parent_layout = (LinearLayout) findViewById(R.id.my_layout);
        parent_layout.setOrientation(LinearLayout.VERTICAL);
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        player = new MidiPlayer(this);
        piano = new Piano(this);

        player.setFileUri(fileUri);

        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2000);
        LinearLayout.LayoutParams player_param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0,1f);
        parent_layout.addView(piano,0);
        parent_layout.addView(layout,1,param);
        parent_layout.addView(player,2,player_param);
        player.SetPiano(piano);
        layout.requestLayout();
        parent_layout.requestLayout();
    }

    private void createMidiFile(){

        MidiTrack noteTrack = new MidiTrack();

        TimeSignature ts = new TimeSignature();
        ts.setTimeSignature(midifile.getTime().getNumerator(), midifile.getTime().getDenominator(), TimeSignature.DEFAULT_METER, TimeSignature.DEFAULT_DIVISION);

        Tempo tempo = new Tempo();
        tempo.setBpm(midifile.getTime().getTempo());

        noteTrack.insertEvent(ts);
        noteTrack.insertEvent(tempo);

        int channel = 0;
        int pitch = 0;
        int velocity = 0; //강세
        long tick = 0;
        long duration = 0;

        //noteTrack.insertNote(channel, pitch, velocity, tick, duration);
        List<MidiTrack> new_tracks = new ArrayList<MidiTrack>();
        new_tracks.add(noteTrack);

        MidiFile midi = new MidiFile(MidiFile.DEFAULT_RESOLUTION, new_tracks);
        midi.setActivity(this);
        // 4. Write the MIDI data to a file
        String sd = Environment.getExternalStorageDirectory().getAbsolutePath();
        String mPath = sd+"/temp.mid";

        try
        {
            File f = new File(mPath);
            if(f.delete()) {
                midi.writeToFile(mPath);
                MidiFile temp = ((ModifySheetMusicActivity) ModifySheetMusicActivity.modifyContext).changeMidiFile(mPath);
                if (temp != null) {
                    midifile = temp;
                }
            }
        }
        catch(IOException e)
        {
            System.err.println(e);
        }
    }
    /** Create the SheetMusic view with the given options */
    private void
    createSheetMusic(MidiOptions options) {
        if (sheet != null) {
            layout.removeView(sheet);
        }

        //piano.setVisibility(options.showPiano ? View.VISIBLE : View.GONE);
        sheet = new SheetMusic(this);
        sheet.init(midifile, options);
        sheet.setPlayer(player);
        layout.addView(sheet);
        piano.SetMidiFile(midifile, options, player);
        piano.SetShadeColors(options.shade1Color, options.shade2Color);

        player.SetMidiFile(midifile, options, sheet);
        layout.requestLayout();
        player.updateToolbarButtons();
        sheet.callOnDraw();
    }


    /** Always display this activity in landscape mode. */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /** When the menu button is pressed, initialize the menus. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (player != null) {
            player.Pause();
        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sheet_menu, menu);
        return true;
    }

    /** Callback when a menu item is selected.
     *  - Choose Song : Choose a new song
     *  - Song Settings : Adjust the sheet music and sound options
     *  - Save As Images: Save the sheet music as PNG images
     *  - Help : Display the HTML help screen
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.choose_song:
                chooseSong();
                return true;
            case R.id.song_settings:
                changeSettings();
                return true;
            case R.id.save_images:
                showSaveImagesDialog();
                return true;
            case R.id.help:
                showHelp();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /** To choose a new song, simply finish this activity.
     *  The previous activity is always the ChooseSongActivity.
     */
    private void chooseSong() {
        this.finish();
    }

    /** To change the sheet music options, start the SettingsActivity.
     *  Pass the current MidiOptions as a parameter to the Intent.
     *  Also pass the 'default' MidiOptions as a parameter to the Intent.
     *  When the SettingsActivity has finished, the onActivityResult()
     *  method will be called.
     */
    private void changeSettings() {
        MidiOptions defaultOptions = new MidiOptions(midifile);
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(SettingsActivity.settingsID, options);
        intent.putExtra(SettingsActivity.defaultSettingsID, defaultOptions);
        startActivityForResult(intent, settingsRequestCode);
    }


    /* Show the "Save As Images" dialog */
    private void showSaveImagesDialog() {
        LayoutInflater inflator = LayoutInflater.from(this);
        final View dialogView= inflator.inflate(R.layout.save_images_dialog, null);
        final EditText filenameView = (EditText)dialogView.findViewById(R.id.save_images_filename);
        filenameView.setText(midifile.getFileName().replace("_", " ") );
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface builder, int whichButton) {
                saveAsImages(filenameView.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface builder, int whichButton) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    /* Save the current sheet music as PNG images. */
    private void saveAsImages(String name) {
        String filename = name;
        try {
            filename = URLEncoder.encode(name, "utf-8");
        }
        catch (UnsupportedEncodingException e) {
            Toast.makeText(this, "Error: unsupported encoding in filename", Toast.LENGTH_SHORT).show();
        }
        if (!options.scrollVert) {
            options.scrollVert = true;
            createSheetMusic(options);
        }
        try {
            int numpages = sheet.GetTotalPages();
            for (int page = 1; page <= numpages; page++) {
                Bitmap image= Bitmap.createBitmap(SheetMusic.PageWidth + 40, SheetMusic.PageHeight + 40, Bitmap.Config.ARGB_8888);
                Canvas imageCanvas = new Canvas(image);
                sheet.DrawPage(imageCanvas, page);
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/MidiSheetMusic");
                File file = new File(path, "" + filename + page + ".png");
                path.mkdirs();
                OutputStream stream = new FileOutputStream(file);
                image.compress(Bitmap.CompressFormat.PNG, 0, stream);
                stream.close();

                // Inform the media scanner about the file
                MediaScannerConnection.scanFile(this, new String[] { file.toString() }, null, null);
            }
        }
        catch (IOException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Error saving image to file " + Environment.DIRECTORY_PICTURES + "/Muzix/" + filename  + ".png");
            builder.setCancelable(false);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
        catch (NullPointerException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Ran out of memory while saving image to file " + Environment.DIRECTORY_PICTURES + "/Muzix/" + filename  + ".png");
            builder.setCancelable(false);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
        if (options.scrollVert) {
            options.scrollVert = false;
            createSheetMusic(options);
        }
    }


    /** Show the HTML help screen. */
    private void showHelp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("설명");
        builder.setMessage("악보의 음표를 클릭 후 실행을 하게 되면 해당 음표 부터 음악을 실행하게 됩니다.\n\n" +
                "화살표를 클릭하게 되면 다음 마디 또는 이전 마디로 이동하게 됩니다.\n\n" +
                "수정 버튼을 클릭하게 되면 수정 창으로 이동하게 됩니다.\n\n" +
                "메뉴에서 출력 악기 사운드를 설정할 수 있으며, 악보를 이미지로 저장할 수 있습니다.\n\n");
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /** This is the callback when the SettingsActivity is finished.
     *  Get the modified MidiOptions (passed as a parameter in the Intent).
     *  Save the MidiOptions.  The key is the CRC checksum of the midi data,
     *  and the value is a JSON dump of the MidiOptions.
     *  Finally, re-create the SheetMusic View with the new options.
     */
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent intent) {
        if(requestCode == 200){
            if(resultCode==Activity.RESULT_OK)
                reBuildView(intent);
            return;
        }
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
        player.invalidate();
        piano.invalidate();
        if (sheet != null) {
            sheet.invalidate();
        }
        layout.requestLayout();
    }

    /** When this activity pauses, stop the music */
    @Override
    protected void onPause() {
        if (player != null) {
            player.Pause();
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

