package org.hippoecm.hst.components.modules.content;

import javax.jcr.Node;

import org.hippoecm.hst.core.HSTHttpAttributes;
import org.hippoecm.hst.core.mapping.URLMapping;
import org.hippoecm.hst.core.template.ContextBase;
import org.hippoecm.hst.core.template.node.el.ContentELNodeImpl;

public class ContentModuleNode extends ContentELNodeImpl{
  
	public ContentModuleNode(ContextBase ctxBase, Node node, URLMapping urlMapping){
		super(node, urlMapping);
	}	
	
}
