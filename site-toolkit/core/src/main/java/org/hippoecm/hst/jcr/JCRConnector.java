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
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final Logger logger = LoggerFactory.getLogger(JCRConnector.class);

    public static final String JCR_SESSION_KEY = "org.hippoecm.hst.JCRSESSION";

    public static Session getJCRSession(HttpSession httpSession) {
        return getJCRSession(httpSession,false);
    }
    
    public static Session getJCRSession(HttpSession httpSession, boolean refreshJcrSession) {
        Session result = null;
        try {
            SessionWrapper wrapper = (SessionWrapper) httpSession.getAttribute(JCR_SESSION_KEY);
            if (wrapper != null && wrapper.jcrSessionOK()) {
                result = wrapper.jcrSession;
            } else {
                httpSession.removeAttribute(JCR_SESSION_KEY);

                // (re)connect
                ServletContext sc = httpSession.getServletContext();
                wrapper = new SessionWrapper(HSTConfiguration.get(sc, HSTConfiguration.KEY_REPOSITORY_ADRESS),
                                             HSTConfiguration.get(sc, HSTConfiguration.KEY_REPOSITORY_USERNAME),
                                             HSTConfiguration.get(sc, HSTConfiguration.KEY_REPOSITORY_PASSWORD));
                httpSession.setAttribute(JCR_SESSION_KEY, wrapper);
                result = wrapper.jcrSession;
            }
            wrapper.refresh(httpSession, refreshJcrSession);
        } catch (LoginException e) {
            e.printStackTrace();
            throw new JCRConnectionException("Failed to login to repository");
        } catch (RepositoryException e) {
            e.printStackTrace();
            throw new JCRConnectionException("Failed to initialize repository");
        }
        return result;
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

        public boolean jcrSessionOK() {
            
            // isLive may fail for instance if the repository was restarted
            try {
                return (jcrSession != null) && jcrSession.isLive();
            }    
            catch (Exception ignore) {
                // do not even log something as will try to reconnect 
                return false;
            }
        }

        public void valueBound(HttpSessionBindingEvent event) {
            logger.debug("Bound JCR session to HTTP session");
        }

        public void valueUnbound(HttpSessionBindingEvent event) {
            // logout itself may fail (connection lost) 
            try {
                jcrSession.logout();
            } catch (Exception ignore) {
            }
            
            logger.debug("Unbound JCR session from HTTP session");
        }

        void refresh(HttpSession httpSession, boolean refreshJcrSession) throws RepositoryException {
            if(refreshJcrSession) {
                jcrSession.refresh(false);
            }
            
            //if(httpSession.getMaxInactiveInterval() >= 0 &&
            //   System.currentTimeMillis() - lastRefreshed >= httpSession.getMaxInactiveInterval()*1000L) {            
                //jcrSession.refresh(false); // NOTICE: never keep changes
            //    logger.info("refreshing session");
            //    lastRefreshed = System.currentTimeMillis();
            //}
        }
    }
}
