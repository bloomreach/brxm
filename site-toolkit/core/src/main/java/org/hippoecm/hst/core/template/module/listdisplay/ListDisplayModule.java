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
package org.hippoecm.hst.core.template.module.listdisplay;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.mapping.URLMapping;
import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.HstFilterBase;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.hst.core.template.node.el.ELNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A module that takes the subnodes of a node and prepares them for display purposes.
 * The subnodes are wrapped in {@see ListDisplayItem} instances and put in a list that
 * is set on the pageContext.
 *
 */
public class ListDisplayModule extends ModuleBase {
	private static final Logger log = LoggerFactory.getLogger(ListDisplayModule.class);
	

	public void render(PageContext pageContext) {
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		URLMapping urlMapping = (URLMapping)request.getAttribute(HSTHttpAttributes.URL_MAPPING_ATTR);
	    List<ELNode> wrappedNodes = new ArrayList<ELNode>();

	    try {	    	
	    	String contentLocation = null;
	    	try {
	    		contentLocation = getPropertyValueFromModuleNode(ModuleNode.CONTENTLOCATION_PROPERTY_NAME);	    	
			} catch (TemplateException e) {				
				log.error("Cannot get property " + ModuleNode.CONTENTLOCATION_PROPERTY_NAME, e);
			}

	    	ContextBase contentContextBase = (ContextBase) request.getAttribute(HSTHttpAttributes.CURRENT_CONTENT_CONTEXTBASE_REQ_ATTRIBUTE);
	    	log.debug("ListDisplayModule > " + contentLocation + " base=" + contentContextBase);
			Node n = contentContextBase.getRelativeNode(contentLocation); 
			if(n!=null) {
			    log.debug("ListDisplayModule.execute() --> " + n.getPath());
			    NodeIterator subNodes =  n.getNodes();
			    while (subNodes.hasNext()) {     
			      Node subNode = subNodes.nextNode();
			      // always check for null in a node iterator
			      if(subNode == null) {continue;}
			      NodeIterator subSubNodes = subNode.getNodes();
			      while (subSubNodes.hasNext()) {
			    	  Node subSubNode = subSubNodes.nextNode();
	                  // always check for null in a node iterator
			    	  if(subSubNodes == null) {
			    		  continue;
			    	  }
			    	  log.debug("ADD NODE: " + subSubNode.getName());
			    	  wrappedNodes.add(new ListDisplayItem(subSubNode, urlMapping));
			      }
			    }
			} else {
			    log.warn("contentLocation '" + contentLocation +"' points to a nonexisting repository node. Return empty list");
			    wrappedNodes = new ArrayList<ELNode>();
			}
		} catch (RepositoryException e) {
			log.error(e.getMessage(), e);
			wrappedNodes = new ArrayList<ELNode>();
		}
		
		pageContext.setAttribute(getVar(), wrappedNodes);
	}


}
