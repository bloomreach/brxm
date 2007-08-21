/*
  THIS CODE IS UNDER CONSTRUCTION, please leave as is until
  work has proceeded to a stable level, at which time this comment
  will be removed.  -- Berry
*/ 

/*
 * Copyright 2007 Hippo.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository;

import java.lang.String;
import java.lang.Integer;
import java.util.Map;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.io.IOException;

import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

public class FacetedNavigationEngineFirstImpl
  implements FacetedNavigationEngine<FacetedNavigationEngineFirstImpl.QueryImpl,
                                     FacetedNavigationEngineFirstImpl.ContextImpl>
{
  class QueryImpl extends FacetedNavigationEngine.Query {
    String xpath;
    public QueryImpl(String xpath) {
      this.xpath = xpath;
    }
  }
  class ResultImpl extends FacetedNavigationEngine.Result {
    int length;
    Iterator<String> iter;
    ResultImpl(int length, Iterator<String> iter) {
      this.length = length;
      this.iter = iter;
    }
    public int length() {
      return length;
    }
    public Iterator<String> iterator() {
      return iter;
    }
  }
  class ContextImpl extends FacetedNavigationEngine.Context {
    Session session;
    String principal;
    Map<String,String> authorizationQuery;
    ContextImpl(Session session, String principal, Map<String,String> authorizationQuery) {
      this.session = session;
      this.principal = principal;
      this.authorizationQuery = authorizationQuery;
    }
  }

  public FacetedNavigationEngineFirstImpl() {
  }

  public ContextImpl prepare(String principal, Map<String,String> authorizationQuery, List<QueryImpl> initialQueries, Session session) {
    return new ContextImpl(session, principal, authorizationQuery);
  }
  public void unprepare(ContextImpl authorization) {
    // deliberate ignore
  }
  public void reload(Map<String,String[]> facetValues) {
    // deliberate ignore
  }
  public boolean requiresReload() {
    return false;
  }
  public boolean requiresNotify() {
    return false;
  }
  public void notify(String docId, Map<String,String[]> oldFacets, Map<String,String[]> newFacets) {
    // deliberate ignore
  }
  public void purge() {
    // deliberate ignore
  }

  private static StringBuffer getSearchQuery(String initialQuery, Map<String,String> facetsQuery, String facet) {
    StringBuffer searchquery = new StringBuffer();
    String clause;
    for(Map.Entry<String,String> entry : facetsQuery.entrySet()) {
      if(searchquery.length() > 0)
        searchquery.append(",");
      searchquery.append("@");
      searchquery.append(entry.getKey());
      searchquery.append("='");
      searchquery.append(entry.getValue());
      searchquery.append("'");
    }
    if(facet != null) {
      if (searchquery.length() > 0)
        searchquery.append(",");
      searchquery.append("@");
      searchquery.append(facet);
    }
    searchquery.insert(0, initialQuery + "//node()" + "[");
    searchquery.append("]");
    if(facet != null) {
      searchquery.append("/@");
      searchquery.append(facet);
    }
    return searchquery;
  }

  public Result view(String queryName, QueryImpl initialQuery, ContextImpl authorization,
                   Map<String,String> facetsQuery, QueryImpl openQuery,
                   Map<String,Map<String,Count>> resultset,
                   Map<Map<String,String>,Map<String,Map<String,Count>>> futureFacetsQueries,
                   boolean hitsRequested) throws UnsupportedOperationException
  {
    try {
      Session session = authorization.session;
      int resultNodeCount = 0;
      for(String facet : resultset.keySet()) {
        String xpath = new String(getSearchQuery(initialQuery.xpath, facetsQuery, facet));
        javax.jcr.query.Query facetValuesQuery = session.getWorkspace().getQueryManager().createQuery(xpath, javax.jcr.query.Query.XPATH);
        QueryResult facetValuesResult = facetValuesQuery.execute();
        int count;
        Map<String,Count> facetValuesMap = resultset.get(facet);
        for(RowIterator iter=facetValuesResult.getRows(); iter.hasNext(); ) {
          String facetValue = iter.nextRow().getValues()[0].getString();
          if(!facetValuesMap.containsKey(facetValue))
            facetValuesMap.put(facetValue,new Count(1));
          else
            facetValuesMap.get(facetValue).count++;
          resultNodeCount++;
        }
      }
      return this . new ResultImpl(resultNodeCount, null);
    } catch(javax.jcr.query.InvalidQueryException ex) {
      throw new UnsupportedOperationException(); // FIXME
    } catch(javax.jcr.ValueFormatException ex) {
      throw new UnsupportedOperationException(); // FIXME
    } catch(javax.jcr.RepositoryException ex) {
      throw new UnsupportedOperationException(); // FIXME
    }
  }

  public Result view(String queryName, QueryImpl initialQuery, ContextImpl authorization,
                     Map<String,String> facetsQuery, QueryImpl openQuery)
  {
    try {
      Session session = authorization.session;
      LinkedList list = new LinkedList<String>();
      int size = 0;
      for(String facet : facetsQuery.keySet()) {
        String query = new String(getSearchQuery(initialQuery.xpath, facetsQuery, facet));
        javax.jcr.query.Query facetValuesQuery = session.getWorkspace().getQueryManager().createQuery(query, javax.jcr.query.Query.XPATH);
        QueryResult facetValuesResult = facetValuesQuery.execute();
        RowIterator iter = facetValuesResult.getRows();
        while(iter.hasNext()) {
          size += iter.getSize();
          String nodeName = iter.nextRow().getValues()[0].getString();
          list.add(nodeName);
        }
      }
      return this . new ResultImpl(size, list.iterator());
    } catch(javax.jcr.query.InvalidQueryException ex) {
      throw new UnsupportedOperationException(); // FIXME
    } catch(javax.jcr.ValueFormatException ex) {
      throw new UnsupportedOperationException(); // FIXME
    } catch(javax.jcr.RepositoryException ex) {
      throw new UnsupportedOperationException(); // FIXME
    }
  }

  public QueryImpl parse(String query)
  {
    return this . new QueryImpl(query);
  }
}
