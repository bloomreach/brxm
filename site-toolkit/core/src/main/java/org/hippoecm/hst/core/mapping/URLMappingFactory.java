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
package org.hippoecm.hst.core.mapping;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URLMappingFactory {

    private static final Logger log = LoggerFactory.getLogger(URLMapping.class);
    private final static Map<String, URLMapping> urlMappings = new HashMap<String, URLMapping>();
    private final static Object mutex = new Object();
    
    public static URLMapping getUrlMapping(Session session, String contextPath, String uriPrefix, String hst_configuration_path,
            int uriLevels) {
        
        String userId = session.getUserID();
        if(userId == null) {
            userId = "anonymous";
        }
        String key = userId+"_"+hst_configuration_path;
        synchronized(mutex) {
            URLMapping urlMapping = urlMappings.get(key);
            if( urlMapping != null) {
                log.debug("return found urlmapping for user and context");
                return urlMapping;
            } else {
                log.debug("no urlmapping found for user and context. Create a new one");
                URLMapping newUrlMapping = new URLMappingImpl(session, contextPath, uriPrefix, hst_configuration_path,uriLevels);
                urlMappings.put(key, newUrlMapping);
                return newUrlMapping;
            }
        }
    }

}
