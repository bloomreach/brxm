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

import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.HstFilterBase;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SearchModule extends ModuleBase implements Search {

    private static final Logger log = LoggerFactory.getLogger(Search.class);

    private String queryText;
    private String statement;

    private String language = DEFAULT_LANGUAGE;
    private int didYouMeanThreshold = DEFAULT_THRESHOLD;
    private int limit = DEFAULT_LIMIT;
    private int offset = DEFAULT_OFFSET;
    private boolean didYouMeanNeeded = DEFAULT_DIDYOUMEANNEEDED;
    private boolean excerptNeeded = DEFAULT_EXCERPTNEEDED;
    private boolean similarNeeded = DEFAULT_SIMILARNEEDED;

    
    /**
     * set search configuration parameters. This method is called directly in the render() method 
     * 
     * @param pageContext
     */
    private void prepareSearch(PageContext pageContext) {
        setDidYouMeanNeeded(false);
        setExcerptNeeded(true);
        
        String maxItems = "";

        if(moduleParameters != null){
            maxItems = moduleParameters.get("maxItems");
        }
        
        if(maxItems != null && !maxItems.equals("")){
            setLimit(Integer.parseInt(maxItems));
        } 
    }
    
    /**
     * prepare the xpath/sql statement. This method needs to be implemented by every concrete class
     * 
     * @param pageContext
     */
    public void prepareStatement(PageContext pageContext) {
        String statement = "//*[@jcr:primaryType = 'businesslease:news'] order by @businesslease:date descending";
        setStatement(statement);
    }
    /**
     * this render is a final, since it is delegate code, and is not meant for extending
     */
    @Override
    public final void render(PageContext pageContext) throws TemplateException {

        prepareSearch(pageContext);
        prepareStatement(pageContext);
        
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ContextBase ctxBase = (ContextBase) request.getAttribute(HstFilterBase.CONTENT_CONTEXT_REQUEST_ATTRIBUTE);

        List<SearchHit> hits = new ArrayList<SearchHit>();
        SearchResult searchResult = new SearchResult(hits);

        String statement = getStatement();
        String querytext = getQueryText();
        String language = getLanguage();

        pageContext.setAttribute(getVar(), searchResult);

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
            log.debug("query " + q.getStatement() + " took " + (System.currentTimeMillis() - start)
                    + " ms to complete.");

            start = System.currentTimeMillis();
            RowIterator rows = queryResult.getRows();
            
            searchResult.setSize(rows.getSize());
            searchResult.setOffset(offset);
            searchResult.setPagesize(limit);
            searchResult.setQuery(querytext);
            try {
                rows.skip(offset);
            } catch (NoSuchElementException e ) {
                log.debug("offset is larger then the number of results");
                return;
            }
            int counter = 0; 
            while (rows.hasNext()) {
                counter++;
                if(counter > limit) {
                    break;
                }
                Row row = rows.nextRow();
                try {
                    Node node = (Node) jcrSession.getItem(row.getValue("jcr:path").getString());
                    double score = row.getValue("jcr:score").getDouble();
                    SearchHit searchHit = new SearchHit(node, (counter+offset), score);
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
                    start = System.currentTimeMillis();
                    Value v = queryMngr.createQuery(
                            "/jcr:root[rep:spellcheck('" + querytext + "')]/(rep:spellcheck())", Query.XPATH).execute()
                            .getRows().nextRow().getValue("rep:spellcheck()");
                    log.debug("Getting 'didyoumean' took " + (System.currentTimeMillis() - start) + " ms to complete.");
                    if (v != null) {
                        searchResult.setDidyoumean(v.getString());
                    }
                } catch (RepositoryException e) {
                    log.error("error trying to find 'didyoumean' term");
                }

            }
            // important debug info in case of performance bottlenecks:
            log.debug("fetching " + hits.size() + " searchresults took " + (System.currentTimeMillis() - start)
                    + " ms to complete.");

            // TODO add similarity search

        } catch (InvalidQueryException e) {
            log.error("InvalidQueryException " + e.getMessage() + " for : " + statement);
        } catch (RepositoryException e) {
            log.error("RepositoryException while executing search " + e.getMessage());
        }

        super.render(pageContext);
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

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public void setSimilarNeeded(boolean similarNeeded) {
        this.similarNeeded = similarNeeded;
    }

}
