/*
 *  Copyright 2011-2012 Hippo.
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

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BehavioralDataHttpSessionStore implements BehavioralDataStore {

    public static final String SESSION_ATTR_NAME = BehavioralDataHttpSessionStore.class.getName()+".session";

    private static final Logger log = LoggerFactory.getLogger(BehavioralDataHttpSessionStore.class);

    private WeakReferenceHttpSessionRegistry httpSessionRegistry;

    public void setHttpSessionRegistry(final WeakReferenceHttpSessionRegistry httpSessionRegistry) {
        this.httpSessionRegistry = httpSessionRegistry;
    }

    @Override
    public Map<String, BehavioralData> readBehavioralData(HttpServletRequest request) {
        HttpSession session = request.getSession(true);

        if (httpSessionRegistry != null) {
            httpSessionRegistry.add(session);
        } else {
            log.warn("No HTTP session registry defined, HTTP sessions will not be stored.");
        }

        Map<String, BehavioralData> behavioralDataMap = (Map<String, BehavioralData>) session.getAttribute(SESSION_ATTR_NAME);
        if (behavioralDataMap == null) {
            behavioralDataMap = new HashMap<String, BehavioralData>();
        }
        return behavioralDataMap;
    }

    @Override
    public void storeBehavioralData(HttpServletRequest request, HttpServletResponse response, Map<String, BehavioralData> behavioralDataMap) {

        HttpSession session = request.getSession(true);

        session.setAttribute(SESSION_ATTR_NAME, behavioralDataMap);

    }
}
