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


import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.behavioral.BehavioralDataProvider;
import org.hippoecm.hst.behavioral.BehavioralNodeTypes;
import org.hippoecm.hst.behavioral.providers.AbstractDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Configuration {
    
    private static final Logger log = LoggerFactory.getLogger(Configuration.class);

    private final Map<String, Dimension> dimensions;
    private final Map<String, List<Rule>> termsToRules;
    private final Map<String, BehavioralDataProvider> providers;
    private final Map<String, Persona> personas;
    
    public Configuration(Node jcrNode) {
        
        try {
            if (!jcrNode.isNodeType(BehavioralNodeTypes.BEHAVIORAL_NODETYPE_CONFIGURATION)) {
                throw new IllegalArgumentException("Configuration node not of the required type. Expected '"+BehavioralNodeTypes.BEHAVIORAL_NODETYPE_CONFIGURATION+"' but was + '"+jcrNode.getPrimaryNodeType().getName()+"' ");
            }
        } catch (RepositoryException e) {
            log.error("Error checking configuration node type");
        }
        
        // load the configured providers
        Map<String, BehavioralDataProvider> providers = new HashMap<String, BehavioralDataProvider>();
        try {
            NodeIterator providerIter = jcrNode.getNode(BehavioralNodeTypes.BEHAVIORAL_NODETYPE_PROVIDERS).getNodes();
            while (providerIter.hasNext()) {
                Node providerNode = providerIter.nextNode();
                String className = providerNode.getProperty(BehavioralNodeTypes.BEHAVIORAL_PROVIDER_PROPERTY_CLASSNAME).getString();
                String providerName = providerNode.getProperty(BehavioralNodeTypes.BEHAVIORAL_GENERAL_PROPERTY_NAME).getString();
                String providerId = providerNode.getName();
                try {
                    Class<BehavioralDataProvider> providerClass = (Class<BehavioralDataProvider>) Class.forName(className);
                    Constructor<BehavioralDataProvider> providerConstructor = providerClass.getConstructor(String.class, String.class, Node.class);
                    BehavioralDataProvider provider = providerConstructor.newInstance(providerId, providerName, providerNode);
                    providers.put(provider.getId(), provider);
                } catch (Exception e) {
                    log.error("Unable to create provider " + providerId, e);
                }
            }
        } catch (RepositoryException e) {
            log.error("Error loading providers", e);
        }
        this.providers = Collections.unmodifiableMap(providers);
        
        // read the dimensions, segments, and rules
        Map<String, Dimension> dimensions = new HashMap<String, Dimension>();
        try {
            NodeIterator dimensionIter = jcrNode.getNode(BehavioralNodeTypes.BEHAVIORAL_NODETYPE_DIMENSIONS).getNodes();
            while (dimensionIter.hasNext()) {
                Node dimensionNode = dimensionIter.nextNode();
                try {
                    Dimension dimension = new Dimension(dimensionNode);
                    dimensions.put(dimension.getId(), dimension);
                } catch (RepositoryException e) {
                    log.error("Unable to create dimension", e);
                } catch (IllegalArgumentException e) {
                    log.error("Unable to create dimension", e);
                }
            }
        } catch (RepositoryException e) {
            log.error("Error creating dimensions", e);
        }
        this.dimensions = Collections.unmodifiableMap(dimensions);
        
        Map<String, Set<String>> providersToTerms = new HashMap<String, Set<String>>();
        Map<String, List<Rule>> termsToRules = new HashMap<String, List<Rule>>();
        for(Dimension dimension : dimensions.values()) {
            for(Segment segment : dimension.getSegments().values()) {
                for(Rule rule : segment.getRules()) {
                    for (String term : rule.getTerms()) {
                        List<Rule> rules = termsToRules.get(term);
                        if(rules == null) {
                            rules = new ArrayList<Rule>();
                            termsToRules.put(term, rules);
                        }
                        rules.add(rule);
                    }
                    // gather the terms data providers should look for
                    Set<String> terms = providersToTerms.get(rule.getProviderId());
                    if (terms == null) {
                        terms = new HashSet<String>();
                        providersToTerms.put(rule.getProviderId(), terms);
                    }
                    terms.addAll(rule.getTerms());
                }
            }
        }

        this.termsToRules = Collections.unmodifiableMap(termsToRules);
        
        // tell the data providers what terms to look out for
        for (Entry<String, Set<String>> entry : providersToTerms.entrySet()) {
            BehavioralDataProvider provider = providers.get(entry.getKey());
            if (provider != null) {
                ((AbstractDataProvider)provider).setConfiguredTerms(entry.getValue());
            }
        }
        
        // load the personas
        Map<String, Persona> personas = new HashMap<String, Persona>();
        try {
            NodeIterator personaIter = jcrNode.getNode(BehavioralNodeTypes.BEHAVIORAL_NODETYPE_PERSONAS).getNodes();
            while (personaIter.hasNext()) {
                Node personaNode = personaIter.nextNode();
                Persona persona = new Persona(personaNode, this);
                personas.put(persona.getId(), persona);
            }
        } catch (RepositoryException e) {
            log.error("Unable to create persona", e);
        } catch (IllegalArgumentException e) {
            log.error("Unable to create persona", e);
        }
        
        this.personas = Collections.unmodifiableMap(personas);
        
    }
    
    public Map<String, Dimension> getDimensions() {
        return dimensions;
    }

    public Map<String, List<Rule>> getTermsToRules() {
        return termsToRules;
    }
    
    public List<BehavioralDataProvider> getDataProviders() {
        return new ArrayList<BehavioralDataProvider>(providers.values());
    }
    
    public BehavioralDataProvider getDataProvider(String providerId) {
        return providers.get(providerId);
    }
    
    public Map<String, Persona> getPersonas() {
        return personas;
    }

}
