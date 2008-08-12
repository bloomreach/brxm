package org.hippoecm.hst.core.template.module;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.node.ModuleNode;

public interface Module {
	public static final String REQUEST_MODULEMAP_ATTRIBUTE  = "moduleMap";
	
    public String execute(HttpServletRequest request, HttpServletResponse response) throws TemplateException;
    public void render(PageContext pageContext) throws TemplateException;
    public void init(HttpServletRequest request);
    public void setModuleNode(ModuleNode node);
    public void setVariableName(String name);
    public String getVariableName();
}
