package org.hippoecm.hst.core.template.module.paginatedbrowse;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;

public class PaginateModule extends ModuleBase {
	private int pageSize;

	
	public PaginateModule() {
		pageSize = 5;
	}
	
	public String execute(HttpServletRequest request, HttpServletResponse response)
			throws TemplateException {
		String pageId = request.getParameter("pageId");		
		return null;
		
	}

	public void init(HttpServletRequest request) {
		// TODO Auto-generated method stub
		
	}

	public void render(PageContext pageContext) {
	   
		//get pageId from request
		int pageId = 0;
		pageId = Integer.parseInt(pageContext.getRequest().getParameter("pageId"));
		
	}

}
