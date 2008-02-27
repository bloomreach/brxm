package org.hippoecm.hst;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class MappingResponseWrapper extends HttpServletResponseWrapper {
    private static final Logger logger = LoggerFactory.getLogger(MappingResponseWrapper.class);

    private Context context;
    private HttpServletRequest request;

    public MappingResponseWrapper(Context context, HttpServletRequest req, HttpServletResponse res) {
        super(res);
        this.context = context;
        this.request = req;
    }

    @Override
    public String encodeURL(String url) {
        return super.encodeURL(reverseURL(request.getContextPath(), url));
    }

    
    @Override
    public String encodeRedirectURL(String url) {
        return super.encodeURL(reverseURL(request.getContextPath(), url));
    }
    
    boolean redirectRepositoryDocument(String mapLocation, String documentPath, boolean include)
            throws RepositoryException, IOException, ServletException {

        // Strip any leading slashes
        while (documentPath.startsWith("/")) {
            documentPath = documentPath.substring(1);
        }
        while (mapLocation.startsWith("/")) {
            mapLocation = mapLocation.substring(1);
        }

        // Fetch the requested document node
        Node documentNode = null;
        Session session = JCRConnector.getJCRSession(request.getSession());
        if (session.getRootNode().hasNode(documentPath)) {
            documentNode = session.getRootNode().getNode(documentPath);
        } else {
            return false;
        }

        // If the requested document node is a handle go one level deeper.
        try {
            if (documentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                documentNode = documentNode.getNode(documentNode.getName());
            }
        } catch (PathNotFoundException ex) {
            // deliberate ignore
        } catch (ValueFormatException ex) {
            // deliberate ignore
        }
        documentPath = documentNode.getPath();

        // Locate the display node associated with the document node.
        Node displayNode = null;
        if (documentNode.isNodeType(HSTNodeTypes.NT_HST_PAGE)) {
            displayNode = documentNode;
        }
        for (NodeIterator iter = session.getRootNode().getNode(mapLocation).getNodes(); iter.hasNext();) {
            Node matchNode = iter.nextNode();
            try {
                if (documentNode.isNodeType(matchNode.getProperty(HSTNodeTypes.HST_NODETYPE).getString())) {
                    displayNode = matchNode.getNode(HSTNodeTypes.HST_DISPLAYPAGE);
                    break;
                }
            } catch (PathNotFoundException ex) {
                // deliberate ignore
            } catch (ValueFormatException ex) {
                // deliberate ignore
            }
        }
        if (displayNode == null) {
            return false;
        }

        context.setPath(documentPath);
        
        // The (jsp) page that will be used  
        String pageFile = displayNode.getProperty(HSTNodeTypes.HST_PAGEFILE).getString();

        // Forward/include the request to that jsp page
        RequestDispatcher dispatcher = request.getRequestDispatcher(pageFile);
        if (include) {
            dispatcher.include(request, this);
        } else {
            dispatcher.forward(request, this);
        }
        return true;
    }
    
    private String reverseURL(String contextPath, String url) {
        /* Something like the following may be more appropriate, but the
         * current sequence is functional enough
         *   Session session = context.session;
         *   Query query = session.getWorkspace().getQueryManager().createQuery(
         *     "select jcr:name from hst:page where hst:pageFile = '"+url+"' and path.startsWith("+urlbasepath+")";
         *   QueryResult result = query.execute();
         *   url = result.getNodes().getNode().getPath();
         */
        String reversedUrl = url;
        try {
            if (url.startsWith(context.getURLBasePath())) {
                reversedUrl = contextPath + url.substring(context.getURLBasePath().length());
            } else {
                String reversedPath = url;
                while (reversedPath.startsWith("/")) {
                    reversedPath = reversedPath.substring(1);
                }

                Session jcrSession = JCRConnector.getJCRSession(request.getSession());
                if (jcrSession.getRootNode().hasNode(reversedPath)) {
                    reversedUrl = contextPath + "/" + reversedPath;
                }
            }

        } catch (RepositoryException ex) {
            logger.error("reverseURL", ex);
        }
        return reversedUrl;
    }
}
