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
package org.hippoecm.repository;

import java.io.IOException;
import java.io.PrintWriter;

import javax.jcr.LoginException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * A simple servlet that can be used to check if the repository is up-n-running. This
 * is especially useful for load balancer checks. The check does the following steps:
 * - obtain the repository with the connection string
 * - login to the default workspace with the specified username and password
 * - try to read the check nod
 * - logout and close session
 * On success the servlet prints "Ok" and returns a 200 status, on failure, the error is 
 * printed and a 500 (internal server error) status is returned.
 * 
 * To enable the servlet add the following to your web.xml
 * <code><![CDATA[
    <servlet>
      <servlet-name>PingServlet</servlet-name>
      <servlet-class>org.hippoecm.repository.PingServlet</servlet-class>
      <init-param>
        <param-name>repository-address</param-name>
        <param-value>rmi://localhost:1099/hipporepository</param-value>
      </init-param>
      <init-param>
        <param-name>check-username</param-name>
        <param-value>admin</param-value>
      </init-param>
      <init-param>
        <param-name>check-password</param-name>
        <param-value>admin</param-value>
      </init-param>
    </servlet>
    <servlet-mapping>
      <servlet-name>PingServlet</servlet-name>
      <url-pattern>/ping/*</url-pattern>
    </servlet-mapping>
 * ]]></code>
 *
 */
public class PingServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    /** The repository address parameter */
    private static final String REPOSITORY_ADDRESS_PARAM = "repository-address";

    /** The username parameter */
    private static final String USERNAME_PARAM = "check-username";

    /** The password parameter */
    private static final String PASSWORD_PARAM = "check-password";

    /** The check node parameter */
    private static final String NODE_PARAM = "check-node";

    /** The default repository connection string */
    private static final String DEFAULT_REPOSITORY_ADDRESS = "rmi://localhost:1099/hipporepository";

    /** The default user */
    private static final String DEFAULT_USERNAME = "admin";

    /** The default password */
    private static final String DEFAULT_PASSWORD = "admin";

    /** The default check node */
    private static final String DEFAULT_NODE = "content/documents";

    private String repositoryLocation;
    private String username;
    private String password;
    private String checkNode;

    public PingServlet() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        repositoryLocation = getParameter(config, REPOSITORY_ADDRESS_PARAM, DEFAULT_REPOSITORY_ADDRESS);
        username = getParameter(config, USERNAME_PARAM, DEFAULT_USERNAME);
        password = getParameter(config, PASSWORD_PARAM, DEFAULT_PASSWORD);
        checkNode = getParameter(config, NODE_PARAM, DEFAULT_NODE);
        if (checkNode.startsWith("/")) {
            checkNode = checkNode.substring(1);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        int resultStatus = HttpServletResponse.SC_OK;
        String resultMessage = "OK - Repository online and accessible";
        try {
            findNodeInRepository();
        } catch (PingException e) {
            resultStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            resultMessage = e.getMessage();
        } catch (RuntimeException e) {
            resultStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            resultMessage = "FAILURE - Serious problem with the ping servlet. Might have lost repository access: "
                    + e.getClass().getName() + ": " + e.getMessage();
        }

        res.setContentType("text/plain");
        res.setStatus(resultStatus);
        PrintWriter writer = res.getWriter();
        writer.println(resultMessage);

        closeHttpSession(req);
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    /**
     * Reads a node from the repository and throws an exception if the repository cannot be found, we cannot obtain
     * a session of the configured path cannot be found.
     *
     * @throws PingException when the repository cannot be opened
     */
    private void findNodeInRepository() throws PingException {
        HippoRepository hippoRepository;
        try {
            hippoRepository = HippoRepositoryFactory.getHippoRepository(repositoryLocation);
        } catch (RepositoryException e) {
            String msg = "FAILURE - Problem obtaining repository connection in ping servlet : Is the property"
                    + " repository-address configured as a context-param?";
            throw new PingException(msg, e);
        }
        Session session = obtainSession(hippoRepository);
        if (session.isLive()) {
            lookupNode(session);
        }
        session.logout();

    }

    /**
     * Logs in to the provided repository and returns the session
     * @param hippoRepository HippoRepository used to obtain a session
     * @throws PingException thrown when the provided credentials cannot be used to login to the repository
     * @return Session that we logged in to with the provided credentials
     */
    private Session obtainSession(HippoRepository hippoRepository) throws PingException {
        try {
            return hippoRepository.login(username, password.toCharArray());
        } catch (LoginException e) {
            String msg = "FAILURE - Wrong credentials for obtaining session from repository in ping servlet : " + ""
                    + "Are the 'check-username' and check-password configured as an init-param or context-param?";
            throw new PingException(msg, e);
        } catch (RepositoryException e) {
            String msg = "FAILURE - Problem obtaining session from repository in ping servlet : Are the 'check-username' and"
                    + " check-password configured as an init-param or context-param?";
            throw new PingException(msg, e);
        }
    }

    /**
     * Use the provided session to lookup a node with the configured path
     *
     * @param session Session to use for obtaining the node
     * @throws PingException Exception thrown when the path for the node cannot be found
     */
    private void lookupNode(Session session) throws PingException {
        String msg;
        try {
            session.getRootNode().getNode(checkNode);
        } catch (PathNotFoundException e) {
            msg = "FAILURE - Path for node to lookup '" + checkNode + "' is not found by ping servelt. ";
            throw new PingException(msg, e);
        } catch (RepositoryException e) {
            msg = "FAILURE - Could not obtain a node, which is at this point unexpected since we already have a connection."
                    + "Maybe we lost the connection to the repository.";
            throw new PingException(msg, e);
        }
    }

    /**
     * Try to close the session if there is one associated with the request.
     * @param req The HttpServletRequest
     */
    private void closeHttpSession(HttpServletRequest req) {
        if (req != null) {
            // close open session
            HttpSession httpSession = req.getSession(false);
            if (httpSession != null) {
                httpSession.invalidate();
            }
        }
    }

    /**
     * Helper method for easily finding init parameters with a default value
     * @param config the servlet configuration
     * @param paramName the name of the parameter
     * @param defaultValue the default
     * @return the value of the parameter or the defaultValue if not set
     */
    private String getParameter(ServletConfig config, String paramName, String defaultValue) {
        String value = config.getInitParameter(paramName);
        if (value == null || value.equals("")) {
            value = config.getServletContext().getInitParameter(paramName);
        }
        if (value == null || value.equals("")) {
            value = defaultValue;
        }
        return value;
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
