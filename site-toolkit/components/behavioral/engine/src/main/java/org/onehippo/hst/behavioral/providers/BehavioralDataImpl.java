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
package org.onehippo.hst.behavioral.providers;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.onehippo.hst.behavioral.BehavioralData;


class BehavioralDataImpl implements BehavioralData {
    private static final long serialVersionUID = 1L;

    private final String providerId;
    private Map<String, Integer> termFreq;
     
    BehavioralDataImpl(final int size, String providerId) {
        this.termFreq = new LinkedHashMap<String, Integer>(size, 0.75f, true){

            private static final long serialVersionUID = 1L;
            @Override
            protected boolean removeEldestEntry(Entry<String, Integer> eldest) {
                return size() >  size;
            }

        };
        this.providerId = providerId;
    }
    
    @Override
    public String getProviderId() {
        return providerId;
    }

    @Override
    public Map<String, Integer> getTermFreq() {
        return termFreq;
    }

    void putTerm(String tag) {
        Integer freq = termFreq.get(tag);
        if(freq == null) {
            termFreq.put(tag, new Integer(1));
        } else {
            termFreq.put(tag, new Integer(freq + 1));
        }
    }
    
 }