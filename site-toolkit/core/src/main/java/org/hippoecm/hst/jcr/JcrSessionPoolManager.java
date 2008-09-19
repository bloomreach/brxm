/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.hst.jcr;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.HSTConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrSessionPoolManager {
    
    private static final Logger log = LoggerFactory.getLogger(JcrSessionPoolManager.class);
    private static final Map<String,JcrSessionPool> jcrSessionPools = new HashMap<String,JcrSessionPool>();
    
    public final static Session getTemplateSession(HttpServletRequest request){
        // TODO a session needed to fetch templates might need other credentials. For now, just fetch a normal 
        // jcr session
        return getSession(request);
    }
    
    public final static ReadOnlyPooledSession getSession(HttpServletRequest request){
        
        // TODO improve speed of getting a session from the pool. 
        
        ServletContext sc = request.getSession().getServletContext();
        String repositoryLocation = HSTConfiguration.get(sc, HSTConfiguration.KEY_REPOSITORY_ADRESS);
        String username = HSTConfiguration.get(sc, HSTConfiguration.KEY_REPOSITORY_USERNAME);
        String password = HSTConfiguration.get(sc, HSTConfiguration.KEY_REPOSITORY_PASSWORD);
        SimpleCredentials smplCred = new SimpleCredentials(username, (password != null ? password.toCharArray() : null));
        
        // TODO a less blocking synronization
        String userId = smplCred.getUserID();
        if(userId == null) {
            userId = "anonymous";
        }
        synchronized(jcrSessionPools) {
            JcrSessionPool jcrSessionPool = jcrSessionPools.get(userId);
            if(jcrSessionPool == null) {
                log.debug("No session pool present for user '" +username+ "'. Create one" );
                jcrSessionPool = new JcrSessionPool(smplCred, repositoryLocation);
                jcrSessionPools.put(userId,jcrSessionPool);
                return jcrSessionPool.getSession(request.getSession());
            }
            log.debug("Return session from pool if an idle valid one is present, otherwise add a new one to the session");
            return jcrSessionPool.getSession(request.getSession());
        }
    }
    
}
