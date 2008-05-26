/*
 * Copyright 2008 Hippo.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.components;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.util.HSTNodeTypes;
import org.hippoecm.repository.api.HippoNodeType;

/**
 * Menu item component used by the menu component.  
 */
public class MenuItem {

    private final List<MenuItem> menuItems = new ArrayList<MenuItem>();
    
    private final int level;
    private final String path;
    private final String label;
    private boolean active = false;
    
    /**
     * Constructor.
     */
    public MenuItem(final Node node, final int level) {
        super();
        
        this.level = level;
        try {
            String label;
            if (node.hasProperty(HSTNodeTypes.PROPERTY_MENU_LABEL)) {
                label = node.getProperty(HSTNodeTypes.PROPERTY_MENU_LABEL).getString();
            }
            else {
                
                // capitalized node name as label  
                label = node.getName().substring(0, 1).toUpperCase();
                if (node.getName().length() > 1) {
                    label += node.getName().substring(1);
                }
            }
            
            this.label = label;
            this.path = node.getPath();
        } 
        catch (RepositoryException re) {
            throw new IllegalStateException(re);
        }
        
        createMenuItems(node);
    }

    public List<MenuItem> getItems() {
        return menuItems;
    }

    public String getLabel() {
        return label;
    }

    public int getLevel() {
        return level;
    }

    public String getPath() {
        return path;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(String activePath) {

        active = false;
        if (activePath.startsWith(path)) {
            active = true;
            
            Iterator<MenuItem> items = menuItems.iterator();
            while (items.hasNext()) {
                items.next().setActive(activePath);
            }
        }
    }

    // for debugging
    public String toString() {
        return super.toString() + "[level=" + level 
                + ", path=" + path + ", name=" + label 
                + ", menuItems=" + menuItems + "]";
    }
    
    private void createMenuItems(final Node node) {
        
        try {
           
            // loop the subnodes and create items from them if applicable
            NodeIterator subNodes =  node.getNodes();
            while (subNodes.hasNext()) {
                
                Node subNode = (Node) subNodes.next();
                
                // on level higher than 0, absence of the property means to  
                // create one if it concerns a document handle or folder 
                if (!subNode.hasProperty(HSTNodeTypes.PROPERTY_IS_MENU_ITEM)) {

                    // skip documents as there are multiple variants
                    if (subNode.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                        continue;
                    }
                    
                    if (subNode.isNodeType(HippoNodeType.NT_UNSTRUCTURED)
                          || subNode.isNodeType("nt:unstructured")) {
                        menuItems.add(new MenuItem(subNode, this.getLevel() + 1));
                    }
                }
                else {
                    // check flag
                    if (subNode.getProperty(HSTNodeTypes.PROPERTY_IS_MENU_ITEM).getBoolean()) {
                        menuItems.add(new MenuItem(subNode, this.getLevel() + 1));
                    }
               }
            }
        } 
        catch (RepositoryException re) {
            throw new IllegalStateException(re);
        }   
    }
}