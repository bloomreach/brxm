/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import javax.jcr.Session;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.BaseFilter;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.query.filter.FilterImpl;
import org.hippoecm.hst.content.beans.query.filter.IsNodeTypeFilter;
import org.hippoecm.hst.content.beans.query.filter.NodeTypeFilter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.diagnosis.HDC;
import org.hippoecm.hst.diagnosis.Task;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.DateTools;
import org.slf4j.LoggerFactory;

public class HstQueryImpl implements HstQuery {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(HstQueryImpl.class);

    private ObjectConverter objectConverter;
    
    private int limit = DEFAULT_LIMIT;
    private int offset = -1;
    private BaseFilter filter;
    private List<Node> scopes = new ArrayList<Node>();
    private List<Node> excludeScopes = new ArrayList<Node>();
    private List<String> orderByList = new ArrayList<String>();
    private NodeTypeFilter nodeTypeFilter;
    private IsNodeTypeFilter isNodeTypeFilter;
    private Session session;
    private DateTools.Resolution defaultResolution;

    /**
     * 
     * @param session the jcr session. This session can be <code>null</code> as long as HSTTWO-1600 is not done
     * @param objectConverter
     * @param scope the scope to search below. Scope can be <code>null</code>
     * @param nodeTypeFilter
     * 
     */
    public HstQueryImpl(Session session, ObjectConverter objectConverter, Node scope, NodeTypeFilter nodeTypeFilter) {
        this.session = session;
        this.objectConverter = objectConverter;
        if(scope != null) {
            scopes.add(scope);
        }
        this.nodeTypeFilter = nodeTypeFilter; 
    }
    
    /**
     * @param session the jcr session. This session can be <code>null</code> as long as HSTTWO-1600 is not done
     * @param objectConverter
     * @param scope the scope to search below. Scope can be <code>null</code>
     * @param isNodeTypeFilter
     */
    public HstQueryImpl(Session session, ObjectConverter objectConverter, Node scope, IsNodeTypeFilter isNodeTypeFilter) {
        this.session = session;
        this.objectConverter = objectConverter;
        if(scope != null) {
            scopes.add(scope);
        }
        this.isNodeTypeFilter = isNodeTypeFilter; 
    }

    public void setDefaultResolution(final DateTools.Resolution defaultResolution) {
        this.defaultResolution = defaultResolution;
    }

    @Override
    public void addOrderByAscending(String fieldNameAttribute) {
        orderByList.add("@"+fieldNameAttribute + " ascending");
    }

    @Override
    public void addOrderByAscendingCaseInsensitive(final String fieldNameAttribute) {
        orderByList.add("fn:lower-case(@"+fieldNameAttribute + ") ascending");
    }

    @Override
    public void addOrderByDescending(String fieldNameAttribute) {
        orderByList.add("@"+fieldNameAttribute + " descending");
    }

    @Override
    public void addOrderByDescendingCaseInsensitive(final String fieldNameAttribute) {
        orderByList.add("fn:lower-case(@"+fieldNameAttribute + ") descending");
    }

    @Override
    public Filter createFilter() {
        // note: the session can be null
        return new FilterImpl(session, defaultResolution);
    }

    @Override
    public BaseFilter getFilter() {
        return this.filter;
    }

    @Override
    public void setFilter(BaseFilter filter) {
        this.filter = filter;
    }

    @Override
    public void setLimit(int limit) {
        this.limit = limit;
    }

    @Override
    public int getLimit() {
        return this.limit;
    }

    @Override
    public void setOffset(int offset) {
        this.offset= offset;
    }

    @Override
    public int getOffset() {
        return this.offset;
    }

    @Override
    public String getQueryAsString(boolean skipDefaultOrderBy) throws QueryException{
        if(this.scopes == null || this.scopes.size() == 0) {
            throw new QueryException("There must be a scope for a search");
        }
        
        // get the list of scope id's to search below:
        StringBuilder scopesWhereClause = new StringBuilder();
        for (Node scope : scopes) {
            try {
                String identifier = scope.getIdentifier();
                if (scopesWhereClause.length() > 0) {
                    scopesWhereClause.append(" or ");
                }
                scopesWhereClause.append("@").append(HippoNodeType.HIPPO_PATHS).append("='").append(identifier).append("'");
            } catch (RepositoryException e) {
               log.warn("RepositoryException while trying to include scope: ", e);
            }
        }

        StringBuilder query = new StringBuilder(256);
        
        if(scopesWhereClause.length() > 0) {
            query.append("(").append(scopesWhereClause.toString()).append(")");
        } else {
            throw new QueryException("No valid scope for search");
        }

        // IF we have a request context, check whether we are preview OR live and include
        // this in the filter
        if ( RequestContextProvider.get() != null ) {
            if (RequestContextProvider.get().isPreview() ) {
                query.append(" and (@").append(HippoNodeType.HIPPO_AVAILABILITY).append("='preview'").append(")");
            } else {
                query.append(" and (@").append(HippoNodeType.HIPPO_AVAILABILITY).append("='live'").append(")");
            }
        }

        // exclude frozen nodes if the version history would be indexed
        query.append(" and not(@jcr:primaryType='nt:frozenNode')");

        if(this.excludeScopes != null && !this.excludeScopes.isEmpty()) {
            StringBuilder excludeExpr = new StringBuilder(80);
            for(Node excludeScope : this.excludeScopes) {
                String scopeUUID = null;
                try {
                    if(excludeScope.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                        scopeUUID = excludeScope.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                    } else {
                        scopeUUID = excludeScope.getIdentifier();
                    }
                } catch (RepositoryException e) {
                    log.error("RepositoryException while excluding scope: ", e);
                }
                
                if(scopeUUID != null) {
                    if(excludeExpr.length() > 0) {
                        excludeExpr.append(" and ");
                    }
                    // do not use a!=b but not(a=b) as this is different for multivalued properties in jcr!
                    excludeExpr.append("not(@").append(HippoNodeType.HIPPO_PATHS).append("='").append(scopeUUID).append("')");
                }
            }
            if(excludeExpr.length() > 0) {
                if(query.length() == 0) {
                    query.append("(").append(excludeExpr.toString()).append(")");
                } else {
                    query.append(" and (").append(excludeExpr.toString()).append(")");
                }
            }
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
        } else if(!skipDefaultOrderBy){
            // default order is by score descending
            query.append(" order by @jcr:score descending ");
        }
        log.debug("Query to execute is '{}'", query.toString());
        return query.toString();
    }

    
    public String getQuery() throws QueryException{
        return getQueryAsString(false);
    }
    
    @Override
    public String toString() {
        String query = null; 
        try { 
            query =  this.getQuery();
        } catch (QueryException e) {
            // already logged, only log message here
            log.warn("cannot get query : ", e.getMessage());
        }
        return super.toString() + " = " + query;
    }

    @Override
    public HstQueryResult execute() throws QueryException {
        Task queryTask = null;

        try {
            String query = getQuery();

            if (HDC.isStarted()) {
                queryTask = HDC.getCurrentTask().startSubtask("HstQuery");
                queryTask.setAttribute("query", query);
            }

            QueryManager jcrQueryManager = getQueryManager();

            Query jcrQuery = jcrQueryManager.createQuery(query, "xpath");
            if(offset > -1){
                jcrQuery.setOffset(offset);
            }
            if(limit > -1){
                jcrQuery.setLimit(limit);
            }

            QueryResult queryResult = jcrQuery.execute();
            return new HstQueryResultImpl(this.objectConverter, queryResult);
        } catch (InvalidQueryException e) {
            throw new QueryException(e.getMessage(), e);
        } catch (LoginException e) {
            log.warn("LoginException. Return null : {}", e);
        } catch (RepositoryException e) {
            throw new QueryException(e.getMessage(), e);
        } finally {
            if (queryTask != null) {
                queryTask.stop();
            }
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

    @Override
    public void addScopes(List<HippoBean> scopes) {
        for(HippoBean scope : scopes) {
            if(scope != null) {
                Node newScope = scope.getNode();
                removeNodeFromList(newScope, this.excludeScopes);
                this.scopes.add(newScope);
            } else {
                this.scopes.add(null);
            }
        }
    }

    @Override
    public void addScopes(Node[] scopes) {
       for(Node scope : scopes) {
           if(scope != null) {
               removeNodeFromList(scope, this.excludeScopes);
               this.scopes.add(scope);
           } else {
               this.scopes.add(null);
           }
       }
    }

    @Override
    public void setSkipInvalidScopes(boolean skipInvalidScopes) {
       log.info("skipInvalidScopes is deprecated since 2.25.02. No need to set it any more");
    }

    @Override
    public void excludeScopes(List<HippoBean> scopes) {
        for(HippoBean scope : scopes) {
            if(scope != null) {
                Node scopeNode = scope.getNode();
                removeNodeFromList(scopeNode, this.scopes);
                this.excludeScopes.add(scopeNode);
            }
        }
    }

    @Override
    public void excludeScopes(Node[] scopes) {
        for(Node scope : scopes) {
            if(scope != null) {
                removeNodeFromList(scope, this.scopes);
                this.excludeScopes.add(scope);
            } 
        }
    }
    
    private void removeNodeFromList(Node remove, List<Node> fromList) {
        List<Node> removeItems = new ArrayList<Node>();
        for(Node node : fromList) {
            try {
                if(node.isSame(remove)) {
                    removeItems.add(node);
                }
            } catch (RepositoryException e) {
               log.error("Ignore remove from list. Repository exception: ", e.getMessage());
            }
        }
        fromList.removeAll(removeItems);
    }

}
