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

import java.util.List;
import java.util.Map;

import javax.jcr.Session;

public class FacetedNavigationEngineWrapperImpl<Q extends FacetedNavigationEngine.Query, C extends FacetedNavigationEngine.Context>
  implements FacetedNavigationEngine<Q,C>
{
  private FacetedNavigationEngine<Q,C> upstream;

  public FacetedNavigationEngineWrapperImpl(FacetedNavigationEngine<Q,C> upstream) {
    this.upstream = upstream;
  }

  public C prepare(String principal, Map<String,String[]> authorizationQuery, List<Q> initialQueries, Session session) {
    Context context = upstream.prepare(principal, authorizationQuery, initialQueries, session);
    return (C) context;
  }

  public void unprepare(C authorization) {
    upstream.unprepare(authorization);
  }

  public void reload(Map<String,String[]> facetValues) {
    upstream.reload(facetValues);
  }

  public boolean requiresReload() {
    boolean rtvalue = upstream.requiresReload();
    return rtvalue;
  }

  public boolean requiresNotify() {
    boolean rtvalue = upstream.requiresNotify();
    return rtvalue;
  }

  public void notify(String docId, Map<String,String[]> oldFacets, Map<String,String[]> newFacets) {
    upstream.notify(docId, oldFacets, newFacets);
  }

  public void purge() {
    upstream.purge();
  }

  public Result view(String queryName, Q initialQuery, C authorization,
                   Map<String,String> facetsQuery, Q openQuery,
                   Map<String,Map<String,Count>> resultset,
                   Map<Map<String,String>,Map<String,Map<String,Count>>> futureFacetsQueries,
                   HitsRequested hitsRequested) throws UnsupportedOperationException
  {
    Result result;
    // System.err.println("FacetedNavigationEngineWrapperImpl.view(\"" + queryName + "\"," + initialQuery + ",\"" + authorization + "\",\"" + facetsQuery + "\",\"..,..,..,"  + hitsRequested + ")");
    result = upstream.view(queryName, initialQuery, authorization, facetsQuery, openQuery, resultset,
                           futureFacetsQueries, hitsRequested);
    return result;
  }

  public Result view(String queryName, Q initialQuery, C authorization,
                     Map<String,String> facetsQuery, Q openQuery, HitsRequested hitsRequested)
  {
    Result result;
    result = upstream.view(queryName, initialQuery, authorization, facetsQuery, openQuery, hitsRequested);
    return result;
  }

  public Q parse(String query)
  {
    return upstream.parse(query);
  }
}
