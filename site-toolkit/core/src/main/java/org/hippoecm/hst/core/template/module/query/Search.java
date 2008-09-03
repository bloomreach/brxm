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

import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

public interface Search {

    public  static final String DEFAULT_LANGUAGE = Query.XPATH;
    public static final int DEFAULT_THRESHOLD = 10;
    public static final int DEFAULT_LIMIT = 10;
    public static final int DEFAULT_OFFSET = 0;
    public static final boolean DEFAULT_DIDYOUMEANNEEDED = false;
    public static final boolean DEFAULT_EXCERPTNEEDED = false;
    public static final boolean DEFAULT_SIMILARNEEDED = false;
    
   
    
    /**
     * Returns the statement set for this query.
     *
     * @return the query statement.
     */
    public String getStatement();
    
    public void setStatement(String statement);

    /**
     * Returns the language set for this query. This will be one of the
     * query language constants returned by
     * {@link QueryManager#getSupportedQueryLanguages}.
     *
     * @return the query language.
     */
    public String getLanguage();
    
    public void setLanguage(String language);

    /**
     * Returns the query text for this search. If the number of hits is below some threshold, this
     * term will be used for find the 'didyoumean' term
     * @return the query text
     */
    public String getQueryText();
    
    public void setQueryText(String querytext);

    /**
     * Returns the maximum number of search hits needed
     * 
     * @return int maximum number of search hits
     */
    
    public int getLimit();
    
    public void setLimit(int limit);
    
    /**
     * Returns the offset where to start the search hits from. This is nice for paging
     * 
     * @return int offset to get the search hits from
     */
    
    public int getOffset();
    
    public void setOffset(int offset);
    
    /**
     * When the number of results is below this threshold, we will look for a 'didyoumean' term
     * 
     * @return the minimum number of hits below which we will look for a 'didyoumean' term 
     */
    
    public int getDidYouMeanThreshold();
    
    public void setDidYouMeanThreshold(int didYouMeanThreshold);
    
    /**
     * When returns true, an excerpt of the text with the matching part will be
     * add to the search result
     * 
     * @return boolean value whether an excerpt is needed or not
     */
    public boolean isExcerptNeeded();
    
    public void setExcerptNeeded(boolean excerptNeeded);
    
    
    /**
     * When needDidYouMean returns true, a didyoumean of the text with the matching part will be
     * searched when the number of search hits is below the getDidYouMeanThreshold()
     * 
     * @return boolean value whether a didyoumean must be searched for
     */
    public boolean isDidYouMeanNeeded();
    
    public void setDidYouMeanNeeded(boolean didYouMeanNeeded);
    
    /**
     * When needSimilar returns true, you can add a similar link to each search hit that will 
     * search for similar documents
     * 
     * @return boolean value whether a 'similar documents' link should be added
     */
    // TODO functionality still needs to be implemented in the AbstractSearchModule
    
    public boolean isSimilarNeeded();
    public void setSimilarNeeded(boolean similarNeeded);
}
