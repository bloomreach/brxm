package org.hippoecm.hst.jcr;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.hippoecm.hst.core.HSTConfiguration;
import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRConnectorWrapper extends JCRConnector {
    private static final Logger log = LoggerFactory.getLogger(JCRConnectorWrapper.class);
    public static final String AUTHENTICATED_USER_SESSION_ATTRIBUTE ="authenticatedUserJcrSession";
    
    public static Session getJCRSession(HttpSession session) {
        Object userSession = session.getAttribute(AUTHENTICATED_USER_SESSION_ATTRIBUTE);
        if (userSession == null) {
            //anonymous session
            return getDefaultJCRSession(session);
        } else {
            Session jcrSession = (Session) userSession;
            if (!jcrSession.isLive()) {
                log.error("Authenticated JCR Session is not live");
                //return an 'anonymous' user session
                return getDefaultJCRSession(session);
            }
            return jcrSession;
        }
    }
    
    public static Session getTemplateJCRSession(HttpSession session) {
        return getDefaultJCRSession(session);
    }
    
    public static Session getJCRSession(HttpSession httpSession, String userName, String password) {
            Session result = null;
            try {
                ServletContext sc = httpSession.getServletContext();
                SessionWrapper wrapper = new SessionWrapper(HSTConfiguration.get(sc, HSTConfiguration.KEY_REPOSITORY_ADRESS),
                                             userName, password);
                //httpSession.setAttribute(JCR_SESSION_KEY, wrapper);
                result = wrapper.jcrSession;
                wrapper.refresh(httpSession);
            } catch (LoginException e) {
                log.error("Failed to login to repository", e);
            } catch (RepositoryException e) {
                log.error("Failed to initialize repository", e);
            }
            return result;
        }
     
     private static Session getDefaultJCRSession(HttpSession httpSession) {
         return JCRConnector.getJCRSession(httpSession);
     }


    private static class SessionWrapper implements HttpSessionBindingListener {
        Session jcrSession;
        long lastRefreshed;
    
        SessionWrapper(String location, String username, String password) throws LoginException, RepositoryException {
        log.info("connecting to repository at " + location + " as " + username);
    
            HippoRepositoryFactory.setDefaultRepository(location);
            HippoRepository repository = HippoRepositoryFactory.getHippoRepository();
            try {
                jcrSession = repository.login(username, (password != null ? password.toCharArray() : null));
    
                log.info("logged in as " + username);
            } catch(LoginException ex) {
                log.warn("login as " + username + " failed, trying as anonymous.");
                jcrSession = repository.login();
                log.info("logged in as anonymous");
            }
            lastRefreshed = System.currentTimeMillis();
        }
    
        public void valueBound(HttpSessionBindingEvent event) {
            log.debug("Bound JCR session to HTTP session");
        }
    
        public void valueUnbound(HttpSessionBindingEvent event) {
            jcrSession.logout();
            log.debug("Unbound JCR session from HTTP session");
        }
    
        void refresh(HttpSession httpSession) throws RepositoryException {
            if(httpSession.getMaxInactiveInterval() >= 0 &&
               System.currentTimeMillis() - lastRefreshed >= httpSession.getMaxInactiveInterval()*1000L) {            
                //jcrSession.refresh(false); // NOTICE: never keep changes
                log.info("refreshing session");
                lastRefreshed = System.currentTimeMillis();
            }
        }
    }
}
