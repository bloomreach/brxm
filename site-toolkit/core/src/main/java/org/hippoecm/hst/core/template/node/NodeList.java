package org.hippoecm.hst.core.template.node;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.template.ContextBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeList<E extends TemplateNode> {
    
    private Logger log = LoggerFactory.getLogger(NodeList.class);
    
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
        List<E> items = new ArrayList<E>();
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
        //a PageNode has childnode PageContainerNode
        String message;
        try {
            Class[] formalArgs = new Class[] { ContextBase.class, Node.class };
            Constructor constructor = listItemsClass.getConstructor(formalArgs);
            Object[] actualArgs = new Object[] { contextBase, n };
            E e = (E) constructor.newInstance(actualArgs);
            return e;

        } catch (InvocationTargetException e) {
            message = e.getTargetException().getClass().getName() + ": " + e.getTargetException().getMessage();
        } catch (Exception e) {
            message = e.getClass().getName() + ": " + e.getMessage();
        }
        log.error("failed to instantiate class " + listItemsClass.getName() + "\n " + message + "\n load default TemplateNode");
        // if error happened, return default TemplateNode
        return (E) new TemplateNode(contextBase, n);

    }

}
