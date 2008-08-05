package org.hippoecm.hst.core.template;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.jcr.JCRConnector;
import org.hippoecm.hst.jcr.JCRConnectorWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContextBase {
	private static final Logger log = LoggerFactory.getLogger(ContextBase.class);
	private static final String DEFAULT_CONTEXT_NAME = "content";
	private static final String DEFAULT_CONTENT_LOCATION = "/content";
	
	private String contextName;
	private Node contextRootNode;
	private Session jcrSession;
	
	public ContextBase(String contextName, String repositoryPath, HttpServletRequest request) throws PathNotFoundException, RepositoryException {
		this(contextName, repositoryPath, request, JCRConnectorWrapper.getJCRSession(request.getSession()));
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
	
	public Node getRelativeNode(String path) throws PathNotFoundException, RepositoryException {
		log.info("get RelativeNode=" + path);
		return contextRootNode.getNode(stripFirstSlash(path));
	}
	
	public static ContextBase getDefaultContextBase(HttpServletRequest request) throws PathNotFoundException, RepositoryException {
		return new ContextBase(DEFAULT_CONTEXT_NAME, DEFAULT_CONTENT_LOCATION, request);
	}
	
	private String stripFirstSlash(String s) {
		return s.startsWith("/") ? s.substring(1) : s;
	}
	
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
