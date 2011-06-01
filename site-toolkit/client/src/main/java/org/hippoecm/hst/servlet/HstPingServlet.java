/*
 *  Copyright 2009 Hippo.
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
package org.hippoecm.hst.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.hippoecm.hst.servlet.utils.SessionUtils;

/**
 * <p>A servlet that can be used to check if the repository is up-and-running. This
 * is especially useful for load balancer checks. The check does the following steps:</p>
 * <ul>
 *   <li>Check for static custom return message, else</li>
 *   <ul>
 *     <li>obtain the session from the specified session pool</li>
 *     <li>try to read the check node</li>
 *     <li>try to write to the repository if enabled</li>
 *     <li>logout and close jcr session</li>
 *   </ul>
 *   <li>logout and close http session</li>
 * </ul>
 * <p>On success the servlet prints "Ok" and returns a 200 status, on failure, the error is 
 * printed and a 500 (internal server error) status is returned.</p>
 * <p>In case the custom message is provided, a service unavailable error (503) is returned</p>
 * 
 * <p>To enable the servlet add the following to your web.xml</p>
 * <code><![CDATA[
    <servlet>
      <servlet-name>HstPingServlet</servlet-name>
      <servlet-class>org.hippoecm.repository.HstPingServlet</servlet-class>
      <init-param>
        <param-name>check-pool</param-name>
        <param-value>default</param-value>
      </init-param>
      <init-param>
        <param-name>check-node</param-name>
        <param-value>content/documents</param-value>
      </init-param>
      <init-param>
        <param-name>write-check-enable</param-name>
        <param-value>false</param-value>
      </init-param>
      <init-param>
        <param-name>write-check-node</param-name>
        <param-value>pingcheck</param-value>
      </init-param>
      <!-- enable while doing upgrades
        init-param>
        <param-name>custom-message</param-name>
        <param-value>Down for upgrade</param-value>
      </init-param -->
    </servlet>
    <servlet-mapping>
      <servlet-name>HstPingServlet</servlet-name>
      <url-pattern>/ping/*</url-pattern>
    </servlet-mapping>
 * ]]></code>
 *
 */
public class HstPingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /** Servlet parameters */
    private static final String POOL_PARAM = "check-pool";
    private static final String NODE_PARAM = "check-node";
    private static final String WRITE_ENABLE_PARAM = "write-check-enable";
    private static final String WRITE_PATH_PARAM = "write-check-path";
    private static final String CUSTOM_MESSAGE_PARAM = "custom-message";

    /** Default values */
    private static final String DEFAULT_POOL = "default";
    private static final String DEFAULT_NODE = "content/documents";
    private static final String DEFAULT_WRITE_ENABLE = "false";
    private static final String DEFAULT_WRITE_PATH = "pingcheck";

    /** Running config */
    private String checkPool;
    private String checkNode;
    private String customMessage;
    private String writeTestPath;
    private boolean writeTestEnabled = false;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        checkNode = makePathRelative(getParameter(config, NODE_PARAM, DEFAULT_NODE));
        writeTestPath = makePathRelative(getParameter(config, WRITE_PATH_PARAM, DEFAULT_WRITE_PATH));
        writeTestEnabled = isTrueOrYes(getParameter(config, WRITE_ENABLE_PARAM, DEFAULT_WRITE_ENABLE));
        customMessage = getParameter(config, CUSTOM_MESSAGE_PARAM, null);
        checkPool = getParameter(config, POOL_PARAM, DEFAULT_POOL);
    }

    private String getParameter(ServletConfig config, String paramName, String defaultValue) {
        String initValue = config.getInitParameter(paramName);
        String contextValue = config.getServletContext().getInitParameter(paramName);

        if (isNotNullAndNotEmpty(initValue)) {
            return initValue;
        } else if (isNotNullAndNotEmpty(contextValue)) {
            return contextValue;
        } else {
            return defaultValue;
        }
    }

    private boolean isNotNullAndNotEmpty(String s) {
        return (s != null && s.length() != 0);
    }

    private String makePathRelative(String path) {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    private boolean isTrueOrYes(String s) {
        return ("true".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s));
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        int resultStatus = HttpServletResponse.SC_OK;
        String resultMessage = "OK - Repository online and accessible.";
        Exception exception = null;

        if (hasCustomMessage()) {
            resultMessage = "CUSTOM - " + customMessage;
            resultStatus = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
        } else {
            if (writeTestEnabled) {
                resultMessage = "OK - Repository online, accessible and writable.";
            }
            try {
                doRepositoryChecks(req);
            } catch (PingException e) {
                resultStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                resultMessage = e.getMessage();
                exception = e;
            } catch (RuntimeException e) {
                resultStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                resultMessage = "FAILURE - Serious problem with the ping servlet. Might have lost repository access: "
                        + e.getClass().getName() + ": " + e.getMessage();
                exception = e;
            }
        }

        res.setContentType("text/plain");
        res.setStatus(resultStatus);
        PrintWriter writer = res.getWriter();
        writer.println(resultMessage);
        if (exception != null) {
            exception.printStackTrace(writer);
        }
        closeHttpSession(req);
    }

    private boolean hasCustomMessage() {
        return (customMessage != null);
    }

    private void closeHttpSession(HttpServletRequest req) {
        if (req != null) {
            // close open session
            HttpSession httpSession = req.getSession(false);
            if (httpSession != null) {
                httpSession.invalidate();
            }
        }
    }

    private synchronized void doRepositoryChecks(HttpServletRequest req) throws PingException {
        Session session = null;
        try {
            session = obtainSession(req);
            doReadTest(session);
            doWriteTestIfEnabled(session);
        } finally {
            closeSession(req, session);
        }
    }

    private Session obtainSession(HttpServletRequest req) throws PingException {
        try {
            Session session = SessionUtils.getPooledSession(req, checkPool);
            if (session == null) {
                throw new PingException("FAILURE - HST is not available to provide session");
            }
            return session;
        } catch (RepositoryException e) {
            String msg = "FAILURE - Problem obtaining session from repository in ping servlet";
            throw new PingException(msg, e);
        }
    }

    private void closeSession(HttpServletRequest req, Session session) {
        SessionUtils.releaseSession(req, session);
    }

    private void doReadTest(Session session) throws PingException {
        String msg;
        try {
            if (checkNode.length() == 0) {
                session.getRootNode();
            } else {
                session.getRootNode().getNode(checkNode);
            }
        } catch (PathNotFoundException e) {
            msg = "FAILURE - Path for node to lookup '" + checkNode + "' is not found by ping servlet. ";
            throw new PingException(msg, e);
        } catch (RepositoryException e) {
            msg = "FAILURE - Could not obtain a node, which is at this point unexpected since we already have a connection."
                    + "Maybe we lost the connection to the repository.";
            throw new PingException(msg, e);
        }
    }

    private void doWriteTestIfEnabled(Session session) throws PingException {
        if (writeTestEnabled) {
            doWriteTest(session);
        }
    }

    private void doWriteTest(Session session) throws PingException {
        try {
            Node writePath = getOrCreateWriteNode(session);
            writePath.setProperty("lastcheck", Calendar.getInstance());
            writePath.save();
        } catch (RepositoryException e) {
            String msg = "FAILURE - Error during write test. There could be an issue with the (connection to) the storage.";
            throw new PingException(msg, e);
        }
    }

    private Node getOrCreateWriteNode(Session session) throws PingException {
        Node path = getOrCreateWritePath(session);
        String clusterId = getClusterNodeId();
        try {
            if (path.hasNode(clusterId)) {
                return path.getNode(clusterId);
            } else {
                Node node = path.addNode(clusterId);
                session.save();
                return node;
            }
        } catch (RepositoryException e) {
            String msg = "FAILURE - Could not obtain the write test node '" + writeTestPath + "/" + clusterId + "'.";
            throw new PingException(msg, e);
        }
    }

    private Node getOrCreateWritePath(Session session) throws PingException {
        Node path;
        try {
            if (session.getRootNode().hasNode(writeTestPath)) {
                path = session.getRootNode().getNode(writeTestPath);
            } else {
                path = session.getRootNode().addNode(writeTestPath);
                session.save();
            }
            return path;
        } catch (RepositoryException e) {
            String msg = "FAILURE - Could not obtain the write path node '" + writeTestPath + "'.";
            throw new PingException(msg, e);
        }
    }

    private String getClusterNodeId() {
        String id = System.getProperty("org.apache.jackrabbit.core.cluster.node_id");
        if (id == null || id.length() == 0) {
            return "default";
        }
        return id;
    }

    /**
     * Internal Exception class to be used internally to communicate the exception during the ping of the repository
     */
    private static final class PingException extends Exception {
        private static final long serialVersionUID = 1L;

        private PingException(String s) {
            super(s);
        }

        private PingException(String s, Exception e) {
            super(s, e);
        }
    }
}
