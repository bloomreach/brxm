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

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.repository.api.HippoNode;

/**
 * HippoBeanContent
 * 
 * @version $Id$
 */
@XmlRootElement(name = "node")
public class HippoBeanContent extends NodeContent {
    
    private HippoBean bean;
    private String canonicalUuid;
    
    public HippoBeanContent() {
        super();
    }
    
    public HippoBeanContent(String name) {
        super(name);
    }
    
    public HippoBeanContent(String name, String path) {
        super(name, path);
    }
    
    public HippoBeanContent(HippoBean bean) throws RepositoryException {
        this(bean, null);
    }
    
    public HippoBeanContent(HippoBean bean, final Set<String> propertyNamesFilledWithValues) throws RepositoryException {
        super(bean.getNode(), propertyNamesFilledWithValues);
        
        this.bean = bean;
        
        if (this.bean.getNode().isNodeType("mix:referenceable")) {
            this.canonicalUuid = bean.getCanonicalUUID();
        }
    }
    
    public HippoBean getBean() {
        return bean;
    }
    
    @XmlAttribute
    public String getCanonicalUuid() {
        return canonicalUuid;
    }
    
    public void setCanonicalUuid(String canonicalUuid) {
        this.canonicalUuid = canonicalUuid;
    }
    
    /**
     * 
     * @return the canonical JCR Node of the backing JCR Node of the HippoBean or <code>null</code> when there is no canonical or the backing jcr node is <code>null</code> 
     * @throws RepositoryException
     */
    public Node getCanonicalNode() throws RepositoryException {
        Node node = getBean().getNode();
        
        if (node == null) {
            return null;
        }
        
        if (node instanceof HippoNode) {
            return ((HippoNode) node).getCanonicalNode();
        }
        
        return node;
    }
}
