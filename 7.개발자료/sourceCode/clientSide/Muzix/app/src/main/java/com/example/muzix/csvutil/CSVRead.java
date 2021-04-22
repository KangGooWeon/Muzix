package com.example.muzix.csvutil;

import android.app.Activity;
import android.os.Environment;

import com.opencsv.CSVReader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/*@Class CSVRead
* Read CSV to data*/
public class CSVRead {
    private String path;

    public CSVRead(String path) {
        this.path = path;
    }

    public List<String[]> readCsv() {

        List<String[]> data = new ArrayList<String[]>();

        try {
            // CSVReader reader = new CSVReader(new FileReader(filename), '\t');
            // UTF-8
            CSVReader reader = new CSVReader(new InputStreamReader(new FileInputStream(
                    path), "UTF-8"));
            String[] s;

            while ((s = reader.readNext()) != null) {
                data.add(s);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
}
