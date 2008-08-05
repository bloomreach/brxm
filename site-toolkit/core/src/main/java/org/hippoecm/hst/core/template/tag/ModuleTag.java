package org.hippoecm.hst.core.template.tag;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.Module;
import org.hippoecm.hst.core.template.node.ModuleNode;

public class ModuleTag extends TagSupport {
	
	private String className;
	private String contextBaseRequestParameter;

	@Override
	public int doEndTag() throws JspException {
		
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();	
		HttpServletResponse response = (HttpServletResponse) pageContext.getResponse();
		ModuleNode moduleNode = (ModuleNode) request.getAttribute("currentModuleNode");
		
		Module module;
		try {
			module = getModule();
		} catch (Exception e) {
			throw new JspException(e);
		}
		//module.setContextBase(contextBase);
/*		try {
			module.setModuleNode(moduleNode);
			module.execute(request, response);
		} catch (TemplateException e) {
			throw new JspException(e);
		}
*/
		return super.doEndTag();
	}
	
	private Module getModule() throws Exception {
		Object o = null;
			o = Class.forName(getClassName()).newInstance();
			if (!Module.class.isInstance(o)) {
				throw new Exception(getClassName() + " does not implement the interface " + Module.class.getName());
			}
		return (Module) o;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
	
	
	

}
