package org.hippoecm.hst.core.template.tag;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.core.template.URLMappingTemplateContextFilter;
import org.hippoecm.hst.core.template.node.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.servlet.*;

public class LayoutAttributeTag extends TagSupport {
	private static final Logger log = LoggerFactory.getLogger(LayoutAttributeTag.class);
    private String name;
    private String module;
    private  List<PageContainerModuleNode> pcmList = null;
    private int index;
    
    @Override
	public int doStartTag() throws JspException {
    	HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();    
        PageNode pageNode = (PageNode) request.getAttribute(URLMappingTemplateContextFilter.PAGENODE_REQUEST_ATTRIBUTE);
        NodeList<PageContainerNode> containerList = pageNode.getContainers();
        PageContainerNode pcNode = pageNode.getContainerNodeByName(getName());
        
        //getModules
        NodeList<PageContainerModuleNode> pcNodeModules = null;
        try {
        	pcNodeModules = pcNode.getModules();
		} catch (RepositoryException e) {		
			log.error("Cannot get modules for a pageNode", e);
			pcNodeModules = new  NodeList<PageContainerModuleNode>();
		}
        
        pcmList = pcNodeModules.getItems();
        
        index = 0;
        log.info("DO START");
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
