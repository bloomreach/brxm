/*
 * Copyright 2007 Hippo.
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
package org.hippoecm.hst;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

public class RewriteFilter
    implements Filter
{
    private ServletContext servletContext;
    private FilterConfig filterConfig;
    private String urlbasehome;
    private String urlbasepath;

    public void init(FilterConfig filterConfig)
        throws ServletException
    {
        this.filterConfig = filterConfig;
        this.servletContext = filterConfig.getServletContext();
        urlbasehome = filterConfig.getInitParameter("urlbasehome");
        urlbasepath = filterConfig.getInitParameter("urlbasepath");
    }

    public void destroy()
    {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain)
        throws IOException, ServletException
    {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String documentPath;

        System.err.println("Rewrite handling:");
        System.err.println("  "+req.getRequestURI());
        System.err.println("  "+req.getRequestURL());
        System.err.println("  "+req.getPathInfo());
        System.err.println("  "+req.getPathTranslated());

        String pathAfterContext = req.getRequestURI().substring(req.getContextPath().length());
        int semicolonIdx = pathAfterContext.indexOf(';');
        if (semicolonIdx != -1) {
            pathAfterContext = pathAfterContext.substring(0, semicolonIdx);
        }
        if(pathAfterContext == "" || pathAfterContext.endsWith("/")) {
            pathAfterContext += urlbasehome;
        }
        System.err.println("Starting with "+pathAfterContext);
        documentPath = urlbasepath;
        if(documentPath == null) {
                documentPath = "/";
        }
        if(!documentPath.endsWith("/") && !pathAfterContext.startsWith("/"))
            documentPath = documentPath + "/" + pathAfterContext;
        else
            documentPath = documentPath + pathAfterContext;

        Context context = new Context();
        context.setRepository(servletContext.getInitParameter("repository-address"));
        context.setURLPageBase(urlbasepath);
        req.setAttribute("context", context); // should be req.setAttribute("org.hippoecm.hst.context", context);

        try {
            String mapLocation = filterConfig.getInitParameter("urlmapping");
            boolean success = redirectRepositoryDocument(req, res, context, mapLocation, documentPath, false);
            if(!success) {
                filterChain.doFilter(req, res);
                return; // no action ALLOWED after this point.
            }
        } catch(RepositoryException ex) {
            throw new ServletException(ex);
        }
    }

    static boolean redirectRepositoryDocument(HttpServletRequest request, HttpServletResponse response,
                                              Context context, String mapLocation, String documentPath,
                                              boolean include)
        throws RepositoryException, IOException, ServletException
    {
        Node displayNode = null;
        Session session = context.session;

        System.err.println("redirectRepositoryDocument");

        while(documentPath.startsWith("/"))
            documentPath = documentPath.substring(1);
        System.err.println("Looking in repository at location "+documentPath);

        Node documentNode = null;
        if(session.getRootNode().hasNode(documentPath)) {
            documentNode = session.getRootNode().getNode(documentPath);
        }

        if(documentNode == null) {
            return false;
        }

        try {
            if(documentNode.isNodeType("hippo:handle")) {
                documentNode = documentNode.getNode(documentNode.getName());
            }
        } catch(PathNotFoundException ex) {
            // deliberate ignore
        } catch(ValueFormatException ex) {
            // deliberate ignore
        }
        documentPath = documentNode.getPath();
        System.err.println("Using document location "+documentPath+" "+documentNode.getPath());

        if(documentNode.isNodeType("hst:page")) {
            System.err.println("Page itself is page ");
            displayNode = documentNode;
        }

        while(mapLocation.startsWith("/")) {
            mapLocation = mapLocation.substring(1);
        }
        System.err.println("Using mapping location "+mapLocation);
        for(NodeIterator iter = session.getRootNode().getNode(mapLocation).getNodes(); iter.hasNext(); ) {
            Node matchNode = iter.nextNode();
            try {
                if(documentNode.isNodeType(matchNode.getProperty("hst:nodetype").getString())) {
                    displayNode = matchNode.getNode("hst:displaypage");
                    break;
                }
            } catch(PathNotFoundException ex) {
                // deliberate ignore
            } catch(ValueFormatException ex) {
                // deliberate ignore
            }
        }
        
        if(displayNode == null) {
            return false;
        }
        
        System.err.println("Using display definition from " + displayNode.getPath());
        
        context.setPath(documentPath);
        HttpServletResponse wrappedResponse = new RewriteHttpServletResponseWrapper(context, request, response);
        RequestDispatcher dispatcher = request.getRequestDispatcher(displayNode.getProperty("hst:pageFile").getString());
        if(include)
            dispatcher.include(request, wrappedResponse);
        else
            dispatcher.forward(request, wrappedResponse);
        return true;
    }

    private static String reverseURL(Context context, String contextPath, String url) {
        String urlbasepath = context.getURLPageBase();
        /* Something like the following may be more appropriate, but the
         * current sequence is functional enough
         *   Session session = context.session;
         *   Query query = session.getWorkspace().getQueryManager().createQuery(
         *     "select jcr:name from hst:page where hst:pageFile = '"+url+"' and path.startsWith("+urlbasepath+")";
         *   QueryResult result = query.execute();
         *   url = result.getNodes().getNode().getPath();
         */
        System.err.print("Reversing URL "+url);
        try {
            if(url.startsWith(urlbasepath)) {
                url = contextPath + url.substring(urlbasepath.length());
            } else {
                String path = url;
                while(path.startsWith("/"))
                    path = path.substring(1);
                if(context.session.getRootNode().hasNode(path)) {
                    url = contextPath + "/" + path;
                }
            }
        } catch(RepositoryException ex) {
            System.err.println(ex.getClass().getName() + ": " + ex.getMessage());
            ex.printStackTrace(System.err);
        }
        System.err.println(" -> "+url);
        return url;
    }

    private static class RewriteHttpServletResponseWrapper extends HttpServletResponseWrapper {

        private Context context;
        private HttpServletRequest request;

        public RewriteHttpServletResponseWrapper(Context context, HttpServletRequest req, HttpServletResponse res) {
            super(res);
            this.request = req;
            this.context = context;
        }

        public String encodeURL(String url) {
            return super.encodeURL(reverseURL(context, request.getContextPath(), url));
        }
        public String encodeRedirectURL(String url) {
            return super.encodeURL(reverseURL(context, request.getContextPath(), url));
         }
    }

}
