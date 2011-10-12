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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

public class RuleImpl implements Rule {
    
    private static final int DEFAULT_FREQUENCY = 1;
    
    private static final String FREQUENCY_THRESHOLD_PROPERTY_NAME = BehavioralNodeTypes.BEHAVIORAL_RULE_PROPERTY_FREQUENCY_THRESHOLD;
    
    private final String providerId;
    private final List<String> terms;
    
    private int frequencyThreshold = DEFAULT_FREQUENCY;
    
    public RuleImpl(Node jcrNode) throws RepositoryException {
        if(!jcrNode.isNodeType(BehavioralNodeTypes.BEHAVIORAL_NODETYPE_RULE)) {
            throw new IllegalArgumentException("Rule node not of the expected type. Expected '"+BehavioralNodeTypes.BEHAVIORAL_NODETYPE_RULE+"' but was + '"+jcrNode.getPrimaryNodeType().getName()+"' ");
        }
        providerId = jcrNode.getProperty(BehavioralNodeTypes.BEHAVIORAL_RULE_PROPERTY_PROVIDER).getString();

        List<String> terms = new ArrayList<String>();
        if (jcrNode.hasProperty(BehavioralNodeTypes.BEHAVIORAL_RULE_PROPERTY_TERMS)) {
            for (Value term : jcrNode.getProperty(BehavioralNodeTypes.BEHAVIORAL_RULE_PROPERTY_TERMS).getValues()) {
                terms.add(term.getString().toLowerCase());
            }
        }
        this.terms = Collections.unmodifiableList(terms);
        
        if (jcrNode.hasProperty(FREQUENCY_THRESHOLD_PROPERTY_NAME)) {
            this.frequencyThreshold = (int)(long)jcrNode.getProperty(FREQUENCY_THRESHOLD_PROPERTY_NAME).getLong();
        }
        if (frequencyThreshold < 1) {
            throw new IllegalArgumentException("Rule frequency should be at least 1");
        }
    }
    
    @Override
    public String getProviderId() {
        return providerId;
    }
    
    @Override
    public List<String> getTerms() {
        return terms;
    }

    @Override
    public int getFrequencyThreshold() {
        return frequencyThreshold;
    }

}
