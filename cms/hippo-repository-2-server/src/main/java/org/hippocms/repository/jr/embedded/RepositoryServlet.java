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

package org.hippocms.repository.jr.embedded;

import java.rmi.Remote;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.StringTokenizer;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.net.MalformedURLException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.hippocms.repository.jr.embedded.HippoRepository;
import org.hippocms.repository.jr.embedded.HippoRepositoryFactory;
import org.hippocms.repository.jr.servicing.server.ServerServicingAdapterFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Value;

public class RepositoryServlet extends HttpServlet {
    protected final Logger log = LoggerFactory.getLogger(HippoRepository.class);
    HippoRepository repository;
    String bindingAddress;
    String storageLocation;

    public RepositoryServlet() {
        storageLocation = null;
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            storageLocation = config.getInitParameter("repository-directory");
            if (storageLocation != null && !storageLocation.equals("")) {
                if (!storageLocation.startsWith("/") && !storageLocation.startsWith("file:")) {
                    storageLocation = config.getServletContext().getRealPath(storageLocation);
                    if (storageLocation == null)
                        throw new ServletException("Cannot determin repository location " + storageLocation);
                }
                repository = HippoRepositoryFactory.getHippoRepository(storageLocation);
            } else {
                storageLocation = null;
                repository = HippoRepositoryFactory.getHippoRepository();
            }
            HippoRepositoryFactory.setDefaultRepository(repository);
            Remote remote = new ServerServicingAdapterFactory().getRemoteRepository(repository.repository);
            System.setProperty("java.rmi.server.useCodebaseOnly", "true");
            bindingAddress = config.getInitParameter("repository-address");
            if (bindingAddress == null || bindingAddress.equals(""))
                bindingAddress = config.getServletContext().getInitParameter("repository-address");
            if (bindingAddress == null || bindingAddress.equals(""))
                bindingAddress = "rmi://localhost:1099/jackrabbit.repository";
            try {
                Context ctx = new InitialContext();
                ctx.rebind(bindingAddress, remote);
                log.info("Server " + config.getServletName() + " available in context on " + bindingAddress);
            } catch (NamingException ex) {
                log.error("Cannot bind to address " + bindingAddress, ex);
                System.err.println("NamingException: " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
            /*
             try {
             Naming.rebind(bindingAddress, remote);
             log.info("Server " + config.getServletName() + " available using RMI on " + bindingAddress);
             } catch (MalformedURLException ex) {
             log.error("Cannot bind to address " + bindingAddress, ex);
             System.err.println("MalformedURLException: " + ex.getMessage());
             ex.printStackTrace(System.err);
             } catch (RemoteException ex) {
             log.error("Generic remoting exception ", ex);
             System.err.println("RemoteException: " + ex.getMessage());
             ex.printStackTrace(System.err);
             }
             */
        } catch (RemoteException ex) {
            log.error("Generic remoting exception ", ex);
            System.err.println("RemoteException: " + ex.getMessage());
            ex.printStackTrace(System.err);
        } catch (RepositoryException ex) {
            log.error("Error while setting up JCR repository: ", ex);
            System.err.println("RepositoryException: " + ex.getMessage());
            ex.printStackTrace(System.err);
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
                    writer.println("    <li type=\"circle\"><a href=\"" + req.getContextPath() + req.getServletPath()
                            + "/" + child.getPath() + "/" + "\">" + child.getName() + "</a>");
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
