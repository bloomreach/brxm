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

import java.util.List;

public class SearchResult {
    List<SearchHit> hits;
    private long size;
    private int offset;
    private int pagesize;
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

    public void setPagesize(int pagesize) {
        this.pagesize = pagesize;
    }

    
    public void setDidyoumean(String didyoumean) {
       this.didyoumean = didyoumean;
    }
    
    public String getDidyoumean(){
        return didyoumean;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
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

}
