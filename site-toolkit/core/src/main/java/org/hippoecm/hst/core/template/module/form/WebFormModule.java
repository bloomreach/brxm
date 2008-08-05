package org.hippoecm.hst.core.template.module.form;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.Module;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.core.template.module.form.el.WebFormBean;
import org.hippoecm.hst.core.template.node.ModuleNode;



public class WebFormModule extends ModuleBase {	

	public String execute(HttpServletRequest request, HttpServletResponse response) throws TemplateException {		
		
		System.out.println("*********************************************");
		Enumeration attribEnum = request.getAttributeNames();
		while (attribEnum.hasMoreElements()) {
			System.out.println("ATTRIB " +  attribEnum.nextElement());
		}
		
		Map parameterMap = request.getParameterMap();
		Iterator i = parameterMap.keySet().iterator();
		while (i.hasNext()) {
			String s =(String) i.next();
			System.out.println("P " +  s + " value=" + request.getParameter(s));
		}
		//;		
		
		String action = getPropertyValueFromModuleNode("action");
		WebFormBean formBean = new WebFormBean();
		formBean.setAction(action);
		request.setAttribute("webform", formBean);
		
		return null;
	}

	public void init(HttpServletRequest request) {
		// TODO Auto-generated method stub
	}

	public void render(PageContext pageContext) {
		// TODO Auto-generated method stub
	}

}
