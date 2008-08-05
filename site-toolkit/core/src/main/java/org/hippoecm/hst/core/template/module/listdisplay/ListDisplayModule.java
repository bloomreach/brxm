package org.hippoecm.hst.core.template.module.listdisplay;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.ContextBaseFilter;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListDisplayModule extends ModuleBase {
	private static final Logger log = LoggerFactory.getLogger(ListDisplayModule.class);
	
	public String execute(HttpServletRequest request,
			HttpServletResponse response) throws TemplateException {
		// TODO Auto-generated method stub
		return null;
	}

	public void init(HttpServletRequest request) {
		// TODO Auto-generated method stub
	}
	

	public void render(PageContext pageContext) {
		System.out.println("DORENDERDREWRADFERERWERWERWERER");
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		String urlPrefix = (String) pageContext.getRequest().getAttribute(ContextBaseFilter.URLBASE_INIT_PARAMETER);		    
		ModuleNode currNode = (ModuleNode) request.getAttribute("currentModuleNode");
		 System.out.println("MMMMMMMMMMMMMMMMMMMMMMNODE" + request.getAttribute("currentModuleNode"));
		 
	    List wrappedNodes = new ArrayList();
	    try {
			Node n = currNode.getContentLocation();      
			    NodeIterator subNodes =  n.getNodes();
			    while (subNodes.hasNext()) {     
			      Node subNode = (Node) subNodes.next();
			      NodeIterator subSubNodes = subNode.getNodes();
			      while (subSubNodes.hasNext()) {
			    	  Node subSubNode = (Node) subSubNodes.next();
			    	  System.out.println("ADD NODE" + subSubNode.getName());
			    	  wrappedNodes.add(new ListDisplayItem(subSubNode));
			      }
			    }
		} catch (RepositoryException e) {
			log.error(e.getMessage(), e);
			wrappedNodes = new ArrayList();
		}
		
		pageContext.setAttribute("items", wrappedNodes);
		
		
	}

}
