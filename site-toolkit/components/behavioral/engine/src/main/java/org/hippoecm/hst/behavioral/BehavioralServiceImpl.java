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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.behavioral.BehavioralData;
import org.hippoecm.hst.behavioral.BehavioralDataProvider;
import org.hippoecm.hst.behavioral.BehavioralDataStore;
import org.hippoecm.hst.behavioral.BehavioralProfile;
import org.hippoecm.hst.behavioral.BehavioralService;

public class BehavioralServiceImpl implements BehavioralService {

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
        
        BehavioralProfile profile = calculateBehavioralProfile(request);
        request.setAttribute(BEHAVIORAL_PROFILE_ATTR, profile);
        return profile;
    }
    

    protected BehavioralProfile calculateBehavioralProfile(HttpServletRequest request) {
        @SuppressWarnings("unchecked")
        List<BehavioralData> behavioralDataList = (List<BehavioralData>) request.getAttribute(BEHAVIORAL_DATA_ATTR);
        Configuration config = getConfiguration(request);
        BehavioralProfileCalculator behavioralProfileCalculator = new BehavioralProfileCalculator(config);
        return behavioralProfileCalculator.calculate(behavioralDataList);
    }
    
    @Override
    public void updateBehavioralData(HttpServletRequest request, HttpServletResponse response) {
        
        Configuration config = getConfiguration(request);
        
        Map<String, BehavioralData> behavioralDataMap = new HashMap<String, BehavioralData>();
        for(BehavioralData behavioralData : behavioralDataStore.readBehavioralData(request)) {
            behavioralDataMap.put(behavioralData.getProviderId(), behavioralData);
        }
        
        // For each provider update the BehavioralData
        for(BehavioralDataProvider provider : config.getDataProviders()) {
            BehavioralData behavioralData = provider.updateBehavioralData(behavioralDataMap.get(provider.getId()), request);
            if (behavioralData != null) {
                behavioralDataMap.put(provider.getId(), behavioralData);
            }
        }
        
        List<BehavioralData> behavioralDataList = new ArrayList<BehavioralData>(behavioralDataMap.values()); 
        // We store the behavioralDataList on the request, even though it is accessible through the store as well: However, depending
        // on the store, fetching it from the store might be more expensive: Think for example of a store that is being accessed through external REST calls
        request.setAttribute(BEHAVIORAL_DATA_ATTR, behavioralDataList);
        
        // Store the updated BehavioralData
        behavioralDataStore.storeBehavioralData(request, response, behavioralDataList);
        
        System.out.println(getBehavioralProfile(request));
    }
    
    private Configuration getConfiguration(HttpServletRequest request) {
        if(request.getAttribute(CONFIGURATION_ATTR) != null) {
            // if there is a Configuration on the request, we use this configuration as for a single request, the Configuration should be the same 
            // instance, even if during the request the Configuration is invalidated by invalidate()
            return (Configuration)request.getAttribute(CONFIGURATION_ATTR);
        }
        Session session = null;
        Configuration config = configuration;
        if(config != null) {
            return config;
        }
        try {
            synchronized (this) {
                try {
                    session = repository.login(credentials);
                    config = buildConfiguration(session);
                } catch (RepositoryException e) {
                    e.printStackTrace();
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
        // return config and not configuration as due to invalidate() configuration can always be set to null.
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
