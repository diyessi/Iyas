/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arabicpostagger;

import static arabicpostagger.ArabicUtils.AllArabicLetters;
import static arabicpostagger.ArabicUtils.AllArabicLettersAndHindiDigits;
import static arabicpostagger.ArabicUtils.normalizeFull;
import static arabicpostagger.ArabicUtils.tokenize;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;

import org.chasen.crfpp.Tagger;

import java.io.Serializable;

import gnu.trove.map.TMap;
import gnu.trove.map.hash.TCustomHashMap;
import gnu.trove.map.hash.THashMap;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DateFormat;
import java.util.Calendar;


/**
 *
 * @author kareemdarwish
 */
public class ArabicNER {

    private static String baseDir;

    private static TMap<String, String> wikipediaArEn = null; // new THashMap<String, String>(300000); // rmWiki.hashMap("wikipediaArEn");
    private static TMap<String, String> entityType = null; // new THashMap<String, String>(300);
    private static TMap<String, Integer> typeCount = null; // new THashMap<String, Integer>(200000);
    
    // private static HashMap<String, String> wikipediaArEn = new HashMap<String, String>();
    // private static HashMap<String, String> entityType = new HashMap<String, String>();
    // private static HashMap<String, Integer> typeCount = new HashMap<String, Integer>();

    private static TMap<String, String> gazetteer = null; // new HashMap<String, String>();
    
    private static TMap<String, ArrayList<String>> clMap = null; // new HashMap<String, ArrayList<String>>();

    private static TMap<String, Double> capPhrases = null; //new HashMap<String, Double>();
    private static TMap<String, Double> wikiPersonNames = null; //new HashMap<String, Double>();
    private static TMap<String, Double> wikiPersonLocations = null; //new HashMap<String, Double>();
    private static TMap<String, Double> wikiPersonOrganizations = null; //new HashMap<String, Double>();

    private static String[] stop = {"\u0648", "\u0645\u0627", "\u0647\u064a", "\u0647\u0648", 
        "\u0647\u0645", "\u0647\u0645\u0627", "\u0647\u0646", "\u0647\u0630\u0627", "\u0647\u0630\u0647", 
        "\u0647\u0630\u0627\u0646", "\u0647\u0621\u0644\u0627\u0621", "\u0647\u0644", "\u0641\u064a", 
        "\u0647\u0646\u0627", "\u0647\u0646\u0627\u0643", "\u0645\u0639", "\u0645\u0646", "\u0639\u0644\u064a", 
        "\u0643\u064a\u0641", "\u0643\u0627\u0646", "\u0627\u0646", "\u0633\u0648\u0641", "\u0644", "\u0641"};
    
    static {
        System.loadLibrary("CRFPP");
    }
    
    private static ArrayList<String> stopWords = new ArrayList<String>();

    private static Tagger nerTagger = null;
    
    private static POSTagger postagger = null;

    public ArabicNER(String DataDir, POSTagger pos) throws FileNotFoundException, IOException, UnsupportedEncodingException, ClassNotFoundException {
        baseDir = DataDir;
        postagger = pos;
        nerTagger = new Tagger("-m " + baseDir + "ner.model");
        nerTagger.clear();
         
        loadStopWords();
        loadWikipediaData();
        loadGaz();
        loadClMap();
//			loadPhrases();
        loadCapPhrases();
        loadWikipediaNames();
        loadWikipediaLocations();
        loadWikipediaOrganizations();
    }

    private void loadWikipediaData() throws UnsupportedEncodingException, FileNotFoundException, IOException, ClassNotFoundException {
        String tab = "\t";
        
        // wikipediaArEn = records.hashMap("wikipediaArEn");
        String line = "";
        // if (wikipediaArEn.isEmpty())
        File file = new File(baseDir + "wikipediaArEn.ser");
//        Calendar cal = Calendar.getInstance();
//        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL,
//                DateFormat.MEDIUM);
        if (file.exists())
        {
            
            // System.out.println(df.format(cal.getTime()));
            ObjectInputStream ios = new ObjectInputStream(new FileInputStream(file));
            wikipediaArEn = (THashMap)ios.readObject();
            // System.out.println(df.format(cal.getTime()));
        }
        else
        {
            // load Arabic to English mappings
            wikipediaArEn = new THashMap<String, String>(300000);
            // System.out.println(df.format(cal.getTime()));
            BufferedReader srArEn = new BufferedReader(new InputStreamReader(new FileInputStream(new File(baseDir + "arwiki-20120928-Arabic-English-titles-w-redirects.txt")), "UTF8"));
            
            while ((line = srArEn.readLine()) != null) {
                String[] s = line.split(tab);
                if (s.length == 2) {
                    s[0] = s[0].replaceAll("\\(.*?\\)", "").trim();
                    // s[1] = Regex.Replace(s[1], "[,_\\-]", " ").trim().ToLower();
                    s[1] = s[1].trim().replaceAll(" ", "_");
                    if (!wikipediaArEn.containsKey(normalizeFull(s[0]))) {
                        wikipediaArEn.put(normalizeFull(s[0]), s[1]);
                    }
                }
            }
            srArEn.close();
            // System.out.println(df.format(cal.getTime()));
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(wikipediaArEn);
            oos.close();
            // records.commit();
        }
        
        // load type counts
        // typeCount = records.hashMap("typeCount");
        file = new File(baseDir + "typeCount.ser");
        if (file.exists())
        {
            ObjectInputStream ios = new ObjectInputStream(new FileInputStream(file));
            typeCount = (THashMap)ios.readObject();
        }
        else
        {
            typeCount = new THashMap<String, Integer>(300);
            BufferedReader srTypes = new BufferedReader(new InputStreamReader(new FileInputStream(new File(baseDir + "types.txt")), "UTF8"));
            while ((line = srTypes.readLine()) != null) {
                String[] s = line.split(tab);
                if (s.length == 2) {
                    typeCount.put(s[0], Integer.parseInt(s[1]));
                }
            }
            // kill some entity types
            typeCount.put("Agent", 1);
            typeCount.put("Work", 1);

            srTypes.close();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(typeCount);
            oos.close();
            // records.commit();
        }
        
        // load most common type for entity
        // entityType = records.hashMap("entityType");
        file = new File(baseDir + "entityType.ser");
        if (file.exists())
        {
            ObjectInputStream ios = new ObjectInputStream(new FileInputStream(file));
            entityType = (THashMap)ios.readObject();
        }
        else
        {
            entityType = new THashMap<String, String>(200000);
            BufferedReader srEntityType = new BufferedReader(new InputStreamReader(new FileInputStream(new File(baseDir + "entry-type.txt.filtered")), "UTF8"));
            while ((line = srEntityType.readLine()) != null) {
                String[] s = line.split(tab);
                if (s.length == 2) {
                    if (!entityType.containsKey(s[0])) {
                        entityType.put(normalizeFull(s[0]), s[1]);
                    } else if (typeCount.get(s[1]) > typeCount.get(entityType.get(s[0]))) {
                        entityType.put(s[0], s[1]);
                    }
                }
            }
            srEntityType.close();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(entityType);
            oos.close();
            // records.commit();
        }
    }

    private void loadGaz() throws UnsupportedEncodingException, FileNotFoundException, IOException, ClassNotFoundException {
        String s = "";
        String tag = "";
        
        // records.hashMap("gazeteer");
        File file = new File(baseDir + "gazetteer.ser");
        if (file.exists())
        {
            ObjectInputStream ios = new ObjectInputStream(new FileInputStream(file));
            gazetteer = (THashMap)ios.readObject();
        }
        else
        {
            gazetteer = new THashMap<String, String>();
            BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(baseDir + "alignedlocgazetteer")), "UTF8"));
            tag = "loc";
            while ((s = sr.readLine()) != null) {
                if (s.matches("[" + AllArabicLetters + "]+")) {
                    if (!gazetteer.containsKey(normalizeFull(s))) {
                        gazetteer.put(normalizeFull(s), tag);
                    }
                }
            }
            sr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(baseDir + "alignedorggazetteer")), "UTF8"));

            tag = "org";
            while ((s = sr.readLine()) != null) {
                if (s.matches("[" + AllArabicLetters + "]+")) {
                    if (!gazetteer.containsKey(normalizeFull(s))) {
                        gazetteer.put(normalizeFull(s), tag);
                    }
                }
            }
            sr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(baseDir + "alignedpersgazetteer")), "UTF8"));
            tag = "per";
            while ((s = sr.readLine()) != null) {
                if (s.matches("[" + AllArabicLetters + "]+")) {
                    if (!gazetteer.containsKey(normalizeFull(s))) {
                        gazetteer.put(normalizeFull(s), tag);
                    }
                }
            }
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(gazetteer);
            oos.close();
        }
    }

    private void loadClMap() throws FileNotFoundException, UnsupportedEncodingException, IOException, ClassNotFoundException {
        String tab = "\t";
        String semicolon = ";";
        String line = "";
        String s = "";

        File file = new File(baseDir + "clMap.ser");
        if (file.exists())
        {
            ObjectInputStream ios = new ObjectInputStream(new FileInputStream(file));
            clMap = (THashMap)ios.readObject();
        }
        else
        {
            clMap = new THashMap<String, ArrayList<String>>();// records.hashMap("clMap");
            BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(baseDir + "lex.2.combined.threshold-0.0001.txt")), "UTF8"));
            while ((s = sr.readLine()) != null) {
                {
                    String[] parts = s.split(tab);
                    parts[0] = normalizeFull(parts[0]);
                    if (parts.length == 3) {
                        if (!clMap.containsKey(parts[0])) {
                            ArrayList<String> tmp = new ArrayList<String>();
                            tmp.add(parts[1] + "\t" + parts[2]);
                            clMap.put(parts[0], tmp);
                        }
                        else
                        {
                            ArrayList<String> tmp = clMap.get(parts[0]);
                            tmp.add(parts[1] + "\t" + parts[2]);
                            clMap.put(parts[0], tmp);
                        }
                    }
                }
            }
            sr.close();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(clMap);
            oos.close();
        }
    }

    private void loadCapPhrases() throws UnsupportedEncodingException, FileNotFoundException, IOException, ClassNotFoundException {
        String tab = "\t";
        String line = "";
        
        File file = new File(baseDir + "capPhrases.ser");
        if (file.exists())
        {
            ObjectInputStream ios = new ObjectInputStream(new FileInputStream(file));
            capPhrases = (THashMap)ios.readObject();
        }
        else
        {
            capPhrases = new THashMap<String, Double>();// records.hashMap("capPhrases");
            BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(baseDir + "phrase-table-caps-filtered-ratio-in-wikipedia.2")), "UTF8"));
            while ((line = sr.readLine()) != null) {
                // load Arabic to English mappings
                String[] s = line.split(tab);
                if (s.length == 2) {
                    if (!capPhrases.containsKey(normalizeFull(s[0]))) {
                        capPhrases.put(normalizeFull(s[0]), Double.parseDouble(s[1]));
                    } else if (Double.parseDouble(s[1]) > capPhrases.get(normalizeFull(s[0]))) {
                        capPhrases.put(normalizeFull(s[0]), Double.parseDouble(s[1]));
                    }
                }
            }
            sr.close();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(capPhrases);
            oos.close();
        }
    }

    private void loadWikipediaNames() throws UnsupportedEncodingException, IOException, ClassNotFoundException {
        // load Wikipedia Person Names
        String s = "";

        File file = new File(baseDir + "wikiPersonNames.ser");
        if (file.exists())
        {
            ObjectInputStream ios = new ObjectInputStream(new FileInputStream(file));
            wikiPersonNames = (THashMap)ios.readObject();
        }
        else
        {
            wikiPersonNames = new THashMap<String, Double>();// records.hashMap("wikiPersonNames");

            BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(baseDir + "people-names.ar.redirect.txt")), "UTF8"));
            while ((s = sr.readLine()) != null) {

                if (!wikiPersonNames.containsKey(normalizeFull(s))) {
                    wikiPersonNames.put(normalizeFull(s), 1d);
                }
            }
            sr.close();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(wikiPersonNames);
            oos.close();
        }
    }

    private void loadWikipediaLocations() throws UnsupportedEncodingException, IOException, ClassNotFoundException {
        // load Wikipedia Person Locations
        String s = "";

        File file = new File(baseDir + "wikiPersonLocations.ser");
        if (file.exists())
        {
            ObjectInputStream ios = new ObjectInputStream(new FileInputStream(file));
            wikiPersonLocations = (THashMap)ios.readObject();
        }
        else
        {
            wikiPersonLocations = new THashMap<String, Double>();// records.hashMap("wikiPersonLocations");

            BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(baseDir + "location-names.ar.redirect.txt")), "UTF8"));
            while ((s = sr.readLine()) != null) {

                if (!wikiPersonLocations.containsKey(normalizeFull(s))) {
                    wikiPersonLocations.put(normalizeFull(s), 1d);
                }
            }
            sr.close();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(wikiPersonLocations);
            oos.close();
        }
    }

    private void loadWikipediaOrganizations() throws UnsupportedEncodingException, IOException, ClassNotFoundException {
        // load Wikipedia Organizations
        String s = "";
        // if (records == null)
        //    records = RecordManagerFactory.createRecordManager(baseDir + "recordManager.db");
        // wikiPersonOrganizations = records.hashMap("wikiPersonOrganizations");
        File file = new File(baseDir + "wikiPersonOrganizations.ser");
        if (file.exists())
        {
            ObjectInputStream ios = new ObjectInputStream(new FileInputStream(file));
            wikiPersonOrganizations = (THashMap)ios.readObject();
        }
        else
        {
            wikiPersonOrganizations = new THashMap<String, Double>();
            BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(baseDir + "organization-names.ar.redirect.txt")), "UTF8"));
            while ((s = sr.readLine()) != null) {

                if (!wikiPersonOrganizations.containsKey(normalizeFull(s))) {
                    wikiPersonOrganizations.put(normalizeFull(s), 1d);
                }
            }
            sr.close();
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
            oos.writeObject(wikiPersonOrganizations);
            oos.close();
        }
    }

    private void loadStopWords() {
        for (String s : stop) {
            stopWords.add(s);
        }
    }

    public ArrayList<String> tag(String inputText, boolean printPOStags) throws IOException, InterruptedException, FileNotFoundException, ClassNotFoundException {
        String space = "[ \t]+";
        String newline = "[\n\r]+";
        String[] lines = inputText.split(newline);
        String s = "";
        for (String line : lines) {

            ArrayList<String> words = tokenize(line);
            // ArrayList<String> words = new ArrayList<String>(s.split(newline, StringSplitOptions.RemoveEmptyEntries));
            ArrayList<String> feats = extractFeatures(words);
            for (String ts : feats) {
                s += ts + "\n";
            }
            s += "\n";
        }
        
        String res = nerTagger.parse(s);

        lines = res.split(newline);
        ArrayList<String> output = new ArrayList<String>();
        for (String line : lines) {
            String[] parts = line.split(space);
            if (parts.length > 2) {
                if (printPOStags == true)
                    output.add(parts[0] + "/" + parts[12] + "/" + parts[parts.length - 1]);
                else
                    output.add(parts[0] + "/" + parts[parts.length - 1]);
            } else {
                output.add("");
            }
        }
        return output;
    }

    private ArrayList<String> getPOStags (ArrayList<String> input) throws IOException, InterruptedException
    {
        ArrayList<String> posTags = new ArrayList<String>();
        String line = "";
        for (String s: input)
        {
            String tmp = s.replaceAll("\\/.*", "") + " ";
            if (tmp.matches(".*[" + AllArabicLettersAndHindiDigits + "0-9a-zA-Z]+.*"))
                tmp = tmp.replaceAll("[^" + AllArabicLettersAndHindiDigits + "0-9a-zA-Z]+", "");
            line += tmp + " ";
        }
        ArrayList<String> posOut = postagger.tag(line, false, true);
        
        String currentWord = "";
        String currentTag = "";
        for (String s: posOut)
        {
            if (s.startsWith("-") && currentWord.length() > 0)
            {
                posTags.add(currentWord + "/" + currentTag);
                currentWord = "";
                currentTag = "";
            }
            else
            {
                if (currentWord.length() > 0)
                {
                    currentWord += "+";
                    currentTag += "+";
                }
                currentWord += s.replaceAll("/.*", "").trim();
                currentTag += s.replaceAll(".*/", "").trim();
            }
        }
        
        return posTags;
    }
    
    private String getStem(String input)
    {
        String[] parts = input.split("/");
        if (parts.length == 2)
        {
            if (parts[0].contains("+"))
            {
                while (parts[1].matches("^(CONJ|PREP|DET).*") && parts[1].contains("+"))
                {
                    parts[0] = parts[0].replaceFirst("^.*?\\+", "");
                    parts[1] = parts[1].replaceFirst("^.*?\\+", "");
                }
            }
            
            return parts[0].replaceAll("\\+", "");
        }
        else
        {
            return input;
        }
    }
    
    private ArrayList<String> extractFeatures(ArrayList<String> input) throws IOException, InterruptedException, FileNotFoundException, ClassNotFoundException {
        // get names of persons from wikipedia
        ArrayList<String> wikiPerson = consultList(input, wikiPersonNames, "WikiPerson");
        ArrayList<String> wikiLocation = consultList(input, wikiPersonLocations, "WikiLocation");
        ArrayList<String> wikiOrganization = consultList(input, wikiPersonOrganizations, "WikiOrganization");

        // get phrase capitalization from phrase table
        ArrayList<String> phraseCaps = consultListDouble(input, capPhrases, "CAPS");

        // get tags from Wikipedia
        ArrayList<String> tags = new ArrayList<String>(input.size());
        for (int i = 0; i < input.size(); i++) {
            tags.add("");
        }
        
        // get POS tags
        // get tokens first, then send to POS tagger
        ArrayList<String> postags = getPOStags(input);
        if (input.size() != postags.size())
        {
            System.out.println("error");
            for (int ii = 0; ii < Math.min(input.size(), postags.size()); ii++)
                System.out.println(input.get(ii) + "\t" + postags.get(ii));
        }
        
        for (int i = 0; i < input.size(); i++) {
            String s = input.get(i).replaceAll("\\/.*", "");
            String token = normalizeFull(s.trim().replaceAll(" .*", ""));
            if (tags.get(i).equals("") && token.matches("[a-zA-Z0-9" + AllArabicLettersAndHindiDigits + "]+") && !stopWords.contains(token)) {
                for (int j = Math.min(i + 5, tags.size()); j > i; j--) {
                    String key = "";
                    for (int k = i; k < j; k++) {
                        key += input.get(k).replaceAll("\\/.*", "").trim() + " ";
                    }
                    key = normalizeFull(key.trim());
                    // check if key exists in Arabic to English mapping
                    boolean phraseFound = false;
                    if (wikipediaArEn.containsKey(key)) {
                        phraseFound = true;
                    } else if (key.startsWith("\u0644\u0644") && wikipediaArEn.containsKey("\u0627" + key.substring(1))) {
                        key = "\u0627" + key.substring(1);
                        phraseFound = true;

                    } else if ((key.startsWith("\u0648") || key.startsWith("\u0641") || key.startsWith("\u0628") || key.startsWith("\u0643") || key.startsWith("\u0644"))
                            && wikipediaArEn.containsKey(key.substring(1))) {
                        key = key.substring(1);
                        phraseFound = true;
                    } else if ((key.startsWith("\u0648\u0644") || key.startsWith("\u0648\u0628") || key.startsWith("\u0648\u0643")
                            || key.startsWith("\u0641\u0644") || key.startsWith("\u0641\u0628") || key.startsWith("\u0641\u0643"))
                            && wikipediaArEn.containsKey(key.substring(2))) {
                        key = key.substring(2);
                        phraseFound = true;
                    } else if ((key.startsWith("\u0648\u0644\u0644") || key.startsWith("\u0641\u0644\u0644"))
                            && wikipediaArEn.containsKey("\u0627" + key.substring(2))) {
                        key = "\u0627" + key.substring(2);
                        phraseFound = true;
                    }
                    // check the type
                    if (phraseFound == true && entityType.containsKey(wikipediaArEn.get(key))) {
                        for (int l = i; l < j; l++) {
                            String sPos = "I-";
                            if (l == i) {
                                sPos = "B-";
                            }
                            tags.set(l, sPos + entityType.get(wikipediaArEn.get(key)));
                        }
                        j = i - 1;
                    }

                }
            }
        }

        ArrayList<String> output = new ArrayList<String>();
        for (int i = 0; i < input.size(); i++) {
            String s = input.get(i).replaceAll("\\/.*", "");
            String token = normalizeFull(s.trim().replaceAll(" .*", ""));
            if (token.matches("[a-zA-Z0-9" + AllArabicLettersAndHindiDigits + "]+")) {
                String feat = token.trim();
                if (token.length() >= 4) {
                    feat += " " + token.substring(0, 4);
                    feat += " " + token.substring(token.length() - 4);
                } else {
                    feat += " - -";
                }
                if (token.length() >= 3) {
                    feat += " " + token.substring(0, 3);
                    feat += " " + token.substring(token.length() - 3);
                } else {
                    feat += " - -";
                }
                if (token.length() >= 2) {
                    feat += " " + token.substring(0, 2);
                    feat += " " + token.substring(token.length() - 2);
                } else {
                    feat += " - -";
                }
                if (token.length() >= 1) {
                    feat += " " + token.substring(0, 1);
                    feat += " " + token.substring(token.length() - 1);
                } else {
                    feat += " - -";
                }

                /* gazatteer features */
                if (gazetteer.containsKey(token)) {
                    feat += " " + gazetteer.get(token);
                } else if (gazetteer.containsKey(token.replaceFirst("^[\u0648\u0641]", ""))) {
                    feat += " " + token.replaceFirst("^[\u0648\u0641]", "");
                } else if (gazetteer.containsKey(token.replaceFirst("^\u0627\u0644", ""))) {
                    feat += " " + token.replaceFirst("^\u0627\u0644", "");
                } else if (gazetteer.containsKey(token.replaceFirst("^\u0648\u0627\u0644", ""))) {
                    feat += " " + token.replaceFirst("^\u0648\u0627\u0644", "");
                } else {
                    feat += " null";
                }
                // what is this about?
                // feat += " " + Regex.Replace(Regex.Replace(s.trim(), "^\\S+ ", "").trim(), "[^a-zA-Z0-9\\-]+", "");

                float score = -0.1f;
                /* cross-language capitalization */
                if (clMap.containsKey(token)) {
                    score = getRatioOfCapitalization(clMap.get(token));
                } else if ((token.startsWith("\u0648") || token.startsWith("\u0641") || token.startsWith("\u0628") || token.startsWith("\u0643") || token.startsWith("\u0644"))
                        && clMap.containsKey(token.substring(1))) {
                    score = getRatioOfCapitalization(clMap.get(token.substring(1)));
                } else if ((token.startsWith("\u0648\u0644") || token.startsWith("\u0648\u0628") || token.startsWith("\u0648\u0643")
                        || token.startsWith("\u0641\u0644") || token.startsWith("\u0641\u0628") || token.startsWith("\u0641\u0643"))
                        && clMap.containsKey(token.substring(2))) {
                    score = getRatioOfCapitalization(clMap.get(token.substring(2)));
                } else if ((token.startsWith("\u0648\u0644\u0644") || token.startsWith("\u0641\u0644\u0644"))
                        && clMap.containsKey("\u0627" + token.substring(2))) {
                    score = getRatioOfCapitalization(clMap.get("\u0627" + token.substring(2)));
                } else if (token.startsWith("\u0644\u0644")
                        && clMap.containsKey("\u0627" + token.substring(1))) {
                    score = getRatioOfCapitalization(clMap.get("\u0627" + token.substring(1)));
                }

                int iScore = Math.round(10 * score);

                feat += " " + iScore;
                /* end cross-language capitalization */


                /* check phrase boundary */
//                if (phrases.containsKey(token)) {
//                    if (phrases[token].containsKey("WB")) {
//                        feat += " WB";
//                    } else {
//                        feat += " -";
//                    }
//                    if (phrases[token].containsKey("WI")) {
//                        feat += " WI";
//                    } else {
//                        feat += " -";
//                    }
//                    if (phrases[token].containsKey("B")) {
//                        feat += " B";
//                    } else {
//                        feat += " -";
//                    }
//                    if (phrases[token].containsKey("I")) {
//                        feat += " I";
//                    } else {
//                        feat += " -";
//                    }
//                } else {
//                    feat += " - - - -";
//                }
                /* end check phrase  boundary */

                /* check transliteration */
//                Dictionary<string, double> translations = new Dictionary<string, double>();
//                if (clMap.containsKey(token)) {
//                    translations = getUniqWords(clMap[token]);
//                } else if ((token.startsWith("\u0648") || token.startsWith("\u0641") || token.startsWith("\u0628") || token.startsWith("\u0643") || token.startsWith("\u0644"))
//                        && clMap.containsKey(token.substring(1))) {
//                    translations = getUniqWords(clMap[token.substring(1)]);
//                } else if ((token.startsWith("\u0648\u0644") || token.startsWith("\u0648\u0628") || token.startsWith("\u0648\u0643")
//                        || token.startsWith("\u0641\u0644") || token.startsWith("\u0641\u0628") || token.startsWith("\u0641\u0643"))
//                        && clMap.containsKey(token.substring(2))) {
//                    translations = getUniqWords(clMap[token.substring(2)]);
//                }
//                if (translations.count > 0) {
//                    SortedDictionary<double, string> translierations = tm.TransliterateWordAgainstWordListGiveRatio(token, translations);
//                    double overallRatio = 0d;
//                    foreach(double d in translierations.Keys
//                    ) overallRatio += d;
//                    feat += " " + (int) (Math.Round(10 * overallRatio, 0));;
//                } else {
//                    // no translation found -- out of vocab
//                    feat += " -1";
//                }
                /* end check transliteration */

                /* light stemmed word */
                // this needs to be replace with actual stem instead of approximation
//                String lightStem = token;
//                if (token.length() > 1 && (token.startsWith("\u0648") || token.startsWith("\u0641") || token.startsWith("\u0628") || token.startsWith("\u0643") || token.startsWith("\u0644"))) {
//                    lightStem = token.substring(1);
//                } else if (token.length() > 2 && (token.startsWith("\u0648\u0644") || token.startsWith("\u0648\u0628") || token.startsWith("\u0648\u0643")
//                        || token.startsWith("\u0641\u0644") || token.startsWith("\u0641\u0628") || token.startsWith("\u0641\u0643"))) {
//                    lightStem = token.substring(2);
//                } else if (token.length() > 3 && (token.startsWith("\u0648\u0644\u0644") || token.startsWith("\u0641\u0644\u0644"))) {
//                    lightStem = "\u0627" + token.substring(2);
//                } else if (token.length() > 2 && token.startsWith("\u0644\u0644")) {
//                    lightStem = "\u0627" + token.substring(1);
//                }                
                // this retires the rule based stemmer with output of POS tagger
                String lightStem = getStem(postags.get(i));
                feat += " " + lightStem;
                /* end light stemmed word */
                
                // put pos Tag
                String fullPosTag = postags.get(i);
                fullPosTag = fullPosTag.replaceFirst(".*/", ""); // remove token & plus signs
                feat += " " + fullPosTag;

                /* check if person name in Wikipedia */
                if (wikiPerson.get(i).equals("")) {
                    feat += " null";
                } else {
                    feat += " " + wikiPerson.get(i);
                }
                /* end check if person name in Wikipedia */

                /* check if location name in Wikipedia */
                if (wikiLocation.get(i).equals("")) {
                    feat += " null";
                } else {
                    feat += " " + wikiLocation.get(i);
                }
                /* end check if location name in Wikipedia */

                /* check if organization name in Wikipedia */
                if (wikiOrganization.get(i).equals("")) {
                    feat += " null";
                } else {
                    feat += " " + wikiOrganization.get(i);
                }
                /* end check if organization name in Wikipedia */

                /* check if equivalent is caps in phrase table */
                if (phraseCaps.get(i).equals("")) {
                    feat += " null";
                } else {
                    feat += " " + phraseCaps.get(i);
                }
                /* end check if equivalent is caps in phrase table */

                /* check wikipedia entry */
                if (tags.get(i).equals("")) {
                    feat += " null";
                } else {
                    feat += " " + tags.get(i);
                }
                /* end check wikipedia entry */

                // if it has a tag, add the tag
                if (input.get(i).contains("/")) {
                    feat += " " + input.get(i).replaceAll(".*\\/", "");
                }

                output.add(feat);
            }
            else if (true)
            {
                output.add(token + " - - - - - - - - null 0 " + postags.get(i).replaceFirst("/.*", "") + " " + postags.get(i).replaceFirst(".*/", "") + " null null null null null O");
            } 
            else 
            {
                output.add("");
            }
        }
        return output;
    }

    private ArrayList<String> consultList(ArrayList<String> input, TMap<String, Double> refList, String tag) {
        ArrayList<String> tags = new ArrayList<String>(input.size());
        for (int i = 0; i < input.size(); i++) {
            tags.add("");
        }
        for (int i = 0; i < input.size(); i++) {
            String s = input.get(i).replaceAll("\\/.*", "");
            String token = normalizeFull(s.trim().replaceAll(" .*", ""));
            if (tags.get(i).length() == 0 && token.matches("[" + AllArabicLetters + "]+")) {
                for (int j = Math.min(i + 5, tags.size()); j > i; j--) {
                    String key = "";
                    for (int k = i; k < j; k++) {
                        key += input.get(k).replaceAll("\\/.*", "").trim() + " ";
                    }
                    key = normalizeFull(key.trim());
                    // check if key exists in wikipedia person names
                    String phraseFound = checkIfExistsInDictionary(key, refList);
                    // check the type
                    if (phraseFound != "") // possible to put condition on the probability of mapping; key kept for this reason
                    {
                        for (int l = i; l < j; l++) {
                            String sPos = "I-";
                            if (l == i) {
                                sPos = "B-";
                            }
                            tags.set(l, sPos + tag);
                        }
                        j = i - 1;
                    }

                }
            }
        }
        return tags;
    }

    private String checkIfExistsInDictionary(String key, TMap<String, Double> dict) {
        boolean hasKey = false;
        if (dict.containsKey(key)) {
            hasKey = true;
            // if it fails try simple stemming to match
        } else if (key.startsWith("\u0644\u0644") && dict.containsKey("\u0627" + key.substring(1))) {
            key = "\u0627" + key.substring(1);
            hasKey = true;

        } else if ((key.startsWith("\u0648") || key.startsWith("\u0641") || key.startsWith("\u0628") || key.startsWith("\u0643") || key.startsWith("\u0644"))
                && dict.containsKey(key.substring(1))) {
            key = key.substring(1);
            hasKey = true;
        } else if ((key.startsWith("\u0648\u0644") || key.startsWith("\u0648\u0628") || key.startsWith("\u0648\u0643")
                || key.startsWith("\u0641\u0644") || key.startsWith("\u0641\u0628") || key.startsWith("\u0641\u0643"))
                && dict.containsKey(key.substring(2))) {
            key = key.substring(2);
            hasKey = true;
        } else if ((key.startsWith("\u0648\u0644\u0644") || key.startsWith("\u0641\u0644\u0644"))
                && dict.containsKey("\u0627" + key.substring(2))) {
            key = "\u0627" + key.substring(2);
            hasKey = true;
        }
        if (hasKey == true) {
            return key;
        } else {
            return "";
        }
    }

    private ArrayList<String> consultListDouble(ArrayList<String> input, TMap<String, Double> refList, String tag) {
        ArrayList<String> tags = new ArrayList<String>(input.size());
        for (int i = 0; i < input.size(); i++) {
            tags.add("");
        }
        for (int i = 0; i < input.size(); i++) {
            String s = input.get(i).replaceAll("\\/.*", "");
            String token = normalizeFull(s.trim().replaceAll(" .*", ""));
            if (tags.get(i).length() == 0 && token.matches("[" + AllArabicLetters + "]+")) {
                for (int j = Math.min(i + 5, tags.size()); j > i; j--) {
                    String key = "";
                    if (!stopWords.contains(input.get(i).replaceAll("\\/.*", ""))
                            && !stopWords.contains(input.get(j - 1).replaceAll("\\/.*", ""))) {
                        for (int k = i; k < j; k++) {
                            key += input.get(k).replaceAll("\\/.*", "").trim() + " ";
                        }
                        key = normalizeFull(key.trim());
                        // check if key exists in wikipedia person names
                        String phraseFound = checkIfExistsInDictionary(key, refList);
                        // check the type
                        if (phraseFound != "") // possible to put condition on the probability of mapping; key kept for this reason
                        {
                            for (int l = i; l < j; l++) {
                                String sPos = "I-";
                                if (l == i) {
                                    sPos = "B-";
                                }
                                tags.set(l, sPos + tag + "-" + Integer.toString(10 * refList.get(phraseFound).intValue()));
                            }
                            j = i - 1;
                        }
                    }
                }
            }
        }
        return tags;
    }

    private float getRatioOfCapitalization(ArrayList<String> input) {
        String tab = "\t";
        float score = 0;
        float caps = 0;
        float notCaps = 0;
        for (String s : input) {
            String[] parts = s.split(tab);
            if (parts.length == 2) {
                if (parts[0].matches("^[A-Z].*")) {
                    caps += Float.parseFloat(parts[1]);
                } else {
                    notCaps += Float.parseFloat(parts[1]);
                }
            }
        }

        score = caps / (caps + notCaps);

        return score;
    }

    public void generateNewFeatureFileFromTrain(String inputFile, String outputFile) throws FileNotFoundException, IOException, InterruptedException, ClassNotFoundException {
        String delimit = "[ \t]+";
        BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(inputFile))));
        BufferedWriter sw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputFile))));

        ArrayList<String> token = new ArrayList<String>();
        String line = "";
        while ((line = sr.readLine()) != null) {
            String[] parts = line.split(delimit);
            if (parts.length >= 2) {
                // get features
                if (parts[0].equals("_"))
                    parts[0] = ":";
                else if (parts[0].equals("/"))
                    parts[0] = "\\";
                
                ArrayList<String> tmp = tokenize(parts[0]);
                if (tmp.size() > 1)
                {
                    for (String sTmp: tmp)
                        token.add(sTmp + "/" + parts[parts.length - 1]);
                }
                else
                {
                    token.add(parts[0] + "/" + parts[parts.length - 1]);
                }
                // List<string> tokenFeatures = extractFeatures(token);
                // foreach (string s in tokenFeatures)
                //	sw.WriteLine(s + " " + parts[parts.Length - 1]);
            } 
            else {
                ArrayList<String> tokenFeatures = extractFeatures(token);
                for (String s : tokenFeatures) {
                    sw.write(s + "\n");
                }
                token = new ArrayList<String>();
                sw.write("\n");
                sw.flush();
            }
        }

        if (token.size() > 0) {
            ArrayList<String> tokenFeatures = extractFeatures(token);
            for (String s : tokenFeatures) {
                sw.write(s + "\n");
            }
            sw.write("\n");
            sw.flush();
        }

        sr.close();
        sw.close();
    }
    
}
