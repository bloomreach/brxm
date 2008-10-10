package org.hippoecm.hst.components.modules.content;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.mapping.URLMapping;
import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.HstFilterBase;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.repository.api.HippoNodeType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContentModule extends ModuleBase {

	private static final Logger log = LoggerFactory.getLogger(ContentModule.class);
	
	public void render(PageContext pageContext) throws TemplateException {
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

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
        
        if(path == null && uuid==null) {
            pageContext.setAttribute(getVar(),null);
            return;
        }
		
		ContextBase ctxBase = (ContextBase) request.getAttribute(HstFilterBase.CONTENT_CONTEXT_REQUEST_ATTRIBUTE);

		ContentModuleNode contentModuleNode = null;
		Node node=null;
		
		if(path!=null && !path.equals("")){
			node = ctxBase.getRelativeNode(path);	
		}
		else if(uuid!=null && !uuid.equals("")) {
			try {
				node = ctxBase.getSession().getNodeByUUID(uuid);
				if(node.isNodeType(HippoNodeType.NT_HANDLE)){
					node = node.getNode(node.getName());
				}				 
			} catch (ItemNotFoundException e) {
				log.error("Connect get Node by uuid ("+uuid+") : + "+ e.getCause());
			} catch (RepositoryException e) {
				log.error("Connect get Node by uuid ("+uuid+") : + "+ e.getCause());
			}
		}
		if(path!=null || uuid!=null) {		
			URLMapping urlMapping = (URLMapping)request.getAttribute(HSTHttpAttributes.URL_MAPPING_ATTR);
			contentModuleNode = new ContentModuleNode(ctxBase, node, urlMapping);
		}

		pageContext.setAttribute(getVar(), contentModuleNode);
	}
}
