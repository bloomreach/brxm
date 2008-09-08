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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class SearchResult {
    private List<SearchHit> hits;
    private List<Page> pages;
    private int size;
    private int offset;
    private int pagesize;
    private int limit;
    private int currentPageNumber;
    private String didyoumean;
    private String query;

    public SearchResult(List<SearchHit> hits) {
        this.hits = hits;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public long getPagesize() {
        return pagesize;
    }
    // limit == pagesize
    public long getLimit() {
        return limit;
    }

    public void setPagesize(int pagesize) {
        this.pagesize = pagesize;
        this.limit = pagesize;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    
    public void setDidyoumean(String didyoumean) {
       this.didyoumean = didyoumean;
    }
    
    public String getDidyoumean(){
        return didyoumean;
    }
    
    public List<SearchHit> getHits() {
        return hits;
    }

    public void setQuery(String querytext) {
        this.query = querytext;
    }
    
    public String getQuery() {
       return this.query;
    }

    public void setCurrentPage(int currentPageNumber) {
       this.currentPageNumber = currentPageNumber;
    }

    public int getCurrentPage(){
        return currentPageNumber;
    }

    public void computePagesAndLinks(Map parameterMap) {
        pages = new ArrayList<Page>();
        StringBuffer queryString = null;
       
        Set params = parameterMap.entrySet();
        Iterator paramIt = params.iterator();
        while(paramIt.hasNext()) {
            Entry e = (Entry)paramIt.next();
            String name = (String)e.getKey();
            if(SearchParameters.PAGE.equals(name)) {
                continue;
            }
            String[] values = (String[])e.getValue();
            for(String val : values) {
                if(queryString == null) {
                    queryString = new StringBuffer("?"+name+"="+val);
                } else {
                    queryString.append("&"+name+"="+val);  
                }
            }
        }
        if(pagesize > 0 && size > 0 ) {
            int nrOfPages = size/pagesize;
            if(pagesize*nrOfPages < size) {
                nrOfPages++;
            }
            String link;
            if(queryString==null || "".equals(queryString)){
                link = "?page=";
            } else {
                link = queryString.toString()+"&page=";
            }
            for(int i = 1 ; i <= nrOfPages ; i++) {
                pages.add(new Page(i,link+i , currentPageNumber==i ));
            }
        }
    }

    public List<Page> getPages() {
        return pages;
    }

    public void setPages(List<Page> pages) {
        this.pages = pages;
    }
    
    public class Page {
        private int number;
        private String link;
        private boolean current;

        public Page(int number, String link, boolean current) {
            this.number =number;
            this.current = current;
            this.link = link;
        }
        
        public boolean isCurrent() {
            return current;
        }

        public void setCurrent(boolean current) {
            this.current = current;
        }

        public int getNumber() {
            return number;
        }

        public void setNumber(int number) {
            this.number = number;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        
    }

}
