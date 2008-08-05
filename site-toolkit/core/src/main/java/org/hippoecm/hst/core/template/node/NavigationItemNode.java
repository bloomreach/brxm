package org.hippoecm.hst.core.template.node;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.core.template.ContextBase;

public class NavigationItemNode extends TemplateNode {
	public static final String SITEMAPITEM_ATTRIBUTE_PROPERTY = "hst:sitemapitem";
	public static final String TITLE_ATTRIBUTE_PROPERTY = "hst:title";
	
	public NavigationItemNode(ContextBase contextBase, String relativePath) throws RepositoryException {		
		super(contextBase, relativePath);		
	}

	public NavigationItemNode(ContextBase contextBase, Node jcrNode) {
		super(contextBase, jcrNode);	
	}
	
	public String getSiteMapItemValue() throws RepositoryException {
	    return jcrNode.getProperty(SITEMAPITEM_ATTRIBUTE_PROPERTY).getValue().getString();
	}
	
	public String getTitleValue() throws RepositoryException {
	    return jcrNode.getProperty(TITLE_ATTRIBUTE_PROPERTY).getValue().getString();
	}
	
	public NodeList<NavigationItemNode> getNavigationSubItems() throws RepositoryException {
		return new NodeList(contextBase, getJcrNode(), NavigationItemNode.class) ;
	}
	
	public String getValueByPropertyName(String propertyName) throws RepositoryException {
		 return jcrNode.getProperty(propertyName).getValue().getString();
	}

}
