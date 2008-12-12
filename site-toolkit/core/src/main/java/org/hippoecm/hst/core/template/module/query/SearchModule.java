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
package org.hippoecm.hst.core.template.module.query;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.context.ContextBase;
import org.hippoecm.hst.core.exception.TemplateException;
import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.hippoecm.hst.core.mapping.URLMapping;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.repository.api.HippoNode;

public class SearchModule extends ModuleBase implements Search {

    private String language = DEFAULT_LANGUAGE;
    private int didYouMeanThreshold = DEFAULT_THRESHOLD;
    private int limit = DEFAULT_LIMIT;
    private int offset = DEFAULT_OFFSET;
    // TODO depth is here for future seasons to search within a depth
    private int depth = DEFAULT_DEPTH;
    private boolean didYouMeanNeeded = DEFAULT_DIDYOUMEANNEEDED;
    private boolean excerptNeeded = DEFAULT_EXCERPTNEEDED;
    private boolean similarNeeded = DEFAULT_SIMILARNEEDED;

    private boolean keepParameters = DEFAULT_KEEPPARAMETERS;
    private String pageName = DEFAULT_PAGENAME;
    private String queryText;
    private String statement;
    private String target;
    private String nodetype;
    private String order;
    private String orderby;
    private String where;
    private int currentPageNumber = 1;
    private boolean validStatement = true;

   
    /**
     * set search configuration parameters. This method is called directly in the render() method 
     * 
     * @param pageContext
     */
    public void prepareSearch(PageContext pageContext) {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        if (moduleParameters == null) {
            validStatement =false;
            return;
        }  

        // initPath 
        if (moduleParameters.containsKey(TARGET)) {
            String target = moduleParameters.get(TARGET);
            if (!"".equals(target)) {
                setTarget(target);
            }
        }
        if (target == null) {
           log.debug("target is null. Using the content context path as the path to search from.");
        }

        // nodetype 
        if (moduleParameters.containsKey(NODETYPE)) {
            String nodetype = moduleParameters.get(NODETYPE);
            if (!"".equals(nodetype)) {
                setNodeType(nodetype);
            }
        }

        // where 
        if (moduleParameters.containsKey(WHERE)) {
            String whereParam = moduleParameters.get(WHERE);
            if (!"".equals(whereParam)) {
                setWhere(whereParam);
            }
        }

        // query text
        if (moduleParameters.containsKey(QUERY)) {
            String queryParam = moduleParameters.get(QUERY);
            if (!"".equals(queryParam)) {
                queryParam.replaceAll("'", "''");
                setQueryText(queryParam);
            }
        }

        // orderby 
        if (moduleParameters.containsKey(ORDERBY)) {
            String orderbyParam = moduleParameters.get(ORDERBY);
            if (!"".equals(orderbyParam)) {
                setOrderBy(orderbyParam);
            }
        }

        // order 
        if (orderby != null) {
            if (moduleParameters.containsKey(ORDER)) {
                String orderParam = moduleParameters.get(ORDER);
                if (orderParam.equals("descending") || orderParam.equals("desc")) {
                    setOrder("descending");
                }
            }
        }


     // limit 
        if (moduleParameters.containsKey(LIMIT)) {
            String limitParam = moduleParameters.get(LIMIT);
            if (!"".equals(limitParam) && isNumber(LIMIT, limitParam)) {
                setLimit(Integer.parseInt(limitParam));
            }
        }
        
     // offset 
        if (moduleParameters.containsKey(OFFSET)) {
            String offsetParam = moduleParameters.get(OFFSET);
            if (!"".equals(offsetParam) && isNumber(OFFSET, offsetParam)) {
                setOffset(Integer.parseInt(offsetParam));
            }
        }

        // pagename 
        if (moduleParameters.containsKey(PAGENAME)) {
            String l_pageName = moduleParameters.get(PAGENAME);
            setPageName(l_pageName);
        }
        
        // current page 
        if (request.getParameter(getPageName()) != null && !"".equals(request.getParameter(getPageName()))) {
            String currentPageParam = request.getParameter(getPageName());
            if (!"".equals(currentPageParam) && isNumber(getPageName(), currentPageParam)) {
                setCurrentPageNumber(Integer.parseInt(currentPageParam));
            }
        }

        // depth 
        if (moduleParameters.containsKey(DEPTH)) {
            String depthParam = moduleParameters.get(DEPTH);
            if (!"".equals(depthParam) && isNumber(DEPTH, depthParam)) {
                setDepth(Integer.parseInt(depthParam));
            }
        }

        // keepparameters 
        if (moduleParameters.containsKey(KEEPPARAMETERS)) {
            String keeparams = moduleParameters.get(KEEPPARAMETERS);
            setKeepParameters(Boolean.valueOf(keeparams));
        }
        
        // didyoumean 
        if (moduleParameters.containsKey(DIDYOUMEAN)) {
            String didyoumeanParam = moduleParameters.get(DIDYOUMEAN);
            setDidYouMeanNeeded(Boolean.valueOf(didyoumeanParam));
        }
        // didyoumean treshhold
        if (moduleParameters.containsKey(DIDYOUMEAN_MINIMUM)) {
            String didyoumean_minimum = moduleParameters.get(DIDYOUMEAN_MINIMUM);
            if (!"".equals(didyoumean_minimum) && isNumber(DIDYOUMEAN_MINIMUM, didyoumean_minimum)) {
                setDidYouMeanThreshold(Integer.parseInt(didyoumean_minimum));
            }
        }

        // similar
        if (moduleParameters.containsKey(SIMILAR)) {
            String show_similar = moduleParameters.get(SIMILAR);
            setSimilarNeeded(new Boolean(show_similar));
        }

        // excerpt
        if (moduleParameters.containsKey(EXCERPT)) {
            String show_excerpt = moduleParameters.get(EXCERPT);
            setExcerptNeeded(new Boolean(show_excerpt));
        }
        // highlight = other term for exceprt
        if (moduleParameters.containsKey(HIGHLIGHT)) {
            String show_excerpt = moduleParameters.get(HIGHLIGHT);
            setExcerptNeeded(new Boolean(show_excerpt));
        }
    }

    /**
     * prepare the xpath/sql statement. Only override this method if the default Search Module cannot create the correct 
     * statement, though you should not do this normally.
     * 
     * @param pageContext
     */
    public void prepareStatement(PageContext pageContext) {
        long start = System.currentTimeMillis();
        if (!validStatement) {
            return;
        }
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        HstRequestContext hstRequestContext = (HstRequestContext)request.getAttribute(HstRequestContext.class.getName());
        HippoNode contentContextNode = hstRequestContext.getContentContextBase().getContextRootNode();
        
        /*
         * if the contextNode is a facetselect node --> take the uuid
         * if the contextNode is a virtual node --> take the canonical node. If no canonical node, log error, and refuse query. 
         * If the canonical node is not referenceable, create the query by using its path). 
         * 
         * Searching based on uuid can be only done for nodes extending from hippo:document type.
         */
        
        String statementPath = null;
        String statementWhere = null;
        String contextWhereClauses = "";
        String statementOrderBy = "";
        
        ContextWhereClause ctxWhereClause = new ContextWhereClause(contentContextNode, target);
        contextWhereClauses = ctxWhereClause.getWhereClause();
        
        if (this.where != null) {
            statementWhere = "(" + this.where + ")";
        }

       
        if (this.nodetype != null) {
            statementPath =  "//element(*," + nodetype + ")";
        } else {
            statementPath =  "//*";
        }

        if (getQueryText() != null && !"".equals(getQueryText())) {
            if (statementWhere == null ) {
                statementWhere = " jcr:contains(., '" + getQueryText() + "')";
            } else {
                statementWhere += " and jcr:contains(., '" + getQueryText() + "')";
            }
        }

        if (this.orderby != null) {
            statementOrderBy = " order by @" + orderby;
            if (order != null) {
                statementOrderBy += " " + order;
            }
        }

        String where = "";
        if(contextWhereClauses==null) {
           log.warn("not a valid search because the target is not valid");
           validStatement = false;
           return;
        } else{
            where +=contextWhereClauses;
        } 
        if(statementWhere!=null) {
            if(where.length() > 0) {
                where += " and ";
            }
            where += statementWhere;
        }
        
        if(where.length() > 0) {
            where = "[" + where + "]";
        }
        
        statement = statementPath + where + statementOrderBy;
        log.debug("xpath statement = " + statement);
        setStatement(statement); 
        log.debug("Preparing search statement took " + (System.currentTimeMillis() - start) + " ms.");
    }

    /**
     * this render is a final, since it is delegate code, and is not meant for extending
     */
    @Override
    public void render(PageContext pageContext, URLMapping urlMapping, ContextBase ctxBase) throws TemplateException {

        prepareSearch(pageContext);
        prepareStatement(pageContext);

        List<SearchHit> hits = new ArrayList<SearchHit>();
        SearchResult searchResult = new SearchResult(hits);
        pageContext.setAttribute(getVar(), searchResult);

        if (!validStatement) {
            return;
        }

        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        
        String statement = getStatement();
        String querytext = getQueryText();
        String language = getLanguage();

        if (statement == null || statement.equals("")) {
            log.warn("empty query statement. Return empty search result");
            return;
        }

        try {
            Session jcrSession = ctxBase.getSession();
            QueryManager queryMngr = jcrSession.getWorkspace().getQueryManager();

            Query q = queryMngr.createQuery(statement, language);
            log.debug("Search query = " + q.getStatement());
            long start = System.currentTimeMillis();
            QueryResult queryResult = q.execute();
            //  important debug info in case of performance bottlenecks:
            log.debug("query took " + (System.currentTimeMillis() - start) + " ms for q = " + q.getStatement());

            start = System.currentTimeMillis();
            RowIterator rows = queryResult.getRows();

            searchResult.setSize((int)rows.getSize());
            searchResult.setCurrentPageNumber(currentPageNumber);
            int pagingOffset = offset + (currentPageNumber-1)*limit;
            searchResult.setPagingOffset(pagingOffset);
            searchResult.setOffset(offset);
            searchResult.setPagesize(limit);
            searchResult.setQuery(querytext);
            searchResult.setNodeType(nodetype);
            searchResult.setPageName(getPageName());
            searchResult.computePagesAndLinks(request.getParameterMap(), isKeepParameters());
            
            if(log.isDebugEnabled()) {
                logSearch(searchResult);
            }
            
            /*
             * Fix for jackrabbit bug: when skip == rows.size then hasNext returns true, but nextRow() returns NoSuchElementException
             */  
            if(pagingOffset == rows.getSize()) {
                log.debug("offset is larger then or equal to the number of results");
                return;
            }
            
            if(rows.getSize()!=0) {
                try {
                    rows.skip(pagingOffset);
                } catch (NoSuchElementException e) {
                    log.debug("offset is larger then the number of results");
                    return;
                }
            }
            
            int counter = 0;
            while (rows.hasNext()) {
                counter++;
                if (counter > limit) {
                    break;
                }
                Row row = rows.nextRow();
                try {
                    Node node = (Node) jcrSession.getItem(row.getValue("jcr:path").getString());
                    double score = row.getValue("jcr:score").getDouble();
                    SearchHit searchHit = new SearchHit(node, urlMapping, (counter + pagingOffset), score);
                    if (isExcerptNeeded()) {
                        searchHit.setExcerpt(row.getValue("rep:excerpt(.)").getString());
                    }
                    hits.add(searchHit);
                } catch (ItemNotFoundException e) {
                    log.warn("Unable to get search hit. Item might be removed. Continue with next " + e.getMessage());
                }

            }
            if (isDidYouMeanNeeded() && querytext != null && !querytext.equals("")
                    && (searchResult.getSize() < getDidYouMeanThreshold())) {
                try {
                    long didyoumeanstart = System.currentTimeMillis();
                    Value v = queryMngr.createQuery(
                            "/jcr:root[rep:spellcheck('" + querytext + "')]/(rep:spellcheck())", Query.XPATH).execute()
                            .getRows().nextRow().getValue("rep:spellcheck()");
                    log.debug("Getting 'didyoumean' took " + (System.currentTimeMillis() - didyoumeanstart) + " ms to complete.");
                    if (v != null) {
                        searchResult.setDidyoumean(v.getString());
                    }
                } catch (RepositoryException e) {
                    log.warn("error trying to find 'didyoumean' term");
                }

            }
            // important debug info in case of performance bottlenecks:
            log.debug("fetching " + hits.size() + " searchresults took " + (System.currentTimeMillis() - start)
                    + " ms to complete.");
            
            // TODO add similarity search
            
            
            
        } catch (InvalidQueryException e) {
            log.warn("InvalidQueryException " + e.getMessage() + " for : " + statement);
        } catch (RepositoryException e) {
            log.warn("RepositoryException while executing search " + e.getMessage());
        }

    }

    private void logSearch(SearchResult searchResult) {
        log.debug("----- SEARCHRESULT ----");
        log.debug("hits: " +searchResult.getSize());
        log.debug("limit: " +searchResult.getLimit());
        log.debug("offset: " +searchResult.getOffset());
        log.debug("---END SEARCHRESULT ---");
    }

    private boolean isNumber(String name, String stringInt) {
        try {
            Integer.parseInt(stringInt);
            return true;
        } catch (NumberFormatException e) {
            log.warn("parameter '" + name + "=" + stringInt + "' cannot be parsed as integer and will be skipped.");
        }
        return false;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public void setStatement(String statement) {
        this.statement = statement;
    }

    public int getDidYouMeanThreshold() {
        return didYouMeanThreshold;
    }

    public String getLanguage() {
        return language;
    }

    public int getLimit() {
        return limit;
    }
    
    public int getOffset() {
        return offset;
    }

    public String getQueryText() {
        return queryText;
    }

    public String getStatement() {
        return statement;
    }

    public boolean isDidYouMeanNeeded() {
        return didYouMeanNeeded;
    }

    public boolean isExcerptNeeded() {
        return excerptNeeded;
    }

    public boolean isSimilarNeeded() {
        return similarNeeded;
    }

    public boolean isKeepParameters() {
        return keepParameters;
    }
    
    public String getPageName() {
        return pageName;
    }

    public void setDidYouMeanNeeded(boolean didYouMeanNeeded) {
        this.didYouMeanNeeded = didYouMeanNeeded;
    }

    public void setDidYouMeanThreshold(int didYouMeanThreshold) {
        this.didYouMeanThreshold = didYouMeanThreshold;
    }

    public void setExcerptNeeded(boolean excerptNeeded) {
        this.excerptNeeded = excerptNeeded;
    }

    public void setKeepParameters(boolean keepParameters) {
        this.keepParameters = keepParameters;
    }
    
    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public void setSimilarNeeded(boolean similarNeeded) {
        this.similarNeeded = similarNeeded;
    }

    public void setTarget(String target) {
        this.target = target;

    }

    public void setNodeType(String nodetype) {
        this.nodetype = nodetype;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public void setOrderBy(String orderby) {
        this.orderby = orderby;
    }

    public void setWhere(String where) {
        this.where = where;
    }

    public String getWhere() {
        return this.where;
    }

    public int getCurrentPageNumber() {
        return currentPageNumber;
    }

    public void setCurrentPageNumber(int currentPageNumber) {
       this.currentPageNumber = currentPageNumber; 
    }
    
}
