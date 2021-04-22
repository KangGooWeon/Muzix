package com.example.muzix;

public class MeasureChordSymbol {
    private int starttime;   /** The start time, in pulses */
    private String text;     /** The chord text */
    private int x;           /** The x (horizontal) position within the staff */

    public MeasureChordSymbol(int starttime, String text) {
        this.starttime = starttime;
        this.text = text;
    }

    public int getStartTime() { return starttime; }
    public void setStartTime(int value) { starttime = value; }

    public String getText() { return text; }
    public void setText(String value) { text = value; }

    public int getX() { return x; }
    public void setX(int value) { x = value; }

    /* Return the minimum width in pixels needed to display this chord.
     * This is an estimation, not exact.
     */
    public int getMinWidth() {
        float widthPerChar = 10.0f * 2.0f/3.0f;
        float width = text.length() * widthPerChar;
        if (text.contains("i")) {
            width -= widthPerChar/2.0f;
        }
        if (text.contains("j")) {
            width -= widthPerChar/2.0f;
        }
        if (text.contains("l")) {
            width -= widthPerChar/2.0f;
        }
        return (int)width;
    }

    @Override
    public String toString() {
        return String.format("Chord start=%1$s x=%2$s text=%3$s",
                starttime, x, text);
    }
}
