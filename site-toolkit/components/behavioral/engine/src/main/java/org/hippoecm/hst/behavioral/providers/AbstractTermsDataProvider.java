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
package org.hippoecm.hst.behavioral.providers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.behavioral.BehavioralData;
import org.hippoecm.hst.behavioral.Rule;

public abstract class AbstractTermsDataProvider extends AbstractDataProvider {
    
    private Set<String> configuredTerms;
    
    public AbstractTermsDataProvider(String id, String name, Node node) throws RepositoryException {
        super(id, name, node);
    }

    public final BehavioralData updateBehavioralData(BehavioralData behavioralData, HttpServletRequest request) throws IllegalArgumentException {
        
        final List<String> terms = extractTerms(request);
        
        if (terms == null || terms.size() == 0) {
            return null;
        }
        
        if(behavioralData != null && !(behavioralData instanceof TermsBehavioralData)) {
            throw new IllegalArgumentException("BehavioralData is not of the expected type.");
        }
        
        for (String term : terms) {
            if (isIncludedTerm(term)) {
                if (behavioralData == null) {
                    behavioralData = new TermsBehavioralData(getId(), size);
                }
                ((TermsBehavioralData)behavioralData).putTerm(term);
            }
        }
        
        return behavioralData;
    }
    
    /**
     * 
     * @param request the {@link HttpServletRequest}
     * @return the {@link List} of extracted terms or <code>null</code> when no terms are available
     */
    protected abstract List<String> extractTerms(HttpServletRequest request);
    
    public void setConfiguredTerms(Set<String> terms) {
        configuredTerms = terms;
    }
    
    protected boolean isIncludedTerm(String term) {
        if (configuredTerms == null) {
            return false;
        }
        return configuredTerms.contains(term);
    }
    
    @Override
    public boolean evaluate(Rule rule, BehavioralData data) {
        if (!(data instanceof TermsBehavioralData)) {
            throw new IllegalArgumentException("Argument data is not of the expected type");
        }
        
        int totalFreq = 0;
        for (String term : rule.getTerms()) {
            Integer freq = ((TermsBehavioralData) data).getTermFreq().get(term);
            if (freq != null) {
                totalFreq += freq;
            }
        }
        return totalFreq >= rule.getFrequencyThreshold();
    }
    
    protected static class TermsBehavioralData extends AbstractBehavioralData implements BehavioralData {
        
        private static final long serialVersionUID = 1L;
        
        private final Map<String, Integer> termFreq;
        
        protected TermsBehavioralData(String providerId, final int size) {
            super(providerId);
            this.termFreq = new LinkedHashMap<String, Integer>(size, 0.75f, true){

                private static final long serialVersionUID = 1L;
                @Override
                protected boolean removeEldestEntry(Entry<String, Integer> eldest) {
                    return size() >  size;
                }

            };
        }
        
        public Map<String, Integer> getTermFreq() {
            return termFreq;
        }

        protected void putTerm(String term) {
            Integer freq = termFreq.get(term);
            if(freq == null) {
                termFreq.put(term, new Integer(1));
            } else {
                termFreq.put(term, new Integer(freq + 1));
            }
        }

    }
}
