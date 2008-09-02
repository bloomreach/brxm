package org.hippoecm.hst.core.template.module;

import java.util.Map;

import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.hst.core.template.node.PageContainerModuleNode;

public abstract class ModuleBase implements Module {
	private PageContainerModuleNode pageContainerModule = null;
	private String var;
	protected Map<String, String> moduleParameters;
	
	//public abstract String execute(HttpServletRequest request, HttpServletResponse response) throws TemplateException;
	
	public final void setPageModuleNode(PageContainerModuleNode node) {
		this.pageContainerModule = node;
	}
	
	public final String getPropertyValueFromModuleNode(String propertyName) throws TemplateException {		
			return pageContainerModule.getPropertyValue(propertyName);	
	}

	public String getVar() {
		return var;
	}

	public final void setVar(String var) {
		this.var = var;
	}

	public String execute(HttpServletRequest request,
			HttpServletResponse response) throws TemplateException {
		return null;
	}

	public void init(HttpServletRequest request) {
	}

	public final void setModuleParameters(Map<String, String> parameters) {
		moduleParameters = parameters;
	}

	public void render(PageContext pageContext) throws TemplateException {		
	}

}
