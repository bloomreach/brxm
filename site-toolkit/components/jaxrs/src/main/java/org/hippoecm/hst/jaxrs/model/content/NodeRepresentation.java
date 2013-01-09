/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlTransient;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;

/**
 * NodeRepresentation
 * @version $Id$
 */
public abstract class NodeRepresentation {
    
    private String name;
    private String localizedName;
    private String path;
    private String primaryNodeTypeName;
    private boolean leaf;
    private List<Link> links;
    
    public NodeRepresentation() {    	
    }
    
    public NodeRepresentation represent(HippoBean hippoBean) throws RepositoryException {
		this.name = hippoBean.getName();
		this.localizedName = hippoBean.getLocalizedName();
		
		this.path = hippoBean.getPath();
        
		// TODO: shouldn't primaryNodeType be added to hippoBean interface?
        primaryNodeTypeName = hippoBean.getNode().getPrimaryNodeType().getName(); 
        leaf = hippoBean.isLeaf();
        
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
    
    @XmlElement(name="path")
    public String getPath() {
        return path;
    }
    
    public String getPrimaryNodeTypeName() {
        return primaryNodeTypeName;
    }
    
    public void setPrimaryNodeTypeName(String primaryNodeTypeName) {
        this.primaryNodeTypeName = primaryNodeTypeName;
    }
    
    @XmlElement(name="leaf")
    public boolean isLeaf() {
    	return leaf;
    }
    
    @XmlElementWrapper(name="links")
    @XmlElements(@XmlElement(name="link"))
    public List<Link> getLinks() {
        return links;
    }
    
    public void setLinks(List<Link> links) {
        this.links = links;
    }
    
    public void addLink(Link link) {
    	if (link != null) {
            if (links == null) {
                links = new ArrayList<Link>();
            }        
            links.add(link);
    	}
    }
    
    /**
     * {@link #getRequestContext()} is a utility method that should not be serialized/deserialized hence the 
     * {@link XmlTransient} annotation
     * @return
     */
    @XmlTransient
    protected HstRequestContext getRequestContext() {
        return RequestContextProvider.get();
    }
}
