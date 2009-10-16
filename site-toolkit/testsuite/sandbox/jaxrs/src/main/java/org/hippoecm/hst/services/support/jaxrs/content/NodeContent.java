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
package org.hippoecm.hst.services.support.jaxrs.content;

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "node")
public class NodeContent extends ItemContent {
    
    private String uuid;
    private String [] property;
    private ItemContent [] childNodes;
    
    public NodeContent() {
        super();
    }
    
    public NodeContent(Node node) throws RepositoryException {
        super(node);
        
        if (node.isNodeType("mix:referenceable")) {
            this.uuid = node.getUUID();
        }
        
        ArrayList<String> propNames = new ArrayList<String>();
        
        for (PropertyIterator it = node.getProperties(); it.hasNext(); ) {
            Property prop = it.nextProperty();
            propNames.add(prop.getName());
        }
        
        this.property = new String[propNames.size()];
        this.property = propNames.toArray(this.property);
        
        ArrayList<ItemContent> itemContents = new ArrayList<ItemContent>();
        
        for (NodeIterator it = node.getNodes(); it.hasNext(); ) {
            Node childNode = it.nextNode();
            
            if (childNode != null) {
                itemContents.add(new ItemContent(childNode));
            }
        }
        
        childNodes = new ItemContent[itemContents.size()];
        childNodes = itemContents.toArray(childNodes);
    }
    
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public String [] getProperty() {
        return property;
    }
    
    public void setProperty(String [] property) {
        this.property = property;
    }
    
    public ItemContent [] getNode() {
        return childNodes;
    }
    
    public void setNode(ItemContent [] childNodes) {
        this.childNodes = childNodes;
    }
    
}
