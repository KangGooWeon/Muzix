package com.example.muzix.csvutil;

import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/*@Class CSVWrite
* write data to CSV*/
public class CSVWrite {

    private String path;

    public CSVWrite(String path) {
        this.path = path;
    }
    public void writeCsv(List<String[]> data) {
        try {
            CSVWriter cw = new CSVWriter(new FileWriter(path), ',', '"');
            Iterator<String[]> it = data.iterator();
            try {
                while (it.hasNext()) {
                    String[] s = (String[]) it.next();
                    cw.writeNext(s);
                }
            } finally {
                cw.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}