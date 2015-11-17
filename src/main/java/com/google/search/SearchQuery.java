package com.google.search;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

//import lombok.Getter;
//import lombok.ToString;



//@ToString
//@Getter
public class SearchQuery 
{
  private final String  query;
  private final String  site;
  private final Integer numResults;

  public SearchQuery(Builder builder) 
  {
    this.query = builder.query;
    this.site = builder.site;
    this.numResults = builder.numResults;
  }

  public static class Builder 
  {

    private	 String  query;
    private  String  site;
    private  Integer numResults = 10;

    public Builder(String query) 
    {
      try 
      {
//		this.query = URLEncoder.encode("\""+query+"\"", "UTF-8");
  		this.query = URLEncoder.encode(query, "UTF-8");    	  
      } 
      catch (UnsupportedEncodingException e) 
      {
		e.printStackTrace();
      }
    }

    public Builder site(String site) 
    {
      this.site = site;
      return this;
    }

    public Builder numResults(Integer numResults) 
    {
      this.numResults = numResults;
      return this;
    }

    public SearchQuery build() 
    {
      return new SearchQuery(this);
    }
  }

  public String getQuery() 
  {
	return this.query;
  }

  public Integer getNumResults() 
  {
	return this.numResults;
  }

  public String getSite() 
  {
	return this.site;
  }
}
