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
package org.hippoecm.hst.core.context;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.exception.ContextBaseException;
import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.slf4j.LoggerFactory;

public class ContextBase {
	private static final org.slf4j.Logger log = LoggerFactory.getLogger(ContextBase.class);
	
	private Node contextRootNode;
	private final Session jcrSession;
	
	public ContextBase(String repositoryPath, Session jcrSession) throws ContextBaseException{
        String relativePath = stripFirstSlash(repositoryPath);
        this.jcrSession = jcrSession;
        if(relativePath == null || "".equals(relativePath)) {
            log.warn("Cannot instantiate a ContextBase because repositoryPath is empty");
            throw new ContextBaseException("Cannot instantiate a ContextBase because repositoryPath is empty");
        } else {
            try {
                this.contextRootNode = jcrSession.getRootNode().getNode(relativePath);
            } catch (PathNotFoundException e) {
                log.warn("PathNotFoundException while instantiating ContextBase for repository path '{}'", repositoryPath);
                throw new ContextBaseException("PathNotFoundException while instantiating ContextBase for repository path '"+repositoryPath+"'");
            } catch (RepositoryException e) {
                log.warn("RepositoryException while instantiating ContextBase for repository path '{}'", repositoryPath);
                throw new ContextBaseException("RepositoryException while instantiating ContextBase for repository path '"+repositoryPath+"'");
            }
        }
        
	}

	public Node getContextRootNode() {
		return contextRootNode;
	}
	
	public Session getSession() {
		return jcrSession;
	}
	 
	public Node getRelativeNode(String path){
	    String strPath = "";
	    String relativePath = stripFirstSlash(path);
	    
	    if (relativePath == null || relativePath.length() == 0) {
	        log.debug("Relative path is null, return null");
	        return null;
	    }
		try {
		    log.debug("Get RelativeNode with rootNode '{}' path={}", contextRootNode.getPath(), relativePath);
            return contextRootNode.getNode(relativePath);
        } catch (PathNotFoundException e) {
            log.debug("Node '{}' cannot be found in the repository below '{}'. Returning null", relativePath, strPath);
        } catch (RepositoryException e) {
            log.error("Unable to find node " + relativePath + " in the repository below '"+strPath+"': " + e.getMessage(), e);
        }
        return null;
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
