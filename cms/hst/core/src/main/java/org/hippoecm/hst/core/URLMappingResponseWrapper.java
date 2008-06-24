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
package org.hippoecm.hst.core;

import java.io.IOException;

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

import org.hippoecm.hst.jcr.JCRConnector;
import org.hippoecm.hst.util.HSTNodeTypes;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URLMappingResponseWrapper extends HttpServletResponseWrapper {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

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
        String reversedURL = this.urlPathTranslator.documentPathToURL(jcrSession, url);
        return super.encodeUrl(reversedURL);
    }

    @Override
    public String encodeRedirectURL(String url) {

        // url in this case is an existing documentPath

        Session jcrSession = JCRConnector.getJCRSession(request.getSession());
        String reversedURL = this.urlPathTranslator.documentPathToURL(jcrSession, url);
        return super.encodeRedirectUrl(reversedURL);
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
        Node handleNode = null;

        // if the requested document node is a handle go one level deeper.
        try {
            if (documentNode.isNodeType(HippoNodeType.NT_HANDLE)) {
                handleNode = documentNode;
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

            // directly, via hst:page mixin
            if (currentNode.isNodeType(HSTNodeTypes.NT_HST_PAGE)) {
                displayNode = currentNode;
            }

            // directly on handle
            else if ((handleNode != null) && handleNode.isNodeType(HSTNodeTypes.NT_HST_PAGE)) {
                displayNode = handleNode;
            }

            else {

                // find node by type in mappingLocation
                if (session.getRootNode().hasNode(mappingLocation)) {

                    Node mappingLocationNode = session.getRootNode().getNode(mappingLocation);
                    for (NodeIterator iter = mappingLocationNode.getNodes(); iter.hasNext();) {
                        Node matchNode = iter.nextNode();
                        try {
                            Property prop = matchNode.getProperty(HSTNodeTypes.HST_NODETYPE);

                            // match by type?
                            if (currentNode.isNodeType(prop.getString())) {

                                // OK if hst:page has been set 
                                if (matchNode.isNodeType(HSTNodeTypes.NT_HST_PAGE)) {
                                    displayNode = matchNode;
                                    break;
                                }
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
}
