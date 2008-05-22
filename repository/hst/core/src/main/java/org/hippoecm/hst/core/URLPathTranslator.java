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
package org.hippoecm.hst.core;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Object that can translate an URL to a repository path (a.k.a. location) 
 * and vice versa. Used in the URL mapping functionality.
 */
public class URLPathTranslator {

    private static final Logger logger = LoggerFactory.getLogger(URLPathTranslator.class);

    private final String contextPath;
    private final String urlBasePath;
    private final String repositoryBaseLocation;

    /**
     * Constructor.
     */
    public URLPathTranslator(final String contextPath, final String urlBasePath, final String repositoryBaseLocation) {
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

        logger.debug("url " + url + " to documentPath " + path);
        return path;
    }

    /**
     * Translate a path in the repository to an URL.
     * 
     * @param documentPath repository path that should start with the base location
     *      as present in this context.  
     */
    public String documentPathToURL(final String documentPath) {
        return this.documentPathToURL(null, documentPath);
    }
    
    /**
     * Translate a path in the repository to an URL, with the possibility to enter an 
     * absolute path, that is check using the jcrSession parameter.  
     * 
     * @param jcrSession JCR session for checking an absolute documentPath parameter.   
     * @param documentPath repository path that either starts with the base location
     *      as present in this context, or is an absolute path.  
     */
    public String documentPathToURL(final Session jcrSession, final String documentPath) {

        String url;

        if (documentPath.startsWith(this.repositoryBaseLocation)) {
            // replace repositoryBaseLocation by urlBasePath 
            url = this.contextPath + this.urlBasePath + documentPath.substring(this.repositoryBaseLocation.length());
        } 
        else {

            // get path absolute to the repositoryBaseLocation
            String absolutePath = documentPath;
            if (!absolutePath.startsWith("/")) {
                absolutePath = "/" + absolutePath;
            }
            
            // if a JCR session is present, we can do a check
            if (jcrSession != null) {
                
                // path to check must not start with "/" 
                String checkPath = this.repositoryBaseLocation + absolutePath;
                while (checkPath.startsWith("/")) {
                    checkPath = checkPath.substring(1);
                }
                
                try {
                    if (!jcrSession.getRootNode().hasNode(checkPath)) {

                        // warn, the returned documentPath won't be valid!
                        logger.warn("Path '" + checkPath 
                                + "' does not represent an absolute node");
                    }
                } catch (RepositoryException re) {
                    throw new IllegalStateException("unexpected error getting node by path " 
                            + checkPath, re);
                }
            } 
            
            // reverse url
            url = this.contextPath + this.urlBasePath + absolutePath;
        }
        
        // UTF-8 encoding
        url = urlEncode(url);
        
        logger.debug("documentPath " + documentPath + " to url " + url);
        return url;
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
                    encoded += URLEncoder.encode(parts[i], URLMappingContextFilter.ENCODING_SCHEME);

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
            throw new IllegalStateException("Unsupported encoding scheme " + URLMappingContextFilter.ENCODING_SCHEME, uee);
        }
    }
}
