package org.hippoecm.hst.components.modules.content;

import javax.jcr.Node;

import org.hippoecm.hst.core.template.node.el.AbstractELNode;

public class ContentModuleNode extends AbstractELNode{
  
	Node node = null;
	
	public ContentModuleNode(Node node){
		super(node);
		this.node = node;
	}	
	
}
