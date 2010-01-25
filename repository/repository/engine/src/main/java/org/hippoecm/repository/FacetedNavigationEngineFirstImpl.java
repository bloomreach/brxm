/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.repository;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.security.auth.Subject;

import org.apache.jackrabbit.core.NodeId;
import org.apache.jackrabbit.spi.Name;
import org.hippoecm.repository.jackrabbit.KeyValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetedNavigationEngineFirstImpl
  implements FacetedNavigationEngine<FacetedNavigationEngineFirstImpl.QueryImpl,
                                     FacetedNavigationEngineFirstImpl.ContextImpl>
{
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";
    private static final Logger log = LoggerFactory.getLogger(FacetedNavigationEngineFirstImpl.class);

    
    class QueryImpl extends FacetedNavigationEngine.Query {
        String xpath;
        public QueryImpl(String xpath) {
            this.xpath = xpath;
        }
        @Override
        public String toString() {
            return xpath;
        }
    }

    class ResultImpl extends FacetedNavigationEngine.Result {
        int length;
        Iterator<NodeId> iter;
        ResultImpl(int length, Iterator<NodeId> iter) {
            this.length = length;
            this.iter = iter;
        }
        @Override
        public int length() {
            return length;
        }
        @Override
        public Iterator<NodeId> iterator() {
            return iter;
        }
        @Override
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
        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            if(subject != null) {
                sb.append("+authorization-query+");
            } else {
                sb.append("(null)");
            }
            sb.insert(0,"query=");
            sb.insert(0,",");
            sb.insert(0,principal);
            sb.insert(0,"[principal=");
            sb.insert(0, getClass().getName());
            sb.append("]");
            return new String(sb);
        }
    }

    public FacetedNavigationEngineFirstImpl() {
    }

    public ContextImpl prepare(String userId, Subject subject, List<QueryImpl> initialQueries,
                               Session session) {
        return new ContextImpl(session, userId, subject);
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

    private static StringBuffer getSearchQuery(String initialQuery, List<KeyValue<String,String>> facetsQuery, String facet) {
        StringBuffer searchquery = new StringBuffer();
        for(KeyValue<String,String> keyValue : facetsQuery ) {
            if(searchquery.length() > 0)
                searchquery.append(",");
            searchquery.append("@");
            searchquery.append(keyValue.getKey());
            searchquery.append("='");
            searchquery.append(keyValue.getValue());
            searchquery.append("'");
        }
        if(facet != null) {
            if (searchquery.length() > 0)
                searchquery.append(",");
            searchquery.append("@");
            searchquery.append(facet);
        }
        if(searchquery.length() > 0) {
            searchquery.insert(0,"[");
            searchquery.append("]");
        }
        searchquery.insert(0, initialQuery + "//node()");
        if(facet != null) {
            searchquery.append("/@");
            searchquery.append(facet);
        }
        return searchquery;
    }

    public Result view(String queryName, QueryImpl initialQuery, ContextImpl authorization,
            List<KeyValue<String,String>> facetsQuery, List<FacetRange> rangeQuery, QueryImpl openQuery,
            Map<String,Map<String,Count>> resultset,
            Map<String,String> inheritedFilter,
            HitsRequested hitsRequested) throws UnsupportedOperationException {
        try {
            if(rangeQuery != null) {
                log.error("Ranges are unsupported in this engine");
                return this . new ResultImpl(0, null);
            }
            Session session = authorization.session;
            int resultNodeCount = 0;
            for(String facet : resultset.keySet()) {
                String xpath = new String(getSearchQuery(initialQuery.xpath, facetsQuery, facet));
                javax.jcr.query.Query facetValuesQuery = session.getWorkspace().getQueryManager().createQuery(xpath,
                                                                                                   javax.jcr.query.Query.XPATH);
                QueryResult facetValuesResult = facetValuesQuery.execute();
                Map<String,Count> facetValuesMap = resultset.get(facet);
                RowIterator iter=facetValuesResult.getRows();
                while(iter.hasNext()) {
                    Row row = iter.nextRow();
                    Value[] values = row.getValues();
                    String facetValue = values[0].getString();
                    if(!facetValuesMap.containsKey(facetValue))
                        facetValuesMap.put(facetValue,new Count(1));
                    else
                        facetValuesMap.get(facetValue).count++;
                    resultNodeCount++;
                }
            }
            return this . new ResultImpl(resultNodeCount, null);
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
                       List<KeyValue<String,String>> facetsQuery, QueryImpl openQuery,
                       Map<String,Map<String,Count>> resultset,
                       Map<String,String> inheritedFilter,
                       HitsRequested hitsRequested) throws UnsupportedOperationException {
        return this.view(queryName, initialQuery, authorization, facetsQuery, null, openQuery, resultset, inheritedFilter, hitsRequested);
    }

    public Result view(String queryName, QueryImpl initialQuery, ContextImpl authorization,
                       List<KeyValue<String,String>> facetsQuery, QueryImpl openQuery, Map<String,String> inheritedFilter, HitsRequested hitsRequested) {
        try {
            Session session = authorization.session;
            LinkedList<NodeId> list = new LinkedList<NodeId>();
            int size = 0;
            String xpath = new String(getSearchQuery(initialQuery.xpath, facetsQuery, null));
            javax.jcr.query.Query facetValuesQuery = session.getWorkspace().getQueryManager().createQuery(xpath,
                                                                                                   javax.jcr.query.Query.XPATH);
            QueryResult facetValuesResult = facetValuesQuery.execute();
            NodeIterator iter = facetValuesResult.getNodes(); // FIXME: should do query on jcr:path and only retrieve those
            size += iter.getSize();
            while(iter.hasNext()) {
                Node node = iter.nextNode();
                list.add(NodeId.valueOf(node.getUUID()));
                //list.add(node.getPath());
            }
            return this . new ResultImpl(size, list.iterator());
        } catch(javax.jcr.query.InvalidQueryException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
            throw new UnsupportedOperationException(); // FIXME
        } catch(javax.jcr.ValueFormatException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
            throw new UnsupportedOperationException(); // FIXME
        } catch(javax.jcr.RepositoryException ex) {
            System.err.println(ex.getClass().getName()+": "+ex.getMessage());
            ex.printStackTrace(System.err);
            throw new UnsupportedOperationException(); // FIXME
        }
    }

    public QueryImpl parse(String query) {
        return this . new QueryImpl(query);
    }

}
