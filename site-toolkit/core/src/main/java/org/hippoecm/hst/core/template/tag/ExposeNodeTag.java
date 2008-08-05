package org.hippoecm.hst.core.template.tag;

import java.io.IOException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.HstFilterBase;
import org.hippoecm.hst.core.template.URLMappingTemplateContextFilter;
import org.hippoecm.hst.core.template.node.PageNode;
import org.hippoecm.hst.core.template.node.TemplateNode;
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

@Override
public void doTag() throws JspException, IOException {
	PageContext pageContext = (PageContext) getJspContext(); 
	HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();    	
	
    TemplateNode node = null;
    try {
		node = getContentNode(request);
	} catch (RepositoryException e) {
		throw new JspException("Cannot get contentnode", e);
	}
    if (node != null) {    	
        pageContext.setAttribute(getVar(), node);
        try {
        	log.info("UUID" +  node.getJcrNode().getUUID());
			request.getSession().setAttribute("UUID", node.getJcrNode().getUUID());
		} catch (UnsupportedRepositoryOperationException e) {			
			log.error(e.getMessage());
		} catch (RepositoryException e) {		
			log.error(e.getMessage());
		}
    }
}

private TemplateNode getContentNode(HttpServletRequest request) throws RepositoryException {
	 PageNode pageNode = (PageNode) request.getAttribute(URLMappingTemplateContextFilter.PAGENODE_REQUEST_ATTRIBUTE);
	 Session jcrSession = (Session) request.getAttribute(URLMappingTemplateContextFilter.JCRSESSION_REQUEST_ATTRIBUTE);
	 
	 ContextBase contentContextBase = (ContextBase) request.getAttribute(HstFilterBase.CONTENT_CONTEXT_REQUEST_ATTRIBUTE);
	 log.info("[[[[[[[[[[[[[[[[[[" + pageNode + " " + contentContextBase);
	 if (pageNode != null && contentContextBase != null) {
		 log.info("++++++++" + contentContextBase + " DOCUMENT="+ pageNode.getRelativeContentPath());
		 Node currentJcrNode = contentContextBase.getRelativeNode(pageNode.getRelativeContentPath());		
		 return (currentJcrNode == null) ? null : new TemplateNode(contentContextBase, currentJcrNode);
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
   
   
   
   
}

