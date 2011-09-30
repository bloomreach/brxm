/*
 *  Copyright 2011 Hippo.
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
package org.onehippo.hst.behavioral;

import javax.servlet.http.HttpServletRequest;

/**
 * A {@link BehavioralDataProvider} knows how to extract behavioral information from a browser {@link HttpServletRequest}.
 * It records terms found in the request and adds them to the {@link BehavioralData} of the current user.
 * <p>
 * For instance a {@link BehavioralDataProvider} could check to see if the current {@link HttpServletRequest} was a search action and if so,
 * add the search terms to the behavioral data. Another {@link BehavioralDataProvider} could check if the current
 * request was for a document that defines keywords and if so add those keywords to the behavioral data.
 * </p>
 * <p>
 * Many possible implementations exist and sites interested in behavioral data may define their own
 * {@link BehavioralDataProvider}s in order to mine data typical for that site.
 * </p>
 * <p>
 * <b>Note</b> that {@link BehavioralDataProvider} implementations must be implemented thread-safe: they are called concurrently by multiple {@link HttpServletRequest}s
 * </p>
 */
public interface BehavioralDataProvider {

    /**
     * The <code>identifier</code> of this BehavioralDataProvider.
     * 
     * @return  the identifier of this BehavioralDataProvider
     */
    String getId();
    
    /**
     * The <code>name</code> of this BehavioralDataProvider.
     * 
     * @return the name of this BehavioralDataProvider
     */
    String getName();
    
    /**
     * 
     * @param behavioralData  the {@link BehavioralData} to update. May be null.
     * @param request  the <code>request</code> to inspect for new behavioral information to add to the data.
     * @return the updated {@link BehavioralData}. Null if both the passed in {@link BehavioralData} was null and there
     *         was no new information to store. 
     * @throws IllegalArgumentException  if the {@link BehavioralData} is not of the type that is handled by this provider.
     */
    BehavioralData updateBehavioralData(BehavioralData behavioralData, HttpServletRequest request) throws IllegalArgumentException;

    /**
     * Some data is more valuable than other data. This method returns the weight that the data provided by this BehavioralDataProvider
     * should be given when calculating the {@link BehavioralProfile}.
     * 
     * @return  the weight (or importance) of the data provided by this BehavioralDataProvider
     */
    Long getWeight();
}
