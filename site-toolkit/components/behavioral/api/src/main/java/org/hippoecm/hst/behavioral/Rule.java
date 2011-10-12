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
package org.hippoecm.hst.behavioral;

import java.util.Collection;

/**
 * Rules are the constituent components that make up an {@link Expression}. Rules can be configured
 * with terms that specify which strings the associated provider should consider. For instance a rule
 * that specifies that a visitor must visit a page of certain types would be configured with the
 * names of those types as terms. When the time comes for a rule to be evaluated in order to determine
 * whether a certain user matches a certain persona, the terms that were gathered by the associated
 * {@link BehavioralDataProvider} (in this case the types of documents visited by the visitor) are
 * compared to the terms configured on the rule, and if the number of times they match bypasses the
 * frequency threshold the rule will pass.
 */
public interface Rule {

    /**
     * @return  the id of the underlying {@link BehavioralDataProvider} of this rule.
     */
    String getProviderId();
    
    /**
     * @return  the terms this rule was configured for.
     */
    Collection<String> getTerms();
    
    /**
     * @return  the number of times this rule must match before the rule is allowed to pass.
     * Depending on the {@link BehavioralDataProvider} this may not be relevant.
     */
    int getFrequencyThreshold();

}
