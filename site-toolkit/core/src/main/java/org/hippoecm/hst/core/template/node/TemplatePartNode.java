package org.hippoecm.hst.core.template.node;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.template.ContextBase;

public class TemplatePartNode extends TemplateNode {
    public TemplatePartNode(ContextBase contextBase, String relativePath) throws RepositoryException {
    	super(contextBase, relativePath);
    }
    
    public TemplatePartNode(ContextBase contextBase, Node jcrNode) {
    	super(contextBase, jcrNode);
    }
}
