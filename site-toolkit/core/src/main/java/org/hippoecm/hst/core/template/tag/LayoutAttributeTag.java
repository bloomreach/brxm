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

import java.util.List;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.filters.URLMappingTemplateContextFilter;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.hst.core.template.node.NodeList;
import org.hippoecm.hst.core.template.node.PageContainerModuleNode;
import org.hippoecm.hst.core.template.node.PageContainerNode;
import org.hippoecm.hst.core.template.node.PageNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LayoutAttributeTag extends TagSupport {
	private static final Logger log = LoggerFactory.getLogger(LayoutAttributeTag.class);
    private String name;
    private String module;
    private  List<PageContainerModuleNode> pcmList = null;
    private int index;
    
    @Override
	public int doStartTag() throws JspException {
    	HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();    
        PageNode pageNode = (PageNode) request.getAttribute(HSTHttpAttributes.CURRENT_PAGE_NODE_REQ_ATTRIBUTE);
        NodeList<PageContainerNode> containerList = pageNode.getContainers();
        PageContainerNode pcNode = pageNode.getContainerNode(getName());
        
        //getModules
        NodeList<PageContainerModuleNode> pcNodeModules = null;
        try {
        	pcNodeModules = pcNode.getModules();
		} catch (RepositoryException e) {		
			log.warn("Cannot get modules for a pageNode", e);
			pcNodeModules = new  NodeList<PageContainerModuleNode>();
		}
        
        pcmList = pcNodeModules.getItems();
        
        index = 0;
        
        if (pcmList == null || pcmList.size() == 0) {
        	return SKIP_BODY;
        }
        
      
        try {
			ModuleNode moduleNode = pcmList.get(index).getModuleNode();
			pageContext.setAttribute(getModule(), moduleNode, PageContext.PAGE_SCOPE);
			index++;
		} catch (RepositoryException e) {		
			e.printStackTrace();
		}
		return EVAL_BODY_INCLUDE;
	}


	@Override
	public int doAfterBody() throws JspException {
		log.info("DO AFTER");
		if (index < pcmList.size()) {
			
			HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();    			
			try {
				ModuleNode moduleNode = pcmList.get(index).getModuleNode();
				pageContext.setAttribute(getModule(), moduleNode, PageContext.PAGE_SCOPE);
			} catch (RepositoryException e) {				
				e.printStackTrace();
			}
			index++;
			
			return EVAL_BODY_AGAIN;
		}
		return SKIP_BODY;
	}


	@Override
	public int doEndTag() throws JspException {
		return super.doEndTag();
	}
    
    /* getters & setters */
    
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}


	public String getModule() {
		return module;
	}


	public void setModule(String module) {
		this.module = module;
	}

	
    
}
