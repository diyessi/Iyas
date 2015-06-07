package qa.qcri.qf.semeval2015_3.textnormalization;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


/**
 * Jsoup replace each end of line with a space.
 * This class contains some methods to preserve end of lines (it replaces them with LINE_START before parsing)
 * 
 * @author Simone Filice
 *
 */
public class JsoupUtils {
	
	public static final String LINE_START = "LINE_START_SUB";
	
	public static final Document getDoc(String path) throws IOException{
		String fileContent = readFile(path, StandardCharsets.UTF_8).replaceAll("(?i)<br[^>]*>", LINE_START).replaceAll("\n", LINE_START);
		
		
		return Jsoup.parse(fileContent);
		
	}
	
	private static String readFile(String path, Charset encoding) throws IOException{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
	
	public static String recoverOriginalText(String text){
		return text.replaceAll(LINE_START, "\n");
	}
	
	public static String specialTrim(String text){
		return text.replaceAll(LINE_START, "\n").trim().replaceAll("\n", LINE_START);
	}

}
