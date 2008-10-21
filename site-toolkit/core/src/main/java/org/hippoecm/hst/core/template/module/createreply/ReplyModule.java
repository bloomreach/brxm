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
package org.hippoecm.hst.core.template.module.createreply;

import java.util.Enumeration;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.URLMappingTemplateContextFilter;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.core.template.module.form.el.WebFormBean;
import org.hippoecm.hst.core.template.node.PageNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplyModule extends ModuleBase {

    private static final Logger log = LoggerFactory.getLogger(ReplyModule.class);
    
	public String execute(HttpServletRequest request, HttpServletResponse response) throws TemplateException {
		if (request.getParameter("MODULE_NAME") != null) {
		String name = request.getParameter("name");
		String title = request.getParameter("title");
		String content = request.getParameter("content");
		
		String urlPrefix = (String) request.getAttribute(HSTHttpAttributes.URI_PREFIX_REQ_ATTRIBUTE);
		urlPrefix = (urlPrefix == null) ?  "" : urlPrefix;
		log.debug(" urlPrefix " + urlPrefix);
		
		log.debug("NAME=" + name);
		log.debug("TITLE=" + title);
		log.debug("CONTENT=" + content);
		
		Enumeration en = request.getParameterNames();
		if(log.isDebugEnabled()) { 
    		while (en.hasMoreElements()) {
    			log.debug(" PRM " + en.nextElement());
    		}
		}
		
		String action = getPropertyValueFromModuleNode("action");
		log.debug("ACTION: " + action);
		WebFormBean formBean = new WebFormBean();
		
		PageNode node = (PageNode) request.getAttribute(URLMappingTemplateContextFilter.PAGENODE_REQUEST_ATTRIBUTE);
		
		formBean.setAction(urlPrefix + action); // + "/"); node.getRelativeContentPath());
		request.setAttribute("webform", formBean);
		
		String documentNodeUUID = (String) request.getSession().getAttribute("UUID");
		//verify that there is a 'current'document and that a form is submitted
		log.debug("submit="  +  request.getParameter("submit") + "documentNodeUUID=" + documentNodeUUID);
		if (request.getParameter("submit") != null && documentNodeUUID != null) {
		    try {
		        log.debug("WRITE DOCUMENT");
				writeNode(request, name, title, content, documentNodeUUID);
			} catch (Exception e) {
				throw new TemplateException(e);
			}
		}
		return urlPrefix + action;
		}
		return null;
	}
	
	
	public void writeNode(HttpServletRequest request, String name, String title, String content, String documentNodeUUID) throws Exception {
		Session session =   (Session)request.getAttribute(HSTHttpAttributes.JCRSESSION_MAPPING_ATTR);
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
		
		Node newNode = rootNode.addNode(parentNodeLocation + "/" + size);
		newNode.setProperty("name", name);
		newNode.setProperty("title", title);
		newNode.setProperty("content", content);
		session.save();
	}


	public void render(PageContext pageContext) throws TemplateException {
		PageNode node = (PageNode) pageContext.getRequest().getAttribute(URLMappingTemplateContextFilter.PAGENODE_REQUEST_ATTRIBUTE);
		WebFormBean formBean = new WebFormBean();
		String urlPrefix = (String) pageContext.getRequest().getAttribute(HSTHttpAttributes.URI_PREFIX_REQ_ATTRIBUTE);
		urlPrefix = (urlPrefix == null) ?  "" : urlPrefix;
		String action = getPropertyValueFromModuleNode("action");
		String applicationContext = ((HttpServletRequest) pageContext.getRequest()).getContextPath();
		formBean.setAction(applicationContext + urlPrefix + action); // + "/"); node.getRelativeContentPath());
		pageContext.getRequest().setAttribute("webform", formBean);
		
	}

}
