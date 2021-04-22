package com.example.muzix;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.muzix.csvutil.CSVWrite;
import com.example.muzix.meta.Tempo;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ModifyTool extends LinearLayout {
    private ImageButton playButton;      /** The play/pause button */
    private ImageButton stopButton;      /** The stop button */
    private ImageButton leftButton;    /** The move left button */
    private ImageButton rightButton;   /** The move right button */
    private ImageButton sharpButton;  /** add sharp button */
    private ImageButton flatButton;   /** add flat button */
    private ImageButton lengthButton; /** change length button */
    private ImageButton divideButton;   /** divide music note button */
    private ImageButton deleteButton; /** delete music note button*/
    private LinearLayout chord_recommend;

    private TextView speedText;          /** The "Speed %" label */
    private SeekBar speedBar;    /** The seekbar for controlling the playback speed */

    /** The index corresponding to left/right hand in the track list */
    private static final int LEFT_TRACK = 1;
    private static final int RIGHT_TRACK = 0;

    private FileUri fileUri;
    int playstate;               /** The playing state of the Midi Player */
    final int stopped   = 1;     /** Currently stopped */
    final int playing   = 2;     /** Currently playing music */
    final int paused    = 3;     /** Currently paused */
    final int initStop  = 4;     /** Transitioning from playing to stop */
    final int initPause = 5;     /** Transitioning from playing to pause */

    final String tempSoundFile = "playing.mid"; /** The filename to play sound from */

    MediaPlayer player;         /** For playing the audio */
    MidiFile midifile;          /** The midi file to play */
    MidiOptions options;        /** The sound options for playing the midi file */
    double pulsesPerMsec;       /** The number of pulses per millisec */
    SheetMusic sheet;           /** The sheet music to shade while playing */
    Piano piano;                /** The piano to shade while playing */
    Handler timer;              /** Timer used to update the sheet music while playing */
    long startTime;             /** Absolute time when music started playing (msec) */
    double startPulseTime;      /** Time (in pulses) when music started playing */
    double currentPulseTime;    /** Time (in pulses) music is currently at */
    double prevPulseTime;       /** Time (in pulses) music was last at */
    Activity activity;          /** The parent activity. */

    int player_height=0;

    /** A listener that allows us to send a request to update the sheet when needed */
    private SheetUpdateRequestListener mSheetUpdateRequestListener;

    public void setSheetUpdateRequestListener(SheetUpdateRequestListener listener) {
        mSheetUpdateRequestListener = listener;
    }

    /** Create a new MidiPlayer, displaying the play/stop buttons, and the
     *  speed bar.  The midifile and sheetmusic are initially null.
     */
    public ModifyTool(Activity activity) {
        super(activity);
        this.activity = activity;
        this.midifile = null;
        this.options = null;
        this.sheet = null;
        playstate = stopped;
        startTime = SystemClock.uptimeMillis();
        startPulseTime = 0;
        currentPulseTime = -10;
        prevPulseTime = -10;
        init();

        player = new MediaPlayer();
        setBackgroundColor(Color.BLACK);

        // Keep screen on
        this.activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public ModifyTool(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ModifyTool(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    /** Create the rewind, play, stop, and fast forward buttons */
    void init() {
        inflate(activity, R.layout.modify_toolbar, this);

        stopButton = findViewById(R.id.btn_replay);
        playButton = findViewById(R.id.btn_play);
        speedText = findViewById(R.id.txt_speed);
        speedBar = findViewById(R.id.speed_bar);

        divideButton = findViewById(R.id.btn_divide_note);
        sharpButton = findViewById(R.id.btn_sharp);
        flatButton = findViewById(R.id.btn_flat);
        leftButton = findViewById(R.id.btn_left);
        rightButton = findViewById(R.id.btn_right);
        lengthButton = findViewById(R.id.btn_length);
        deleteButton = findViewById(R.id.btn_delete_note);

        chord_recommend = findViewById(R.id.chord_recommend);

        divideButton.setOnClickListener(v -> divideNote());
        deleteButton.setOnClickListener(v -> deleteNote());
        sharpButton.setOnClickListener(v -> addSharp());
        flatButton.setOnClickListener(v -> addFlat());
        lengthButton.setOnClickListener(v -> changeLength());
        leftButton.setOnClickListener(v -> moveLeft());
        stopButton.setOnClickListener(v -> Stop());
        playButton.setOnClickListener(v -> Play());
        rightButton.setOnClickListener(v -> moveRight());
        chord_recommend.setOnClickListener(v->chordRecommend());
        speedBar.getProgressDrawable().setColorFilter(Color.parseColor("#00BB87"), PorterDuff.Mode.SRC_IN);
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                speedText.setText(String.format(Locale.US, "%3d", progress) + "%");
            }
            public void onStartTrackingTouch(SeekBar bar) {
            }
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });

        /* Initialize the timer used for playback, but don't start
         * the timer yet (enabled = false).
         */
        timer = new Handler();
    }

    /** Move to modify Activity*/
    public void setFileUri(FileUri uri){
        this.fileUri = uri;
    }

    /** Update the status of the toolbar buttons (show, hide, opacity, etc.) */
    public void updateToolbarButtons(){
        // pianoButton.setAlpha(options.showPiano ? (float) 1.0 : (float) 0.5);

        float leftAlpha = (float) 0.5;
        float rightAlpha = (float) 0.5;
        if (LEFT_TRACK < options.tracks.length) {
            leftAlpha = options.tracks[LEFT_TRACK] ? (float) 1.0 : (float) 0.5;
        }
        if (RIGHT_TRACK < options.tracks.length) {
            rightAlpha = options.tracks[RIGHT_TRACK] ? (float) 1.0 : (float) 0.5;
        }
        //leftHandButton.setVisibility(LEFT_TRACK < options.tracks.length ? View.VISIBLE : View.GONE);
        // rightHandButton.setVisibility(RIGHT_TRACK < options.tracks.length ? View.VISIBLE : View.GONE);
        //leftHandButton.setAlpha(leftAlpha);
        // rightHandButton.setAlpha(rightAlpha);
    }

    /** Get the preferred width/height given the screen width/height */
    public static Point getPreferredSize(int screenwidth, int screenheight) {
        int height = (int) (9.0 * screenwidth / ( 2 + Piano.KeysPerOctave * Piano.MaxOctave));
        //height = height * 2/3 ;
        height = height*3;
        return new Point(screenwidth, height);
    }

    /** Determine the measured width and height.
     *  Resize the individual buttons according to the new width/height.
     */
    @Override
    protected void onMeasure(int widthspec, int heightspec) {
        super.onMeasure(widthspec, heightspec);
        int screenwidth = MeasureSpec.getSize(widthspec);
        /* Make the button height 2/3 the piano WhiteKeyHeight */
        int height = (int) (9.0 * screenwidth / ( 2 + Piano.KeysPerOctave * Piano.MaxOctave));
        //height = height * 2/3;
        height = height*3;
        player_height = height;
        setMeasuredDimension(screenwidth, height);
    }

    public void SetPiano(Piano p) {
        piano = p;
    }

    /** The MidiFile and/or SheetMusic has changed. Stop any playback sound,
     *  and store the current midifile and sheet music.
     */
    public void SetMidiFile(MidiFile file, MidiOptions opt, SheetMusic s) {

        /* If we're paused, and using the same midi file, redraw the
         * highlighted notes.
         */
        if ((file == midifile && midifile != null && playstate == paused)) {
            options = opt;
            sheet = s;
            sheet.ShadeNotes((int)currentPulseTime, (int)-1, SheetMusic.DontScroll);

            /* We have to wait some time (200 msec) for the sheet music
             * to scroll and redraw, before we can re-shade.
             */
            timer.removeCallbacks(TimerCallback);
            timer.postDelayed(ReShade, 500);
        }
        else {
            Stop();
            midifile = file;
            options = opt;
            sheet = s;
        }
    }

    /** If we're paused, reshade the sheet music and piano. */
    Runnable ReShade = new Runnable() {
        public void run() {
            if (playstate == paused || playstate == stopped) {
                sheet.ShadeNotes((int)currentPulseTime, (int)-10, SheetMusic.ImmediateScroll);
                piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
            }
        }
    };


    /** Return the number of tracks selected in the MidiOptions.
     *  If the number of tracks is 0, there is no sound to play.
     */
    private int numberTracks() {
        int count = 0;
        for (int i = 0; i < options.tracks.length; i++) {
            if (options.tracks[i] && !options.mute[i]) {
                count += 1;
            }
        }
        return count;
    }

    /** Create a new midi file with all the MidiOptions incorporated.
     *  Save the new file to playing.mid, and store
     *  this temporary filename in tempSoundFile.
     */
    private void CreateMidiFile() {
        double inverse_tempo = 1.0 / midifile.getTime().getTempo();
        double inverse_tempo_scaled = inverse_tempo * speedBar.getProgress() / 100.0;
        // double inverse_tempo_scaled = inverse_tempo * 100.0 / 100.0;
        options.tempo = (int)(1.0 / inverse_tempo_scaled);
        pulsesPerMsec = midifile.getTime().getQuarter() * (1000.0 / options.tempo);

        try {
            FileOutputStream dest = activity.openFileOutput(tempSoundFile, Context.MODE_PRIVATE);
            midifile.ChangeSound(dest, options);
            dest.close();
            // checkFile(tempSoundFile);
        }
        catch (IOException e) {
            Toast toast = Toast.makeText(activity, "Error: Unable to create MIDI file for playing.", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void checkFile(String name) {
        try {
            FileInputStream in = activity.openFileInput(name);
            byte[] data = new byte[4096];
            int total = 0, len = 0;
            while (true) {
                len = in.read(data, 0, 4096);
                if (len > 0)
                    total += len;
                else
                    break;
            }
            in.close();
            data = new byte[total];
            in = activity.openFileInput(name);
            int offset = 0;
            while (offset < total) {
                len = in.read(data, offset, total - offset);
                if (len > 0)
                    offset += len;
            }
            in.close();
        }
        catch (IOException e) {
            Toast toast = Toast.makeText(activity, "CheckFile: " + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        }
        catch (MidiFileException e) {
            Toast toast = Toast.makeText(activity, "CheckFile midi: " + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        }
    }


    /** Play the sound for the given MIDI file */
    private void PlaySound(String filename) {
        if (player == null)
            return;
        try {
            FileInputStream input = activity.openFileInput(filename);
            player.reset();
            player.setDataSource(input.getFD());
            input.close();
            player.prepare();
            player.start();
        }
        catch (IOException e) {
            Toast toast = Toast.makeText(activity, "Error: Unable to play MIDI sound", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    /** Stop playing the MIDI music */
    private void StopSound() {
        if (player == null)
            return;
        player.stop();
        player.reset();
    }


    /** The callback for the play button.
     *  If we're stopped or pause, then play the midi file.
     */
    private void Play() {
        if (midifile == null || sheet == null || numberTracks() == 0) {
            return;
        }
        else if (playstate == initStop || playstate == initPause || playstate == playing) {
            return;
        }
        // playstate is stopped or paused

        // Hide the midi player, wait a little for the view
        // to refresh, and then start playing
        //this.setVisibility(View.GONE);
        midifile.setTracks(sheet.getTracks());
        SetMidiFile(midifile,options,sheet);
        timer.removeCallbacks(TimerCallback);
        timer.postDelayed(DoPlay, 1000);
    }

    Runnable DoPlay = new Runnable() {
        public void run() {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            /* The startPulseTime is the pulse time of the midi file when
             * we first start playing the music.  It's used during shading.
             */
            if (options.playMeasuresInLoop) {
                /* If we're playing measures in a loop, make sure the
                 * currentPulseTime is somewhere inside the loop measures.
                 */
                int measure = (int)(currentPulseTime / midifile.getTime().getMeasure());
                if ((measure < options.playMeasuresInLoopStart) ||
                        (measure > options.playMeasuresInLoopEnd)) {
                    currentPulseTime = options.playMeasuresInLoopStart * midifile.getTime().getMeasure();
                }
                startPulseTime = currentPulseTime;
                options.pauseTime = (int)(currentPulseTime - options.shifttime);
            }
            else if (playstate == paused) {
                startPulseTime = currentPulseTime;
                options.pauseTime = (int)(currentPulseTime - options.shifttime);
            }
            else {
                options.pauseTime = 0;
                startPulseTime = options.shifttime;
                currentPulseTime = options.shifttime;
                prevPulseTime = options.shifttime - midifile.getTime().getQuarter();
            }

            CreateMidiFile();
            playstate = playing;
            PlaySound(tempSoundFile);
            startTime = SystemClock.uptimeMillis();

            timer.removeCallbacks(TimerCallback);
            timer.removeCallbacks(ReShade);
            timer.postDelayed(TimerCallback, 100);

            sheet.setNote_location(sheet.getNote_location()+1);
            sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.GradualScroll);
            piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
        }
    };


    /** The callback for pausing playback.
     *  If we're currently playing, pause the music.
     *  The actual pause is done when the timer is invoked.
     */
    public void Pause() {
        //this.setVisibility(View.VISIBLE);
        LinearLayout layout = (LinearLayout)this.getParent();
        layout.requestLayout();
        this.requestLayout();
        this.invalidate();

        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (midifile == null || sheet == null || numberTracks() == 0) {
            return;
        }
        if (playstate == playing) {
            playstate = initPause;
        }
    }


    /** The callback for the Stop button.
     *  If playing, initiate a stop and wait for the timer to finish.
     *  Then do the actual stop.
     */
    void Stop() {
        //this.setVisibility(View.VISIBLE);
        if (midifile == null || sheet == null || playstate == stopped) {
            return;
        }

        if (playstate == initPause || playstate == initStop || playstate == playing) {
            /* Wait for timer to finish */
            playstate = initStop;
            DoStop();
        }
        else if (playstate == paused) {
            DoStop();
        }
    }

    /** Perform the actual stop, by stopping the sound,
     *  removing any shading, and clearing the state.
     */
    void DoStop() {
        playstate = stopped;
        timer.removeCallbacks(TimerCallback);
        // Scroll to the beginning
        sheet.ShadeNotes(0, 0, SheetMusic.ImmediateScroll);
        // Remove shading
        sheet.ShadeNotes(-10, (int)prevPulseTime, SheetMusic.DontScroll);
        sheet.ShadeNotes(-10, (int)currentPulseTime, SheetMusic.DontScroll);
        piano.ShadeNotes(-10, (int)prevPulseTime);
        piano.ShadeNotes(-10, (int)currentPulseTime);
        startPulseTime = 0;
        currentPulseTime = 0;
        prevPulseTime = 0;
        //setVisibility(View.VISIBLE);
        StopSound();
    }

    /*Create Temp File for Modify Sheet and MidiFile
    * When you want Finish the modify you need to click confirm button
    * and this temp File will send to Server and recived*/
    private void createTempMidiFile(){
        List<MidiTrack> tracks=midifile.getTracks();

        MidiTrack noteTrack = new MidiTrack();

        TimeSignature ts = new TimeSignature();
        ts.setTimeSignature(midifile.getTime().getNumerator(), midifile.getTime().getDenominator(), TimeSignature.DEFAULT_METER, TimeSignature.DEFAULT_DIVISION);

       Tempo tempo = new Tempo();
       tempo.setBpm(midifile.getTime().getTempo());

        noteTrack.insertEvent(ts);
        noteTrack.insertEvent(tempo);

        int size = tracks.size();
        int channel = 0;
        int pitch = 0;
        int velocity = 0; //강세
        long tick = 0;
        long duration = 0;
        for(int i =0; i< size; i++){
            List<MidiNote> notes = tracks.get(i).getNotes();
            int notes_size = notes.size();
            for(int j = 0; j<notes_size; j++) {
                MidiNote note = notes.get(j);
                channel = note.getChannel();
                pitch = note.getNumber();
                if (i == 1)
                    velocity = 100; //강세
                else
                    velocity = 100;
                tick = note.getStartTime();
                duration = note.getDuration();

                noteTrack.insertNote(channel, pitch, velocity, tick, duration);
            }
        }
        List<MidiTrack> new_tracks = new ArrayList<MidiTrack>();

        new_tracks.add(noteTrack);

        MidiFile midi = new MidiFile(MidiFile.DEFAULT_RESOLUTION, new_tracks);
        midi.setActivity(activity);
        // 4. Write the MIDI data to a file
        String sd = Environment.getExternalStorageDirectory().getAbsolutePath();
        String mPath = sd+"/temp.mid";

        try {
            File f = new File(mPath);
            if (f.exists())
                f.delete();
            midi.writeToFile(mPath);
            MidiFile temp = ((ModifySheetMusicActivity) ModifySheetMusicActivity.modifyContext).changeMidiFile(mPath);

            if (temp != null) {
                midifile = temp;
                ((ModifySheetMusicActivity) ModifySheetMusicActivity.modifyContext).setMidiTracks(temp.getTracks());
                CreateMidiFile();
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }
    void divideNote(){
        /*선택한 음표를 반박자를 가진 2개의 음표로 분할한다.*/
        sheet.divideNote((int)currentPulseTime);
        midifile.setTracks(sheet.getTracks());
        timer.postDelayed(ReShade,100);
        createTempMidiFile();
    }

    void deleteNote(){
        /*선택한 음표 삭제*/
        sheet.deleteNote((int)currentPulseTime);
        midifile.setTracks(sheet.getTracks());
        timer.postDelayed(ReShade,100);
        createTempMidiFile();
    }

    void addFlat(){
        /*선택된 음표를 반음 내린다*/
        sheet.changeNoteKey(0,(int)currentPulseTime);
        midifile.setTracks(sheet.getTracks());
        timer.postDelayed(ReShade,100);
        createTempMidiFile();
    }

    void addSharp(){
        /*선택된 음표를 반음 올린다*/
        sheet.changeNoteKey(1,(int)currentPulseTime);
        midifile.setTracks(sheet.getTracks());
        timer.postDelayed(ReShade,100);
        createTempMidiFile();
    }

    void changeLength(){
        /*선택된 음표의 음 길이를 수정한다.
          120~1080으로 수정 된다.
          즉 16분음표 부터 점 음표 포함하여 온음표 까지*/
        sheet.changeNoteDuration((int)currentPulseTime);
        midifile.setTracks(sheet.getTracks());
        timer.postDelayed(ReShade,100);
        createTempMidiFile();
    }

    /** Rewind the midi music back one measure.
     *  The music must be in the paused state.
     *  When we resume in playPause, we start at the currentPulseTime.
     *  So to rewind, just decrease the currentPulseTime,
     *  and re-shade the sheet music.
     */
    void moveLeft() {
        if (midifile == null || sheet == null || playstate != paused) {
            return;
        }

        /* Remove any highlighted notes */
        sheet.ShadeNotes(-10, (int)currentPulseTime, SheetMusic.DontScroll);
        piano.ShadeNotes(-10, (int)currentPulseTime);

        playstate = paused;

        ArrayList<MidiNote>notes = sheet.getTracks().get(0).getNotes();
        int note_location = sheet.getNote_location();

        if(note_location==0) {
            /*처음일 때의 예외처리
            * 처음일 경우는 이동을 하지않고 그대로 놔둔다.
            * currentPulseTime과 prevPulseTime을 초기화 시킨다.
            */
            currentPulseTime = -10;
            prevPulseTime = currentPulseTime;
            currentPulseTime = notes.get(note_location).getStartTime();
        }
        else {
            if (prevPulseTime >= 0) {
                prevPulseTime = notes.get(note_location).getStartTime();

                note_location--;
                currentPulseTime = notes.get(note_location).getStartTime();
                sheet.setNote_location(note_location);
            } else {
                prevPulseTime = currentPulseTime;
                currentPulseTime = notes.get(note_location).getStartTime();
            }
        }
        sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.ImmediateScroll);
        piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
    }

    /** Fast forward the midi music by one measure.
     *  The music must be in the paused/stopped state.
     *  When we resume in playPause, we start at the currentPulseTime.
     *  So to fast forward, just increase the currentPulseTime,
     *  and re-shade the sheet music.
     */
    void moveRight() {
        if (midifile == null || sheet == null) {
            return;
        }
        if (playstate != paused && playstate != stopped) {
            return;
        }
        playstate = paused;

        /* Remove any highlighted notes */
        sheet.ShadeNotes(-10, (int)currentPulseTime, SheetMusic.DontScroll);
        piano.ShadeNotes(-10, (int)currentPulseTime);


        ArrayList<MidiNote>notes = sheet.getTracks().get(0).getNotes();
        int size = notes.size();
        int note_location = sheet.getNote_location();

        if(currentPulseTime>=0&& note_location==0) {
            /*처음일 때 인지*/
            note_location++;
            sheet.setNote_location(note_location);
        }

        if(prevPulseTime>=0) {
            /*나의 다음 음표로 이동 시키는 법
            * prevPulseTime에서 currentPulseTime을 비교하여 그림을 그려준다.
            * 따라서 prevPulseTime은 바로 전 음표, currentPulseTime은 그림을 그려야할 음표 및 고쳐야할 음표*/

            if (note_location + 1 >= size)
                return;

            prevPulseTime = notes.get(note_location).getStartTime();

            note_location++;
            currentPulseTime = notes.get(note_location).getStartTime();
            sheet.setNote_location(note_location);
        }
        else{
            prevPulseTime = currentPulseTime;
            currentPulseTime = notes.get(note_location).getStartTime();
        }
        sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.ImmediateScroll);
        piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
    }

    private void chordRecommend(){
        MidiFile temp = ((ModifySheetMusicActivity) ModifySheetMusicActivity.modifyContext).getMidifile();
        ArrayList<MidiTrack> tracks = temp.getTracks();
        //"",key_sig,tempo,pitch,velocity,start_time,duration
        List<String[]> csv_data = new ArrayList<String[]>();
        csv_data.add(new String[] {"","key_sig","tempo","pitch","velocity","start_time","duration"});
        TimeSignature ts = temp.getTime();

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
        String path = Environment.getExternalStorageDirectory().getAbsolutePath()+"/melody_temp.csv";
        File f = new File(path);
        if(f.exists())
            f.delete();
        CSVWrite csvWrite = new CSVWrite(path);
        csvWrite.writeCsv(csv_data);
        SharedPreferences pref = activity.getSharedPreferences("MuzixAutoLogin", Activity.MODE_PRIVATE);
        String writer_id = pref.getString("MUZIXID", null);
        new FileUploader(f).execute(activity.getString(R.string.chord_recommend),writer_id);
    }
    /** Move the current position to the location clicked.
     *  The music must be in the paused/stopped state.
     *  When we resume in playPause, we start at the currentPulseTime.
     *  So, set the currentPulseTime to the position clicked.
     */
    public void MoveToClicked(int x, int y) {
        if (midifile == null || sheet == null) {
            return;
        }
        if (playstate != paused && playstate != stopped) {
            return;
        }
        playstate = paused;

        /* Remove any highlighted notes */
        sheet.ShadeNotes(-10, (int)currentPulseTime, SheetMusic.DontScroll);
        piano.ShadeNotes(-10, (int)currentPulseTime);

        currentPulseTime = sheet.PulseTimeForPoint(new Point(x, y));
        prevPulseTime = currentPulseTime - midifile.getTime().getMeasure();
        if (currentPulseTime > midifile.getTotalPulses()) {
            currentPulseTime -= midifile.getTime().getMeasure();
        }
        sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.DontScroll);
        piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
    }


    /** The callback for the timer. If the midi is still playing,
     *  update the currentPulseTime and shade the sheet music.
     *  If a stop or pause has been initiated (by someone clicking
     *  the stop or pause button), then stop the timer.
     */
    Runnable TimerCallback = new Runnable() {
        public void run() {
            if (midifile == null || sheet == null) {
                playstate = stopped;
            }
            else if (playstate == stopped || playstate == paused) {
                /* This case should never happen */
                return;
            }
            else if (playstate == initStop) {
                return;
            }
            else if (playstate == playing) {
                long msec = SystemClock.uptimeMillis() - startTime;
                prevPulseTime = currentPulseTime;
                currentPulseTime = startPulseTime + msec * pulsesPerMsec;

                /* If we're playing in a loop, stop and restart */
                if (options.playMeasuresInLoop) {
                    double nearEndTime = currentPulseTime + pulsesPerMsec*10;
                    int measure = (int)(nearEndTime / midifile.getTime().getMeasure());
                    if (measure > options.playMeasuresInLoopEnd) {
                        RestartPlayMeasuresInLoop();
                        return;
                    }
                }

                /* Stop if we've reached the end of the song */
                if (currentPulseTime > midifile.getTotalPulses()) {
                    DoStop();
                    return;
                }
                sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.GradualScroll);
                piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
                timer.postDelayed(TimerCallback, 100);
            }
            else if (playstate == initPause) {
                long msec = SystemClock.uptimeMillis() - startTime;
                StopSound();

                prevPulseTime = currentPulseTime;
                currentPulseTime = startPulseTime + msec * pulsesPerMsec;
                sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.ImmediateScroll);
                piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
                playstate = paused;
                timer.postDelayed(ReShade, 1000);
            }
        }
    };

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
    /** The "Play Measures in a Loop" feature is enabled, and we've reached
     *  the last measure. Stop the sound, unshade the music, and then
     *  start playing again.
     */
    private void RestartPlayMeasuresInLoop() {
        playstate = stopped;
        piano.ShadeNotes(-10, (int)prevPulseTime);
        sheet.ShadeNotes(-10, (int)prevPulseTime, SheetMusic.DontScroll);
        currentPulseTime = 0;
        prevPulseTime = -1;
        StopSound();
        timer.postDelayed(DoPlay, 300);
    }

    private class PythonServerConnector extends AsyncTask<Void,Void, String> {
        private String serverIP = "54.180.95.158";

        private int serverPort = 8011;


        private String sd;
        private InetAddress serverAddr;
        private Socket sock;
        private String member_id;
        private PrintWriter printwriter;
        private BufferedReader bufferedReader;

        private int type = 0;
        public PythonServerConnector(String keysig, String member_id) {
            this.member_id = member_id;
            sd = Environment.getExternalStorageDirectory().getAbsolutePath();
        }


        @Override
        protected String doInBackground(Void... voids) {
            try {
                sock = new Socket(serverIP, serverPort); // Creating the server socket.


                if (sock != null) {

                    //자동 flushing 기능이 있는 PrintWriter 객체를 생성한다.

                    //client.getOutputStream() 서버에 출력하기 위한 스트림을 얻는다.

                    printwriter = new PrintWriter(sock.getOutputStream(), true);

                    InputStreamReader isr = new InputStreamReader(sock.getInputStream());

                    printwriter.write("type" + "\n" + "modify" + "\n");
                    printwriter.flush();

                    //입력 스트림 inputStreamReader에 대해 기본 크기의 버퍼를 갖는 객체를 생성한다.

                    bufferedReader = new BufferedReader(isr);

                    while (true) {
                        try {
                            //스트림으로부터 읽어올수 있으면 true를 반환한다.
                            if (bufferedReader.ready()) {
                                //'\n', '\r'을 만날 때까지 읽어온다.(한줄을 읽어온다.)
                                String message = bufferedReader.readLine();
                                if(message.equals("getType")){
                                    printwriter.write("member_id" + "\n" + member_id + "\n");
                                    printwriter.flush();
                                    publishProgress(null);
                                    continue;
                                }
                                else if (message.equals("getMemberId")) {
                                    printwriter.write("generate" + "\n");
                                    printwriter.flush();
                                    publishProgress(null);
                                    continue;
                                }  else if (message.equals("generated")) {
                                    bufferedReader.close();
                                    isr.close();
                                    printwriter.close();
                                    sock.close();
                                    return "success";
                                }
                            }
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    errorHandle("서버 연결을 실패했습니다.");
                }
            } catch (UnknownHostException e) {
                errorHandle("서버 연결을 실패했습니다.");
                e.printStackTrace();
            } catch (IOException e) {
                errorHandle("서버 연결을 실패했습니다.");
                e.printStackTrace();

            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s.equals("success")) {
                ((ModifySheetMusicActivity)ModifySheetMusicActivity.modifyContext).setChords(
                        "http://54.180.95.158:8080/server/filestorage/"+member_id+"/chord.txt");
            } else
                errorHandle("파일 불러오기를 실패했습니다.");
        }
    }
    private class FileUploader extends AsyncTask<String, Void, JSONObject> {
        File file;

        public FileUploader(File file) {
            this.file = file;
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
                writer.append(strings[1]).append(LINE_FEED);
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
                    SharedPreferences pref = activity.getSharedPreferences("MuzixAutoLogin", Activity.MODE_PRIVATE);
                    String writer_id = pref.getString("MUZIXID", null);
                    String key_sig = midifile.getTime().getNumerator()+"/"+midifile.getTime().getDenominator();
                    new PythonServerConnector(key_sig, writer_id).execute();
                } else {
                    errorHandle("네트워크 오류");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}



