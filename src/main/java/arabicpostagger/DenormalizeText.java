/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arabicpostagger;

import static arabicpostagger.ArabicUtils.normalizeFull;
import static arabicpostagger.ArabicUtils.removeDiacritics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author kareemdarwish
 */
public class DenormalizeText {

    private static String baseDir = "";
    private static String kenlmDir = "";
    private static Process process = null;
    private static BufferedReader brLM = null;
    private static BufferedWriter bwLM = null;
    private static HashMap<String, String> candidatesUnigram = new HashMap<String, String>();

    public DenormalizeText(String dir, String lmDir) throws IOException, FileNotFoundException, ClassNotFoundException, InterruptedException {
        baseDir = dir;
        kenlmDir = lmDir;
        if (!dir.endsWith("/")) {
            baseDir += "/";
        }
        if (!kenlmDir.endsWith("/")) {
            kenlmDir += "/";
        }

        String[] args = {
            kenlmDir + "query",
            baseDir + "lm.bin"
        };
        try {
            process = new ProcessBuilder(args).start();

            brLM = new BufferedReader(new InputStreamReader(process.getInputStream()));
            bwLM = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

            candidatesUnigram = loadCandidates(baseDir + "lm.words.uni.trim2");
        } 
        catch(IOException ex){
            //System.out.println (ex.toString());
            System.out.println("No such file or directory! " + kenlmDir);
            System.exit(0); // ???
        }
        
    }

    public String denormalize(String input) throws IOException {
        HashMap<Integer, ArrayList<String>> latice = buildLatice(ArabicUtils.tokenize(input));
        return findBestPath(latice);
    }

    private HashMap<String, String> loadCandidates(String filePath) throws FileNotFoundException, IOException, ClassNotFoundException {
        HashMap<String, String> candidates = new HashMap<String, String>();
        // char[] tab = {'\t'};

        String line = "";
        // if (wikipediaArEn.isEmpty())
        File file = new File(filePath + ".ser");
        if (file.exists()) {

            // System.out.println(df.format(cal.getTime()));
            ObjectInputStream ios = new ObjectInputStream(new FileInputStream(file));
            candidates = (HashMap) ios.readObject();
            // System.out.println(df.format(cal.getTime()));
        } else {
            BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(filePath))));
            while ((line = sr.readLine()) != null) {
                if (line.length() > 0) {
                    String[] lineParts = line.split("\t");
                    if (line.length() > 0 && lineParts.length > 0) // && Regex.IsMatch("^[0-9\\.\\-]$"))
                    {
                        candidates.put(lineParts[0], lineParts[1]);
                        /*
                         String norm = normalizeFull(line);
                         if (!candidates.containsKey(norm)) {
                         candidates.put(norm, line);
                         }
                         else
                         {
                         String temp = candidates.get(norm) + " " + line;
                         candidates.put(norm, temp);
                         }
                         */
                    }
                }
            }
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(candidates);
            oos.close();
        }
        return candidates;
    }

    private String findBestPath(HashMap<Integer, ArrayList<String>> latice) throws IOException {
        String space = " +";
        HashMap<Integer, String> finalAnswer = new HashMap<Integer, String>();
        finalAnswer.put(0, "<s>");

        for (int i = 1; i <= latice.keySet().size() - 1; i++) {
            String sBase = finalAnswer.get(0);
            for (int j = 1; j < i; j++) {
                sBase += " " + finalAnswer.get(j);
            }

            ArrayList<String> paths = new ArrayList<String>();
            // add options for current node
            for (String sol : latice.get(i)) {
                paths.add(sBase + " " + correctLeadingLamAlefLam(sol));
            }

            ArrayList<String> pathsNext = new ArrayList<String>();
            // add options for next node
            for (String s : paths) {
                for (String sol : latice.get(i + 1)) {
                    pathsNext.add(s + " " + correctLeadingLamAlefLam(sol));
                }
            }

            // determine best option for current word
            // this would be done using the language model
            String[] bestPath = findBestPathLM(pathsNext).trim().split(space);
            if (bestPath.length == i + 2) {
                finalAnswer.put(i, bestPath[i]);
            } else {
                System.err.println("ERROR");
            }
        }
        String sBest = finalAnswer.get(1);
        for (int k = 2; k < finalAnswer.keySet().size(); k++) {
            sBest += " " + finalAnswer.get(k);
        }
        return sBest;
    }

    private static String correctLeadingLamAlefLam(String s) {
        if (s.startsWith("لال")) {
            s = "لل" + s.substring(3);
        }
        return s;
    }

    private static String findBestPathLM(ArrayList<String> paths) throws IOException {
        if (paths.size() == 1)
        {
            return paths.get(0);
        }
        else
        {
            double bestScore = -1000;
            String bestPath = "";
            for (String s : paths) {
                bwLM.write(s + "\n");
                bwLM.flush();
                String stemp = brLM.readLine();
                if (stemp.contains("Total:")) {
                    stemp = stemp.replaceFirst(".*Total\\:", "").trim();
                    stemp = stemp.replaceFirst("OOV.*", "").trim();
                } else {
                    stemp = "-1000";
                }
                double finalScore = Double.parseDouble(stemp);
                if (bestScore < finalScore) {
                    bestScore = finalScore;
                    bestPath = s;
                }
            }
            return bestPath;
        }
    }

    private static HashMap<Integer, ArrayList<String>> buildLatice(ArrayList<String> words) {
        HashMap<Integer, ArrayList<String>> latice = new HashMap<Integer, ArrayList<String>>();
        int i = 0;

        ArrayList<String> temp = new ArrayList<String>();
        temp.add("<s>");
        i++;
        latice.put(i, temp);

        for (String w : words) {
            // if (bStem == false) {
            String norm = correctLeadingLamAlefLam(normalizeFull(w));
            if (candidatesUnigram.containsKey(norm)) {
                temp = new ArrayList<String>();
                for (String s : candidatesUnigram.get(norm).split(" +")) {
                    if (s.length() > 0)
                        temp.add(s);
                }
                if (temp.size() > 0)
                    latice.put(i, temp);
            } else {
                temp = new ArrayList<String>();
                temp.add(correctLeadingLamAlefLam(w));
                latice.put(i, temp);
            }
            i++;
        }
        temp = new ArrayList<String>();
        temp.add("</s>");
        latice.put(i, temp);

        return latice;
    }

}
