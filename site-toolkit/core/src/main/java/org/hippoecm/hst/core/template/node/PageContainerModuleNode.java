package org.hippoecm.hst.core.template.node;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.template.ContextBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageContainerModuleNode extends TemplateNode {
private Logger log = LoggerFactory.getLogger(PageContainerModuleNode.class);	 
	
	public static final String MODULE_NAME_PROPERTY = "hst:module";
	
	public PageContainerModuleNode(ContextBase contextBase, String relativePath) throws RepositoryException {
		super(contextBase, relativePath);		
	} 
	
	public PageContainerModuleNode(ContextBase contextBase, Node jcrNode) {
		super(contextBase, jcrNode);		
	} 
	
	public ModuleNode getModuleNode() throws RepositoryException {	 	
	    return new ModuleNode(contextBase, getTemplateNodeFromPropertyValue(MODULE_NAME_PROPERTY));
	}
}
