package org.hippoecm.hst.core.template.node;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.template.ContextBase;

import com.sun.corba.se.spi.activation.Repository;

public class NodeList<E extends TemplateNode> {
    private Node mapNode;
    private Class listItemsClass;
    private List<E> listItems;
    private ContextBase contextBase;
    
    public NodeList() {
    	listItems = new ArrayList<E>();
    }
    
	public NodeList(ContextBase contextBase, String relativePath, Class itemsClass) throws RepositoryException {
		this.contextBase = contextBase;
		this.mapNode = contextBase.getRelativeNode(relativePath);		
		this.listItemsClass = itemsClass;	
		listItems = createItemList();
	}
	
	public NodeList(ContextBase contextBase, Node mapNode, Class itemsClass) throws RepositoryException {
		this.contextBase = contextBase;
		this.mapNode = mapNode;
		this.listItemsClass = itemsClass;	
		listItems = createItemList();
	}
	
	public List<E> getItems() {
		return listItems;
	}
	
	public E getItemByNodeName(String nodeName) throws RepositoryException {
		for (E item : listItems) {
			if (item.getJcrNode().getName().equals(nodeName)) {
				return item;
			}
		}
		return null;
	}
	
	private List<E> createItemList() {
		List<E> items = new ArrayList<E> ();
		try {
			NodeIterator iter = mapNode.getNodes();
			while (iter.hasNext()) {
				Node node = (Node) iter.next();
				items.add(getNode(node));
			}
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return items;
	}
	
	private E getNode(Node n) {
		//a PageNode has childnode PageContainerNod
	
		if (listItemsClass.equals(PageContainerNode.class)) {
		    return (E) new PageContainerNode(contextBase, n);		  
		}
		if (listItemsClass.equals(TemplatePartNode.class)) {
			return (E) new TemplatePartNode(contextBase, n);
		}
		if (listItemsClass.equals(PageContainerModuleNode.class)) {
			return (E) new PageContainerModuleNode(contextBase, n);
		}
		if (listItemsClass.equals(LayoutAttributeNode.class)) {
			return (E) new LayoutAttributeNode(contextBase, n);
		}
		if (listItemsClass.equals(NavigationItemNode.class)) {
			return (E) new NavigationItemNode(contextBase, n);
		}
		return (E) new TemplateNode(contextBase, n);		
	}
	
	
}
