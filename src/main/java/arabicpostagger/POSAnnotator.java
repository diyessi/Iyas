/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package arabicpostagger;

import static arabicpostagger.ArabicUtils.ALLDelimiters;
import static arabicpostagger.ArabicUtils.AllArabicLetters;
import static arabicpostagger.ArabicUtils.AllHindiDigits;
import static arabicpostagger.ArabicUtils.buck2morph;
import static arabicpostagger.ArabicUtils.normalize;
import static arabicpostagger.ArabicUtils.prefixes;
import static arabicpostagger.ArabicUtils.suffixes;
import static arabicpostagger.ArabicUtils.tokenize;
import static arabicpostagger.ArabicUtils.utf82buck;
import gnu.trove.map.TMap;
import gnu.trove.map.hash.THashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.chasen.crfpp.Tagger;

/**
 *
 * @author Kareem Darwish
 */
public class POSAnnotator {

    private static String BinDir;
    private static Tagger tokenTagger = null;
    private static Tagger posTagger = null;
    private static final HashMap<String, Integer> hPrefixes = new HashMap<String, Integer>();
    private static final HashMap<String, Integer> hSuffixes = new HashMap<String, Integer>();

    private static HashMap<String, Double> hmRoot = new HashMap<String, Double>();
    private static HashMap<String, Double> hmTemplate = new HashMap<String, Double>();
    private static HashMap<Integer, ArrayList<String>> Templates = new HashMap<Integer, ArrayList<String>>();
    private static HashMap<String, Integer> hmNumber = new HashMap<String, Integer>();
    private static HashMap<String, Integer> hmGender = new HashMap<String, Integer>();
    private static Integer iGenderCountPublic = 0;
    private static HashMap<String, Integer> hmFeatureLabelCount = new HashMap<String, Integer>();

    private static TMap<String, Integer> IntegerLanguageModel = null;

    static {
        System.loadLibrary("CRFPP");
    }
    
    private void initAnalyzer() throws FileNotFoundException, UnsupportedEncodingException, IOException {
        
    	for (int i = 0; i < prefixes.length; i++) {
            hPrefixes.put(prefixes[i].toString(), 1);
        }
        for (int i = 0; i < suffixes.length; i++) {
            hSuffixes.put(suffixes[i].toString(), 1);
        }
        
        if (!BinDir.endsWith("/")) {
            BinDir += "/";
        }
        
        BufferedReader brRoot = new BufferedReader(new InputStreamReader(new FileInputStream(new File(BinDir + "roots.txt")), "UTF8"));
        BufferedReader brTemplate = new BufferedReader(new InputStreamReader(new FileInputStream(new File(BinDir + "template-count.txt")), "UTF8"));
        BufferedReader brNum = new BufferedReader(new InputStreamReader(new FileInputStream(new File(BinDir + "number-gaz.txt")), "UTF8"));
        
        String line = "";
        while ((line = brRoot.readLine()) != null) {
            String[] parts = line.split("\t");
            if (parts.length == 2) {
                hmRoot.put(parts[0], Double.parseDouble(parts[1]));
            }
        }

        while ((line = brTemplate.readLine()) != null) {
            String[] parts = line.split("\t");
            if (parts.length == 2) {
                int len = parts[0].length();
                if (!Templates.containsKey(len)) {
                    Templates.put(len, new ArrayList<String>());
                }
                Templates.get(len).add(parts[0]);
                if (!hmTemplate.containsKey(parts[0])) {
                    hmTemplate.put(parts[0], Double.parseDouble(parts[1]));
                }
            }
        }

        while ((line = brNum.readLine()) != null) {
            if (!hmNumber.containsKey(line.trim())) {
                hmNumber.put(line.trim(), 1);
            }
        }

        brRoot.close();
        brTemplate.close();
        brNum.close();

        loadGenderTrain();
    }

    private ArrayList<String> getGenderFeatures(String word, String posTag, String template) {
        ArrayList<String> output = new ArrayList<String>();
        String suffix = "#";
        output.add(template);
        if (posTag.contains("NSUFF")) {
            String typeOfNSUFF = posTag.replaceAll(".*NSUFF_", "");
            posTag = posTag.replaceAll("\\+NSUFF.*", "+NSUFF");
            int suffixPos = word.indexOf("+");
            if (suffixPos >= 0) {
                suffix = word.substring(suffixPos + 1).replace("+", "");
            } else {
                if (typeOfNSUFF.equals("FEM_SG")) {
                    suffix = word.substring(word.length() - 1);
                } else if (typeOfNSUFF.equals("FEM_DU") || typeOfNSUFF.equals("MASC_DU") || typeOfNSUFF.equals("MASC_PL")) {
                    if (word.endsWith("ن")) {
                        suffix = word.substring(word.length() - 2);
                    } else {
                        suffix = word.substring(word.length() - 1);
                    }
                } else if (typeOfNSUFF.equals("FEM_PL")) {
                    suffix = "\u0627\u062a"; // suffix = "ات";
                }
            }
        }
        output.add(posTag);
        output.add(suffix);
//        output.add(word.substring(word.length()-2));
//        output.add(word.substring(word.length()-1));
        output.add(Integer.toString(template.length()));
        if (word.endsWith("\u0629") || word.endsWith("\u0627\u062a")) // ends with At or p
        {
            output.add("YES");
        } else {
            output.add("NO");
        }

        return output;
    }

    private void loadGenderTrain() throws FileNotFoundException, UnsupportedEncodingException, IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(new File(BinDir + "train.lang.all.utf8")), "UTF8"));
        String line = "";
        while ((line = br.readLine()) != null) {
            String[] parts = line.split("\t");
            if (parts.length == 2) {
                String[] train = parts[0].split("/");
                if (train.length == 4) {
                    String word = train[0];
                    String posTag = train[2];
                    String gender = train[3];

                    // keep count of gender tags -- for prob priors
                    if (hmGender.containsKey(gender)) {
                        hmGender.put(gender, hmGender.get(gender) + 1);
                    } else {
                        hmGender.put(gender, 1);
                    }
                    iGenderCountPublic++;
                    String tmpWord = word;
                    posTag = posTag.replaceFirst("_.*", "");
                    if (posTag.endsWith("NSUFF"))
                        tmpWord = word.replaceAll("(\u0647|\u0647\u0627|\u0643|\u064a|\u0647\u0645\u0627|\u0643\u0645\u0627|\u0646\u0627|\u0643\u0645|\u0647\u0645|\u0647\u0646|\u0643\u0646|\u0627|\u0627\u0646|\u064a\u0646|\u0648\u0646|\u0648\u0627|\u0627\u062a|\u062a|\u0646|\u0629)$", "");
                    else if (posTag.endsWith("CASE") && word.endsWith("\u0627"))
                        tmpWord = word.substring(0, word.length() - 1);
                    if (posTag.startsWith("DET"))
                        tmpWord = tmpWord.substring(2);
                    String template = fitTemplate(tmpWord);
                    ArrayList<String> features = getGenderFeatures(word, posTag, template);

                    for (String f : features) {
                        String key = gender + "_" + f;
                        if (hmFeatureLabelCount.containsKey(key)) {
                            hmFeatureLabelCount.put(key, hmFeatureLabelCount.get(key) + 1);
                        } else {
                            hmFeatureLabelCount.put(key, 1);
                        }
                    }
                }
            }
        }
    }

    private String getGenderTag(String word, String posTag, String template) {
        // // System.err.println(word + " " + posTag + " " + template);
        String gender = "";
        ArrayList<String> features = getGenderFeatures(word, posTag, template);
        double bestLabelProb = -1;
        for (String l : hmGender.keySet()) {
            // // System.err.println(l);
            double res = (double) hmGender.get(l) / (double) iGenderCountPublic;
            for (String f : features) {
                String key = l + "_" + f;
                // System.err.print(key + "\t");
                if (hmFeatureLabelCount.containsKey(key)) {
                    res *= (double) (hmFeatureLabelCount.get(key) + 1d) / (double) hmGender.get(l);
                    // System.err.println((double) (hmFeatureLabelCount.get(key) + 1d) / (double) hmGender.get(l));
                } else {
                    res *= 1d / (double) hmGender.get(l);
                    // System.err.println(1d / (double) hmGender.get(l));
                }
            }
            // System.err.println(res);
            if (res > bestLabelProb) {
                gender = l;
                bestLabelProb = res;
            }
        }
        return gender;
    }

    public POSAnnotator(String dir) throws IOException, InterruptedException, FileNotFoundException, ClassNotFoundException {
        BinDir = dir;
        
        // initialize CRF
        LoadCRFModels(BinDir);

        PrepareCRFPP(BinDir + "wordcount.txt",
                BinDir + "tokenizer.model",
                BinDir + "pos.model",
                BinDir + "phrase.model");
        
        initAnalyzer();
    }

    public List<String> tag(String s, boolean stemOnly, boolean printGenderTags)
    		throws IOException, InterruptedException {
    	List<String> outputArray = null;
        
        List<String> tokens = new ArrayList<String>(CreateTestArray(s));
        List<String> outputTok = AnalyzeWordCommandLinePipe(tokens, false);

        if (stemOnly) {
        	outputArray = CreateOutputArray(outputTok);
        } else {
        	List<String> outputPos = AnalyzeWordCommandLinePipe(CreateTestArrayPOS(outputTok), true);
            List<String> outputPosWithGender = getGenderTags(outputPos, printGenderTags);
            outputArray = outputPosWithGender;
        }
        
        return outputArray;
    }

    private List<String> getGenderTags(List<String> outputPos, boolean printGenderTags) {
        List<String> output = new ArrayList<String>();
        boolean task = true;
        int frag = 0;
        ArrayList<String> wordTags = new ArrayList<String>();
        boolean getGender = false;
        for (String s : outputPos) {
            if (s.trim().length() == 0) {
                output.add("\n");
            } else if (s.startsWith("-")) {
                // do nothing
                if (task) {
                    if (getGender) {
                        String thisWord = "";
                        String thisTemplate = "";
                        String thisPosTag = "";
                        for (String w : wordTags) {
                            if (w.contains("NOUN") || w.contains("ADJ") || w.contains("NUM") || w.contains("NSUFF")) {
                                String[] feats = w.split("\t");
                                if (!w.contains("NSUFF")) {
                                    thisPosTag += "NOUN";
                                    thisTemplate = feats[1];
                                    thisWord += feats[0];
                                } else {
                                    if (thisPosTag.trim().length() > 0) {
                                        thisPosTag += "+" + feats[feats.length - 1];
                                    } else {
                                        thisPosTag += feats[feats.length - 1];
                                    }
                                    thisWord += "+" + feats[0];
                                }
                            }
                        }
                        for (String w : wordTags) {
                            if (printGenderTags && (w.contains("NOUN") || w.contains("ADJ") || w.contains("NUM"))) {
                                output.add(w.replaceAll("\t.*\t", "/") + "-" + getGenderTag(thisWord, thisPosTag, thisTemplate) + " ");
                            } else {
                                output.add(w.replaceAll("\t.*\t", "/") + " ");
                            }
                        }
                    } else {
                        for (String w : wordTags) {
                            output.add(w.replaceAll("\t.*\t", "/") + " ");
                        }
                    }
                    getGender = false;
                    wordTags.clear();
                }
                frag = -1;
                output.add("-");
            } else {
                if (task) {
                    wordTags.add(s);
                    if (s.contains("NOUN") || s.contains("ADJ") || (s.contains("NUM") && !s.matches("[0-9٠-٩]+"))) {
                        getGender = true;
                    }
                    // bw.write(s.replaceAll("\t.*\t", "/") + " ");
                } else {
                    if (outputPos.indexOf(s) == 0) {
                        output.add("" + s.replaceAll("\t", "/") + "");
                    } else if (frag == 0) {
                        output.add(" " + s.replaceAll("\t", "/") + "");
                    } else {
                        output.add("/" + s.replaceAll("\t", "/") + "");
                    }
                }
            }
            frag++;
        }
        return output;
    }
    
    private List<String> AnalyzeWordCommandLinePipe(List<String> input, boolean task) throws IOException, InterruptedException // 0 = tokenizer, 1 = pos, 2 = phrase detection
    {
        List<String> output = new ArrayList<String>();
        String res = new String();
        String ins = new String();

        for (String elt : input) {
            ins += elt + "\n";
        }
        if (task) {
            res = posTagger.parse(ins);
        } else {
            res = tokenTagger.parse(ins);
        }
        String[] parts = res.split("\n");
        for (int i = 0; i < parts.length; i++) {
            output.add(parts[i]);
        }

        return output;

    }
    
    private ArrayList<String> CreateTestArray(String text) throws IOException {
        ArrayList<String> out = new ArrayList<String>();
        String OutputText = "";
        // ArrayList<String> tokenSeq = tokenizeText(normalize(text.trim()));
        ArrayList<String> tokenSeq = tokenize(normalize(text.trim()));
        OutputText += " ";
        // out.add("- -1 -1 -300000 -300000 N N");
        for (int j = 0; j < tokenSeq.size(); j++) {
            String s = tokenSeq.get(j);
            String token = s;
            if (s.startsWith("لل"))
                s = "لال" + s.substring(2);
            if (s.trim().length() > 0) {
                OutputText += s.substring(0, 1);
                Integer preScore = -300000;
                Integer sufScore = -300000;
                if (IntegerLanguageModel.containsKey(token)) {
                    sufScore = IntegerLanguageModel.get(token);
                }
                String tmp = String.format("%s %s %s %s %s %s %s", token.substring(0, 1), "0", String.valueOf(token.length()), String.valueOf(preScore), String.valueOf(sufScore), "N", token);
                out.add(tmp);
                for (int i = 1; i < token.length(); i++) {
                    preScore = -300000;
                    sufScore = -300000;
                    if (IntegerLanguageModel.containsKey(token.substring(0, i))) {
                        preScore = IntegerLanguageModel.get(token.substring(0, i));
                    }
                    if (IntegerLanguageModel.containsKey(token.substring(i))) {
                        sufScore = IntegerLanguageModel.get(token.substring(i));
                    }
                    tmp = String.format("%s %s %s %s %s %s %s", token.substring(i, i + 1), String.valueOf(i), String.valueOf(token.length() - i), String.valueOf(preScore), String.valueOf(sufScore), token.substring(0, i), token.substring(i));
                    out.add(tmp);
                    OutputText += s.substring(i, i + 1);
                }
                out.add("- -1 -1 -300000 -300000 N N");
                // out.add("");
                OutputText += " ";
            } else {
                out.add("");
            }
        }

        return out;
    }

    private List<String> CreateOutputArray(List<String> outputTok) throws IOException {
        List<String> out = new ArrayList<String>();
        String lastSeg = "";

        for (String s : outputTok) {
            s = s.trim();
            if (s.endsWith("B") || s.endsWith("O")) {
                if (!lastSeg.isEmpty()) {
                    out.add(lastSeg);
                }
                if (s.endsWith("O")) {
                    out.add("_");
                    lastSeg = "";
                } else {
                    lastSeg = s.substring(0, 1);
                }
            } else if (s.endsWith("I")) {
                lastSeg += s.substring(0, 1);
            } else {
                out.add("");
            }
        }
        if (!lastSeg.isEmpty()) {
            out.add(lastSeg);
        }
        return out;
    }

    
    private List<String> CreateTestArrayPOS(List<String> outputTok) throws IOException {
        List<String> out = CreateOutputArray(outputTok);
//        String lastSeg = "";
//
//        for (String s : input) {
//            s = s.trim();
//            if (s.endsWith("B") || s.endsWith("O")) {
//                if (!lastSeg.isEmpty()) {
//                    out.add(lastSeg);
//                }
//                if (s.endsWith("O")) {
//                    out.add("_");
//                    lastSeg = "";
//                } else {
//                    lastSeg = s.substring(0, 1);
//                }
//            } else if (s.endsWith("I")) {
//                lastSeg += s.substring(0, 1);
//            } else {
//                out.add("");
//            }
//        }
//        if (!lastSeg.isEmpty()) {
//            out.add(lastSeg);
//        }

        ArrayList<String> word = new ArrayList<String>();
        ArrayList<String> out2 = new ArrayList<String>();
        // correct erroneous splits
        // check from the beginning & end of string
        // check for prefixes
        // put prefixes
        String currentPrefix = "";
        String previousPrefix = "";
        int posInSentence = 1;
        for (int j = 0; j < out.size(); j++) {
            if (out.get(j).equals("_")) {
                if (word.size() > 0) {
                    int iValidPrefix = -1;
                    while (iValidPrefix + 1 < word.size() && hPrefixes.containsKey(word.get(iValidPrefix + 1))) {
                        iValidPrefix++;
                    }

                    int iValidSuffix = word.size();

                    while (iValidSuffix > Math.max(iValidPrefix, 0) && (hSuffixes.containsKey(word.get(iValidSuffix - 1)) || word.get(iValidSuffix - 1) == "_")) {
                        iValidSuffix--;
                    }

                    for (int i = 0; i <= iValidPrefix; i++) {
                        // word2.add(word.get(i));
                        out2.add(word.get(i) + "\t"
                                + "Y" + "\t"
                                // + isNumber(word.get(i)) + "\t"
                                + isForeign(word.get(i)) + "\t"
                                + "Y" + "\t"
                                + "0");
                        currentPrefix += word.get(i);
                    }
                    String stemPart = "";
                    for (int i = iValidPrefix + 1; i < iValidSuffix; i++) {
                        stemPart += word.get(i);
                    }
                    if (previousPrefix.trim().length() == 0) {
                        previousPrefix = "#";
                    }
                    if (currentPrefix.trim().length() == 0) {
                        currentPrefix = "#";
                    }
                    if (stemPart.matches("[" + AllArabicLetters + "]+")) {
                        out2.add(stemPart + "\t"
                                + fitTemplate(stemPart) + "\t"
                                // + isNumber(stemPart) + "\t"
                                + isForeign(stemPart) + "\t"
                                + previousPrefix + "-" + currentPrefix + "\t"
                                + posInSentence);
                    } else if (stemPart.length() > 0) {
                        out2.add(stemPart + "\t"
                                + "Y" + "\t"
                                // + isNumber(stemPart) + "\t"
                                + isForeign(stemPart) + "\t"
                                + "Y" + "\t"
                                + "0");
                    }

                    if (posInSentence < 5) {
                        posInSentence++;
                    }

                    if (iValidSuffix == iValidPrefix)
                    {
                        iValidSuffix++;
                    }
                    
                    for (int i = iValidSuffix; i < word.size() && iValidSuffix != iValidPrefix; i++) {
                        // word2.add(word.get(i));
                        out2.add(word.get(i) + "\t"
                                + "Y" + "\t"
                                //+ isNumber(word.get(i)) + "\t"
                                + isForeign(word.get(i)) + "\t"
                                + "Y" + "\t"
                                + "0");
                    }

                }
                out2.add("-" + "\t"
                        + "Y" + "\t"
                        //+ "NOT" + "\t"
                        + isForeign("-") + "\t"
                        + "Y" + "\t"
                        + "0");
                word.clear();
                previousPrefix = currentPrefix;
                currentPrefix = "";
            } else if (out.get(j).trim().length() == 0) {
                out2.add("");
                posInSentence = 1;
            } else {
                word.add(out.get(j));
            }
        }
        return out2;
    }

    private String isNumber(String input) {
        if (hmNumber.containsKey(input.trim()) || input.matches("[" + AllHindiDigits + "0-9\\.,\u00BC-\u00BE]+")) {
            return "NUM";
        } else {
            return "NOT";
        }
    }

    private String isForeign(String input) {
        if (isNumber(input.trim()).equals("NUM")) {
            return "NUM";
        } else if (input.trim().equals("-")) {
            return "B";
        } else if (input.trim().matches(".*[a-zA-z]+.*")) {
            return "FOREIGN";
        } else if (input.trim().matches("[" + AllArabicLetters + "]+")) {
            return "ARAB";
        } else if (input.trim().matches(".*[" + ALLDelimiters + "]+.*")) {
            return "PUNC";
        } else {
            return "OTHER";
        }
    }
    
    private void LoadCRFModels(String PathDir) throws IOException, InterruptedException {
        // load CRF Models
        tokenTagger = new Tagger("-m " + PathDir + "tokenizer.model");
        tokenTagger.clear();
        posTagger = new Tagger("-m " + PathDir + "pos.model");
        posTagger.clear();
    }

    private void PrepareCRFPP(String wordCountFile, String tokenModel, String posModel, String phraseModel) throws FileNotFoundException, IOException, ClassNotFoundException {
        // load integerLanguageModel
        File file = new File(BinDir + "IntegerLanguageModel.ser");
        if (IntegerLanguageModel != null && !IntegerLanguageModel.isEmpty()) {
            // do nothing
        }
        if (file.exists()) {
            ObjectInputStream ios = new ObjectInputStream(new FileInputStream(file));
            IntegerLanguageModel = (THashMap<String, Integer>) ios.readObject();
        } else {
            BufferedReader lmFile
                    = // new BufferedReader(new InputStreamReader(this.getClass().getClassLoader().getResourceAsStream(wordCountFile)));
                    // new BufferedReader(new FileReader(wordCountFile));
                    new BufferedReader(new InputStreamReader(new FileInputStream(wordCountFile), "UTF8"));
            IntegerLanguageModel = new THashMap<String, Integer>(1600000); // new HashMap<String, Integer>();
            String sLine = "";
            if (IntegerLanguageModel.isEmpty()) {
                while ((sLine = lmFile.readLine()) != null) {
                    String parts[] = sLine.split("\t");
                    if (parts.length == 2 && parts[0].matches("[0-9\\-\\.]+")) {
                        Integer score = new Integer((int) (1000 * Float.parseFloat(parts[0])));
                        IntegerLanguageModel.put(parts[1], score);
                    }
                }
                lmFile.close();
                ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file));
                oos.writeObject(IntegerLanguageModel);
                oos.close();
            }
        }
    }

    private String fitTemplate(String line) {
        String tmp = fitStemTemplate(utf82buck(line));
        if (tmp.contains("Y") && (line.endsWith("\u0629") || line.endsWith("\u064a"))) { // ends with ta marbouta or alef maqsoura
            tmp = fitStemTemplate(utf82buck(line.substring(0, line.length() - 1)));
        }
        if (tmp.contains("Y") && (line.endsWith("\u064a\u0629"))) { // ends with ya + ta marbouta
            tmp = fitStemTemplate(utf82buck(line.substring(0, line.length() - 2)));
        }
        if (tmp.contains("Y") && (line.endsWith("\u0649"))) { // ends with alef maqsoura
            tmp = fitStemTemplate(utf82buck(line.substring(0, line.length() - 1) + "\u064a"));
        }
        if (tmp.contains("Y") && (line.contains("\u0623") || line.contains("\u0622") || line.contains("\u0625"))) { // contains any form of alef
            tmp = fitStemTemplate(line.replaceAll("[\u0625\u0623\u0622]", "\u0627")); // normalize alef
        }
        if (tmp.contains("Y")) {
            tmp = fitStemTemplate(line + line.substring(line.length() - 1));
        }
        return tmp;
    }

    private String fitStemTemplate(String stem) {
        ArrayList<String> template = new ArrayList<String>();
        int len = stem.length();
        if (!Templates.containsKey(len)) {
            template.add("Y");
            return "Y";
        } else {
            if (len == 2) {
                if (hmRoot.containsKey(buck2morph(stem + stem.substring(1)))) {
                    template.add("fE");
                    return "fE";
                }
            } else {
                ArrayList<String> t = Templates.get(len);
                for (String s : t) {
                    String root = "";
                    int lastF = -1;
                    int lastL = -1;
                    boolean broken = false;
                    for (int i = 0; i < s.length() && broken == false; i++) {
                        if (s.substring(i, i + 1).equals("f")) {
                            root += stem.substring(i, i + 1);
                        } else if (s.substring(i, i + 1).equals("E")) {
                            // check if repeated letter in the root
                            if (lastF == -1) // letter not repeated
                            {
                                root += stem.substring(i, i + 1);
                                lastF = i;
                            } else // letter repeated
                            {
                                if (stem.substring(i, i + 1) != stem.substring(lastF, lastF + 1)) {
                                    // stem template is broken
                                    broken = true;
                                }
                            }
                        } else if (s.substring(i, i + 1).equals("l")) {
                            // check if repeated letter in the root
                            if (lastL == -1) // letter not repeated
                            {
                                root += stem.substring(i, i + 1);
                                lastL = i;
                            } else // letter repeated
                            {
                                if (stem.substring(i, i + 1) != stem.substring(lastL, lastL + 1)) {
                                    // stem template is broken
                                    broken = true;
                                }
                            }
                        } else if (s.substring(i, i + 1).equals("C")) {
                            root += stem.substring(i, i + 1);
                        } else {
                            if (!stem.substring(i, i + 1).equals(s.substring(i, i + 1))) {
                                // template is broken
                                broken = true;
                            }
                        }
                    }

                    root = buck2morph(root);

                    ArrayList<String> altRoot = new ArrayList<String>();
                    if (broken == false && !hmRoot.containsKey(root)) {

                        for (int j = 0; j < root.length(); j++) {
                            if (root.substring(j, j + 1).equals("y") || root.substring(j, j + 1).equals("A") || root.substring(j, j + 1).equals("w")) {
                                String head = root.substring(0, j);
                                String tail = root.substring(j + 1);
                                if (hmRoot.containsKey(head + "w" + tail)) {
                                    altRoot.add(head + "w" + tail);
                                }
                                if (hmRoot.containsKey(head + "y" + tail)) {
                                    altRoot.add(head + "y" + tail);
                                }
                                if (hmRoot.containsKey(head + "A" + tail)) {
                                    altRoot.add(head + "A" + tail);
                                }
                            }
                        }
                    }
                    if (broken == false && hmRoot.containsKey(root)) {
                        template.add(s + "/" + root);
                    }
                    for (String ss : altRoot) {
                        template.add(s + "/" + ss);
                    }
                }
            }

        }
        if (template.size() == 0) {
            template.add("Y");
            return "Y";
        }

        ArrayList<String> templateWithC = new ArrayList<String>();
        ArrayList<String> templateWithoutC = new ArrayList<String>();

        for (String ss : template) {
            if (ss.contains("C")) {
                templateWithC.add(ss);
            } else {
                templateWithoutC.add(ss);
            }
        }
        if (templateWithoutC.size() == 0) {
            return getBestTemplate(template);
        } else {
            return getBestTemplate(templateWithoutC);
        }
    }

    private String getBestTemplate(ArrayList<String> template) {
        double bestScore = 0;
        String bestTemplate = "";
        for (String s : template) {
            String[] parts = s.split("/");
            if (parts.length == 2) {
                double score = hmRoot.get(parts[1]) * hmTemplate.get(parts[0]);
                if (bestScore < score) {
                    bestScore = score;
                    bestTemplate = parts[0];
                }
            }
        }
        return bestTemplate;
    }

}
