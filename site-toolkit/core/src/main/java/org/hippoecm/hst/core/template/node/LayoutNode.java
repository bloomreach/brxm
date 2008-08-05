package org.hippoecm.hst.core.template.node;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.hippoecm.hst.core.template.ContextBase;

public class LayoutNode  extends TemplateNode{   
	public static final String TEMPLATEPAGE_PROPERTY_NAME = "hst:template";
   
    
    public LayoutNode(ContextBase contextBase, Node jcrPageLayoutNode) {
    	super(contextBase, jcrPageLayoutNode);
    }
    
  /*  public List<Node> getSubNodes() {
    	List<Node> subNodes = new ArrayList<Node>();
    	try {
			NodeIterator nodeIter = jcrNode.getNodes();
			while (nodeIter.hasNext()) {
				subNodes.add((Node) nodeIter.next());
			}
		} catch (RepositoryException e) {			
			e.printStackTrace();
			return null;
		}
    	return subNodes;
    } */
    
    public String getTemplatePage() throws PathNotFoundException, ValueFormatException, RepositoryException {
    	return jcrNode.getProperty(TEMPLATEPAGE_PROPERTY_NAME).getString();
    }
    
    public NodeList<LayoutAttributeNode> getSubNodes() throws RepositoryException {
    	return new NodeList(contextBase, jcrNode, LayoutAttributeNode.class);
    }
    
    
    
}
