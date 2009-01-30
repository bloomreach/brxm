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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.template.node.NodeList;
import org.hippoecm.hst.core.template.node.PageContainerModuleNode;
import org.hippoecm.hst.core.template.node.PageContainerNode;
import org.hippoecm.hst.core.template.node.PageNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LayoutModulesTag  extends BodyTagSupport {
	
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(LayoutModulesTag.class);
	
    private String name;
    
	@Override
	public int doStartTag() throws JspException {  
		
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();    	  
        HstRequestContext hstRequestContext = (HstRequestContext)request.getAttribute(HstRequestContext.class.getName());
        PageNode pageNode = hstRequestContext.getPageNode();
        PageContainerNode pcNode = pageNode.getContainerNode(getName());
        
        boolean debugView = false;
        String disableTemplate = "";
        if(request.getParameter("debugview")!=null && (request.getParameter("debugview").equals(getName()) || request.getParameter("debugview").equalsIgnoreCase("all"))){
           log.debug("show template names for container: " + getName());
           debugView = true;
        }
        
        if(request.getParameter("disabletemplate")!=null){
            disableTemplate = request.getParameter("disabletemplate");
            log.debug("disabled template = " + disableTemplate);
        }
        
        if(pcNode == null ) {
            log.warn("PageContainerNode is null for layout module '" + name + "'. Fix the hst:configuration for this.");
            return SKIP_BODY;
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
    
        int amountRenderedModules = 0;
        for (int index=0; index < pcmList.size(); index++) {
        	String templatePage = null;
            try {
        		PageContainerModuleNode pcm = pcmList.get(index);
				request.setAttribute(HSTHttpAttributes.CURRENT_PAGE_MODULE_NAME_REQ_ATTRIBUTE, pcm);
				try {
				    templatePage = pcm.getTemplatePage();
				} catch (RepositoryException e) {
				    log.warn("Error fetching template page '" + templatePage + "'. Skipping this template page \n " +
				    		"The page container module node for this template is: '" + pcm.getPath()+ "'");
				    continue;
				}
				if(templatePage == null) {
				    log.warn("Unable to get correct template page. Skipping template \n " +
                            "The page container module node for this template is: '" + pcm.getPath()+ "'");
				    continue;
				}
				
				if(debugView) {
				    if(index > 0) {
				        pageContext.getOut().write("<br/>");  
				    }
				    log.debug("disabled '" + templatePage + "' because view " + getName() + " is disabled.");
				    pageContext.getOut().write("disabled-"+index+" : " + templatePage);
				} else if(disableTemplate!=null && disableTemplate.equals(templatePage)) {
				    log.debug("disabled '" + templatePage);
				    pageContext.getOut().write("disabled-"+index+" : " + templatePage);
                } else {
                    log.debug("including: " + templatePage);
                    pageContext.include(templatePage);
                }
				amountRenderedModules++;
			} catch (IOException e) {
                log.warn("IOException:", e);
			} catch (ServletException e) {
			    log.warn("ServletException: " + e.getMessage() + " for template page: " + templatePage);
			    log.debug("ServletException: " , e);
			}
        }
        
        if (amountRenderedModules == 0) {
            log.debug("empty pcmList: eval body");
            return EVAL_BODY_INCLUDE;
        }
        return SKIP_BODY;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
   

}
