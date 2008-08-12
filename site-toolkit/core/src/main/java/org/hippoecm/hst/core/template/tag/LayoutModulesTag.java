package org.hippoecm.hst.core.template.tag;

import java.io.IOException;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.hippoecm.hst.core.template.URLMappingTemplateContextFilter;
import org.hippoecm.hst.core.template.node.ModuleNode;
import org.hippoecm.hst.core.template.node.NodeList;
import org.hippoecm.hst.core.template.node.PageContainerModuleNode;
import org.hippoecm.hst.core.template.node.PageContainerNode;
import org.hippoecm.hst.core.template.node.PageNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LayoutModulesTag extends SimpleTagSupport {
	private static final Logger log = LoggerFactory.getLogger(LayoutModulesTag.class);
	
    private String name;
	@Override
	public void doTag() throws JspException, IOException {
	
		
		PageContext pageContext = (PageContext) getJspContext(); 
		HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();    	    	
        PageNode pageNode = (PageNode) request.getAttribute(URLMappingTemplateContextFilter.PAGENODE_REQUEST_ATTRIBUTE);
        NodeList<PageContainerNode> containerList = pageNode.getContainers();
        PageContainerNode pcNode = pageNode.getContainerNodeByName(getName());
        
        //getModules
        NodeList<PageContainerModuleNode> pcNodeModules = null;
        try {
        	System.out.println("pcNode: " + pcNode);
			pcNodeModules = pcNode.getModules();
		} catch (RepositoryException e) {
			log.error("Cannot get modules", e);
			pcNodeModules = new NodeList<PageContainerModuleNode>();
		}
        List<PageContainerModuleNode> pcmList = pcNodeModules.getItems();;
    
        for (int index=0; index < pcmList.size(); index++) {
        	try {
        		PageContainerModuleNode pcm = pcmList.get(index);
				ModuleNode moduleNode = pcm.getModuleNode();
				moduleNode.setPageContainerModuleNode(pcm);				
				request.setAttribute("currentModuleNode", moduleNode);
				pageContext.include(moduleNode.getTemplatePage());
			} catch (RepositoryException e) {
				log.error("RepositoryException:", e);
				throw new JspException(e);
			} catch (ServletException e) {
				throw new JspException(e);
			}
        }
       
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

}
