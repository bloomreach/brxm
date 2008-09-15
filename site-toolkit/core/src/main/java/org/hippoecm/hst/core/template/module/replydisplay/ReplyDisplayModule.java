package org.hippoecm.hst.core.template.module.replydisplay;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.jcr.JcrSessionFactory;

public class ReplyDisplayModule extends ModuleBase {

    public String execute(HttpServletRequest request, HttpServletResponse response) throws TemplateException {   
        Session session =  JcrSessionFactory.getSession(request);
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


    public void render(PageContext pageContext) {
        Session session =  JcrSessionFactory.getSession((HttpServletRequest)pageContext.getRequest());
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

