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

import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;

/**
 * The result of the execution of the HstQuery. 
 */
public interface HstQueryResult {
    
    /**
     * Returns the total number of hits. A hit from a HstQuery is always a single hit. If the hit matches the search criteria multiple
     * times, it still results as a single hit. It does however influence the Lucene scoring, hence, when no sorting is applied, the hit
     * might have a higher precendence than other hits.
     * @return the total number of hits. 
     */
    int getSize();
    
    /**
     * This returns a HippoBeanIterator, which is a lazy loading proxy for accessing the beans in the HstQueryResult. This is really efficient
     * as the beans are only really fetched when being called by {@link HippoBeanIterator#nextHippoBean()} or {@link HippoBeanIterator#next()}
     * @return a HippoBeanIterator
     */
    HippoBeanIterator getHippoBeans();
}
