package org.hippoecm.hst.core.template.node;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.template.ContextBase;

public class PageContainerNode extends TemplateNode {
	public static final String LAYOUT_ATTRIBUTE_PROPERTY = "hst:layoutAttribute";
    
    public PageContainerNode(ContextBase contextBase, Node jcrNode) {
 	   super(contextBase, jcrNode);	  
 	}
    
    
    public NodeList<PageContainerModuleNode> getModules() throws RepositoryException {
    	return new NodeList(contextBase, getJcrNode(), PageContainerModuleNode.class) ;
    }
    
    public String getLayoutAttributeValue() throws RepositoryException {
    	return jcrNode.getProperty(LAYOUT_ATTRIBUTE_PROPERTY).getValue().getString();
    }
}
