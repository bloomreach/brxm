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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringUtils;

@XmlRootElement(name = "node")
public class NodeContent extends ItemContent {
    
    private String uuid;
    private PropertyContent [] propertyContents;
    private NodeContent [] nodeContents;
    
    public NodeContent() {
        super();
    }
    
    public NodeContent(String name) {
        super(name);
    }
    
    public NodeContent(String name, String path) {
        super(name, path);
    }
    
    public NodeContent(Node node) throws RepositoryException {
        super(node);
        
        if (node.isNodeType("mix:referenceable")) {
            this.uuid = node.getUUID();
        }
        
        ArrayList<PropertyContent> propContentList = new ArrayList<PropertyContent>();
        
        for (PropertyIterator it = node.getProperties(); it.hasNext(); ) {
            Property prop = it.nextProperty();
            propContentList.add(new PropertyContent(prop.getName(), prop.getPath()));
        }
        
        this.propertyContents = new PropertyContent[propContentList.size()];
        this.propertyContents = propContentList.toArray(this.propertyContents);
        
        ArrayList<NodeContent> nodeContentList = new ArrayList<NodeContent>();
        
        for (NodeIterator it = node.getNodes(); it.hasNext(); ) {
            Node childNode = it.nextNode();
            
            if (childNode != null) {
                nodeContentList.add(new NodeContent(childNode.getName(), childNode.getPath()));
            }
        }
        
        nodeContents = new NodeContent[nodeContentList.size()];
        nodeContents = nodeContentList.toArray(nodeContents);
    }
    
    @XmlAttribute
    public String getUuid() {
        return uuid;
    }
    
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
    
    public PropertyContent [] getProperty() {
        return propertyContents;
    }
    
    public void setProperty(PropertyContent [] propertyContents) {
        this.propertyContents = propertyContents;
    }
    
    public NodeContent [] getNode() {
        return nodeContents;
    }
    
    public void setNode(NodeContent [] nodeContents) {
        this.nodeContents = nodeContents;
    }
    
    public void buildUrl(String urlBase, String siteContentPath, String encoding) throws UnsupportedEncodingException {
        if (encoding == null) {
            encoding = "ISO-8859-1";
        }
        
        String relativeContentPath = "";
        
        String path = getPath();
        
        if (path != null && path.startsWith(siteContentPath)) {
            relativeContentPath = path.substring(siteContentPath.length());
        }
        
        if (relativeContentPath != null) {
            StringBuilder relativeContentPathBuilder = new StringBuilder(relativeContentPath.length());
            String [] pathParts = StringUtils.splitPreserveAllTokens(StringUtils.removeStart(relativeContentPath, "/"), '/');
            
            for (String pathPart : pathParts) {
                relativeContentPathBuilder.append('/').append(URLEncoder.encode(pathPart, encoding));
            }
            
            relativeContentPath = relativeContentPathBuilder.toString();
        }
        
        setUrl(URI.create(urlBase + relativeContentPath));
    }
    
    public void buildChildUrls(String urlBase, String siteContentPath, String encoding) throws UnsupportedEncodingException {
        if (propertyContents != null) {
            for (PropertyContent propertyContent : propertyContents) {
                propertyContent.buildUrl(urlBase, siteContentPath, encoding);
            }
        }
        
        if (nodeContents != null) {
            for (NodeContent nodeContent : nodeContents) {
                nodeContent.buildUrl(urlBase, siteContentPath, encoding);
            }
        }
    }
    
}
