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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.context.ContextBase;
import org.hippoecm.hst.core.exception.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A node that represents an instance of a Module in a sitemap item.
 *
 */
public class PageContainerModuleNode extends TemplateNode {
private Logger log = LoggerFactory.getLogger(PageContainerModuleNode.class);	 
	
	public static final String MODULE_NAME_PROPERTY = "hst:module";
	
	public PageContainerModuleNode(ContextBase contextBase, String relativePath) throws RepositoryException {
		super(contextBase, relativePath);		
	} 
	
	public PageContainerModuleNode(ContextBase contextBase, Node jcrNode) {
		super(contextBase, jcrNode);		
	} 
	
	public ModuleNode getModuleNode() throws RepositoryException {	 	
	    return new ModuleNode(contextBase, getTemplateNodeFromPropertyValue(MODULE_NAME_PROPERTY));
	}
	
	
	public String getTemplatePage() throws RepositoryException {	
		if (jcrNode.hasProperty(ModuleNode.TEMPLATE_PROPERTY_NAME)) {
			try {
				return getPropertyValue(ModuleNode.TEMPLATE_PROPERTY_NAME);
			} catch (TemplateException e) {
			    log.error("No " + ModuleNode.TEMPLATE_PROPERTY_NAME+ " property in current node " + jcrNode.getPath());
			    return null;
			}
		} else {			
			return getModuleNode().getPropertyValue(ModuleNode.TEMPLATE_PROPERTY_NAME);
		}
				
	}
	   
	public Node getContentLocation() throws RepositoryException {	   
		try {
			return getTemplateNodeFromPropertyValue(ModuleNode.CONTENTLOCATION_PROPERTY_NAME);
		} catch (Exception e) {
			return getModuleNode().getTemplateNodeFromPropertyValue(ModuleNode.CONTENTLOCATION_PROPERTY_NAME);
		}
	}
	
	public String getPropertyValue(String propertyName) throws TemplateException {
		   try {
			   if (jcrNode.hasProperty(propertyName)) {
				  return super.getPropertyValue(propertyName);			
			   } else {
				   return getModuleNode().getPropertyValue(propertyName); 			   
			   }			   
			} catch (Exception e) {		
				try {
					log.error("Cannot get property " + propertyName + " from " + jcrNode.getPath());
				} catch (RepositoryException e1) {
					log.error("Cannot get property " + propertyName);
				}
				throw new TemplateException(e);
			}
	   }
}
