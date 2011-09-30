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

import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.onehippo.hst.behavioral.BehavioralData;
import org.onehippo.hst.behavioral.BehavioralDataProvider;
import org.onehippo.hst.behavioral.BehavioralNodeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractDataProvider implements BehavioralDataProvider {
    
    private static final Logger log = LoggerFactory.getLogger("com.onehippo.hst.behavioral.providers");

    private static final String SIZE_PROPERTY_NAME = BehavioralNodeTypes.BEHAVIORAL_PROVIDER_PROPERTY_SIZE;
    private static final String WEIGHT_PROPERTY_NAME = BehavioralNodeTypes.BEHAVIORAL_PROVIDER_PROPERTY_WEIGHT;
    
    private static final int DEFAULT_SIZE = 100;
    private static final Long DEFAULT_WEIGHT = 1L;
    
    private final String id;
    private final String name;
    private int size = DEFAULT_SIZE;
    private Long weight = DEFAULT_WEIGHT;
    private Set<String> configuredTerms;

    public AbstractDataProvider(String id, String name, Node node) throws RepositoryException {
        this.id = id;
        this.name = name;
        if (node.hasProperty(SIZE_PROPERTY_NAME)) {
            this.size = (int) node.getProperty(SIZE_PROPERTY_NAME).getLong();
        }
        if (node.hasProperty(WEIGHT_PROPERTY_NAME)) {
            this.weight = node.getProperty(WEIGHT_PROPERTY_NAME).getLong();
        }
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final Long getWeight() {
        return weight;
    }
    
    public final BehavioralData updateBehavioralData(BehavioralData behavioralData, HttpServletRequest request) throws IllegalArgumentException {
        
        final List<String> terms = extractTerms(request);
        
        if (terms == null || terms.size() == 0) {
            return null;
        }
        
        if(behavioralData != null && !(behavioralData instanceof BehavioralDataImpl)) {
            throw new IllegalArgumentException("BehavioralData not of the expected type.");
        }
        
        for (String term : terms) {
            if (isIncludedTerm(term)) {
                if (log.isDebugEnabled()) {
                    log.debug("Adding term " + term + " to behavioral data");
                }
                if (behavioralData == null) {
                    behavioralData = new BehavioralDataImpl(size, id);
                }
                ((BehavioralDataImpl)behavioralData).putTerm(term);
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
        return configuredTerms.contains(term);
    }
    
}