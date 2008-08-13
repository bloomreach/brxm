package org.hippoecm.hst.core.template.module;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.node.ModuleNode;

public abstract class ModuleBase implements Module {
	private ModuleNode moduleNode = null;
	private String var = "result";
	
	/**
	 * Override this method your subclasses to execute business logic. Add execute="true" to the jsp tag.
	 */
    public String execute(HttpServletRequest request, HttpServletResponse response) throws TemplateException {
       return null; 
    }

    /**
     * Override this method your subclasses. 
     */
    public void init(HttpServletRequest request) {
        
    }

    /**
     * Override this method your subclasses to render logic. Add render="true" to the jsp tag.
     */
    public void render(PageContext pageContext) throws TemplateException {
        
    }

	
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
