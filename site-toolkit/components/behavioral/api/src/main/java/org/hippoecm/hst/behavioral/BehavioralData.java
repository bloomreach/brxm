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

import java.io.Serializable;
import java.util.Map;

/**
 * {@link BehavioralData} represents the raw data that is mined by a {@link BehavioralDataProvider}.
 * It's a simple mapping from terms to the frequency those terms were encountered.
 *
 * <p>
 * A {@link BehavioralData} object is linked to the {@link BehavioralDataProvider#getId()} that provided it so that  when
 * the time comes to calculate the {@link BehavioralProfile} the weight this data should be given is accessible.
 * </p>
 */
public interface BehavioralData extends Serializable {
    
    /**
     * @return the mapping between terms and the frequency those terms were encountered. If there are no mappings, an empty map is returned
     */
    Map<String, Integer> getTermFreq();
    
    /**
     * @return  the id of the {@link BehavioralDataProvider} this BehavioralData object was provided by. 
     */
    String getProviderId();
    
}
