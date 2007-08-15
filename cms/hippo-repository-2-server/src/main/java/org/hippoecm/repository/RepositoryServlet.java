/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hippoecm.repository;

import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.repository.servicing.ServicingNode;
import org.hippoecm.repository.servicing.server.ServerServicingAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryServlet extends HttpServlet {

    protected final Logger log = LoggerFactory.getLogger(HippoRepository.class);

    /** Parameter name of the repository storage directory */
    public final static String REPOSITORY_ADDRESS_PARAM = "repository-directory";

    /** Parameter name of the binging address */
    public final static String REPOSITORY_BINDING_PARAM = "repository-address";

    /** Parameter name of the repository config file */
    public final static String REPOSITORY_CONFIG_PARAM = "repository-config";

    /** Default binding address for server */
    public final static String DEFAULT_BINDING_ADDRESS = "rmi://localhost:1099/jackrabbit.repository";

    /** System property for overriding the repostiory config file */
    public final static String SYSTEM_SERVLETCONFIG_PROPERTY = "repo.servletconfig";

    /** Default config file */
    public final static String DEFAULT_REPOSITORY_CONFIG = "repository.xml";

    HippoRepository repository;
    String bindingAddress;
    String storageLocation;
    String repositoryConfig;

    public RepositoryServlet() {
        storageLocation = null;
    }

    private void parseInitParameters(ServletConfig config) throws ServletException {
        findStorageLocation(config);
        findBindingAddress(config);
        findRepositoryConfig(config);
    }

    /**
     * Try to extract the binding address from the config or use the DEFAULT_BINDING_ADDRESS
     * @param config
     */
    private void findBindingAddress(ServletConfig config) {
        // try to get bind address from the config or servletContext
        bindingAddress = config.getInitParameter(REPOSITORY_BINDING_PARAM);
        if (bindingAddress == null || bindingAddress.equals("")) {
            // fall back to global context setting
            bindingAddress = config.getServletContext().getInitParameter(REPOSITORY_BINDING_PARAM);
        }

        // still got nothing, use default
        if (bindingAddress == null || bindingAddress.equals("")) {
            bindingAddress = DEFAULT_BINDING_ADDRESS;
        }
    }

    /**
     * 
     * @param config
     * @throws ServletException
     */
    private void findStorageLocation(ServletConfig config) throws ServletException {
        storageLocation = config.getInitParameter("repository-directory");

        // basic sanity
        if (storageLocation == null) {
            return;
        }
        if ("".equals(storageLocation)) {
            storageLocation = null;
            return;
        }

        // absolute path
        //if (storageLocation.startsWith("/") || storageLocation.startsWith("file:")) {
        //    return;
        //}

        // try to parse the path
        storageLocation = config.getServletContext().getRealPath(storageLocation);
        if (storageLocation == null) {
            throw new ServletException("Cannot determin repository location "
                    + config.getInitParameter("repository-directory"));
        }
    }

    /**
     * Try to extract the repository config file from the config or use the DEFAULT_REPOSITORY_CONFIG
     * @param config
     */
    private void findRepositoryConfig(ServletConfig config) {
        // try to get repository config file name from the config or servletContext
        repositoryConfig = config.getInitParameter(REPOSITORY_CONFIG_PARAM);
        if (repositoryConfig == null || repositoryConfig.equals("")) {
            // fall back to global context setting
            repositoryConfig = config.getServletContext().getInitParameter(REPOSITORY_CONFIG_PARAM);
        }

        // still got nothing, use default
        if (repositoryConfig == null || repositoryConfig.equals("")) {
            repositoryConfig = DEFAULT_REPOSITORY_CONFIG;
        }
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        parseInitParameters(config);
        System.setProperty(SYSTEM_SERVLETCONFIG_PROPERTY, repositoryConfig);

        try {
            if (storageLocation == null) {
                repository = HippoRepositoryFactory.getHippoRepository();
            } else {
                repository = HippoRepositoryFactory.getHippoRepository(storageLocation);
            }
            HippoRepositoryFactory.setDefaultRepository(repository);
            Remote remote = new ServerServicingAdapterFactory().getRemoteRepository(repository.repository);
            System.setProperty("java.rmi.server.useCodebaseOnly", "true");

            try {
                Context ctx = new InitialContext();
                ctx.rebind(bindingAddress, remote);
                log.info("Server " + config.getServletName() + " available in context on " + bindingAddress);
            } catch (NamingException ex) {
                log.error("Cannot bind to address " + bindingAddress, ex);
                throw new ServletException("NamingException: " + ex.getMessage());
            }

        } catch (RemoteException ex) {
            log.error("Generic remoting exception ", ex);
            throw new ServletException("RemoteException: " + ex.getMessage());
        } catch (RepositoryException ex) {
            log.error("Error while setting up JCR repository: ", ex);
            throw new ServletException("RepositoryException: " + ex.getMessage());
        }
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String path = req.getRequestURI();
        if (path.startsWith(req.getContextPath()))
            path = path.substring(req.getContextPath().length());
        if (path.startsWith(req.getServletPath()))
            path = path.substring(req.getServletPath().length());
        res.setStatus(HttpServletResponse.SC_OK);
        res.setContentType("text/html");
        PrintWriter writer = res.getWriter();
        writer.println("<html><body>");
        writer.println("  <h1>Hippo Repository Console</h1>");
        writer.println("  <h2>Request parameters</h2>");
        writer.println("    <table><tr><th>name</th><th>value</th></tr>");
        writer.println("    <tr><td>context path</td><td>" + req.getContextPath() + "</td></tr>");
        writer.println("    <tr><td>servlet path</td><td>" + req.getServletPath() + "</td></tr>");
        writer.println("    <tr><td>request uri</td><td>" + req.getRequestURI() + "</td></tr>");
        writer.println("    <tr><td>relative path</td><td>" + path + "</td></tr>");
        writer.println("    </table>");
        writer.println("  <h2>Referenced node</h2>");
        Session session = null;
        try {
            session = repository.login();
            Node node = session.getRootNode();
            if (path.startsWith("//")) {
                path = path.substring("//".length());
                if (path.equals("/") || path.equals("")) {
                    writer.println("Accessing root node");
                } else {
                    if (path.startsWith("/"))
                        path = path.substring(1);
                    writer.print("Accessing node <code>");
                    String currentPath = "";
                    String previousPath = "";
                    for (StringTokenizer pathElts = new StringTokenizer(path, "/"); pathElts.hasMoreTokens(); previousPath = currentPath) {
                        String pathElt = pathElts.nextToken();
                        currentPath += "/" + pathElt;
                        writer.print("<a href=\"" + req.getContextPath() + req.getServletPath() + "/"
                                + (previousPath.equals("") ? "/" : previousPath) + "\">/</a><a href=\""
                                + req.getContextPath() + req.getServletPath() + "/" + currentPath + "\">" + pathElt
                                + "</a>");
                        node = node.getNode(pathElt);
                    }
                    writer.println("</code>");
                }
                writer.println("    <ul>");
                for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                    Node child = iter.nextNode();
                    writer.print("    <li type=\"circle\"><a href=\"" + req.getContextPath() + req.getServletPath()
                            + "/" + child.getPath() + "/" + "\">");
                    if (child.hasProperty("hippo:count")) {
                        writer.print(((ServicingNode) child).getDisplayName() + " ["
                                + child.getProperty("hippo:count").getLong() + "]");
                    } else {
                        writer.print(((ServicingNode) child).getDisplayName());
                    }
                    writer.println("</a>");
                }
                for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
                    Property prop = iter.nextProperty();
                    writer.print("    <li type=\"disc\">");
                    writer.print(prop.getPath() + " [name=" + prop.getName() + "] = ");
                    if (prop.getDefinition().isMultiple()) {
                        Value[] values = prop.getValues();
                        writer.print("[ ");
                        for (int i = 0; i < values.length; i++) {
                            writer.print((i > 0 ? ", " : "") + values[i].getString());
                        }
                        writer.println(" ]");
                    } else {
                        writer.println(prop.getString());
                    }
                }
                writer.println("    </ul>");
            } else {
                writer.println("No node accessed, start browsing at the <a href=\"" + req.getContextPath()
                        + req.getServletPath() + "//\">root</a> node.");
            }
        } catch (RepositoryException ex) {
            writer.println("<p>Error while accessing the repository, exception reads as follows:");
            writer.println("<pre>" + ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(writer);
            writer.println("</pre>");
        } finally {
            if (session != null)
                session.logout();
        }
        writer.println("</body></html>");
    }

    public void destroy() {
        try {
            Context ctx = new InitialContext();
            ctx.unbind(bindingAddress);
        } catch (NamingException ex) {
            log.warn("Cannot unbind from address " + bindingAddress, ex);
        }
        repository.close();
    }
}
