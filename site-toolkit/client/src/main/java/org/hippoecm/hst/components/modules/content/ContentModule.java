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
package org.hippoecm.hst.components.modules.content;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.exception.TemplateException;
import org.hippoecm.hst.core.filters.base.HstRequestContext;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentModule extends ModuleBase {

	private static final Logger log = LoggerFactory.getLogger(ContentModule.class);
	
	@Override
	public void render(PageContext pageContext, HstRequestContext hstRequestContext) throws TemplateException {
	
		String path = null;
		String uuid = null;
		
        if (moduleParameters != null) {
            if (moduleParameters.containsKey(ModuleNode.CONTENTLOCATION_PROPERTY_NAME)) {
                path = moduleParameters.get(ModuleNode.CONTENTLOCATION_PROPERTY_NAME);
            }      
            else if(moduleParameters.containsKey("uuid")){
            	uuid = moduleParameters.get("uuid");
            }   
        }
        else {
    		try {
    			path = getPropertyValueFromModuleNode(ModuleNode.CONTENTLOCATION_PROPERTY_NAME);
    		} catch (TemplateException e) {
    			log.error("Cannot get property " + ModuleNode.CONTENTLOCATION_PROPERTY_NAME, e);
    		}
        }
        
        if((path == null || path.equals("")) && (uuid==null || uuid.equals(""))) {
            pageContext.setAttribute(getVar(),null);
            return;
        }
		ContentModuleNode contentModuleNode = null;
		Node node=null;
		if(path!=null && !path.equals("")){
			node = hstRequestContext.getContentContextBase().getRelativeNode(path);	
		}
		else if(uuid!=null && !uuid.equals("")) {
		    Session session = null;
			try {
			    session = hstRequestContext.getRepository().login();
				node = session.getNodeByUUID(uuid);
				if(node.isNodeType(HippoNodeType.NT_HANDLE)){
					node = node.getNode(node.getName());
				}				 
			} catch (ItemNotFoundException e) {
				log.error("Cannot get Node by uuid ("+uuid+") : + "+ e.getCause());
			} catch (RepositoryException e) {
				log.error("Cannot get Node by uuid ("+uuid+") : + "+ e.getCause());
			}
			finally
			{
			    if (session != null)
			        session.logout();
			}
		}		
		contentModuleNode = new ContentModuleNode(node, hstRequestContext);
		pageContext.setAttribute(getVar(), contentModuleNode);
	}
}
