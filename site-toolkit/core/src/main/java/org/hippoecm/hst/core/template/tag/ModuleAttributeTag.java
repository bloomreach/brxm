package org.hippoecm.hst.core.template.tag;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.node.PageContainerModuleNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModuleAttributeTag extends SimpleTagSupport{
	private static final Logger log = LoggerFactory.getLogger(ModuleAttributeTag.class);
	
	private String var;
	private String propertyName;
	
	@Override
	public void doTag() throws JspException, IOException {
		PageContext pageContext = (PageContext) getJspContext(); 
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		PageContainerModuleNode pcmoduleNode = (PageContainerModuleNode) request.getAttribute(HSTHttpAttributes.CURRENT_PAGE_MODULE_NAME_REQ_ATTRIBUTE);
		log.debug("Getting moduleNode {} for property {}", pcmoduleNode,propertyName);
		
	    String propertyValue = "";
	    
	    if(pcmoduleNode!=null) {
	    	try {
				propertyValue = pcmoduleNode.getPropertyValue(propertyName);
			} catch (TemplateException e) {
				log.debug("Cannot get attribute value for attribute with name {} ", propertyName);
			}
	    	log.debug("Trying to fetch module attribute value for property {} with value {}", propertyName,propertyValue);
	    }
	    
	    if (propertyValue != null) {    	
	        pageContext.setAttribute(getVar(), propertyValue);
	    }
	}

	private String getVar() {
		return var;
	}

	public void setVar(String var) {
		this.var = var;
	}
	
	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}

}