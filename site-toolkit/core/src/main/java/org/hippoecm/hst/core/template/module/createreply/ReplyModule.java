package org.hippoecm.hst.core.template.module.createreply;

import java.io.IOException;
import java.util.Enumeration;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.Module;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.core.template.module.form.el.WebFormBean;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.hst.core.template.node.PageNode;
import org.hippoecm.hst.jcr.JCRConnector;
import org.hippoecm.hst.jcr.JCRConnectorWrapper;
import org.hippoecm.hst.core.template.*;

public class ReplyModule extends ModuleBase {

	public String execute(HttpServletRequest request, HttpServletResponse response) throws TemplateException {
		System.out.println("VVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVVV");
		if (request.getParameter("MODULE_NAME") != null) {
			System.out.println("WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW");
		String name = request.getParameter("name");
		String title = request.getParameter("title");
		String content = request.getParameter("content");
		
		String urlPrefix = (String) request.getAttribute(ContextBaseFilter.URLBASE_INIT_PARAMETER);
		urlPrefix = (urlPrefix == null) ?  "" : urlPrefix;
		System.out.println(" urlPrefix " + urlPrefix);
		
		System.out.println("NAME=" + name);
		System.out.println("TITLE=" + title);
		System.out.println("CONTENT=" + content);
		
		Enumeration en = request.getParameterNames();
		while (en.hasMoreElements()) {
			System.out.println(" PRM " + en.nextElement());
		}
		
		String action = getPropertyValueFromModuleNode("action");
		System.out.println("ACTION+++" + action);
		WebFormBean formBean = new WebFormBean();
		
		PageNode node = (PageNode) request.getAttribute(URLMappingTemplateContextFilter.PAGENODE_REQUEST_ATTRIBUTE);
		
		formBean.setAction(urlPrefix + action); // + "/"); node.getRelativeContentPath());
		request.setAttribute("webform", formBean);
		
		String documentNodeUUID = (String) request.getSession().getAttribute("UUID");
		//verify that there is a 'current'document and that a form is submitted
		System.out.println("submit="  +  request.getParameter("submit") + "documentNodeUUID=" + documentNodeUUID);
		if (request.getParameter("submit") != null && documentNodeUUID != null) {
		    try {
		    	System.out.println("WRITE DOCUMENT=");
				writeNode(request, name, title, content, documentNodeUUID);
			} catch (Exception e) {
				throw new TemplateException(e);
			}
		}
		
		return action;
		}
		return null;
	}
	
	
	public void writeNode(HttpServletRequest request, String name, String title, String content, String documentNodeUUID) throws Exception {
		Session session = JCRConnectorWrapper.getJCRSession(request.getSession());
		Node rootNode = session.getRootNode();	
		
		Node documentReplyParentNode = null;
	    String parentNodeLocation = "content/reply/" + documentNodeUUID;
		//create node for reply's to the current document
		if (!session.itemExists("/" + parentNodeLocation)) {
			documentReplyParentNode = rootNode.addNode(parentNodeLocation);			
		} else {
			documentReplyParentNode = rootNode.getNode(parentNodeLocation);
		}
		
		//create the reply node, giving it the next number as name
		long size = documentReplyParentNode.getNodes().getSize();
		System.out.println("SIZE=" + size);
		
		Node newNode = rootNode.addNode(parentNodeLocation + "/" + size);
		newNode.setProperty("name", name);
		newNode.setProperty("title", title);
		newNode.setProperty("content", content);
		session.save();
	}


	public void init(HttpServletRequest request) {
	
	}


	public void render(PageContext pageContext) throws TemplateException {
		PageNode node = (PageNode) pageContext.getRequest().getAttribute(URLMappingTemplateContextFilter.PAGENODE_REQUEST_ATTRIBUTE);
		WebFormBean formBean = new WebFormBean();
		String urlPrefix = (String) pageContext.getRequest().getAttribute(ContextBaseFilter.URLBASE_INIT_PARAMETER);
		urlPrefix = (urlPrefix == null) ?  "" : urlPrefix;
		String action = getPropertyValueFromModuleNode("action");
		formBean.setAction(urlPrefix + action); // + "/"); node.getRelativeContentPath());
		pageContext.getRequest().setAttribute("webform", formBean);
		
		System.out.println("0000000000000000000000000000000000");
	}
}
