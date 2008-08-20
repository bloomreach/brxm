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
package org.hippoecm.hst.core.template.node;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.node.el.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateNode extends AbstractELNode {
	private Logger log = LoggerFactory.getLogger(TemplateNode.class);	
	
	protected String relativePath;	
	protected ContextBase contextBase;
	
	protected String relativeContentPath;
	
	public TemplateNode(ContextBase contextBase, Node jcrNode) {
        super(jcrNode);
		this.contextBase = contextBase;
	}
	
	public TemplateNode(ContextBase contextBase, String relativePath) throws RepositoryException {
	    super(contextBase, relativePath);
	    this.contextBase = contextBase;
		this.relativePath = relativePath;
	}
	
	public void setContextBase(ContextBase contextBase) {
		this.contextBase = contextBase;
	}

	public Node getNode(ContextBase contentContextBase, String relativePath) throws PathNotFoundException, RepositoryException {		
		log.info("getNode=" + relativePath);		
		return contentContextBase.getRelativeNode(relativePath);
	}
	
	public String getPropertyValue(String propertyName) throws TemplateException {
		try {
            return jcrNode.getProperty(propertyName).getString();
        } catch (ValueFormatException e) {
            throw new TemplateException(e);
        } catch (PathNotFoundException e) {
        	 throw new TemplateException(e);
        } catch (RepositoryException e) {
        	 throw new TemplateException(e);
        }
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
			return contextBase.getRelativeNode(layoutProperty.getValue().getString());
		} catch (PathNotFoundException e) {
			log.error("PathNotFoundException: " +e.getMessage());
			throw e;
		} catch (ValueFormatException e) {
			log.error("ValueFormatException: " + e.getMessage());
			throw e;
		} 
	}
	
	public String getRelativeContentPath() {
		return relativeContentPath;
	}

	public void setRelativeContentPath(String relativeContentPath) {
		this.relativeContentPath = relativeContentPath;
	}

	
}
