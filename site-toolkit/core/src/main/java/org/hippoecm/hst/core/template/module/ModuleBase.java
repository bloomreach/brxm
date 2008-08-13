package org.hippoecm.hst.core.template.module;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.node.ModuleNode;

public abstract class ModuleBase implements Module {
	private ModuleNode moduleNode = null;
	private String var = "result";
	
	//public abstract String execute(HttpServletRequest request, HttpServletResponse response) throws TemplateException;
	
	public void setVar(String name) {
		if(name!=null){
			this.var = name;	
		}
	}
	
	public String getVar() {
	    return var;
	}
	
	public void setModuleNode(ModuleNode node) {
		this.moduleNode = node;
	}
	
	public String getPropertyValueFromModuleNode(String propertyName) throws TemplateException {
		String action = moduleNode.getPropertyValue(propertyName);						
		return action;
	}

}
