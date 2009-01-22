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

public class PageContainerNode extends TemplateNode {
	public static final String LAYOUT_ATTRIBUTE_PROPERTY = "hst:layoutAttribute";
    
    public PageContainerNode(ContextBase contextBase, Node jcrNode) {
 	   super(contextBase, jcrNode);	  
 	}
    
    
    public NodeList<PageContainerModuleNode> getModules() throws RepositoryException {
    	return new NodeList<PageContainerModuleNode>(contextBase, getJcrNode(), PageContainerModuleNode.class) ;
    }
    
    public String getLayoutAttributeValue() throws RepositoryException {
    	return jcrNode.getProperty(LAYOUT_ATTRIBUTE_PROPERTY).getValue().getString();
    }
    
    public PageContainerModuleNode getContainerModuleNodeByModuleName(String moduleName) throws RepositoryException {
    	NodeList<PageContainerModuleNode> modules = getModules();
    	
    	/* search for the pagecontainernode that refers to the modulenode with the name to search for */
    	for (PageContainerModuleNode item : modules.getItems()) {		
    		ModuleNode moduleNode = item.getModuleNode();    		
			if (moduleNode.getJcrNode().getName().equals(moduleName)) {
				return item;
			}
		}
		return null;
    }
    
    public PageContainerModuleNode getContainerModuleNodeByName(String name) throws RepositoryException {
    	NodeList<PageContainerModuleNode> modules = getModules();
    	
    	/* search for the pagecontainernode that refers to the modulenode with the name to search for */
    	for (PageContainerModuleNode item : modules.getItems()) {		    				
			if (item.getJcrNode().getName().equals(name)) {
				return item;
			}
		}
		return null;
    }
}
