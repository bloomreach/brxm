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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BehavioralServiceImpl implements BehavioralService {

	private static Logger log = LoggerFactory.getLogger(BehavioralServiceImpl.class);
	
    private static final String BEHAVIORAL_PROFILE_ATTR =  BehavioralService.class.getName()+".profile";
    private static final String BEHAVIORAL_DATA_ATTR =  BehavioralService.class.getName()+".data";
    private static final String CONFIGURATION_ATTR =  BehavioralService.class.getName()+".config";
    
    private BehavioralDataStore behavioralDataStore;

    private volatile Configuration configuration;
    private Repository repository;
    private Credentials credentials;
    private String configNodePath;
    
    
    public void setBehavioralDataStore(BehavioralDataStore behavioralDataStore) {
        this.behavioralDataStore = behavioralDataStore;
    }
    
    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

    public void setConfigNodePath(String configNodePath) {
        this.configNodePath = configNodePath;
    }

    @Override
    public BehavioralProfile getBehavioralProfile(HttpServletRequest request) {
        if(request.getAttribute(BEHAVIORAL_PROFILE_ATTR) != null) {
            return (BehavioralProfile) request.getAttribute(BEHAVIORAL_PROFILE_ATTR);
        }
        
        BehavioralProfile profile = createBehavioralProfile(request);
        request.setAttribute(BEHAVIORAL_PROFILE_ATTR, profile);
        return profile;
    }
    

    protected BehavioralProfile createBehavioralProfile(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        Map<String, BehavioralData> behavioralDataMap = (Map<String, BehavioralData>) request.getAttribute(BEHAVIORAL_DATA_ATTR);
        Configuration config = getConfiguration(request);
        Set<String> matchingPersonas = new HashSet<String>(config.getPersonas().size());
        for (Persona persona : config.getPersonas().values()) {
            if (persona.getExpression().evaluate(behavioralDataMap)) {
                matchingPersonas.add(persona.getId());
            }
        }
        return new BehavioralProfileImpl(matchingPersonas);
    }
    
    @Override
    public void updateBehavioralData(HttpServletRequest request, HttpServletResponse response) {
        
        Configuration config = getConfiguration(request);
        
        Map<String, BehavioralData> behavioralDataMap = behavioralDataStore.readBehavioralData(request);
        
        // For each provider update the BehavioralData
        for(BehavioralDataProvider provider : config.getDataProviders()) {
            if (provider.isSessionLevel() && !request.getSession().isNew()) {
                continue;
            }
            BehavioralData behavioralData = provider.updateBehavioralData(behavioralDataMap.get(provider.getId()), request);
            if (behavioralData != null) {
                behavioralDataMap.put(provider.getId(), behavioralData);
            }
        }
        
        // We store the behavioralDataMap on the request, even though it is accessible through the store as well: However, depending
        // on the store, fetching it from the store might be more expensive: Think for example of a store that is being accessed through external REST calls
        request.setAttribute(BEHAVIORAL_DATA_ATTR, behavioralDataMap);
        
        // Store the updated BehavioralData
        behavioralDataStore.storeBehavioralData(request, response, behavioralDataMap);

    }
    
    private Configuration getConfiguration(HttpServletRequest request) {
        if(request.getAttribute(CONFIGURATION_ATTR) != null) {
            // if there is a configuration on the request, we use this configuration 
        	// because for a single request the configuration should be the same instance, 
        	// even if during the request the configuration is invalidated by invalidate()
            return (Configuration)request.getAttribute(CONFIGURATION_ATTR);
        }
        Session session = null;
        Configuration config = configuration;
        if (config == null) {
            try {
                synchronized (this) {
                    try {
                        session = repository.login(credentials);
                        config = buildConfiguration(session);
                    } catch (RepositoryException e) {
                    	log.error("Failed to build behavioral targeting configuration", e);
                    }
                    configuration = config;
                }
            }
            finally {
                if (session != null) {
                    synchronized(this) {
                        if (session != null) {
                            session.logout();
                            session = null;
                        }
                    }
                }
            }        	
        }
        // return config and not configuration because invalidate() can always set configuration to null.
        request.setAttribute(CONFIGURATION_ATTR, config);
        return config;
    }

   
    private Configuration buildConfiguration(Session session) throws RepositoryException {
        Node configNode = session.getNode(configNodePath);
        return new Configuration(configNode);
    }

    @Override
    public void invalidate() {
        synchronized(this) {
            configuration = null;
        }
    }
  
}
