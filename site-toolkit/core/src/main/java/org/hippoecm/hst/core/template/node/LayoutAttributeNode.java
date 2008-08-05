package org.hippoecm.hst.core.template.node;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.template.ContextBase;

public class LayoutAttributeNode extends TemplateNode {
	
	public LayoutAttributeNode(ContextBase contextBase, String relativePath) throws RepositoryException {		
		super(contextBase, relativePath);
		
	}

	public LayoutAttributeNode(ContextBase contextBase, Node jcrNode) {
		super(contextBase, jcrNode);	
	}

}
