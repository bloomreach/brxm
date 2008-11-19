/*
 *  Copyright 2008 Hippo.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.core.template.node;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.core.context.ContextBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A list wrapper for lists with TemplateNode elements.
 *
 * @param <E> Type of list elements.
 */
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
            log.error("ItemList creation failed", e);
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
