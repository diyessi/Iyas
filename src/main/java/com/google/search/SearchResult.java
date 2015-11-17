package com.google.search;

import java.util.ArrayList;
import java.util.List;

//import javax.annotation.concurrent.NotThreadSafe;

//import lombok.Getter;


//@NotThreadSafe
//@Getter
public class SearchResult 
{
  private final List<SearchHit> hits;
  private final SearchQuery query;

  public SearchResult(SearchQuery query, List<SearchHit> hits) 
  {
    this.query = query;
    this.hits = hits;
  }

  public String getQuery()
  {
	return this.query.getQuery();
  }
  
  public List<String> getUrls()
  {
    List<String> urls = new ArrayList<String>(hits.size());
    for (SearchHit hit : hits) 
    {
      urls.add(hit.getUrl());
    }
    return urls;
  }

  public int getSize()
  {
    return hits.size();
  }
}
