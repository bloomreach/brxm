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

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.jcr.Session;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.core.SearchManager;
import org.apache.jackrabbit.core.query.lucene.MultiIndexReader;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.spi.Name;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Searchable;
import org.apache.lucene.search.BooleanClause.Occur;
import org.hippoecm.repository.decorating.RepositoryDecorator;
import org.hippoecm.repository.jackrabbit.RepositoryImpl;
import org.hippoecm.repository.query.lucene.AuthorizationQuery;
import org.hippoecm.repository.query.lucene.CachingFacetResultCollector;
import org.hippoecm.repository.query.lucene.FacetPropExistsQuery;
import org.hippoecm.repository.query.lucene.FacetResultCollector;
import org.hippoecm.repository.query.lucene.FacetsQuery;
import org.hippoecm.repository.query.lucene.ParallelMultiSearcher;
import org.hippoecm.repository.query.lucene.ServicingIndexingConfiguration;
import org.hippoecm.repository.query.lucene.ServicingSearchIndex;
import org.hippoecm.repository.security.principals.FacetAuthPrincipal;

public class FacetedNavigationEngineFourthImpl extends ServicingSearchIndex
  implements FacetedNavigationEngine<FacetedNavigationEngineFourthImpl.QueryImpl, FacetedNavigationEngineFourthImpl.ContextImpl>
{
  class QueryImpl extends FacetedNavigationEngine.Query {
    String xpath;
    public QueryImpl(String xpath) {
      this.xpath = xpath;
    }
    public String toString() {
      return xpath;
    }
  }

  class ResultImpl extends FacetedNavigationEngine.Result {
      int length;
      Iterator<NodeId> iter = null;
      ResultImpl(int length, Set<NodeId> result) {
          this.length = length;
          if(result!= null) {
              this.iter = result.iterator();
          }
      }
      public int length() {
          return length;
      }
      public Iterator<NodeId> iterator() {
          return iter;
      }
      public String toString() {
          return getClass().getName()+"[length="+length+"]";
      }
  }

  class ContextImpl extends FacetedNavigationEngine.Context {
    Session session;
    String principal;
    Subject subject;
    ContextImpl(Session session, String principal, Subject subject) {
      this.session = session;
      this.principal = principal;
      this.subject = subject;
    }
  }

 private Map<IndexReader, Map<String,Map<Integer, String[]>>> tfvCache ;

  public FacetedNavigationEngineFourthImpl() {
      this.tfvCache = new WeakHashMap<IndexReader, Map<String,Map<Integer, String[]>>>();
  }

  public ContextImpl prepare(String principal, Subject subject, List<QueryImpl> initialQueries, Session session) {
    return new ContextImpl(session, principal, subject);
  }
  public void unprepare(ContextImpl authorization) {
    // deliberate ignore
  }
  public void reload(Map<Name,String[]> facetValues) {
    // deliberate ignore
  }
  public boolean requiresReload() {
    return false;
  }
  public boolean requiresNotify() {
    return false;
  }
  public void notify(String docId, Map<Name,String[]> oldFacets, Map<Name,String[]> newFacets) {
    // deliberate ignore
  }
  public void purge() {
    // deliberate ignore
  }

  public Result view(String queryName, QueryImpl initialQuery, ContextImpl contextImpl,
                   Map<String,String> facetsQueryMap, QueryImpl openQuery,
                   Map<String,Map<String,Count>> resultset,
                   Map<Map<String,String>,Map<String,Map<String,Count>>> futureFacetsQueries,
                   HitsRequested hitsRequested) throws UnsupportedOperationException
  {
    try {
      Session session = contextImpl.session;
      RepositoryImpl repository = (RepositoryImpl) RepositoryDecorator.unwrap(session.getRepository());
      SearchManager searchManager = repository.getSearchManager(session.getWorkspace().getName()) ;
      NamespaceMappings nsMappings = getNamespaceMappings();

      /*
       * facetsQuery: get the query for the facets that are asked for
       */
      FacetsQuery facetsQuery = new FacetsQuery(facetsQueryMap, nsMappings, (ServicingIndexingConfiguration)getIndexingConfig());

      /*
       * authorizationQuery: get the query for the facets the person is allowed to see (which
       * is again a facetsQuery)
       */

      AuthorizationQuery authorizationQuery = null;

      FacetResultCollector collector = null;
      CachingFacetResultCollector cachingCollector = null;
      IndexReader indexReader = null;
      IndexSearcher searcher = null;
      try {
          indexReader = getIndex().getIndexReader();
          searcher = new IndexSearcher(indexReader);


          MultiIndexReader multiIndexReader = (MultiIndexReader)indexReader;

          // Optimize search in multithreaded searches
          Searchable[] indexSearchers = new IndexSearcher[multiIndexReader.getIndexReaders().length];
          for(int i = 0; i < multiIndexReader.getIndexReaders().length; i++){
              indexSearchers[i] = new IndexSearcher(multiIndexReader.getIndexReaders()[i]);
          }
          ParallelMultiSearcher parallelMultiSearcher = new ParallelMultiSearcher(indexSearchers);

          // In principle, below, there is always one facet
          for(String facet : resultset.keySet()) {
              /*
               * facetPropExists: the document must have the property as facet
               */
              FacetPropExistsQuery facetPropExists = new FacetPropExistsQuery(facet, nsMappings, (ServicingIndexingConfiguration)getIndexingConfig());

              BooleanQuery searchQuery = new BooleanQuery();
              searchQuery.add(facetPropExists.getQuery(), Occur.MUST);

              if(facetsQuery.getQuery().clauses().size() > 0){
                  searchQuery.add(facetsQuery.getQuery(), Occur.MUST);
              }
              // TODO perhaps create cached user specific filter for authorisation to gain speed
              if(authorizationQuery.getQuery().clauses().size() > 0){
                  searchQuery.add(authorizationQuery.getQuery(), Occur.MUST);
              }

              long start = System.currentTimeMillis();
              cachingCollector = new CachingFacetResultCollector(indexReader, tfvCache, facet, resultset, hitsRequested, nsMappings);

              // cache in the parallelMultiSearcher for each searcher/reader seperately
              parallelMultiSearcher.search(searchQuery, cachingCollector);

          }

      } catch (IOException e) {
          e.printStackTrace();
      } finally {
          if(searcher != null){
              try {
                 searcher.close();
             } catch (IOException e) {
                 e.printStackTrace();
             }
         }
         if(indexReader != null) {
            try {
                indexReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
         }
         // TODO close the parallelMultiSearcher and sub searchers if this is not done through the multiindex already
      }

      return this . new ResultImpl(collector.getNumhits(), collector.getHits());
    } catch(javax.jcr.query.InvalidQueryException ex) {
      System.err.println(ex.getClass().getName()+": "+ex.getMessage());
      throw new UnsupportedOperationException(); // FIXME
    } catch(javax.jcr.ValueFormatException ex) {
      System.err.println(ex.getClass().getName()+": "+ex.getMessage());
      throw new UnsupportedOperationException(); // FIXME
    } catch(javax.jcr.RepositoryException ex) {
      System.err.println(ex.getClass().getName()+": "+ex.getMessage());
      throw new UnsupportedOperationException(); // FIXME
    }
  }

  public Result view(String queryName, QueryImpl initialQuery, ContextImpl authorization,
                     Map<String,String> facetsQuery, QueryImpl openQuery,HitsRequested hitsRequested)
  {
      Session session = authorization.session;
      LinkedList list = new LinkedList<String>();
      int size = 0;

      return this . new  ResultImpl(0,null) ;

  }

  public QueryImpl parse(String query)
  {
    return this . new QueryImpl(query);
  }
}
