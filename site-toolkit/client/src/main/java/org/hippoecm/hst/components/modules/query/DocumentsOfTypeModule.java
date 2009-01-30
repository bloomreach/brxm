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
package org.hippoecm.hst.components.modules.query;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.exception.TemplateException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.core.template.module.query.ContextWhereClause;
import org.hippoecm.hst.core.template.node.el.ContentELNodeImpl;
import org.hippoecm.hst.core.template.node.el.ELNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentsOfTypeModule extends ModuleBase {
	
	private static final Logger log = LoggerFactory.getLogger(DocumentsOfTypeModule.class);
	public static final String DOCUMENT_TYPE = "documentType";
	private String docType="";
	
	@Override
	public void render(PageContext pageContext, HstRequestContext hstRequestContext) throws TemplateException {
		
        List<ELNode> wrappedNodes = new ArrayList<ELNode>();
        
        boolean params = false;
        if (moduleParameters != null) {
            params = true;
        }
        if (params && moduleParameters.containsKey(DOCUMENT_TYPE)) {
            String type = moduleParameters.get(DOCUMENT_TYPE);
            if (!"".equals(type)) {
                setDocumentType(type);
            }
        }
        
        wrappedNodes = getDocuments(hstRequestContext);
        pageContext.setAttribute(getVar(), wrappedNodes);
	
	}
        
    private void setDocumentType(String type){
      this.docType=type;	
    }
	
	private List<ELNode> getDocuments(HstRequestContext hstRequestContext){
		List<ELNode> wrappedNodes = new ArrayList<ELNode>();
		if(docType!=null || !docType.equals("")) {
		    Session session = null;
	        try {
	            ContextWhereClause ctxWhereClause = new ContextWhereClause(hstRequestContext.getContentContextBase().getContextRootNode(), "content");
	            String contextWhereClauses = ctxWhereClause.getWhereClause();
	            String xpath = "//*["+contextWhereClauses+ " and @jcr:primaryType='"+docType+"']";
	            session = hstRequestContext.getRepository().login();
	            QueryManager qMgr = session.getWorkspace().getQueryManager();
	            QueryResult result = qMgr.createQuery(xpath,Query.XPATH).execute();
	            NodeIterator iter = result.getNodes();
	            while (iter.hasNext()) {
	                Node node = iter.nextNode();
                	wrappedNodes.add(new ContentELNodeImpl(node,hstRequestContext));
	            }
	            
	        } catch (RepositoryException ex) {
	           ex.printStackTrace();
	        }
	        finally
	        {
	            if (session != null)
	                session.logout();
	        }
		}
        return wrappedNodes;
	}
}
