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

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang.StringUtils;

/**
 * ItemContent
 * 
 * @version $Id$
 */
@XmlRootElement(name = "item")
public class ItemContent {
    
    private String name;
    private String path;
    private URI uri;
    private transient Item item;
    
    public ItemContent() {
    }
    
    public ItemContent(String name) {
        this(name, null);
    }
    
    public ItemContent(String name, String path) {
        this.name = name;
        this.path = path;
    }
    
    public ItemContent(Item item) throws RepositoryException {
        this(item.getName(), item.getPath());
    }
    
    public Item getItem() {
        return item;
    }
    
    @XmlAttribute
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    @XmlAttribute
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    @XmlAttribute
    public URI getUri() {
        return uri;
    }
    
    public void setUri(URI uri) {
        this.uri = uri;
    }
    
    public void buildUri(String urlBase, String siteContentPath, String encoding) throws UnsupportedEncodingException {
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
                int offset = pathPart.indexOf(':');
                
                if (offset == -1) {
                    relativeContentPathBuilder.append('/').append(URLEncoder.encode(pathPart, encoding));
                } else {
                    relativeContentPathBuilder.append('/')
                    .append(URLEncoder.encode(pathPart.substring(0, offset), encoding))
                    .append(':')
                    .append(URLEncoder.encode(pathPart.substring(offset + 1), encoding));
                }
            }
            
            relativeContentPath = relativeContentPathBuilder.toString();
        }
        
        setUri(URI.create(urlBase + relativeContentPath));
    }
    
}
