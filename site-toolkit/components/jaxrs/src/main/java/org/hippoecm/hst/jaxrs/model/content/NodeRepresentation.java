/*
 *  Copyright 2010 Hippo.
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
package org.hippoecm.hst.jaxrs.model.content;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.content.beans.standard.HippoBean;

/**
 * @version $Id$
 *
 */
@XmlRootElement(name = "node")
public class NodeRepresentation {

    private String name;
    private String localizedName;
    private String path;
    private String primaryNodeTypeName;
    private boolean leaf;
    private List<NodeProperty> properties;
    
    public NodeRepresentation() {    	
    }
    
	public NodeRepresentation represent(HippoBean hippoBean, Set<String> propertyFilters) throws RepositoryException {
		this.name = hippoBean.getName();
		this.localizedName = hippoBean.getLocalizedName();
		
		this.path = hippoBean.getPath();
        
		// TODO: shouldn't primaryNodeType be added to hippoBean interface?
        primaryNodeTypeName = hippoBean.getNode().getPrimaryNodeType().getName(); 
        leaf = hippoBean.isLeaf();
        
        properties = new ArrayList<NodeProperty>();
        
        for (PropertyIterator it = hippoBean.getNode().getProperties(); it.hasNext(); ) {
            Property prop = it.nextProperty();
            
            if (propertyFilters == null || propertyFilters.contains(prop.getName())) {
                properties.add(new NodeProperty(prop));
            }
        }
        
        return this;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getLocalizedName() {
    	return localizedName;
    }
    
    public void setLocalizedName(String localizedName) {
    	this.localizedName = localizedName;
    }
    
    public String getPath() {
        return path;
    }
    
    public String getPrimaryNodeTypeName() {
        return primaryNodeTypeName;
    }
    
    public void setPrimaryNodeTypeName(String primaryNodeTypeName) {
        this.primaryNodeTypeName = primaryNodeTypeName;
    }
    
    public boolean isLeaf() {
    	return leaf;
    }
    
    @XmlElementWrapper(name="properties")
    @XmlElements(@XmlElement(name="property"))
    public List<NodeProperty> getProperties() {
        return properties;
    }
    
    public void setProperties(List<NodeProperty> properties) {
        this.properties = properties;
    }
    
    public NodeProperty getProperty(String propertyName) {
        if (properties != null) {
            for (NodeProperty property : properties) {
                if (property.getName().equals(propertyName)) {
                    return property;
                }
            }
        }
        return null;
    }
}
