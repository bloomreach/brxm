package org.hippoecm.hst.core.template.node;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.template.ContextBase;

public class NavigationNode extends TemplateNode {
	
	public NavigationNode(ContextBase contextBase, String relativePath) throws RepositoryException {		
		super(contextBase, relativePath);
		
	}

	public NavigationNode(ContextBase contextBase, Node jcrNode) {
		super(contextBase, jcrNode);	
	}
	
	 public NodeList<NavigationItemNode> getNavigationItems() throws RepositoryException {
	    return new NodeList(contextBase, getJcrNode(), NavigationItemNode.class) ;
	 }
	
}
