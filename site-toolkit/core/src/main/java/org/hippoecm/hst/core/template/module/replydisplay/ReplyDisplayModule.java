/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.core.template.module.replydisplay;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.jcr.JcrSessionPoolManager;

public class ReplyDisplayModule extends ModuleBase {

    public String execute(HttpServletRequest request, HttpServletResponse response) throws TemplateException {   
        Session session =   (Session)request.getAttribute(HSTHttpAttributes.JCRSESSION_MAPPING_ATTR);
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
        HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();
        Session session =  (Session)request.getAttribute(HSTHttpAttributes.JCRSESSION_MAPPING_ATTR);
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

