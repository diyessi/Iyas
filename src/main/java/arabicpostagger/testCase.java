/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package arabicpostagger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

/**
 *
 * @author kareemdarwish
 */
public class testCase {
    public static void main(String[] args) throws IOException, InterruptedException, FileNotFoundException, ClassNotFoundException
    {
        
                int i=0;
                int mode = 1;
                boolean klm = false;
                String kenlmDir="";
		String arg;
		int args_flag = 0; // correct set of arguments
                
		while (i < args.length) {
			arg = args[i++];
			// 
			if (arg.equals("--help") || arg.equals("-h")) {
				System.out.println("Usage: ArabicPOSTaggerLib <--help|-h> [task|-t pos|tok|ner] <[--klm|-k][kenlmDir]>");
				System.exit(-1);
			} 
			
			if (arg.equals("--task") || arg.equals("-t")) {
                                args_flag++;
				if(args[i].equals("pos")) {
					mode = 2;
				}
				if(args[i].equals("tok")) {
					mode = 1;
				} 
				if(args[i].equals("ner")) {
					mode = 3;
				}
                                args_flag++;
			}

                        if ((arg.equals("--klm") || arg.equals("-k"))&& args.length>=i) {
                            args_flag++;
                            kenlmDir = args[i];
                            klm = true;
                            
                        } 

		} 
                //System.out.println("Args len "+args.length+" flag:"+args_flag+" taks:"+mode+" klmdir="+kenlmDir);
                
		if(args_flag==0 || args.length<2) {
			System.out.println("Usage: ArabicPOSTaggerLib <--help|-h> [task|-t pos|tok|ner] <[--klm|-k][kenlmDir]>");
			System.exit(-1);
		}

                String dataDirectory = System.getProperty("java.library.path");
                processSTDIN(dataDirectory, kenlmDir, mode, klm);
        
        // DenormalizeFile(args[0], args[1], args[2]);
        
        // arguments for processFile: 
        //      dataDirectory, kenlmDirectory, inputFile, outputFile, bDenormalizeText, mode
        // this code has 3 modes:
        //      1 performs word-breaking only
        //      2 performs word-breaking + POS tagging
        //      3 performs word-breaking + POS tagging + NER 
        // processFile(args[0], args[1], args[2], args[3], true, Integer.parseInt(args[4]));
        
        
        /*
        String dataDirectory = "/Users/kareemdarwish/RESEARCH/ArabicProcessingTools-master/POSandNERData/";
        POSTagger tagger = new POSTagger(dataDirectory);
       
        // this word-breaks the text only
        ArrayList<String> output = tagger.tag("وقال الرئيس الأمريكي باراك أوباما أن الولايات المتحدة لن تأخذ قرارا في هذا الشأن .", true);
        for (String s: output)
            System.out.println(s);
        
        // this word-breaks the text and POS tags
        output = tagger.tag("مفاصل ، أماكن ، أمراض", false);
        for (String s: output)
            System.out.println(s);
        */
        // this word-breaks, POS tags, and NE tags
        /*
        ArabicNER ner = new ArabicNER(dataDirectory, tagger);
        
        output = ner.tag("وقال الرئيس الأمريكي باراك أوباما أن الولايات المتحدة لن تأخذ قرارا في هذا الشأن .", true);
        for (String s: output)
            System.out.println(s);
        */
        // ner.generateNewFeatureFileFromTrain("/Users/kareemdarwish/RESEARCH/ner/ACL-SUBMISSION/BinAjeeba.test.seg", "/Users/kareemdarwish/RESEARCH/ner/ACL-SUBMISSION/BinAjeeba.test.seg.out");
        // ner.generateNewFeatureFileFromTrain("/Users/kareemdarwish/RESEARCH/ner/ACL-SUBMISSION/BinAjeeba.train.seg", "/Users/kareemdarwish/RESEARCH/ner/ACL-SUBMISSION/BinAjeeba.train.seg.out");
        
    }
    
    public static void processSTDIN(String dataDirectory, String kenlmDirectory, int mode, boolean bDenormalizeText) throws IOException, FileNotFoundException, ClassNotFoundException, InterruptedException
    {
        DenormalizeText dnt = null;
        if (bDenormalizeText)
        {
            dnt = new DenormalizeText(dataDirectory, kenlmDirectory);
        }
        // this code has 3 modes:
        // 1 performs word-breaking only
        // 2 performs word-breaking + POS tagging
        // 3 performs word-breaking + POS tagging + NER 
        POSTagger tagger = new POSTagger(dataDirectory);
        ArabicNER ner = null; 
        if (mode >= 3)
            ner = new ArabicNER(dataDirectory, tagger);
        BufferedReader sr = new BufferedReader(new InputStreamReader(System.in));
        BufferedWriter sw = new BufferedWriter(new OutputStreamWriter(System.out));
        
        String line = "";
        while ((line = sr.readLine()) != null) {
            if (line.trim().length() > 0)
            {
                if (bDenormalizeText == true)
                    line = dnt.denormalize(line);
                    
                ArrayList<String> output = null;
                if (mode == 1)
                    output = tagger.tag(line, true, false);
                else if (mode == 2)
                    output = tagger.tag(line, false, false);
                else if (mode == 3)
                {
                    output = ner.tag(line, true);
                }
                for (String s : output)
                {
                    if (!s.equals("-") && !s.equals("_"))
                        sw.write(s + " ");
                }
                sw.write("\n");
                sw.flush();
            }
        }
    }
    
    public static void DenormalizeFile(String dataDirectory, String inputFile, String outputFile) throws IOException, InterruptedException, FileNotFoundException, ClassNotFoundException
    {
        
        DenormalizeText dnt = new DenormalizeText("/Users/kareemdarwish/RESEARCH/ArabicProcessingTools-master/POSandNERData/", "/Users/kareemdarwish/RESEARCH/LM/KENLM/kenlm/bin/");
        
        BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(inputFile))));
        BufferedWriter sw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputFile))));
        
        String line = "";
        while ((line = sr.readLine()) != null) {
            if (line.trim().length() > 0)
            {
                String output = dnt.denormalize(line);
                sw.write(output + "\n");
            }
        }
        sw.close();
    }
    
    public static void processFile(String dataDirectory, String kenlmDirectory, String inputFile, String outputFile, boolean bDenormalizeText, int mode) throws IOException, InterruptedException, FileNotFoundException, ClassNotFoundException
    {
        DenormalizeText dnt = null;
        if (bDenormalizeText)
        {
            dnt = new DenormalizeText(dataDirectory, kenlmDirectory);
        }
        // this code has 3 modes:
        // 1 performs word-breaking only
        // 2 performs word-breaking + POS tagging
        // 3 performs word-breaking + POS tagging + NER 
        POSTagger tagger = new POSTagger(dataDirectory);
        ArabicNER ner = null; 
        if (mode >= 3)
            ner = new ArabicNER(dataDirectory, tagger);
        BufferedReader sr = new BufferedReader(new InputStreamReader(new FileInputStream(new File(inputFile))));
        BufferedWriter sw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(outputFile))));
        
        String line = "";
        while ((line = sr.readLine()) != null) {
            if (line.trim().length() > 0)
            {
                if (bDenormalizeText == true)
                    line = dnt.denormalize(line);
                    
                ArrayList<String> output = null;
                if (mode == 1)
                    output = tagger.tag(line, true, false);
                else if (mode == 2)
                    output = tagger.tag(line, false, false);
                else if (mode == 3)
                {
                    output = ner.tag(line, true);
                }
                for (String s : output)
                {
                    if (!s.equals("-"))
                        sw.write(s + " ");
                }
                sw.write("\n");
            }
        }
        sw.close();
    }
}
