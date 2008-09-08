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
import javax.jcr.PathNotFoundException;
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

import org.hippoecm.hst.core.Timer;
import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.HstFilterBase;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoNodeType;

public class SearchModule extends ModuleBase implements Search {

    private String language = DEFAULT_LANGUAGE;
    private int didYouMeanThreshold = DEFAULT_THRESHOLD;
    private int limit = DEFAULT_LIMIT;
    private int depth = DEFAULT_DEPTH;
    private boolean didYouMeanNeeded = DEFAULT_DIDYOUMEANNEEDED;
    private boolean excerptNeeded = DEFAULT_EXCERPTNEEDED;
    private boolean similarNeeded = DEFAULT_SIMILARNEEDED;

    private String queryText;
    private String statement;
    private String target;
    private String nodetype;
    private String order;
    private String orderby;
    private String where;
    private int currentPageNumber = 1;
    private boolean validStatement = true;

    
    // TODO fixme 
    
    
    /**
     * set search configuration parameters. This method is called directly in the render() method 
     * 
     * @param pageContext
     */
    public void prepareSearch(PageContext pageContext) {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

        boolean params = false;
        if (moduleParameters != null) {
            params = true;
        }

        // initPath 
        if (request.getParameter(TARGET) != null && !"".equals(request.getParameter(TARGET))) {
            String target = request.getParameter(TARGET);
            if (!"".equals(target)) {
                setTarget(target);
            }
        } else if (params && moduleParameters.containsKey(TARGET)) {
            String target = moduleParameters.get(TARGET);
            if (!"".equals(target)) {
                setTarget(target);
            }
        }
        if (target == null) {
            log.error("Target is not allowed to be null: skipping search");
            validStatement = false;
            return;
        }

        // nodetype 
        if (request.getParameter(NODETYPE) != null && !"".equals(request.getParameter(NODETYPE))) {
            setNodeType(request.getParameter(NODETYPE));
        } else if (params && moduleParameters.containsKey(NODETYPE)) {
            String nodetype = moduleParameters.get(NODETYPE);
            if (!"".equals(nodetype)) {
                setNodeType(nodetype);
            }
        }

        // where 
        if (params && moduleParameters.containsKey(WHERE)) {
            String whereParam = moduleParameters.get(WHERE);
            if (!"".equals(whereParam)) {
                setWhere(whereParam);
            }
        }

        // query text
        if (request.getParameter(QUERY) != null && !"".equals(request.getParameter(QUERY))) {
            String queryParam = request.getParameter(QUERY);
            if (!"".equals(queryParam)) {
                queryParam.replaceAll("'", "''");
                setQueryText(queryParam);
            }
        } else if (params && moduleParameters.containsKey(QUERY)) {
            String queryParam = moduleParameters.get(QUERY);
            if (!"".equals(queryParam)) {
                queryParam.replaceAll("'", "''");
                setQueryText(queryParam);
            }
        }

        // orderby 
        if (request.getParameter(ORDERBY) != null && !"".equals(request.getParameter(ORDERBY))) {
            String orderbyParam = request.getParameter(ORDERBY);
            if (!"".equals(orderbyParam)) {
                setOrderBy(orderbyParam);
            }
        } else if (params && moduleParameters.containsKey(ORDERBY)) {
            String orderbyParam = moduleParameters.get(ORDERBY);
            if (!"".equals(orderbyParam)) {
                setOrderBy(orderbyParam);
            }
        }

        // order 
        if (orderby != null) {
            if (request.getParameter(ORDER) != null && !"".equals(request.getParameter(ORDER))) {
                String orderParam = request.getParameter(ORDER);
                if (orderParam.equals("descending") || orderParam.equals("desc")) {
                    setOrder(orderParam);
                }
            } else if (params && moduleParameters.containsKey(ORDER)) {
                String orderParam = moduleParameters.get(ORDER);
                if (orderParam.equals("descending") || orderParam.equals("desc")) {
                    setOrder("descending");
                }
            }
        }


        // limit 
        if (request.getParameter(LIMIT) != null && !"".equals(request.getParameter(LIMIT))) {
            String limitParam = request.getParameter(LIMIT);
            if (!"".equals(limitParam) && isNumber(LIMIT, limitParam)) {
                setLimit(Integer.parseInt(limitParam));
            }
        } else if (params && moduleParameters.containsKey(LIMIT)) {
            String limitParam = moduleParameters.get(LIMIT);
            if (!"".equals(limitParam) && isNumber(LIMIT, limitParam)) {
                setLimit(Integer.parseInt(limitParam));
            }
        }

        // current page 
        if (request.getParameter(PAGE) != null && !"".equals(request.getParameter(PAGE))) {
            String currentPageParam = request.getParameter(PAGE);
            if (!"".equals(currentPageParam) && isNumber(PAGE, currentPageParam)) {
                setCurrentPageNumber(Integer.parseInt(currentPageParam));
            }
        }

        // depth 
        if (params && moduleParameters.containsKey(DEPTH)) {
            String depthParam = moduleParameters.get(DEPTH);
            if (!"".equals(depthParam) && isNumber(DEPTH, depthParam)) {
                setDepth(Integer.parseInt(depthParam));
            }
        }

        // didyoumean 
        if (params && moduleParameters.containsKey(DIDYOUMEAN)) {
            String didyoumeanParam = moduleParameters.get(DIDYOUMEAN);
            setDidYouMeanNeeded(new Boolean(didyoumeanParam));
        }
        // didyoumean treshhold
        if (params && moduleParameters.containsKey(DIDYOUMEAN_MINIMUM)) {
            String didyoumean_minimum = moduleParameters.get(DIDYOUMEAN_MINIMUM);
            if (!"".equals(didyoumean_minimum) && isNumber(DIDYOUMEAN_MINIMUM, didyoumean_minimum)) {
                setDidYouMeanThreshold(Integer.parseInt(didyoumean_minimum));
            }
        }

        // similar
        if (params && moduleParameters.containsKey(SIMILAR)) {
            String show_similar = moduleParameters.get(SIMILAR);
            setSimilarNeeded(new Boolean(show_similar));
        }

        // excerpt
        if (params && (moduleParameters.containsKey(EXCERPT))) {
            String show_excerpt = moduleParameters.get(EXCERPT);
            setExcerptNeeded(new Boolean(show_excerpt));
        }
        // highlight = other term for exceprt
        if (params && (moduleParameters.containsKey(HIGHLIGHT))) {
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
        ContextBase ctxBase = (ContextBase) request.getAttribute(HstFilterBase.CONTENT_CONTEXT_REQUEST_ATTRIBUTE);
        Session jcrSession = ctxBase.getSession();
        Node contextNode = (HippoNode) ctxBase.getContextRootNode();
        HippoNode contentBaseNode = null;
        /*
         * if the contextNode is a facetselect node --> take the uuid
         * if the contextNode is a virtual node --> take the canonical node. If no canonical node, log error, and refuse query. 
         * If the canonical node is not referenceable, create the query by using its path). 
         * 
         * Searching based on uuid can be only done for nodes extending from hippo:document type.
         */
        String contextBasePath = null;
        String contentBaseUuid = null;
        String contentBasePath = null;
        String contextClauses = "";
        try {
            contextBasePath = contextNode.getPath();
            try {
                contentBaseNode = (HippoNode) contextNode.getNode(target);
            } catch (PathNotFoundException e) {
                log.warn("target '" + target + "' is not a subnode of '" + contextBasePath + "'.");
                validStatement = false;
                return;
            }
            if (contentBaseNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                contentBaseUuid = contentBaseNode.getProperty(HippoNodeType.HIPPO_DOCBASE).getString();
                try {
                    contentBasePath = jcrSession.getNodeByUUID(contentBaseUuid).getPath();
                } catch (ItemNotFoundException e) {
                    log.error("There is no node with a uuid that is present in the '" + HippoNodeType.HIPPO_DOCBASE
                            + "' property for " + contextBasePath);
                    validStatement = false;
                    return;
                }
            } else if (contentBaseNode.getCanonicalNode() != null
                    && contentBaseNode.getCanonicalNode().isNodeType("mix:referenceable")) {
                contentBaseUuid = contentBaseNode.getCanonicalNode().getUUID();
            } else {
                log
                        .warn("Target is not pointing to a referenceable node. Searching will be done by a path prefix, which can become slow");
                contentBasePath = contentBaseNode.getPath();
            }

            contextClauses = getContextWhereClauses(contextNode, contentBaseNode, jcrSession.getRootNode());

        } catch (RepositoryException e) {
            log.error("Cannot prepare statement: RepositoryException " + e.getMessage());
            validStatement = false;
            return;
        }

        String statementPath = "";
        String statementWhere = "[";
        String statementOrderBy = "";

        if (this.where != null) {
            statementWhere += "(" + this.where + ")";
        }

        if (contentBaseUuid == null) {
            statementPath = "/jcr:root" + contentBasePath;
        } else {
            if (!statementWhere.equals("[")) {
                statementWhere += " and ";
            }
            statementWhere += "@" + HippoNodeType.HIPPO_PATHS + "='" + contentBaseUuid + "'";
        }

        if (this.nodetype != null) {
            statementPath = statementPath + "//element(*," + nodetype + ")";
        } else {
            statementPath = statementPath + "//*";
        }

        if (getQueryText() != null && !"".equals(getQueryText())) {
            if (!statementWhere.equals("[")) {
                statementWhere += " and ";
            }
            statementWhere += " jcr:contains(., '" + getQueryText() + "')";
        }

        statementWhere += "]";

        if (this.orderby != null) {
            statementOrderBy = " order by @" + orderby;
            if (order != null) {
                statementOrderBy += " " + order;
            }
        }

        if (statementWhere.equals("[]")) {
            statementWhere = "";
        }
        statement = statementPath + contextClauses + statementWhere + statementOrderBy;
        log.debug("xpath statement = " + statement);
        setStatement(statement);
       
        Timer.log.debug("Preparing search statement took " + (System.currentTimeMillis() - start) + " ms.");
    }

    private String getContextWhereClauses(Node contextNode, Node contentBaseNode, Node rootNode)
            throws RepositoryException {
        //String contextClause = "[(hippostd:state='unpublished' or (hippostd:state='published' and hippostd:stateSummary!='changed'))]" ;
        long start = System.currentTimeMillis();
        String contextClauses = "";
        /*
         * find all facetselects in the contentBaseNode and up untill we are at the contextNode. For every facetselect found
         * add the filter as a query
         */

        while (!contentBaseNode.isSame(contextNode) && !contentBaseNode.isSame(rootNode)) {
            if (contentBaseNode.isNodeType(HippoNodeType.NT_FACETSELECT)) {
                // below all mandatory multivalued props
                Value[] modes = contentBaseNode.getProperty(HippoNodeType.HIPPO_MODES).getValues();
                Value[] facets = contentBaseNode.getProperty(HippoNodeType.HIPPO_FACETS).getValues();
                Value[] values = contentBaseNode.getProperty(HippoNodeType.HIPPO_VALUES).getValues();
                if (modes.length == facets.length && facets.length == values.length) {
                    for (int i = 0; i < modes.length; i++) {
                        String mode = modes[i].getString();
                        String facet = facets[i].getString();
                        String value = values[i].getString();
                        if (mode.equals("clear") || mode.equals("stick")) {
                            // do not know how to handle these in a query
                            log.debug("skipping mode 'clear' or 'stick' because ambigous how to handle them");
                            continue;
                        } else {
                            if ("hippostd:state".equals(facet) && "unpublished".equals(value)) {
                                // special case
                                contextClauses += "[(@hippostd:state='unpublished' or (@hippostd:state='published' and @hippostd:stateSummary!='changed'))]";
                            } else {
                                contextClauses += "[@" + facet + "='" + value + "']";
                            }
                        }
                    }
                } else {
                    log
                            .warn("invalid facetselect encoutered where there are an unequal number of 'modes', 'facets' and 'values'");
                }
            }
            contentBaseNode = contentBaseNode.getParent();
        }
        Timer.log.debug("creating search context where clauses took " + (System.currentTimeMillis() - start) + " ms.");
        return contextClauses;
    }

    /**
     * this render is a final, since it is delegate code, and is not meant for extending
     */
    @Override
    public final void render(PageContext pageContext) throws TemplateException {

        prepareSearch(pageContext);
        prepareStatement(pageContext);

        List<SearchHit> hits = new ArrayList<SearchHit>();
        SearchResult searchResult = new SearchResult(hits);
        pageContext.setAttribute(getVar(), searchResult);

        if (!validStatement) {
            return;
        }

        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ContextBase ctxBase = (ContextBase) request.getAttribute(HstFilterBase.CONTENT_CONTEXT_REQUEST_ATTRIBUTE);

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

            long start = System.currentTimeMillis();
            QueryResult queryResult = q.execute();
            //  important debug info in case of performance bottlenecks:
            Timer.log.debug("query took " + (System.currentTimeMillis() - start) + " ms for q = " + q.getStatement());

            start = System.currentTimeMillis();
            RowIterator rows = queryResult.getRows();

            searchResult.setSize((int)rows.getSize());
            searchResult.setCurrentPageNumber(currentPageNumber);
            int offset = (currentPageNumber-1)*limit;
            searchResult.setOffset(offset);
            searchResult.setPagesize(limit);
            searchResult.setQuery(querytext);
            
            searchResult.computePagesAndLinks(request.getParameterMap());
            
            try {
                rows.skip(offset);
            } catch (NoSuchElementException e) {
                log.debug("offset is larger then the number of results");
                return;
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
                    SearchHit searchHit = new SearchHit(node, (counter + offset), score);
                    if (isExcerptNeeded()) {
                        searchHit.setExcerpt(row.getValue("rep:excerpt(.)").getString());
                    }
                    hits.add(searchHit);
                } catch (ItemNotFoundException e) {
                    log.error("Unable to get search hit. Item might be removed. Continue with next " + e.getMessage());
                }

            }
            if (isDidYouMeanNeeded() && querytext != null && !querytext.equals("")
                    && (searchResult.getSize() < getDidYouMeanThreshold())) {
                try {
                    long didyoumeanstart = System.currentTimeMillis();
                    Value v = queryMngr.createQuery(
                            "/jcr:root[rep:spellcheck('" + querytext + "')]/(rep:spellcheck())", Query.XPATH).execute()
                            .getRows().nextRow().getValue("rep:spellcheck()");
                    Timer.log.debug("Getting 'didyoumean' took " + (System.currentTimeMillis() - didyoumeanstart) + " ms to complete.");
                    if (v != null) {
                        searchResult.setDidyoumean(v.getString());
                    }
                } catch (RepositoryException e) {
                    log.error("error trying to find 'didyoumean' term");
                }

            }
            // important debug info in case of performance bottlenecks:
            Timer.log.debug("fetching " + hits.size() + " searchresults took " + (System.currentTimeMillis() - start)
                    + " ms to complete.");
            
            // TODO add similarity search
            
            
            
        } catch (InvalidQueryException e) {
            log.error("InvalidQueryException " + e.getMessage() + " for : " + statement);
        } catch (RepositoryException e) {
            log.error("RepositoryException while executing search " + e.getMessage());
        }

        super.render(pageContext);
    }

    private boolean isNumber(String name, String stringInt) {
        try {
            Integer.parseInt(stringInt);
            return true;
        } catch (NumberFormatException e) {
            log.error("parameter '" + name + "=" + stringInt + "' cannot be parsed as integer and will be skipped.");
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

    public void setDidYouMeanNeeded(boolean didYouMeanNeeded) {
        this.didYouMeanNeeded = didYouMeanNeeded;
    }

    public void setDidYouMeanThreshold(int didYouMeanThreshold) {
        this.didYouMeanThreshold = didYouMeanThreshold;
    }

    public void setExcerptNeeded(boolean excerptNeeded) {
        this.excerptNeeded = excerptNeeded;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setLimit(int limit) {
        this.limit = limit;
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

    public int getCurrentPageNumber() {
        return currentPageNumber;
    }

    public void setCurrentPageNumber(int currentPageNumber) {
       this.currentPageNumber = currentPageNumber; 
    }
}
