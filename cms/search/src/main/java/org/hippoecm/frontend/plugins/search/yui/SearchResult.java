/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.search.yui;

/**
 * Helper bean for an easy jcr-nodes2JSON translation
 */
public class SearchResult {

    private ResultItem[] results;
    private int totalHits;
    
    public int getTotalHits() {
        return totalHits;
    }

    public void setTotalHits(int totalHits) {
        this.totalHits = totalHits;
    }

    public ResultItem[] getResults() {
        return results;
    }

    public void setResults(ResultItem[] results) {
        this.results = results;
    }

    public SearchResult(ResultItem[] results) {
        this.results = results;
        this.totalHits = results.length;
    }
}
