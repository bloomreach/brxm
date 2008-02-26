package org.hippoecm.hst;

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
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.hippoecm.repository.HippoRepository;
import org.hippoecm.repository.HippoRepositoryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCRConnector {
    public static final Logger logger = LoggerFactory.getLogger(JCRConnector.class);

    public static final String REPOSITORY_ADRESS_PARAM = "repository-address";
    public static final String REPOSITORY_USERNAME_PARAM = "repository-username";
    public static final String REPOSITORY_PASSWORD_PARAM = "repository-password";
    public static final String JCR_SESSION_KEY = "org.hippoecm.hst.JCRSESSION";

    static Session getJCRSession(HttpSession httpSession) {
        Session result = null;
        String location = httpSession.getServletContext().getInitParameter(REPOSITORY_ADRESS_PARAM);
        String username = httpSession.getServletContext().getInitParameter(REPOSITORY_USERNAME_PARAM);
        String password = httpSession.getServletContext().getInitParameter(REPOSITORY_PASSWORD_PARAM);
        try {
            SessionWrapper wrapper = (SessionWrapper) httpSession.getAttribute(JCR_SESSION_KEY);
            if (wrapper != null && wrapper.jcrSession.isLive()) {
                result = wrapper.jcrSession;
            } else {
                httpSession.removeAttribute(JCR_SESSION_KEY);
                wrapper = new SessionWrapper(location, username, password);
                //httpSession.setAttribute(JCR_SESSION_KEY, wrapper);
                result = wrapper.jcrSession;
            }
        } catch (LoginException e) {
            logger.error("Failed to login to repository", e);
        } catch (RepositoryException e) {
            logger.error("Failed to initialize repository", e);
        }
        return result;
    }
    
    static Item getItem(Session session, String path) throws RepositoryException {
        Node node = session.getRootNode();
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        String[] pathElts = path.split("/");
        for (int pathIdx = 0; pathIdx < pathElts.length && node != null; pathIdx++) {
            String relPath = pathElts[pathIdx];
            Map<String, String> conditions = null;
            if (relPath.contains("[") && relPath.endsWith("]")) {
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
            if (conditions == null || conditions.size() == 0) {
                if (pathIdx + 1 == pathElts.length && node.hasProperty(relPath)) {
                    try {
                        return node.getProperty(relPath);
                    } catch (PathNotFoundException ex) {
                        return null;
                    }
                } else if (node.hasNode(relPath)) {
                    try {
                        node = node.getNode(relPath);
                    } catch (PathNotFoundException ex) {
                        return null;
                    }
                } else
                    return null;
            } else {
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
                    if (node != null)
                        break;
                }
            }
        }
        return node;
    }

    private static class SessionWrapper implements HttpSessionBindingListener {
        Session jcrSession;

        SessionWrapper(String location, String username, String password) throws LoginException, RepositoryException {
            HippoRepositoryFactory.setDefaultRepository(location);
            HippoRepository repository = HippoRepositoryFactory.getHippoRepository();
            try {
                jcrSession = repository.login(username, password.toCharArray());
                logger.info("logged in as " + username);
            } catch(LoginException ex) {
                logger.warn("login as " + username + " failed, trying as anonymous.");
                jcrSession = repository.login();
                logger.info("logged in as anonymous");
            }
        }

        public void valueBound(HttpSessionBindingEvent event) {
            logger.debug("Bound JCR session to HTTP session");
        }

        public void valueUnbound(HttpSessionBindingEvent event) {
            jcrSession.logout();
            logger.debug("Unbound JCR session from HTTP session");
        }
    }
}
