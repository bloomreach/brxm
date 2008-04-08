/*
 * Copyright 2008 Hippo.
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class URLMappingResponseWrapper extends HttpServletResponseWrapper {
    private static final Logger logger = LoggerFactory.getLogger(URLMappingResponseWrapper.class);

    private final Context context;
    private final HttpServletRequest request;
    private final URLPathTranslator urlPathTranslator;

    public URLMappingResponseWrapper(Context context, URLPathTranslator urlPathTranslator, HttpServletRequest req, HttpServletResponse res) {
        super(res);
        this.context = context;
        this.request = req;
        this.urlPathTranslator = urlPathTranslator;
    }

    @Override
    public String encodeURL(String url) {
        
        // url in this case is an existing documentPath
        
        Session jcrSession = JCRConnector.getJCRSession(request.getSession());
        String reversedURL = super.encodeUrl(this.urlPathTranslator.documentPathToURL(jcrSession, url));
        return urlEncode(reversedURL);
    }

    @Override
    public String encodeRedirectURL(String url) {

        // url in this case is an existing documentPath
        
        Session jcrSession = JCRConnector.getJCRSession(request.getSession());
        String reversedURL = super.encodeRedirectUrl(this.urlPathTranslator.documentPathToURL(jcrSession, url));
        return urlEncode(reversedURL);
    }
    
    public String mapRepositoryDocument(String documentPath, String mappingLocation)
        throws RepositoryException, IOException, ServletException {

        while (documentPath.startsWith("/")) {
            documentPath = documentPath.substring(1);
        }
        
        while (mappingLocation.startsWith("/")) {
            mappingLocation = mappingLocation.substring(1);
        }

        // check session
        Session session = JCRConnector.getJCRSession(request.getSession());
        
        if (session == null) {
            throw new ServletException("No JCR session to repository");
        }

        // fetch the requested document node
        if (!session.getRootNode().hasNode(documentPath)) {
            logger.debug("Cannot find node by path " + documentPath);
            return null;
        }   

        Node documentNode = session.getRootNode().getNode(documentPath);
 
        // if the requested document node is a handle go one level deeper.
        try {
            if (documentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                documentNode = documentNode.getNode(documentNode.getName());
            }
        } catch (PathNotFoundException ex) {
            // deliberate ignore
        } catch (ValueFormatException ex) {
            // deliberate ignore
        }

        // update path, [BvH] set path also if not directly a handle
        context.setRelativeLocation(documentNode.getPath());

        // locate the display node associated with the document node
        Node currentNode = documentNode;
        Node displayNode = null;
        
        while (currentNode != null) {
            
            if (currentNode.isNodeType(HSTNodeTypes.NT_HST_PAGE)) {
                displayNode = currentNode;
            }
            else {
                
                // find node by type in mappingLocation
                if (session.getRootNode().hasNode(mappingLocation)) {
                    
                    Node mappingLocationNode = session.getRootNode().getNode(mappingLocation);
                    for (NodeIterator iter = mappingLocationNode.getNodes(); iter.hasNext();) {
                        Node matchNode = iter.nextNode();
                        try {
                            Property prop = matchNode.getProperty(HSTNodeTypes.HST_NODETYPE);
                            if (currentNode.isNodeType(prop.getString())) {
                                displayNode = matchNode.getNode(HSTNodeTypes.HST_DISPLAYPAGE);
                                break;
                            }
                        } catch (PathNotFoundException ex) {
                            throw new ServletException(ex);
                        } catch (ValueFormatException ex) {
                            throw new ServletException(ex);
                        }
                    }
                }
            }
            
            // found
            if (displayNode != null) {
                break;
            }
            
            // stop at root node
            if (currentNode.getPath().equals("/")) {
                break;
            }
            
            // try parent
            currentNode = currentNode.getParent();
        }

        if (displayNode == null) {
            logger.debug("displayNode cannot be found by documentNode with path " + documentNode.getPath()
                        + " and mappingLocation " + mappingLocation);
            return null;
        }

        if (!displayNode.hasProperty(HSTNodeTypes.HST_PAGEFILE)) {
            logger.debug("displayNode with path " + displayNode.getPath() + " has no property " + HSTNodeTypes.HST_PAGEFILE);
            return null;
        }
    
        // return the page that will be used
        String pageFile = displayNode.getProperty(HSTNodeTypes.HST_PAGEFILE).getString();

        logger.debug("mapped document path " + documentPath + " to page " + pageFile +
                        ", mappingLocation is " + mappingLocation);
        return pageFile;
    }
    
    private String urlEncode(String url) {
        
        // encode the url parts between the slashes: the slashes are or not to be 
        // encoded into %2F because the path won't be valid anymore
        String[] parts = url.split("/");
        
        String encoded = url.startsWith("/") ? "/" : "";
        
        try {
            for (int i = 0; i < parts.length; i++) {

                // part is empty if url starts with /
                if (parts[i].length() > 0) {
                    encoded += URLEncoder.encode(parts[i], ContextFilter.ENCODING_SCHEME);

                    if (i < (parts.length - 1)) {
                        encoded += "/";
                    }
                }   
            }
        
            if (url.endsWith("/")) {
                encoded += "/";
            }

            return encoded;
        }
        catch (UnsupportedEncodingException uee) {
            throw new IllegalStateException("Unsupported encoding scheme " + ContextFilter.ENCODING_SCHEME, uee);
        }
    }
}
