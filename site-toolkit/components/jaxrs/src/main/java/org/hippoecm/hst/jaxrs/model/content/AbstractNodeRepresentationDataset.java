/*
 * Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.jaxrs.model.content;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;

public abstract class AbstractNodeRepresentationDataset extends AbstractDataset {
    
    private static final long serialVersionUID = 1L;
    
    private List<? extends NodeRepresentation> nodeRepresentations;
    private List<Link> links;
    
    public AbstractNodeRepresentationDataset() {
        
    }
    
    protected List<? extends NodeRepresentation> getNodeRepresentations() {
        return nodeRepresentations;
    }
    
    public void setNodeRepresentations(List<? extends NodeRepresentation> nodeRepresentations) {
        this.nodeRepresentations = nodeRepresentations;
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
        if (links == null) {
            links = new ArrayList<Link>();
        }
        
        links.add(link);
    }
}
