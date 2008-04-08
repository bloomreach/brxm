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

/**
 * Object that can encode a repository path to a url, used for the URL mapping
 * functionality to generated valid url's, so the reversed path. 
 *  
 * 
 * @author jhoffman
 */
package org.hippoecm.hst;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Object that can translate an URL to a repository path (a.k.a. location) 
 * and vice versa. Used in the URL mapping functionality.
 *  
 * @author jhoffman
 */
public class URLPathTranslator {

    private static final Logger logger = LoggerFactory.getLogger(URLPathTranslator.class);

    private final String contextPath;
    private final String urlBasePath;
    private final String repositoryBaseLocation;

    /**
     * Constructor.
     */
    URLPathTranslator(final String contextPath, final String urlBasePath,
            final String repositoryBaseLocation) {
        super();

        this.contextPath = contextPath;
        this.urlBasePath = urlBasePath;
        this.repositoryBaseLocation = repositoryBaseLocation;
    }

    /**
     * Translate an URL to a path in the repository.
     */
    public String urlToDocumentPath(final String url) {
        
        String path = url;
        
        // remove end /
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // remove contextPath if present
        if (path.startsWith(this.contextPath)) {
            path = path.substring(this.contextPath.length());
        }

        // remove urlBasePath if present
        if (path.startsWith(this.urlBasePath)) {
            path = path.substring(this.urlBasePath.length());
        }

        // prepend repositoryBaseLocation if not present
        if (!path.startsWith(this.repositoryBaseLocation)) {
            path = this.repositoryBaseLocation + path;
        }

        logger.debug("url " + url +  " to documentPath " + path);
        return path;
    }

    /**
     * Translate a path in the repository to an URL.
     */
    public String documentPathToURL(final Session jcrSession, final String documentPath) {
        
        // Something like the following may be more appropriate, but the
        //  current sequence is functional enough
        //    Session session = context.session;
        //    Query query = session.getWorkspace().getQueryManager().createQuery(
        //      "select jcr:name from hst:page where hst:pageFile = '"+url+"' and path.startsWith("+urlbasepath+")";
        //    QueryResult result = query.execute();
        //    url = result.getNodes().getNode().getPath();

        if (documentPath.startsWith(this.repositoryBaseLocation)) {
            // replace repositoryBaseLocation by urlBasePath 
            return this.contextPath + this.urlBasePath 
                    + documentPath.substring(this.repositoryBaseLocation.length());
        } else {
            
            try {
                // if documentPath actually matches a node path, we can construct the reversed url
                String path = documentPath;
                while (path.startsWith("/")) {
                    path = path.substring(1);
                }

                if (jcrSession.getRootNode().hasNode(path)) {
                    return this.contextPath + this.urlBasePath + "/" + path;
                }

                // error, the returned documentPath won't be valid!
                logger.warn("documentPath " + documentPath + " does not represent a node");
                return documentPath;
            } catch (RepositoryException re) {
                throw new IllegalStateException("unexpected error getting node by path " + documentPath, re);
            }
        }
    }
}
