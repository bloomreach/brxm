package org.hippoecm.hst.core.template.module.replydisplay;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.jcr.JCRConnector;
import org.hippoecm.hst.jcr.JCRConnectorWrapper;

public class ReplyDisplayModule extends ModuleBase {

	public String execute(HttpServletRequest request, HttpServletResponse response) throws TemplateException {
		Session session = JCRConnectorWrapper.getJCRSession(request.getSession());				
        try {
			Node rootNode = session.getRootNode();	
			
			String documentNodeUUID = (String) request.getSession().getAttribute("UUID");
			if (documentNodeUUID != null) {
				Node documentReplyParentNode = null;
			    String parentNodeLocation = "content/reply/" + documentNodeUUID;
			    
			    if (session.itemExists("/" + parentNodeLocation)) {
			       Node repliesNode = rootNode.getNode(parentNodeLocation);
			       NodeIterator nodeIter = repliesNode.getNodes();
			       List elNodes = new ArrayList();
			       while (nodeIter.hasNext()) {
			    	   elNodes.add(new ReplyBean((Node) nodeIter.next()));
			       }
			       request.setAttribute("replies", elNodes);
			    }
			} 
			
		} catch (Exception e) {
			throw new TemplateException(e);
		}
		
		return null;
		
	}

	public void init(HttpServletRequest request) {
		// TODO Auto-generated method stub
		
	}

	public void render(PageContext pageContext) {
		Session session = JCRConnectorWrapper.getJCRSession(pageContext.getSession());				
        try {
			Node rootNode = session.getRootNode();	
			
			String documentNodeUUID = (String) pageContext.getSession().getAttribute("UUID");
			if (documentNodeUUID != null) {
				Node documentReplyParentNode = null;
			    String parentNodeLocation = "content/reply/" + documentNodeUUID;
			    
			    if (session.itemExists("/" + parentNodeLocation)) {
			       Node repliesNode = rootNode.getNode(parentNodeLocation);
			       NodeIterator nodeIter = repliesNode.getNodes();
			       List elNodes = new ArrayList();
			       while (nodeIter.hasNext()) {
			    	   elNodes.add(new ReplyBean((Node) nodeIter.next()));
			       }
			       pageContext.setAttribute("replies", elNodes);
			    }
			} 
			
		} catch (Exception e) {
		    e.printStackTrace();
		}
		
	}
}

