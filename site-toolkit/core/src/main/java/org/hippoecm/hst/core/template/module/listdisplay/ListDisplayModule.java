package org.hippoecm.hst.core.template.module.listdisplay;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.HstFilterBase;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.hst.core.template.node.el.ELNode;
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
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
		ModuleNode currNode = (ModuleNode) request.getAttribute("currentModuleNode");
	    List<ELNode> wrappedNodes = new ArrayList<ELNode>();
	    try {
	    	log.info("currentModuleNode path: " + currNode.getJcrNode().getPath());
	    	String contentLocation = currNode.getPropertyValue(ModuleNode.CONTENTLOCATION_PROPERTY_NAME);	    	
	    	ContextBase contentContextBase = (ContextBase) request.getAttribute(HstFilterBase.CONTENT_CONTEXT_REQUEST_ATTRIBUTE);
	    	log.info("ListDisplayModule > " + contentLocation + " base=" + contentContextBase);
			Node n = contentContextBase.getRelativeNode(contentLocation); //currNode.getContentLocation();     
			    log.info("ListDisplayModule.execute() --> " + n.getPath());
			    NodeIterator subNodes =  n.getNodes();
			    while (subNodes.hasNext()) {     
			      Node subNode = subNodes.nextNode();
			      // always check for null in a node iterator
			      if(subNode == null) {continue;}
			      NodeIterator subSubNodes = subNode.getNodes();
			      while (subSubNodes.hasNext()) {
			    	  Node subSubNode = subSubNodes.nextNode();
	                  // always check for null in a node iterator
			    	  if(subSubNodes == null) {continue;}
			    	  log.info("ADD NODE: " + subSubNode.getName());
			    	  wrappedNodes.add(new ListDisplayItem(subSubNode));
			      }
			    }
		} catch (RepositoryException e) {
			log.error(e.getMessage(), e);
			wrappedNodes = new ArrayList<ELNode>();
		}
		
		pageContext.setAttribute(getVar(), wrappedNodes);
	}


}
