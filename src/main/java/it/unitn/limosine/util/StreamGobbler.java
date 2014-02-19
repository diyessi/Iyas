package it.unitn.limosine.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/***
 * Class to handle process output/input stream reading without deadlocking
 * 
 * Inspired from: http://stackoverflow.com/questions/1068713/how-can-i-write-large-output-to-process-getoutputstream
 */
public class StreamGobbler implements Runnable {
    private BufferedReader reader;
    private List<String> output;

    public StreamGobbler(InputStream inputStream) {
            this.reader = new BufferedReader(new InputStreamReader(inputStream));
    }

    public void run() {
            String line;
            this.output = new ArrayList<String>();
            try {
                    while((line = this.reader.readLine()) != null) {
                            this.output.add(line + "\n");
                    }
                    this.reader.close();
            }
            catch (IOException e) {
                    System.err.println("ERROR: " + e.getMessage());
            }
    }

    public List<String> getOuput() {
            return this.output;
    }
}
