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

import com.example.muzix.meta.GenericMetaEvent;
import com.example.muzix.meta.MetaEvent;
import com.example.muzix.util.MidiEventWriter;
import com.example.muzix.util.VariableLengthInt;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;

/** @class TimeSignature
 * The TimeSignature class represents
 * - The time signature of the song, such as 4/4, 3/4, or 6/8 time, and
 * - The number of pulses per quarter note
 * - The number of microseconds per quarter note
 *
 * In midi files, all time is measured in "pulses".  Each note has
 * a start time (measured in pulses), and a duration (measured in 
 * pulses).  This class is used mainly to convert pulse durations
 * (like 120, 240, etc) into note durations (half, quarter, eighth, etc).
 */

public class TimeSignature extends MetaEvent implements Serializable {
    public static final int METER_EIGHTH = 12;
    public static final int METER_QUARTER = 24;
    public static final int METER_HALF = 48;
    public static final int METER_WHOLE = 96;

    public static final int DEFAULT_METER = METER_QUARTER;
    public static final int DEFAULT_DIVISION = 8;
    private int mMeter;
    private int mDivision;


    private int numerator;      /** Numerator of the time signature */
    private int denominator;    /** Denominator of the time signature */
    private int quarternote;    /** Number of pulses per quarter note */
    private int measure;        /** Number of pulses per measure */
    private int tempo;          /** Number of microseconds per quarter note */

    /** Get the numerator of the time signature */
    public int getNumerator() { return numerator; }

    /** Get the denominator of the time signature */
    public int getDenominator() { return denominator; }

    /** Get the number of pulses per quarter note */
    public int getQuarter() { return quarternote; }

    /** Get the number of pulses per measure */
    public int getMeasure() { return measure; }

    /** Get the number of microseconds per quarter note */ 
    public int getTempo() { return tempo; }

    /** Create a new time signature, with the given numerator,
     * denominator, pulses per quarter note, and tempo.
     */
    public TimeSignature(int numerator, int denominator, int quarternote, int tempo) {
        super(0,0,0,null);
        if (numerator <= 0 || denominator <= 0 || quarternote <= 0) {
            throw new MidiFileException("Invalid time signature", 0);
        }

        /* Midi File gives wrong time signature sometimes */
        if (numerator == 5) {
            numerator = 4;
        }

        this.numerator = numerator;
        this.denominator = denominator;
        this.quarternote = quarternote;
        this.tempo = tempo;

        int beat;
        if (denominator < 4)
            beat = quarternote * 2;
        else
            beat = quarternote / (denominator/4);

        measure = numerator * beat;
    }

    /** Return which measure the given time (in pulses) belongs to. */
    public int GetMeasure(int time) {
        return time / measure;
    }

    /** Given a duration in pulses, return the closest note duration. */
    public NoteDuration GetNoteDuration(int duration) {
        int whole = quarternote * 4;

        /**
         1       = 32/32
         3/4     = 24/32
         1/2     = 16/32
         3/8     = 12/32
         1/4     =  8/32
         3/16    =  6/32
         1/8     =  4/32 =    8/64
         triplet         = 5.33/64
         1/16    =  2/32 =    4/64
         1/32    =  1/32 =    2/64
         **/ 

        if      (duration >= 28*whole/32)
            return NoteDuration.Whole;
        else if (duration >= 20*whole/32) 
            return NoteDuration.DottedHalf;
        else if (duration >= 14*whole/32)
            return NoteDuration.Half;
        else if (duration >= 10*whole/32)
            return NoteDuration.DottedQuarter;
        else if (duration >=  7*whole/32)
            return NoteDuration.Quarter;
        else if (duration >=  5*whole/32)
            return NoteDuration.DottedEighth;
        else if (duration >=  6*whole/64)
            return NoteDuration.Eighth;
        else if (duration >=  5*whole/64)
            return NoteDuration.Triplet;
        else if (duration >=  3*whole/64)
            return NoteDuration.Sixteenth;
        else
            return NoteDuration.ThirtySecond;
    }

    /** Convert a note duration into a stem duration.  Dotted durations
     * are converted into their non-dotted equivalents.
     */
    public static NoteDuration GetStemDuration(NoteDuration dur) {
        if (dur == NoteDuration.DottedHalf)
            return NoteDuration.Half;
        else if (dur == NoteDuration.DottedQuarter)
            return NoteDuration.Quarter;
        else if (dur == NoteDuration.DottedEighth)
            return NoteDuration.Eighth;
        else
            return dur;
    }

    /** Return the time period (in pulses) the the given duration spans */
    public int DurationToTime(NoteDuration dur) {
        int eighth = quarternote/2;
        int sixteenth = eighth/2;

        switch (dur) {
            case Whole:         return quarternote * 4; 
            case DottedHalf:    return quarternote * 3; 
            case Half:          return quarternote * 2; 
            case DottedQuarter: return 3*eighth; 
            case Quarter:       return quarternote; 
            case DottedEighth:  return 3*sixteenth;
            case Eighth:        return eighth;
            case Triplet:       return quarternote/3; 
            case Sixteenth:     return sixteenth;
            case ThirtySecond:  return sixteenth/2; 
            default:                         return 0;
       }
    }

    /*For Write Mdi File*/
    public TimeSignature()
    {
        this(0, 0, 4, 4, DEFAULT_METER, DEFAULT_DIVISION);
    }

    public TimeSignature(long tick, long delta, int num, int den, int meter, int div)
    {
        super(tick, delta, MetaEvent.TIME_SIGNATURE, new VariableLengthInt(4));

        setTimeSignature(num, den, meter, div);
    }

    public void setTimeSignature(int num, int den, int meter, int div)
    {
        numerator = num;
        denominator = log2(den);
        mMeter = meter;
        mDivision = div;
    }

    public int getRealDenominator()
    {
        return (int) Math.pow(2, denominator);
    }

    public int getMeter()
    {
        return mMeter;
    }

    public int getDivision()
    {
        return mDivision;
    }

    @Override
    protected int getEventSize()
    {
        return 7;
    }

    @Override
    public void writeToFile(OutputStream out) throws IOException
    {
        super.writeToFile(out);

        out.write(4);
        out.write(numerator);
        out.write(denominator);
        out.write(mMeter);
        out.write(mDivision);
    }

    public static MetaEvent parseTimeSignature(long tick, long delta, MetaEventData info)
    {
        if(info.length.getValue() != 4)
        {
            return new GenericMetaEvent(tick, delta, info);
        }

        int num = info.data[0];
        int den = info.data[1];
        int met = info.data[2];
        int fps = info.data[3];

        den = (int) Math.pow(2, den);

        return new TimeSignature(tick, delta, num, den, met, fps);
    }

    private int log2(int den)
    {
        switch(den)
        {
            case 2:
                return 1;
            case 4:
                return 2;
            case 8:
                return 3;
            case 16:
                return 4;
            case 32:
                return 5;
        }
        return 0;
    }
    public int compareTo(MidiEventWriter other)
    {
        if(mTick != other.getTick())
        {
            return mTick < other.getTick() ? -1 : 1;
        }
        if(mDelta.getValue() != other.getDelta())
        {
            return mDelta.getValue() < other.getDelta() ? 1 : -1;
        }

        if(!(other instanceof TimeSignature))
        {
            return 1;
        }

        TimeSignature o = (TimeSignature) other;

        if(numerator != o.numerator)
        {
            return numerator < o.numerator ? -1 : 1;
        }
        if(denominator != o.denominator)
        {
            return denominator < o.denominator ? -1 : 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return String.format("TimeSignature=%1$s/%2$s quarter=%3$s tempo=%4$s",
                             numerator, denominator, quarternote, tempo);
    }

}


