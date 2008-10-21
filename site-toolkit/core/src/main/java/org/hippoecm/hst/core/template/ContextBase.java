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
package org.hippoecm.hst.core.template;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.slf4j.LoggerFactory;

public class ContextBase {
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ContextBase.class);
	private static final String DEFAULT_CONTEXT_NAME = "content";
	private static final String DEFAULT_CONTENT_LOCATION = "/content";
	
	private final String contextName;
	private Node contextRootNode;
	private final Session jcrSession;
	
	public ContextBase(String contextName, String repositoryPath, HttpServletRequest request) throws PathNotFoundException, RepositoryException {
		this(contextName, repositoryPath, request, (Session)request.getAttribute(HSTHttpAttributes.JCRSESSION_MAPPING_ATTR));
	}
	
	public ContextBase (String contextName, String repositoryPath, HttpServletRequest request, Session session) throws PathNotFoundException, RepositoryException {
		this.contextName = contextName;	
		this.jcrSession = session;
    		
		String relativePath = stripFirstSlash(repositoryPath);
		
		log.info("constructor() with repositoryPath= " + relativePath);
		if (relativePath.trim().length() == 0) {
			this.contextRootNode = session.getRootNode();
		} else {
			this.contextRootNode = session.getRootNode().getNode(stripFirstSlash(repositoryPath));
		}
	}

	public String getContextName() {
		return contextName;
	}

	public Node getContextRootNode() {
		return contextRootNode;
	}
	
	public Session getSession() {
		return jcrSession;
	}
	 
	public ContextBase getRelativeContextBase(String name, String relativePath, HttpServletRequest request) throws PathNotFoundException, RepositoryException {
	    String contextRootPath = stripFirstSlash(contextRootNode.getPath());
	    String relativeContextRootPath = contextRootPath + (contextRootPath.endsWith("/") ? "" : "/") + stripFirstSlash(relativePath);
	    log.info("create relativeContextBase with path=" + relativeContextRootPath);
	    if (relativeContextRootPath.trim().length() == 0) {
	    	return new ContextBase(name, relativeContextRootPath, request);
	    } else {
		    return new ContextBase(name, contextRootPath, request);
	    }  
	}
	
	public Node getRelativeNode(String path){
	    String strPath = "";
	    String relativePath = stripFirstSlash(path);
	    
	    if (relativePath == null) {
	        log.debug("Relative path is null, return null");
	        return null;
	    }
	    if (relativePath.length() == 0) {
            log.debug("Relative path is empty, return null");
            return null;
	    }
		try {
		    strPath = contextRootNode.getPath();
		    log.debug("Get RelativeNode with rootNode '{}' path={}", strPath, relativePath);
            return contextRootNode.getNode(relativePath);
        } catch (PathNotFoundException e) {
            log.debug("Node '{}' cannot be found in the repository below '{}'. Returning null", relativePath, strPath);
        } catch (RepositoryException e) {
            log.error("Unable to find node " + relativePath + " in the repository below '"+strPath+"': " + e.getMessage(), e);
        }
        return null;
	}
	
	public static ContextBase getDefaultContextBase(HttpServletRequest request) throws PathNotFoundException, RepositoryException {
		return new ContextBase(DEFAULT_CONTEXT_NAME, DEFAULT_CONTENT_LOCATION, request);
	}
	
	public static String stripFirstSlash(String s) {
	    if (s == null || "".equals(s)) {
	        return s;
	    }
		return s.startsWith("/") ? s.substring(1) : s;
	}
	
	@Override
    public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("(").append(this.getClass().getName()).append(":[");
		sb.append("contextName=").append(contextName);
		try {
			sb.append(",contextRootNode=").append(contextRootNode.getPath());
		} catch (RepositoryException e) {
			e.printStackTrace();
			sb.append(",contextRootNode=").append("UNAVAILABLE");
		}
		sb.append("]");
		return sb.toString();
	}

	
	
     
     
}
