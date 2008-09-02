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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.TagSupport;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.template.ModuleRenderAttributes;
import org.hippoecm.hst.core.template.URLMappingTemplateContextFilter;
import org.hippoecm.hst.core.template.module.Module;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.hst.core.template.node.PageContainerModuleNode;
import org.hippoecm.hst.core.template.node.PageContainerNode;
import org.hippoecm.hst.core.template.node.PageNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The tag class that performs the render() and or execute() methods in a module template (JSP).
 *
 */
public class ModuleRenderTag extends BodyTagSupport {
	
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(ModuleRenderTag.class);
    
    private String name;
	private String className;
	private String var;
	private boolean doExecute = false;
	private boolean doRender = true;
	
	private Map <String, String> parameters;
	
	public int doStartTag() throws JspException {		
	    return EVAL_BODY_BUFFERED;
	}


	public int doEndTag() throws JspException {
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		PageContainerModuleNode pcm = null;
		if (doExecute || doRender) {
			pcm = (PageContainerModuleNode) request.getAttribute(HSTHttpAttributes.CURRENT_PAGE_MODULE_NAME_REQ_ATTRIBUTE);
		}
		if (doExecute) {
			//set trigger for filter
		   
			   PageContainerNode pcNode = (PageContainerNode) request.getAttribute(HSTHttpAttributes.CURRENT_PAGE_CONTAINER_NAME_REQ_ATTRIBUTE);
			   PageNode pageNode = (PageNode) request.getAttribute(URLMappingTemplateContextFilter.PAGENODE_REQUEST_ATTRIBUTE);			   
			   ModuleRenderAttributes attributes = new ModuleRenderAttributes(pageNode.getName(), pcNode.getName(), pcm.getName(), getClassName());
			   addModuleMapAttribute(request, attributes);
			
		}
		
		if (doRender) {
		   try {
			Module module = getModule();
			   module.setVar(var);
			   module.setPageModuleNode(getPageModuleNode(request, pcm.getName()));
               module.setModuleParameters(parameters);
			   module.render(pageContext);
			} catch (Exception e) {
				throw new JspException(e);
			}
		}
		
		return EVAL_PAGE;
	}
	
	private PageContainerModuleNode getPageModuleNode(HttpServletRequest request, String moduleName)  throws RepositoryException {
		PageContainerNode pcn = (PageContainerNode) request.getAttribute(HSTHttpAttributes.CURRENT_PAGE_CONTAINER_NAME_REQ_ATTRIBUTE);	
		return pcn.getContainerModuleNodeByName(moduleName);
	}
	
	private void addModuleMapAttribute(HttpServletRequest request, ModuleRenderAttributes renderAttributes) {
		 List moduleList = (List) request.getSession().getAttribute(Module.HTTP_MODULEMAP_ATTRIBUTE);
		 if (moduleList == null) {
			 moduleList = new ArrayList();
		 }
		
		 moduleList.add(renderAttributes);
		 request.getSession().setAttribute(Module.HTTP_MODULEMAP_ATTRIBUTE, moduleList);	
	}
		 
	
	private Module getModule() throws Exception {
		Object o = null;
	    log.info("Create instance of class " + getClassName());
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
	
	public String getVar() {
		return var;
	}

	public void setVar(String varName) {
		this.var = varName;
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
	
	
	protected final void addParameter(String name, String value) {
		if (parameters == null) {
			parameters = new HashMap<String, String>();
		}
		parameters.put(name, value);
	}
	
	protected String getParameter(String name) {
		if (parameters != null) {
		   return parameters.get(name);
		}
		return null;
	}

}

class ParameterBean {
	String name;
	String value;
	
	public ParameterBean(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
