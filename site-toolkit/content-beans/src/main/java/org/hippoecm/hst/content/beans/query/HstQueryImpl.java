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
package org.hippoecm.hst.content.beans.query;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.exceptions.ScopeException;
import org.hippoecm.hst.content.beans.query.filter.BaseFilter;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.query.filter.FilterImpl;
import org.hippoecm.hst.content.beans.query.filter.HstCtxWhereFilter;
import org.hippoecm.hst.content.beans.query.filter.IsNodeTypeFilter;
import org.hippoecm.hst.content.beans.query.filter.NodeTypeFilter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.search.HstContextualizeException;
import org.hippoecm.hst.core.search.HstCtxWhereClauseComputer;
import org.hippoecm.repository.api.HippoQuery;
import org.slf4j.LoggerFactory;

public class HstQueryImpl implements HstQuery {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstQueryImpl.class);

    private ObjectConverter objectConverter;
    private HstCtxWhereClauseComputer hstCtxWhereClauseComputer;
    /*
     * By default, if you do not use setLimit(int limit), we use a limit of 1000. This is for performance reasons (internal repo)
     */
    private final static int DEFAULT_LIMIT = 1000;
    private int limit = DEFAULT_LIMIT;
    private int offset = -1;
    private BaseFilter filter;
    private List<Node> scopes = new ArrayList<Node>();
    private boolean skipInvalidScopes = false;
    private List<String> orderByList = new ArrayList<String>();
    private NodeTypeFilter nodeTypeFilter;
    private IsNodeTypeFilter isNodeTypeFilter;

    /**
     * 
     * @param hstCtxWhereClauseComputer
     * @param objectConverter
     * @param scope
     * @param nodeTypeFilter
     */
    public HstQueryImpl(HstCtxWhereClauseComputer hstCtxWhereClauseComputer, ObjectConverter objectConverter, Node scope, NodeTypeFilter nodeTypeFilter) {
        this.hstCtxWhereClauseComputer = hstCtxWhereClauseComputer;
        this.objectConverter = objectConverter;
        if(scope != null) {
            scopes.add(scope);
        }
        this.nodeTypeFilter = nodeTypeFilter; 
    }
    
    /**
     * 
     * @param hstCtxWhereClauseComputer
     * @param objectConverter
     * @param scope
     * @param isNodeTypeFilter
     */
    public HstQueryImpl(HstCtxWhereClauseComputer hstCtxWhereClauseComputer, ObjectConverter objectConverter, Node scope, IsNodeTypeFilter isNodeTypeFilter) {
        this.hstCtxWhereClauseComputer = hstCtxWhereClauseComputer;
        this.objectConverter = objectConverter;
        if(scope != null) {
            scopes.add(scope);
        }
        this.isNodeTypeFilter = isNodeTypeFilter; 
    }

   
    public void addOrderByAscending(String fieldNameAttribute) {
        orderByList.add("@"+fieldNameAttribute + " ascending");
    }

    public void addOrderByDescending(String fieldNameAttribute) {
        orderByList.add("@"+fieldNameAttribute + " descending");
    }
    

    
    public Filter createFilter() {
        return new FilterImpl();
    }

    public BaseFilter getFilter() {
        return this.filter;
    }

    public void setFilter(BaseFilter filter) {
        this.filter = filter;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void setOffset(int offset) {
        this.offset= offset;
    }

    public String getQuery() throws QueryException{
        
        if(this.scopes == null || this.scopes.size() == 0) {
            throw new QueryException("There must be a scope for a search");
        }
        BaseFilter ctxWhereFilter = new HstCtxWhereFilter(this.hstCtxWhereClauseComputer, this.scopes, this.skipInvalidScopes);
        
        StringBuilder query = new StringBuilder();
        
        if(ctxWhereFilter.getJcrExpression() == null || "".equals(ctxWhereFilter.getJcrExpression())) {
            // no ctxWhereFilter will be applied
        } else {
            query.append("(").append(ctxWhereFilter.getJcrExpression()).append(")");
        }
        
        String jcrExpression;
        if(this.getFilter() != null && (jcrExpression = this.getFilter().getJcrExpression()) != null) {
            if(query.length() == 0) {
                query.append(jcrExpression);
            } else {
                query.append(" and (").append(jcrExpression).append(")");
            }
         }
        
        if(this.nodeTypeFilter != null && (jcrExpression = this.nodeTypeFilter.getJcrExpression()) != null) {
            if(query.length() == 0) {
                query.append(jcrExpression);
            } else {
                query.append(" and (").append(jcrExpression).append(")");
            }
         }
        
        if(this.isNodeTypeFilter != null) {
            query.insert(0, isNodeTypeFilter.getJcrExpression() + "["); 
        } else {
            query.insert(0, "//*["); 
        }
        
        query.append("]");
        
        if(orderByList.size() > 0) {
            query.append(" order by ");
            boolean first = true;
            for(String orderBy : orderByList) {
                if(!first) {
                    query.append(",");
                }
                query.append(orderBy);
                first = false;
            }
        } else {
            // default order is by score descending
            query.append(" order by @jcr:score descending ");
        }
        log.debug("Query to execute is '{}'", query.toString());
        return query.toString();
    }
    
    public HstQueryResult execute() throws QueryException {
        String query = getQuery();
        try {
            QueryManager jcrQueryManager = getQueryManager();
            
            Query jcrQuery = jcrQueryManager.createQuery(query, "xpath");
            if(jcrQuery instanceof HippoQuery)  {
                if(offset > -1){
                    ((HippoQuery)jcrQuery).setOffset(offset);
                }
                if(limit > -1){
                    ((HippoQuery)jcrQuery).setLimit(limit);
                }
            } else {
                log.warn("Query not instanceof of HippoQuery: cannot set limit and offset");
            }
            long start = System.currentTimeMillis();
            QueryResult queryResult = jcrQuery.execute();
            log.debug("Executing query took --({})-- ms to complete for '{}'", (System.currentTimeMillis() - start), query);
            return new HstQueryResultImpl(this.objectConverter, queryResult, this.hstCtxWhereClauseComputer.getVirtualizer(scopes, this.skipInvalidScopes));
        } catch (InvalidQueryException e) {
            throw new QueryException(e.getMessage(), e);
        } catch (LoginException e) {
            log.warn("LoginException. Return null : {}", e);
        } catch (RepositoryException e) {
            throw new QueryException(e.getMessage(), e);
        } catch (HstContextualizeException e) {
            throw new QueryException(e.getMessage(), e);
        }
        return null;
    }

    private QueryManager getQueryManager() throws RepositoryException, QueryException {
        for(Node scope : scopes) {
            if(scope != null) {
                return scope.getSession().getWorkspace().getQueryManager();
            }
        }
        throw new QueryException("Unable to get QueryManager");
    }

    public void addScopes(List<HippoBean> scopes) {
        for(HippoBean scope : scopes) {
            if(scope != null) {
                this.scopes.add(scope.getNode());
            } else {
                this.scopes.add(null);
            }
        }
    }

    public void addScopes(Node[] scopes) {
       for(Node scope : scopes) {
           if(scope != null) {
               this.scopes.add(scope);
           } else {
               this.scopes.add(null);
           }
       }
    }

    public void setSkipInvalidScopes(boolean skipInvalidScopes) {
        this.skipInvalidScopes = skipInvalidScopes;
    }

}
