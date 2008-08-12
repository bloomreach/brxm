package org.hippoecm.hst.core.template.module;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.node.ModuleNode;

public abstract class ModuleBase implements Module {
	private ModuleNode moduleNode = null;
	private String varName = null;
	
	//public abstract String execute(HttpServletRequest request, HttpServletResponse response) throws TemplateException;
	
	public void setVarName(String name) {
		this.varName = name;
	}
	
	public String getVarName() {
	    return varName;
	}
	
	public void setModuleNode(ModuleNode node) {
		this.moduleNode = node;
	}
	
	public String getPropertyValueFromModuleNode(String propertyName) throws TemplateException {
		String action = moduleNode.getPropertyValue(propertyName);						
		return action;
	}

}
