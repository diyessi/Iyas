package com.google.search;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import lombok.Data;
//import lombok.extern.slf4j.Slf4j;


//@Slf4j
public class GoogleWebSearch 
{

  private SearchConfig CONFIG = new SearchConfig();

  public GoogleWebSearch() 
  {
  }

  public GoogleWebSearch(SearchConfig config) 
  {
    this.CONFIG = config;
  }

  public SearchResult search(SearchQuery query) 
  {
    HttpEntity entity = getResponse(query).getEntity();
    List<SearchHit> hitsUrls = null;
    try 
    {
      hitsUrls = parseResponse(EntityUtils.toString(entity));
    } 
    catch (IOException e) 
    {
      throw new RuntimeException(e);
    }
    return new SearchResult(query, hitsUrls);
  }

  public SearchResult search(String query, int numResults) 
  {
    SearchQuery searchQuery = new SearchQuery.Builder(query).numResults(numResults).build();
    return search(searchQuery);
  }


  private HttpResponse getResponse(SearchQuery query) 
  {
    String uri = getUri(query);
    HttpClient client = HttpClientBuilder.create().build();
    HttpGet request = new HttpGet(uri);
    HttpResponse result = null;
    try 
    {
      result = client.execute(request);
    } 
    catch (IOException e) 
    {
      e.printStackTrace();
    }
    return result;
  }

  private String getUri(SearchQuery query) 
  {
    String uri = CONFIG.getGoogleSearchUrl().replaceAll(CONFIG.PLHD_QUERY, query.getQuery());
    uri = uri.replaceAll(CONFIG.PLHD_RESULTS_NUM, ifPresent(CONFIG.PLHD_RESULTS_NUM, query.getNumResults()));
    uri = uri.replaceAll(CONFIG.PLHD_SITE, ifPresent(CONFIG.PLHD_SITE, query.getSite()));
    return uri;
  }

  private String ifPresent(String plhd, Object param) 
  {
    if (param != null) 
    {
      return plhd + param;
    } 
    else 
    {
      return "";
    }
  }

  protected List<SearchHit> parseResponse(String document) 
  {
    Document searchDoc = Jsoup.parse(document);
    Elements contentDiv = searchDoc.select("div#search");
    List<SearchHit> hitsUrls = new ArrayList<SearchHit>();
    Elements articlesLinks = contentDiv.select("a[href]"); // a with href
    for (Element link : articlesLinks) 
    {
      String linkHref = link.attr("href");
      Matcher matcher = CONFIG.PURE_URL_PATTERN.matcher(linkHref);
      if (matcher.matches()) 
      {
        String pureUrl = matcher.group(1);
        if (!pureUrl.startsWith(CONFIG.CACHE_URL)) 
        {
          hitsUrls.add(new SearchHit(pureUrl));
        }
      }
    }
    return hitsUrls;
  }

  //@Data
  public static class SearchConfig 
  {
    private final String PLHD_QUERY       = "__query__";
    private       String PLHD_RESULTS_NUM = "&num=";
    private       String PLHD_SITE        = "&as_sitesearch=";

    private       String  PURE_URL_REGEX  = "/url\\?q=(.*)&sa.*";
    private final Pattern PURE_URL_PATTERN = Pattern.compile(PURE_URL_REGEX);

    private String CACHE_URL                = "http://webcache.googleusercontent.com";

    private String GOOGLE_SEARCH_URL_PREFIX = "https://www.google.com/search?";

    public String getGoogleSearchUrl()
    {
      return  GOOGLE_SEARCH_URL_PREFIX + "q=" + PLHD_QUERY + PLHD_RESULTS_NUM + PLHD_SITE;
    }
    
    public void setGOOGLE_SEARCH_URL_PREFIX(String prefix)
    {
    	this.GOOGLE_SEARCH_URL_PREFIX = prefix;
    }
  }
}
