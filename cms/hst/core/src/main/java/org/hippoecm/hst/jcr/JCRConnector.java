/*
 * Copyright 2007-2008 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.jcr;

import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Item;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.hippoecm.hst.core.HSTConfiguration;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRConnector {
    private static final Logger logger = LoggerFactory.getLogger(JCRConnector.class);

    public static final String JCR_SESSION_KEY = "org.hippoecm.hst.JCRSESSION";

    public static Session getJCRSession(HttpSession httpSession) {
        Session result = null;
        try {
            SessionWrapper wrapper = (SessionWrapper) httpSession.getAttribute(JCR_SESSION_KEY);
            if (wrapper != null && wrapper.jcrSession.isLive()) {
                result = wrapper.jcrSession;
            } else {
                httpSession.removeAttribute(JCR_SESSION_KEY);
                
                ServletContext sc = httpSession.getServletContext(); 
                wrapper = new SessionWrapper(HSTConfiguration.get(sc, HSTConfiguration.KEY_REPOSITORY_ADRESS),
                                             HSTConfiguration.get(sc, HSTConfiguration.KEY_REPOSITORY_USERNAME),
                                             HSTConfiguration.get(sc, HSTConfiguration.KEY_REPOSITORY_PASSWORD));
                httpSession.setAttribute(JCR_SESSION_KEY, wrapper);
                result = wrapper.jcrSession;
            }
            wrapper.refresh(httpSession);
        } catch (LoginException e) {
            logger.error("Failed to login to repository", e);
        } catch (RepositoryException e) {
            logger.error("Failed to initialize repository", e);
        }
        return result;
    }
    
    public static Item getItem(Session session, String path) throws RepositoryException {

        Node node = session.getRootNode();

        // strip first slash
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        
        // loop all path elements and interpret them
        String[] pathElts = path.split("/");
        for (int pathIdx = 0; pathIdx < pathElts.length && node != null; pathIdx++) {
            String relPath = pathElts[pathIdx];
            if(relPath.equals(""))
                continue;

            // determine if this (last) part of the path is a property or argument
            // with [] notation
            Map<String, String> conditions = null;
            if (relPath.contains("[") && relPath.endsWith("]") 
                    && !Character.isDigit(relPath.charAt(relPath.indexOf("[")+1))) {
                conditions = new TreeMap<String, String>();
                int beginIndex = relPath.indexOf("[") + 1;
                int endIndex = relPath.lastIndexOf("]");
                String[] conditionElts = relPath.substring(beginIndex, endIndex).split(",");
                for (int conditionIdx = 0; conditionIdx < conditionElts.length; conditionIdx++) {
                    int pos = conditionElts[conditionIdx].indexOf("=");
                    if (pos >= 0) {
                        String key = conditionElts[conditionIdx].substring(0, pos);
                        String value = conditionElts[conditionIdx].substring(pos + 1);
                        if (value.startsWith("'") && value.endsWith("'")) {
                            value = value.substring(1, value.length() - 1);
                        }
                        conditions.put(key, value);
                    } else {
                        conditions.put(conditionElts[conditionIdx], null);
                    }
                }
                relPath = relPath.substring(0, relPath.indexOf("["));
            }

            // current path element is doesn't have [] notation
            if (conditions == null || conditions.size() == 0) {
                
                // a property with . notation
                if (pathIdx + 1 == pathElts.length && 
                        !relPath.contains("[") && node.hasProperty(relPath)) {
                    try {
                        return node.getProperty(relPath);
                    } catch (PathNotFoundException ex) {
                        return null;
                    }
                } 
                
                // a subnode
                else if (node.hasNode(relPath)) {
                    try {
                        node = node.getNode(relPath);
                    } catch (PathNotFoundException ex) {
                        return null;
                    }
                } else {
                    return null;
                }    
            } 

            // current path element is a property or argument: loop nodes and stop if the condition are ok  
            else {
                for (NodeIterator iter = node.getNodes(relPath); iter.hasNext();) {
                    node = iter.nextNode();
                    for (Map.Entry<String, String> condition : conditions.entrySet()) {
                        if (node.hasProperty(condition.getKey())) {
                            if (condition.getValue() != null) {
                                try {
                                    if (!node.getProperty(condition.getKey()).getString().equals(condition.getValue())) {
                                        node = null;
                                        break;
                                    }
                                } catch (PathNotFoundException ex) {
                                    node = null;
                                    break;
                                } catch (ValueFormatException ex) {
                                    node = null;
                                    break;
                                }
                            }
                        } else {
                            node = null;
                            break;
                        }
                    }

                    // node found
                    if (node != null) {
                        break;
                    }    
                }
            }
        }

        return node;
    }

    private static class SessionWrapper implements HttpSessionBindingListener {
        Session jcrSession;
        long lastRefreshed;

        SessionWrapper(String location, String username, String password) throws LoginException, RepositoryException {              
        logger.info("connecting to repository at " + location + " as " + username);

            HippoRepositoryFactory.setDefaultRepository(location);
            HippoRepository repository = HippoRepositoryFactory.getHippoRepository();
            try {
                jcrSession = repository.login(username, (password != null ? password.toCharArray() : null));

                logger.info("logged in as " + username);
            } catch(LoginException ex) {
                logger.warn("login as " + username + " failed, trying as anonymous.");
                jcrSession = repository.login();
                logger.info("logged in as anonymous");
            }
            lastRefreshed = System.currentTimeMillis();
        }

        public void valueBound(HttpSessionBindingEvent event) {
            logger.debug("Bound JCR session to HTTP session");
        }

        public void valueUnbound(HttpSessionBindingEvent event) {
            jcrSession.logout();
            logger.debug("Unbound JCR session from HTTP session");
        }

        void refresh(HttpSession httpSession) throws RepositoryException {
            if(httpSession.getMaxInactiveInterval() >= 0 &&
               System.currentTimeMillis() - lastRefreshed >= httpSession.getMaxInactiveInterval()*1000L) {
                jcrSession.refresh(false); // NOTICE: never keep changes
                logger.info("refreshing session");
                lastRefreshed = System.currentTimeMillis();
            }
        }
    }
}
