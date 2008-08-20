package org.hippoecm.hst.components.modules.navigation;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.HstFilterBase;
import org.hippoecm.hst.core.template.TemplateException;
import org.hippoecm.hst.core.template.module.ModuleBase;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RepositoryBasedNavigationModule extends ModuleBase {
    private static final Logger log = LoggerFactory.getLogger(RepositoryBasedNavigationModule.class);


    /**
     * Puts an List of wrapped JCR Nodes on the pageContext that can be used by the corresponding JSP.
     * The name of the object can be set with an attribute named "var" on the corresponding module tag.
     *    
     * @see    PageContext
     */    
    public void render(PageContext pageContext) {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        ModuleNode currNode = (ModuleNode) request.getAttribute("currentModuleNode");

        String path = null;
        try {
        	path = getPropertyValueFromModuleNode(ModuleNode.CONTENTLOCATION_PROPERTY_NAME);	    	
		} catch (TemplateException e) {				
			log.error("Cannot get property " + ModuleNode.CONTENTLOCATION_PROPERTY_NAME, e);
		}
		
        if(path == null) {
            pageContext.setAttribute(getVar(),new ArrayList<NavigationItem>());
            return;
        }
        ContextBase ctxBase = (ContextBase) request.getAttribute(HstFilterBase.CONTENT_CONTEXT_REQUEST_ATTRIBUTE);
        List<NavigationItem> wrappedNodes = new ArrayList<NavigationItem>();
        try {
            Node n = ctxBase.getRelativeNode(path);
            NodeIterator subNodes = n.getNodes();
            while (subNodes.hasNext()) {
                Node subNode = subNodes.nextNode();
                if(subNode == null) {continue;}
                wrappedNodes.add(new NavigationItem(subNode));
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage(), e);
            wrappedNodes = new ArrayList();
        }
        
        pageContext.setAttribute(getVar(), wrappedNodes);

    }

}