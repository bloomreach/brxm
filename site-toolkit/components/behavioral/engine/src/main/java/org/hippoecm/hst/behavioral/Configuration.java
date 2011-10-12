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

import org.hippoecm.hst.behavioral.providers.AbstractTermsDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Configuration {
    
    private static final Logger log = LoggerFactory.getLogger(Configuration.class);

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
        
        // load the persona's
        Map<String, Persona> personas = new HashMap<String, Persona>();
        Map<String, Set<String>> providersToTerms = new HashMap<String, Set<String>>();
        try {
            NodeIterator personaIter = jcrNode.getNode(BehavioralNodeTypes.BEHAVIORAL_NODETYPE_PERSONAS).getNodes();
            while (personaIter.hasNext()) {
                Node personaNode = personaIter.nextNode();
                PersonaImpl persona = new PersonaImpl(personaNode, this);
                personas.put(persona.getId(), persona);
                for (Rule rule : persona.getRules()) {
                    Set<String> terms = providersToTerms.get(rule.getProviderId());
                    if (terms == null) {
                        terms = new HashSet<String>();
                        providersToTerms.put(rule.getProviderId(), terms);
                    }
                    terms.addAll(rule.getTerms());
                }
            }
        } catch (RepositoryException e) {
            log.error("Unable to create persona", e);
        } catch (IllegalArgumentException e) {
            log.error("Unable to create persona", e);
        }
        
        this.personas = Collections.unmodifiableMap(personas);

        // tell the terms data providers what terms to look out for
        for (Entry<String, Set<String>> entry : providersToTerms.entrySet()) {
            BehavioralDataProvider provider = providers.get(entry.getKey());
            if (provider != null && provider instanceof AbstractTermsDataProvider) {
                ((AbstractTermsDataProvider)provider).setConfiguredTerms(entry.getValue());
            }
        }

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
