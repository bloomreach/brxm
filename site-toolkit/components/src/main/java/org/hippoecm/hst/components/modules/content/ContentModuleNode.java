package org.hippoecm.hst.components.modules.content;

import javax.jcr.Node;

import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.node.el.AbstractELNode;

public class ContentModuleNode extends AbstractELNode{
  
	public ContentModuleNode(ContextBase ctxBase, Node node){
		super(ctxBase,node);
	}	
	
}
