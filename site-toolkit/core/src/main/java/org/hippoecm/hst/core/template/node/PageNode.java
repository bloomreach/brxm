package org.hippoecm.hst.core.template.node;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

import org.hippoecm.hst.core.template.ContextBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PageNode extends TemplateNode{
   private Logger log = LoggerFactory.getLogger(PageNode.class);	 
   public static final String LAYOUTPAGE_PROPERTY_NAME = "hst:pageLayout";
   public static final String URLMAPPING_PROPERTY_NAME = "hst:urlmapping";
   
   private NodeList<PageContainerNode> containerNodes;
   
   public PageNode(ContextBase contextBase, Node jcrNode) throws RepositoryException {
	   super(contextBase, jcrNode);
	   containerNodes = new NodeList(contextBase, getJcrNode(), PageContainerNode.class);
   }
   
   public PageNode(ContextBase contextBase, String relativePath) throws RepositoryException {
	   super(contextBase, relativePath);	  
	   jcrNode = contextBase.getRelativeNode(relativePath);
	   containerNodes = new NodeList(contextBase, getJcrNode(), PageContainerNode.class);
   }
   
   public LayoutNode getLayoutNode() throws RepositoryException {	   
	    return new LayoutNode(contextBase, getTemplateNodeFromPropertyValue(LAYOUTPAGE_PROPERTY_NAME));
   }
   
   public String getURLMappingValue() throws RepositoryException {
	   return jcrNode.getProperty(URLMAPPING_PROPERTY_NAME).getValue().getString();
   }
  
   public NodeList<PageContainerNode> getContainers() {
	   return containerNodes;
   }
   
   public ModuleNode getModuleNodeByName(String nodeName) throws RepositoryException {	   
	   List<PageContainerNode> cNodes = getContainers().getItems();
	   for (int i= 0; cNodes != null && i < cNodes.size(); i++) {
		   List<PageContainerModuleNode> pNodeList = cNodes.get(i).getModules().getItems();
		   for (int j=0; pNodeList != null && j < pNodeList.size(); j++) {
			   PageContainerModuleNode pcmNode = pNodeList.get(j);
			   ModuleNode moduleNode = pcmNode.getModuleNode();
			   if (moduleNode.getJcrNode().getName().equals(nodeName)) {
				   return moduleNode;
			   }
		   }
	   }
	   return null;
   }
   
   public PageContainerNode getContainerNodeByName(String layoutAttributeName) {
	   try {
		for (PageContainerNode containerNode : containerNodes.getItems()) {
			   if (containerNode.getLayoutAttributeValue().equals(layoutAttributeName)) {
				   return containerNode;
			   }
		   }
	} catch (RepositoryException e) {
	    log.error("No containerNode with layoutAttribute " + layoutAttributeName);
	}
	   return null;
   }
   
   
 }
