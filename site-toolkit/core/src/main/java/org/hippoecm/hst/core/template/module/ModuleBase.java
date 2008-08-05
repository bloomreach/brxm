package org.hippoecm.hst.core.template.module;

import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.node.ModuleNode;

public abstract class ModuleBase implements Module {
	private ModuleNode moduleNode = null;
	
	//public abstract String execute(HttpServletRequest request, HttpServletResponse response) throws TemplateException;
	
	public void setModuleNode(ModuleNode node) {
		this.moduleNode = node;
	}
	
	public String getPropertyValueFromModuleNode(String propertyName) throws TemplateException {
		String action = null;
		try {
			action = moduleNode.getPropertyValue(propertyName);						
			return action;
		} catch (PathNotFoundException e) {
			throw new TemplateException(e.getMessage());
			
		} catch (RepositoryException e) {
			throw new TemplateException(e.getMessage());
		}	
	}

}
