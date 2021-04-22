/*
 * Copyright (c) 2007-2011 Madhav Vaidyanathan
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

import com.example.muzix.meta.EndOfTrack;
import com.example.muzix.meta.Tempo;
import com.example.muzix.util.MidiEventWriter;
import com.example.muzix.util.MidiUtil;
import com.example.muzix.util.VariableLengthInt;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;


/** @class MidiTrack
 * The MidiTrack takes as input the raw MidiEvents for the track, and gets:
 * - The list of midi notes in the track.
 * - The first instrument used in the track.
 *
 * For each NoteOn event in the midi file, a new MidiNote is created
 * and added to the track, using the AddNote() method.
 * 
 * The NoteOff() method is called when a NoteOff event is encountered,
 * in order to update the duration of the MidiNote.
 */ 
public class MidiTrack {
    private static final boolean VERBOSE = false;

    public static final byte[] IDENTIFIER = { 'M', 'T', 'r', 'k' };

    private int mSize;
    private boolean mSizeNeedsRecalculating;
    private boolean mClosed;
    private long mEndOfTrackDelta;

    private TreeSet<MidiEventWriter> mEvents;

    private int tracknum;                 /** The track number */
    private ArrayList<MidiNote> notes;    /** List of Midi notes */
    private int instrument;               /** Instrument for this track */
    private ArrayList<MidiEvent> lyrics;  /** The lyrics in this track */

    public MidiTrack()
    {
        mEvents = new TreeSet<MidiEventWriter>();
        mSize = 0;
        mSizeNeedsRecalculating = false;
        mClosed = false;
        mEndOfTrackDelta = 0;
    }

    public MidiTrack(InputStream in) throws IOException
    {
        this();

        byte[] buffer = new byte[4];
        in.read(buffer);

        if(!MidiUtil.bytesEqual(buffer, IDENTIFIER, 0, 4))
        {
            System.err.println("Track identifier did not match MTrk!");
            return;
        }

        in.read(buffer);
        mSize = MidiUtil.bytesToInt(buffer, 0, 4);

        buffer = new byte[mSize];
        in.read(buffer);

        this.readTrackData(buffer);
    }
    /** Create an empty MidiTrack.  Used by the Clone method */
    public MidiTrack(int tracknum) {
        this.tracknum = tracknum;
        notes = new ArrayList<MidiNote>(20);
        instrument = 0;
    } 

    /** Create a MidiTrack based on the Midi events.  Extract the NoteOn/NoteOff
     *  events to gather the list of MidiNotes.
     */
    public MidiTrack(ArrayList<MidiEvent> events, int tracknum) {
        this.tracknum = tracknum;
        notes = new ArrayList<MidiNote>(events.size());
        instrument = 0;
 
        for (MidiEvent mevent : events) {
            if (mevent.EventFlag == MidiFile.EventNoteOn && mevent.Velocity > 0) {
                MidiNote note = new MidiNote(mevent.StartTime, mevent.Channel, mevent.Notenumber, 0);
                AddNote(note);
            }
            else if (mevent.EventFlag == MidiFile.EventNoteOn && mevent.Velocity == 0) {
                NoteOff(mevent.Channel, mevent.Notenumber, mevent.StartTime);
            }
            else if (mevent.EventFlag == MidiFile.EventNoteOff) {
                NoteOff(mevent.Channel, mevent.Notenumber, mevent.StartTime);
            }
            else if (mevent.EventFlag == MidiFile.EventProgramChange) {
                instrument = mevent.Instrument;
            }
            else if (mevent.Metaevent == MidiFile.MetaEventLyric) {
                AddLyric(mevent);
                if (lyrics == null) {
                    lyrics = new ArrayList<MidiEvent>();
                }
                lyrics.add(mevent);
            }
        }
        if (notes.size() > 0 && notes.get(0).getChannel() == 9)  {
            instrument = 128;  /* Percussion */
        }
    }

    public int trackNumber() { return tracknum; }

    public void setNotes(ArrayList<MidiNote> notes){
        this.notes = notes;
    }
    public ArrayList<MidiNote> getNotes() { return notes; }

    public int getInstrument() { return instrument; }
    public void setInstrument(int value) { instrument = value; }

    public ArrayList<MidiEvent> getLyrics() { return lyrics; }
    public void setLyrics(ArrayList<MidiEvent> value) { lyrics = value; }


    public String getInstrumentName() { if (instrument >= 0 && instrument <= 128)
                  return MidiFile.Instruments[instrument];
              else
                  return "";
            }

    /** Add a MidiNote to this track.  This is called for each NoteOn event */
    public void AddNote(MidiNote m) {
        notes.add(m);
    }

    /** A NoteOff event occured.  Find the MidiNote of the corresponding
     * NoteOn event, and update the duration of the MidiNote.
     */
    public void NoteOff(int channel, int notenumber, int endtime) {
        for (int i = notes.size()-1; i >= 0; i--) {
            MidiNote note = notes.get(i);
            if (note.getChannel() == channel && note.getNumber() == notenumber &&
                note.getDuration() == 0) {
                note.NoteOff(endtime);
                return;
            }
        }
    }

    /** Add a lyric event to this track */
    public void AddLyric(MidiEvent mevent) { 
        if (lyrics == null) {
            lyrics = new ArrayList<MidiEvent>();
        }
        lyrics.add(mevent);
    }

    /** Return a deep copy clone of this MidiTrack. */
    public MidiTrack Clone() {
        MidiTrack track = new MidiTrack(trackNumber());
        track.instrument = instrument;
        for (MidiNote note : notes) {
            track.notes.add( note.Clone() );
        }
        if (lyrics != null) {
            track.lyrics = new ArrayList<MidiEvent>();
            track.lyrics.addAll(lyrics);
        }
        return track;
    }


    /*======================================================================================================*/
    /*                                                                                                      */
    /*                                        This is For Write midiFile                                    */
    /*                                                                                                      */
    /*                                                                                                      */
    /*                                                                                                      */
    /*======================================================================================================*/
    public static MidiTrack createTempoTrack()
    {
        MidiTrack T = new MidiTrack();

        T.insertEvent(new TimeSignature());
        T.insertEvent(new Tempo());

        return T;
    }

    private void readTrackData(byte[] data) throws IOException
    {
        InputStream in = new ByteArrayInputStream(data);

        long totalTicks = 0;

        while(in.available() > 0)
        {
            VariableLengthInt delta = new VariableLengthInt(in);
            totalTicks += delta.getValue();

            MidiEventWriter E = MidiEventWriter.parseEvent(totalTicks, delta.getValue(), in);
            if(E == null)
            {
                System.out.println("Event skipped!");
                continue;
            }

            if(VERBOSE)
            {
                System.out.println(E);
            }

            // Not adding the EndOfTrack event here allows the track to be
            // edited
            // after being read in from file.
            if(E.getClass().equals(EndOfTrack.class))
            {
                mEndOfTrackDelta = E.getDelta();
                break;
            }
            mEvents.add(E);
        }
    }

    public TreeSet<MidiEventWriter> getEvents()
    {
        return mEvents;
    }

    public int getEventCount()
    {
        return mEvents.size();
    }

    public int getSize()
    {
        if(mSizeNeedsRecalculating)
        {
            recalculateSize();
        }
        return mSize;
    }

    public long getLengthInTicks()
    {
        if(mEvents.size() == 0)
        {
            return 0;
        }

        MidiEventWriter E = mEvents.last();
        return E.getTick();
    }

    public long getEndOfTrackDelta()
    {
        return mEndOfTrackDelta;
    }

    public void setEndOfTrackDelta(long delta)
    {
        mEndOfTrackDelta = delta;
    }

    public void insertNote(int channel, int pitch, int velocity, long tick, long duration)
    {

        insertEvent(new NoteOn(tick, channel, pitch, velocity));
        insertEvent(new NoteOn(tick + duration, channel, pitch, 0));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void insertEvent(MidiEventWriter newEvent)
    {
        if(newEvent == null)
        {
            return;
        }

        if(mClosed)
        {
            System.err.println("Error: Cannot add an event to a closed track.");
            return;
        }

        MidiEventWriter prev = null, next = null;

        // floor() and ceiling() are not supported on Android before API Level 9
        // (Gingerbread)
        try
        {
            Class treeSet = Class.forName("java.util.TreeSet");
            Method floor = treeSet.getMethod("floor", Object.class);
            Method ceiling = treeSet.getMethod("ceiling", Object.class);

            prev = (MidiEventWriter) floor.invoke(mEvents, newEvent);
            next = (MidiEventWriter) ceiling.invoke(mEvents, newEvent);

        }
        catch(Exception e)
        {
            // methods are not supported - must perform linear search
            Iterator<MidiEventWriter> it = mEvents.iterator();

            while(it.hasNext())
            {
                next = it.next();

                if(next.getTick() > newEvent.getTick())
                {
                    break;
                }

                prev = next;
                next = null;
            }
        }

        mEvents.add(newEvent);
        mSizeNeedsRecalculating = true;

        // Set its delta time based on the previous event (or itself if no
        // previous event exists)
        if(prev != null)
        {
            newEvent.setDelta(newEvent.getTick() - prev.getTick());
        }
        else
        {
            newEvent.setDelta(newEvent.getTick());
        }

        // Update the next event's delta time relative to the new event.
        if(next != null)
        {
            next.setDelta(next.getTick() - newEvent.getTick());
        }

        mSize += newEvent.getSize();

        if(newEvent.getClass().equals(EndOfTrack.class))
        {
            if(next != null)
            {
                throw new IllegalArgumentException("Attempting to insert EndOfTrack before an existing event. Use closeTrack() when finished with MidiTrack.");
            }
            mClosed = true;
        }
    }

    public boolean removeEvent(MidiEvent E)
    {
        Iterator<MidiEventWriter> it = mEvents.iterator();
        MidiEventWriter prev = null, curr = null, next = null;

        while(it.hasNext())
        {
            next = it.next();

            if(E.equals(curr))
            {
                break;
            }

            prev = curr;
            curr = next;
            next = null;
        }

        if(next == null)
        {
            // Either the event was not found in the track,
            // or this is the last event in the track.
            // Either way, we won't need to update any delta times
            return mEvents.remove(curr);
        }

        if(!mEvents.remove(curr))
        {
            return false;
        }

        if(prev != null)
        {
            next.setDelta(next.getTick() - prev.getTick());
        }
        else
        {
            next.setDelta(next.getTick());
        }
        return true;
    }

    public void closeTrack()
    {
        long lastTick = 0;
        if(mEvents.size() > 0)
        {
            MidiEventWriter last = mEvents.last();
            lastTick = last.getTick();
        }
        EndOfTrack eot = new EndOfTrack(lastTick + mEndOfTrackDelta, 0);
        insertEvent(eot);
    }

    public void dumpEvents()
    {
        Iterator<MidiEventWriter> it = mEvents.iterator();
        while(it.hasNext())
        {
            System.out.println(it.next());
        }
    }

    private void recalculateSize()
    {
        mSize = 0;

        Iterator<MidiEventWriter> it = mEvents.iterator();
        MidiEventWriter last = null;
        while(it.hasNext())
        {
            MidiEventWriter E = it.next();
            mSize += E.getSize();

            // If an event is of the same type as the previous event,
            // no status byte is written.
            if(last != null && !E.requiresStatusByte(last))
            {
                mSize--;
            }
            last = E;
        }

        mSizeNeedsRecalculating = false;
    }

    public void writeToFile(OutputStream out) throws IOException
    {
        if(!mClosed)
        {
            closeTrack();
        }

        if(mSizeNeedsRecalculating)
        {
            recalculateSize();
        }

        out.write(IDENTIFIER);
        out.write(MidiUtil.intToBytes(mSize, 4));

        Iterator<MidiEventWriter> it = mEvents.iterator();
        MidiEventWriter lastEvent = null;

        while(it.hasNext())
        {
            MidiEventWriter event = it.next();
            if(VERBOSE)
            {
                System.out.println("Writing: " + event);
            }

            event.writeToFile(out, event.requiresStatusByte(lastEvent));

            lastEvent = event;
        }
    }
    /*======================================================================================================*/
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(
                "Track number=" + tracknum + " instrument=" + instrument + "\n");
        for (MidiNote n : notes) {
           result.append(n).append("\n");
        }
        result.append("End Track\n");
        return result.toString();
    }
}


