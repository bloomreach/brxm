package org.hippoecm.hst.core.template.module.form;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.core.template.module.form.el.WebFormBean;



public class WebFormModule extends ModuleBase {	

	public String execute(HttpServletRequest request, HttpServletResponse response) throws TemplateException {		
		
		Enumeration attribEnum = request.getAttributeNames();
		
		Map parameterMap = request.getParameterMap();
		Iterator i = parameterMap.keySet().iterator();
	
		String action = getPropertyValueFromModuleNode("action");
		WebFormBean formBean = new WebFormBean();
		formBean.setAction(action);
		request.setAttribute("webform", formBean);
		
		return null;
	}

}
