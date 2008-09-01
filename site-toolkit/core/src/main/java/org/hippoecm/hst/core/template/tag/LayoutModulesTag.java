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
package org.hippoecm.hst.core.template.tag;

import java.io.IOException;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.template.HstFilterBase;
import org.hippoecm.hst.core.template.URLMappingTemplateContextFilter;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.hst.core.template.node.NodeList;
import org.hippoecm.hst.core.template.node.PageContainerModuleNode;
import org.hippoecm.hst.core.template.node.PageContainerNode;
import org.hippoecm.hst.core.template.node.PageNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LayoutModulesTag extends SimpleTagSupport {
	private static final Logger log = LoggerFactory.getLogger(LayoutModulesTag.class);
	
    private String name;
	@Override
	public void doTag() throws JspException, IOException {
	
		
		PageContext pageContext = (PageContext) getJspContext(); 
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();    	    	
        PageNode pageNode = (PageNode) request.getAttribute(URLMappingTemplateContextFilter.PAGENODE_REQUEST_ATTRIBUTE);
        NodeList<PageContainerNode> containerList = pageNode.getContainers();
        PageContainerNode pcNode = pageNode.getContainerNode(getName());
        
        if(pcNode == null ) {
            log.error("PageContainerNode is null for layout module '" + name + "'. Fix the hst:configuration for this.");
            return;
        }
        request.setAttribute(HSTHttpAttributes.CURRENT_PAGE_CONTAINER_NAME_REQ_ATTRIBUTE, pcNode);
        
        //getModules
        NodeList<PageContainerModuleNode> pcNodeModules = null;
        try {        	
			pcNodeModules = pcNode.getModules();
		} catch (RepositoryException e) {
			log.error("Cannot get modules", e);
			pcNodeModules = new NodeList<PageContainerModuleNode>();
		}
        List<PageContainerModuleNode> pcmList = pcNodeModules.getItems();;
    
        for (int index=0; index < pcmList.size(); index++) {
        	try {
        		PageContainerModuleNode pcm = pcmList.get(index);				
				request.setAttribute(HSTHttpAttributes.CURRENT_PAGE_MODULE_NAME_REQ_ATTRIBUTE, pcm);			
				pageContext.include(pcm.getTemplatePage());
			} catch (RepositoryException e) {
				log.error("RepositoryException:", e);
				throw new JspException(e);
			} catch (ServletException e) {
				throw new JspException(e);
			}
        }
       
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

}
