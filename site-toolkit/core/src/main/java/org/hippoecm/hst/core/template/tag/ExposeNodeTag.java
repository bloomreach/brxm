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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.mapping.URLMapping;
import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.HstFilterBase;
import org.hippoecm.hst.core.template.URLMappingTemplateContextFilter;
import org.hippoecm.hst.core.template.node.PageNode;
import org.hippoecm.hst.core.template.node.el.ContentELNode;
import org.hippoecm.hst.core.template.node.el.ContentELNodeImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Expose a node with EL stuff to the pageContext. For now only the current PageNode!
 * 
 * @author mmeijnhard
 *
 */
public class ExposeNodeTag extends  SimpleTagSupport {
	private static final Logger log = LoggerFactory.getLogger(ExposeNodeTag.class);
	

	   private String var;
	   private String location;

@Override
public void doTag() throws JspException, IOException {
	PageContext pageContext = (PageContext) getJspContext(); 
	HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();    	
	
    ContentELNode node = null;
    try {
		node = getContentNode(request);
	} catch (RepositoryException e) {
		throw new JspException("Cannot get contentnode", e);
	}
    if (node != null) {    	
        pageContext.setAttribute(getVar(), node);
        try {
            if(node.getJcrNode().isNodeType("mix:referenceable")) {
                request.getSession().setAttribute("UUID", node.getJcrNode().getUUID());
            } else {
                log.debug("Node is not referenceable hence cannot get uuid. ");
            }
		} catch (UnsupportedRepositoryOperationException e) {			
			log.warn("Method not supported by the repository: {}",e.getMessage());
		} catch (RepositoryException e) {
			log.error("Error while getting checking if node is referenceable: {}",e.getMessage());
			log.debug("RepositoryException:", e);
		}
    }
}

private ContentELNodeImpl getContentNode(HttpServletRequest request) throws RepositoryException {
	 PageNode pageNode = (PageNode) request.getAttribute(HSTHttpAttributes.CURRENT_PAGE_NODE_REQ_ATTRIBUTE);
	 
	 ContextBase contentContextBase = (ContextBase) request.getAttribute(HSTHttpAttributes.CURRENT_CONTENT_CONTEXTBASE_REQ_ATTRIBUTE);
	 URLMapping urlMapping = (URLMapping)request.getAttribute(HSTHttpAttributes.URL_MAPPING_ATTR);
	 if (pageNode != null && contentContextBase != null) {
		 String relPath = null;
	     if(location != null) {
	         log.debug("using location '"+location+"' from the hst node tag instead of from sitemap item");
	         relPath = location;
		 } else {
		     relPath = pageNode.getRelativeContentPath();
		 }
	     log.debug("Fetching relative node '" + relPath + "' from context base " + contentContextBase.getContextRootNode().getPath() );
	     Node currentJcrNode = contentContextBase.getRelativeNode(relPath);		
		 return (currentJcrNode == null) ? null : new ContentELNodeImpl(currentJcrNode, urlMapping);
	 }
	 return null;
}

/* getters & setters */

public String getVar() {
    return var;
}

public void setVar(String var) {
    this.var = var;
}

public String getLocation() {
    return location;
}

public void setLocation(String location) {
    if(location!=null) {
        if(location.startsWith("/")) {
            location = location.substring(1);
        }
        if(location.endsWith("/")) {
            location = location.substring(location.length()-1);
        }
        if(!"".equals(location)) {
            this.location = location;
        }
    }
}


   
   
   
   
}

