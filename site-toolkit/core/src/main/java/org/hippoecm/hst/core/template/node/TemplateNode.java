package org.hippoecm.hst.core.template.node;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.node.el.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateNode implements ELNode {
	private Logger log = LoggerFactory.getLogger(TemplateNode.class);	
	
	protected Node jcrNode;	
	protected String relativePath;	
	protected ContextBase contextBase;
	
	protected String relativeContentPath;
	
	public TemplateNode(ContextBase contextBase, Node jcrNode) {
		this.contextBase = contextBase;
		this.jcrNode = jcrNode;
	}
	
	public TemplateNode(ContextBase contextBase, String relativePath) throws RepositoryException {
		this.contextBase = contextBase;
		this.relativePath = relativePath;
		jcrNode = contextBase.getRelativeNode(relativePath);
	}

	public Node getJcrNode() {
		return jcrNode;
	}
	
	public void setContextBase(ContextBase contextBase) {
		this.contextBase = contextBase;
	}

	public Node getNode(ContextBase contentContextBase, String relativePath) throws PathNotFoundException, RepositoryException {		
		log.info("getNode=" + relativePath);		
		return contentContextBase.getRelativeNode(relativePath);
	}
	
	public String getPropertyValue(String propertyName) throws PathNotFoundException, ValueFormatException, RepositoryException {
		return jcrNode.getProperty(propertyName).getString();
	}

	/**
	 * Returns the node that is referred to in a property of the current node.
	 * @param session
	 * @param propertyName the property with the path of the node to return
	 * @return
	 */
	protected Node getTemplateNodeFromPropertyValue(String propertyName)
	 throws RepositoryException {
		try {
			log.info("retrieving property " + propertyName + " of node " + jcrNode.getPath());
			Property layoutProperty = jcrNode.getProperty(propertyName);	
			log.info("RRRR "+ contextBase + " rest " + layoutProperty.getValue().getString());
			return contextBase.getRelativeNode(layoutProperty.getValue().getString());
		} catch (PathNotFoundException e) {
			log.error(e.getMessage());
			throw new RepositoryException(e.getMessage());
		} catch (ValueFormatException e) {
			log.error(e.getMessage());
			throw new RepositoryException(e.getMessage());
		} 
	}
	
	public String getRelativeContentPath() {
		return relativeContentPath;
	}

	public void setRelativeContentPath(String relativeContentPath) {
		this.relativeContentPath = relativeContentPath;
	}

	/* EL stuff */
	

	public Map getProperty() {
        return new ELPseudoMap() {
            public Object get(Object propertyName) {
            	try {
					return jcrNode.getProperty((String) propertyName).getValue().getString();
				} catch (ValueFormatException e) {					
					e.printStackTrace();
				} catch (IllegalStateException e) {					
					e.printStackTrace();
				} catch (PathNotFoundException e) {				
					e.printStackTrace();
				} catch (RepositoryException e) {				
					e.printStackTrace();
				}
				return "";
            }
        };
    }
}
