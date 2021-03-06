/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package arabicpostagger;

import java.util.ArrayList;

/**
 *
 * @author kareemdarwish
 */
public class ArabicUtils {
    // ALL Arabic letters \U0621-\U063A\U0641-\U064A
    public static final String AllArabicLetters = "\u0621-\u063A\u0641-\u064A";
    // ALL Hindi digits \U0660-\U0669
    public static final String AllHindiDigits = "\u0660-\u0669";
    // ALL Arabic letters and Hindi digits \U0621-\U063A\U0641-\U064A\U0660-\U0669
    public static final String AllArabicLettersAndHindiDigits = "\u0621-\u063A\u0641-\u064A\u0660-\u0669";
    public static final String ALLDelimiters = "\u0000-\u002F\u003A-\u0040\u007B-\u00BB\u005B-\u005D\u005F-\u0060\\^\u0600-\u060C\u06D4-\u06ED";
    
    public static final char ALEF = '\u0627';
    public static final char ALEF_MADDA = '\u0622';
    public static final char ALEF_HAMZA_ABOVE = '\u0623';
    public static final char ALEF_HAMZA_BELOW = '\u0625';

    public static final char HAMZA = '\u0621';
    public static final char HAMZA_ON_NABRA = '\u0624';
    public static final char HAMZA_ON_WAW = '\u0626';

    public static final char YEH = '\u064A';
    public static final char DOTLESS_YEH = '\u0649';

    public static final char TEH_MARBUTA = '\u0629';
    public static final char HEH = '\u0647';
    
    public static final String prefixes[] = {
        // "ال", "و", "ف", "ب", "ك", "ل", "لل"
        "\u0627\u0644", "\u0648", "\u0641", "\u0628", "\u0643", "\u0644", "\u0644\u0644"
    };

    public static final String suffixes[] = {
        // "ه", "ها", "ك", "ي", "هما", "كما", "نا", "كم", "هم", "هن", "كن",
        // "ا", "ان", "ين", "ون", "وا", "ات", "ت", "ن", "ة"
        "\u0647", "\u0647\u0627", "\u0643", "\u064a", "\u0647\u0645\u0627", "\u0643\u0645\u0627", "\u0646\u0627", "\u0643\u0645", "\u0647\u0645", "\u0647\u0646", "\u0643\u0646",
        "\u0627", "\u0627\u0646", "\u064a\u0646", "\u0648\u0646", "\u0648\u0627", "\u0627\u062a", "\u062a", "\u0646", "\u0629"
    }; 
    
    public static String buck2morph(String input) {
        input = input.replace('$', 'P').replace('Y', 'y').replace('\'', 'A').replace('|', 'A').replace('&', 'A').replace('}', 'A').replace('*', 'O');
        return input;
    }

    public static String utf82buck(String input) {
        input = input.replaceAll("\u0627", "A").replaceAll("\u0625", "<").replaceAll("\u0622", "|").replaceAll("\u0623", ">").replaceAll("\u0621", "'");
        input = input.replaceAll("\u0628", "b").replaceAll("\u062a", "t").replaceAll("\u062b", "v").replaceAll("\u062c", "j").replaceAll("\u062d", "H");
        input = input.replaceAll("\u062e", "x").replaceAll("\u062f", "d").replaceAll("\u0630", "*").replaceAll("\u0631", "r").replaceAll("\u0632", "z");
        input = input.replaceAll("\u0633", "s").replaceAll("\u0634", "\\$").replaceAll("\u0635", "S").replaceAll("\u0636", "D").replaceAll("\u0637", "T");
        input = input.replaceAll("\u0638", "Z").replaceAll("\u0639", "E").replaceAll("\u063a", "g").replaceAll("\u0641", "f").replaceAll("\u0642", "q");
        input = input.replaceAll("\u0643", "k").replaceAll("\u0644", "l").replaceAll("\u0645", "m").replaceAll("\u0646", "n").replaceAll("\u0647", "h");
        input = input.replaceAll("\u0648", "w").replaceAll("\u064a", "y").replaceAll("\u0649", "Y").replaceAll("\u0629", "p").replaceAll("\u0624", "&");
        input = input.replaceAll("\u0626", "}");
        return input;
    }
    
    public static ArrayList<String> tokenizeText(String input) {

        char[] charInput = input.toCharArray();
        input = "";
        for (int i = 0; i < charInput.length; i++) {
            int c = charInput[i];
            if (c <= 32 || c == 127 || (c >= 194128 && c <= 194160)) {
                input += " ";
            } else {
                input += charInput[i];
            }
        }

        input = input.replace("\u200B", "").replace("    ", "");
        ArrayList<String> output = new ArrayList<String>();
        String[] words = input.split("[\\\u061f \t\n\r,\\-<>\"\\?\\:;\\&]+");
        for (int i = 0; i < words.length; i++) {
            if (words[i].startsWith("#")
                    || words[i].startsWith("@")
                    // || words[i].startsWith(":")
                    || words[i].startsWith(";")
                    || words[i].startsWith("http://")
                    || words[i].matches("[a-zA-Z0-9\\-\\._]+@[a-zA-Z0-9\\-\\._]+")) {
                if (words[i].endsWith(":") || words[i].endsWith("\'")) {
                    words[i] = words[i].substring(0, words[i].length() - 1);
                }
                output.add(normalize(words[i].trim()));
            } else {
                // String[] tmp = words[i].split("[~<>_\"\\-,\\.،\\!\\#\\$\\%\\?\\^\\&\\*\\(\\)\\[\\]\\{\\}\\/\\|\\\\]+");
                String[] tmp = words[i].split("[" + ArabicUtils.ALLDelimiters + "]+");
                for (int j = 0; j < tmp.length; j++) {
                    while (tmp[j].startsWith("\'")) {
                        tmp[j] = tmp[j].substring(1);
                    }
                    while (tmp[j].endsWith("\'") || tmp[j].endsWith("\"") || tmp[j].endsWith(":")) {
                        tmp[j] = tmp[j].substring(0, tmp[j].length() - 1);
                    }
                    if (!tmp[j].isEmpty() && tmp[j].length() > 0) //
                    {
                        output.add(normalize(tmp[j].trim()));
                    }
                }
            }
        }
        return output;
    }
    
     public static ArrayList<String> tokenize(String s) {
        s = removeNonCharacters(s);
        s = removeDiacritics(s);
        s = s.replaceAll("[\t\n\r]", " ");

        ArrayList<String> output = new ArrayList<String>();

        String[] words = s.split(" ");
        for (int i = 0; i < words.length; i++) {
            if ( //words[i].StartsWith("#")
                    //||
                    words[i].startsWith("@")
                    || words[i].startsWith(":")
                    || words[i].startsWith(";")
                    || words[i].startsWith("http://")
                    || words[i].matches("[a-zA-Z0-9\\-\\._]+@[a-zA-Z0-9\\-\\._]+")) {
                // if (words[i].endsWith(":") || words[i].endsWith("\'")) {
                //    words[i] = words[i].substring(0, words[i].length() - 1);
                // }
                output.add(words[i]);
            } else {
                for (String ss : charBasedTonkenizer(words[i]).split(" ")) {
                    if (ss.length() > 0) {
                        if (ss.startsWith("لل"))
                            output.add("لال" + ss.substring(2));
                        else
                            output.add(ss);
                    }
                }
            }
        }
        return output;
    }

    private static String charBasedTonkenizer(String s) {
//        String[] seperator = {"~", "»", "«", "؛", "<", ">", "_", "\"", "-", "،", "!", "#", "?", "^", "&", "*", "(", ")", "[", "]", "{", "}", "|", "\\", "-", "<", ">", "\"", "?", "،", "؟", ";", ":"};
//        ArrayList<String> seperatorList = new ArrayList<String>();
//        for (String ss : seperator) {
//            seperatorList.add(ss);
//        }

        String sFinal = "";

        for (int i = 0; i < s.length(); i++) {
            // if (seperatorList.contains(s.substring(i, i + 1))) {
            if (s.substring(i, i + 1).matches("[" + ArabicUtils.ALLDelimiters + "]")) {
                sFinal += " " + s.substring(i, i + 1) + " ";
            } else if (s.substring(i, i + 1) == "." || s.substring(i, i + 1) == "," || s.substring(i, i + 1) == ".") {
                if (i == 0) {
                    sFinal += s.substring(i, i + 1) + " ";
                } else if (i == s.length() - 1) {
                    sFinal += " " + s.substring(i, i + 1);
                } else if (s.substring(i - 1, i).matches("[0-9]") && s.substring(i + 1, i + 2).matches("[0-9]")) {
                    sFinal += s.substring(i, i + 1);
                } else {
                    sFinal += " " + s.substring(i, i + 1) + " ";
                }
            } else if (!s.substring(i, i + 1).matches("[" + ArabicUtils.AllArabicLettersAndHindiDigits + "a-zA-Z0-9]")) {
                sFinal += " " + s.substring(i, i + 1) + " ";
            } else {
                if (i == 0) {
                    sFinal += s.substring(i, i + 1);
                } else {
                    if ((s.substring(i, i + 1).matches("[0-9]") && s.substring(i - 1, i).matches("[" + ArabicUtils.AllArabicLetters + "]"))
                            || (s.substring(i - 1, i).matches("[0-9]") && s.substring(i, i + 1).matches("[" + ArabicUtils.AllArabicLetters + "]"))) {
                        sFinal += " " + s.substring(i, i + 1);
                    } else {
                        sFinal += s.substring(i, i + 1);
                    }

                }
            }
        }
        return sFinal;
    }
    
    public static String normalize(String s) {
        // IF Starts with lam-lam
        if (s.startsWith("\u0644\u0644")) //
        {
            // need to insert an ALEF into the word
            s = "\u0644\u0627\u0644" + s.substring(2);
        }
        // If starts with waw-lam-lam
        if (s.startsWith("\u0648\u0644\u0644")) //
        {
            // need to insert an ALEF into the word
            s = "\u0648\u0644\u0627\u0644" + s.substring(3);
        }
		// If starts with fa-lam-lam
		/* // Until fix the CRFPP training model
         if (s.startsWith("\u0641\u0644\u0644")) //
         {
         // need to insert an ALEF into the word
         s = "\u0641\u0644\u0627\u0644" + s.substring(3);
         }
         */
        /* skip normalization of hamza, ta marbouta and alef maqsoura
         s = s.replace(ALEF_MADDA, ALEF).replace(ALEF_HAMZA_ABOVE, ALEF).replace(ALEF_HAMZA_BELOW, ALEF);
         s = s.replace(DOTLESS_YEH, YEH);
         s = s.replace(HAMZA_ON_NABRA, HAMZA).replace(HAMZA_ON_WAW, HAMZA);
         s = s.replace(TEH_MARBUTA, HEH);
         */
        s = s.replaceAll("[\u0640\u064b\u064c\u064d\u064e\u064f\u0650~\u0651\u0652\u0670]+", ""); // .replace(KASRATAN, EMPTY).replace(DAMMATAN, EMPTY).replace(FATHATAN, EMPTY).replace(FATHA, EMPTY).replace(DAMMA, EMPTY).replace(KASRA, EMPTY).replace(SHADDA, EMPTY).replace(SUKUN, EMPTY);
        return s;
    }
    
    public static String normalizeFull(String s) {
        // IF Starts with lam-lam
        if (s.startsWith("\u0644\u0644")) //
        {
            // need to insert an ALEF into the word
            s = "\u0644\u0627\u0644" + s.substring(2);
        }
        // If starts with waw-lam-lam
        if (s.startsWith("\u0648\u0644\u0644")) //
        {
            // need to insert an ALEF into the word
            s = "\u0648\u0644\u0627\u0644" + s.substring(3);
        }
		// If starts with fa-lam-lam
		/* // Until fix the CRFPP training model
         if (s.startsWith("\u0641\u0644\u0644")) //
         {
         // need to insert an ALEF into the word
         s = "\u0641\u0644\u0627\u0644" + s.substring(3);
         }
         */
        
         s = s.replace(ALEF_MADDA, ALEF).replace(ALEF_HAMZA_ABOVE, ALEF).replace(ALEF_HAMZA_BELOW, ALEF);
         s = s.replace(DOTLESS_YEH, YEH);
         s = s.replace(HAMZA_ON_NABRA, HAMZA).replace(HAMZA_ON_WAW, HAMZA);
         s = s.replace(TEH_MARBUTA, HEH);
         
        s = s.replaceAll("[\u0640\u064b\u064c\u064d\u064e\u064f\u0650~\u0651\u0652\u0670]+", ""); // .replace(KASRATAN, EMPTY).replace(DAMMATAN, EMPTY).replace(FATHATAN, EMPTY).replace(FATHA, EMPTY).replace(DAMMA, EMPTY).replace(KASRA, EMPTY).replace(SHADDA, EMPTY).replace(SUKUN, EMPTY);
        return s;
    }

    public static String removeDiacritics(String s) {
        s = s.replaceAll("[\u0640\u064b\u064c\u064d\u064e\u064f\u0650\u0651\u0652\u0670]", "");
        return s;
    }

    private static String removeNonCharacters(String s) {
        s = s.replaceAll("[\u2000-\u200F\u2028-\u202F\u205F-\u206F]+", " ");
        return s;
    }
}
