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
package org.hippoecm.repository.standardworkflow;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeTypeManager;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.updater.UpdaterNode;
import org.hippoecm.repository.updater.UpdaterProperty;

class ChangeImpl {
    @SuppressWarnings("unused")
    private static final String SVN_ID = "$Id$";

    private ChangeType changeType;
    private String itemRelPath;
    private String itemParentRelPath = null;
    private String itemName;
    private String newName = null;
    private boolean isNode = true; // assume it is a node unless otherwise detected
    private Node prototypeNode = null;
    private Value prototypeValue = null;
    private Value[] prototypeValues = null;
    private ChangeImpl(Change change, Session session) throws RepositoryException {
        changeType = change.type;
        itemRelPath = change.relPath;

        int pos = itemRelPath.lastIndexOf("/");
        if(pos > 0) {
            itemParentRelPath = itemRelPath.substring(0, pos);
            itemName = itemRelPath.substring(pos+1);
            if(itemName.startsWith("@")) {
                isNode = false;
                itemName = itemName.substring(1);
                itemRelPath = itemParentRelPath + "/" + itemName;
            }
        } else {
            if(itemRelPath.startsWith("@")) {
                itemRelPath = itemRelPath.substring(1);
            }
            itemName = itemRelPath;
        }
        if(itemName.contains("[")) {
            // warning: deleting or adding a specific item isn't possible anyway
            itemName = itemName.substring(0, itemName.indexOf("["));
        }

        switch(changeType) {
        case ADDITION:
            Item item = session.getItem(change.absPath);
            if((isNode = item.isNode())) {
                prototypeNode = (Node) item;
            } else {
                Property property = (Property) item;
                if(property.getDefinition().isMultiple()) {
                    prototypeValues = property.getValues();
                } else {
                    prototypeValue = property.getValue();
                }
            }
            break;
        case DROPPED:
            break;
        case RENAMED:
            newName = change.newName;
            break;
        }
    }
    static ChangeImpl create(Change change, Session session) throws RepositoryException {
        return new ChangeImpl(change, session);
    }
    boolean change(UpdaterNode node) throws RepositoryException {
        boolean changed = false;
        switch(changeType) {
        case ADDITION:
            if(isNode) {
                if(!node.hasNode(itemRelPath)) {
                    ((HippoSession)node.getSession()).copy(prototypeNode, node.getPath()+"/"+itemRelPath);
                    changed = true;
                }
            } else {
                if(!node.hasProperty(itemRelPath)) {
                    if(prototypeValue == null) {
                        node.setProperty(itemRelPath, prototypeValues);
                    } else {
                        node.setProperty(itemRelPath, prototypeValue);
                    }
                    changed = true;
                }
            }
            break;
        case DROPPED:
            if(isNode || node.hasNode(itemRelPath)) {
                if(node.hasNode(itemRelPath)) {
                    Node parent = (itemParentRelPath != null ? node.getNode(itemParentRelPath) : node);
                    for(NodeIterator iter = parent.getNodes(itemName); iter.hasNext(); ) {
                        iter.nextNode().remove();
                        changed = true;
                    }
                }
            } else {
                if(node.hasProperty(itemRelPath)) {
                    node.getProperty(itemRelPath).remove();
                    changed = true;
                }
            }
            break;
        case RENAMED:
            if(isNode || node.hasNode(itemRelPath)) {
                if(node.hasNode(itemRelPath)) {
                    Node parent = (itemParentRelPath != null ? node.getNode(itemParentRelPath) : node);
                    for(NodeIterator iter = parent.getNodes(itemName); iter.hasNext(); ) {
                        ((UpdaterNode)iter.nextNode()).setName(newName);
                        changed = true;
                    }
                }
            } else {
                if(node.hasProperty(itemRelPath)) {
                    ((UpdaterProperty)node.getProperty(itemRelPath)).setName(newName);
                    changed = true;
                }
            }
            break;
        }
        return changed;
    }
    static Map<String,List<ChangeImpl>> convert(Map<String,List<Change>> changes, Session session) throws RepositoryException {
        NodeTypeManager ntMgr = session.getWorkspace().getNodeTypeManager();
        Map<String,List<ChangeImpl>> map = new TreeMap<String,List<ChangeImpl>>();
        for(Map.Entry<String,List<Change>> entry : changes.entrySet()) {
            // check whether nodetype exists.  If it doesn't, there is nothing to do!
            try {
                /* NodeType nt = */ ntMgr.getNodeType(entry.getKey());
            } catch (NoSuchNodeTypeException nte) {
                continue;
            }
            List<ChangeImpl> list = new LinkedList<ChangeImpl>();
            map.put(entry.getKey(), list);
            for(Change change : entry.getValue()) {
                list.add(create(change, session));
            }
        }
        return map;
    }
    static boolean change(UpdaterNode node, Map<String,List<ChangeImpl>> changes) throws RepositoryException {
        boolean changed = false;
        for(Map.Entry<String,List<ChangeImpl>> entry : changes.entrySet()) {
            if(node.isNodeType(entry.getKey())) {
                for(ChangeImpl change : entry.getValue()) {
                    changed |= change.change(node);
                }
            }
        }
        return changed;
    }
}
