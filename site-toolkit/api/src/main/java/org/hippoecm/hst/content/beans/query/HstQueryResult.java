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

import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;

/**
 * The result of the execution of the HstQuery. 
 */
public interface HstQueryResult {
    
    /**
     * <p>
     * Returns the total number of hits. A hit from a HstQuery is always a single hit. If the hit matches the search criteria multiple
     * times, it still results as a single hit.
     * </p>
     * <p>
     * Note that when a limit is set on the query, for example through {@link HstQuery#setLimit(int)}, then this method will never
     * give a higher value then this limit. If you need the total hit number you can use {@link #getTotalSize()}. You can better not
     * set the limit to {@link Integer#MAX_VALUE} to get the actual total hits, as this means all hits need to be
     * authorized. In that case, {@link #getTotalSize()} if much faster as it does not authorize the hits. 
     * </p>
     * <p>
     * The {@link #getTotalSize()} will return the correct authorized size once we have tackled this in the Repository, see HREPTWO-619
     * </p>
     * @see #getTotalSize()
     * @return the total number of authorized hits. 
     */
    int getSize();
    
    /**
     * <p>
     * Returns the total number of hits. This is the total size of hits, even if a {@link HstQuery#setLimit(int)} was used that was smaller.
     * This is different then {@link #getSize()}. Also this method does not imply that every hit needs to be authorized. Hence, this call
     * is much more efficient than {@link #getSize()}.
     * </p>
     * <p>
     *  The {@link #getTotalSize()} will return the correct authorized size once we have tackled this in the Repository, see HREPTWO-619
     * </p>
     * @see #getSize()
     * @return the total number of authorized hits. 
     */
    int getTotalSize();
    
    /**
     * This returns a HippoBeanIterator, which is a lazy loading proxy for accessing the beans in the HstQueryResult. This is really efficient
     * as the beans are only really fetched when being called by {@link HippoBeanIterator#nextHippoBean()} or {@link HippoBeanIterator#next()}
     * @return a HippoBeanIterator
     */
    HippoBeanIterator getHippoBeans();
}
