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
import org.hippoecm.hst.core.template.module.listdisplay.ListDisplayModule;
import org.hippoecm.hst.core.template.node.el.AbstractELNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractSearchModule extends ModuleBase implements Search {

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
            while (rows.hasNext()) {
                Row row = rows.nextRow();
                try {
                    Node node = (Node) jcrSession.getItem(row.getValue("jcr:path").getString());
                    SearchHit searchHit = new SearchHit(node);
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
                        System.out.println("getString " + v.getString());
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
