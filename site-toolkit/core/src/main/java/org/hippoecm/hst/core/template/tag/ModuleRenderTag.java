package org.hippoecm.hst.core.template.tag;


import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.core.template.URLMappingTemplateContextFilter;
import org.hippoecm.hst.core.template.module.Module;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.hst.core.template.node.PageNode;



public class ModuleRenderTag extends TagSupport {
	
    private static final long serialVersionUID = 1L;
    
    private String name;
	private String className;
	private String varName;
	private boolean doExecute = false;
	private boolean doRender = true;
	

	@Override
	public int doEndTag() throws JspException {
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		if (doExecute) {
			//set trigger for filter
		   addModuleMapAttribute(request, getName(), className);
		}
		
		if (doRender) {
		   try {
			Module module = getModule();
			   module.setVariableName(varName);
			   module.setModuleNode(getModuleNode(request, getName()));
			   module.render(pageContext);
			} catch (Exception e) {
				throw new JspException(e);
			}
		}
		
		return SKIP_BODY;
	}
	
	private ModuleNode getModuleNode(HttpServletRequest request, String moduleName) throws RepositoryException {
		PageNode currentPageNode = (PageNode) request.getAttribute(URLMappingTemplateContextFilter.PAGENODE_REQUEST_ATTRIBUTE);
		ModuleNode moduleNode = currentPageNode.getModuleNodeByName(moduleName);
		return moduleNode;
	}
	
	private void addModuleMapAttribute(HttpServletRequest request, String name, Object value) {
		 Map moduleMap = (Map) request.getAttribute(Module.REQUEST_MODULEMAP_ATTRIBUTE);
		 if (moduleMap == null) {
			 moduleMap = new HashMap();
		 }
		 moduleMap.put(name, value);
		 request.getSession().setAttribute(Module.REQUEST_MODULEMAP_ATTRIBUTE, moduleMap);	
	}
		 
	
	private Module getModule() throws Exception {
		Object o = null;
			o = Class.forName(getClassName()).newInstance();
			if (!Module.class.isInstance(o)) {
				throw new Exception(getClassName() + " does not implement the interface " + Module.class.getName());
			}
		return (Module) o;
	}
	
	/* getters & setters */

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getExecute() {
		return doExecute ? "true" : "false";
	}

	public void setExecute(String execute) {
		doExecute = execute.toLowerCase().trim().equals("true");
	}
	
	public String getVarName() {
		return varName;
	}

	public void setVarName(String varName) {
		this.varName = varName;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
	
	public void setRender(String render) {
		doRender = render.toLowerCase().trim().equals("true");
	}
	
	public String getRender() {
		return doRender ? "true" : "false";
	}
	
	

}
