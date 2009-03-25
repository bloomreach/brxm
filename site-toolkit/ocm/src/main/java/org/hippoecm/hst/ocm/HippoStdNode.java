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
package org.hippoecm.hst.ocm;

import java.util.Collections;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.mapper.impl.annotation.Field;
import org.hippoecm.hst.provider.jcr.JCRValueProvider;
import org.hippoecm.hst.provider.jcr.JCRValueProviderImpl;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HippoStdNode implements NodeAware, SimpleObjectConverterAware {

    private static Logger log = LoggerFactory.getLogger(HippoStdFolder.class);
    
    protected String path;
    
    private transient SimpleObjectConverter simpleObjectConverter;
    private JCRValueProvider valueProvider;
    private HippoStdFolder parentFolder;

    public javax.jcr.Node getNode() {
        javax.jcr.Node node = null;
        
        if (this.valueProvider != null) {
            node = this.valueProvider.getJcrNode();
        }
        
        return node;
    }

    public void setNode(javax.jcr.Node node) {
        if (this.valueProvider == null) {
            this.valueProvider = new JCRValueProviderImpl(node);
        }
    }

    public SimpleObjectConverter getSimpleObjectConverter() {
        return this.simpleObjectConverter;
    }

    public void setSimpleObjectConverter(SimpleObjectConverter simpleObjectConverter) {
        this.simpleObjectConverter = simpleObjectConverter;
    }

    // TODO replace below with getPath from valueprovider?
    @Field(path = true)
    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    
    public String getName() {
        String name = "";

        if (this.valueProvider != null) {
            name = this.valueProvider.getName();
        }

        return name;
    }

    public Map<String, Object> getProperty() {
        Map<String, Object> properties = null;

        if (this.valueProvider == null) {
            properties = Collections.emptyMap();
        } else {
            properties = this.valueProvider.getProperties();
        }
        
        return properties;
    }
    
    public HippoStdNode getHippoStdNode(String relPath){
        if(relPath == null) {
            log.warn("Cannot get HippoStdNode for a relative path that is null.");
            return null;
        }
        if(!relPath.equals(PathUtils.normalizePath(relPath))) {
            log.warn("Relative path does end or start with a slash. Removing leading and trailing slashes");
            relPath = PathUtils.normalizePath(relPath);
        }
        if(this.getNode() == null) {
            log.warn("Node is detached. Cannot get document with relative path '{}'", relPath);
            return null;
        }
        Session session = null;
        try {
            session = getNode().getSession();
        } catch (RepositoryException e) {
            log.warn("Node's session is available. Cannot get document with relative path '{}'", relPath);
            return null;
        }
        String absPath = this.getPath() + "/" + relPath;
        Object o = getSimpleObjectConverter().getObject(session, absPath);
        if(o instanceof HippoStdNode) {
            return (HippoStdNode)o;
        } else {
            log.warn("Cannot return a HippoStdNode for location '{}'. Return null", absPath);
            return null;
        }
        
    }
    

    public HippoStdFolder getParentFolder() {
        if (this.parentFolder == null) {
            if (getNode() != null && getSimpleObjectConverter() != null) {
                try {
                    javax.jcr.Node parent = getNode().getParent();
                    
                    if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                        parent = parent.getParent();
                    }
                    
                    this.parentFolder = (HippoStdFolder) getSimpleObjectConverter().getObject(getNode().getSession(), parent.getPath());
                } catch (RepositoryException e) {
                    if (log.isDebugEnabled()) {
                        log.warn("Cannot retrieve parent folder: {}", e.getMessage(), e);
                    } else if (log.isWarnEnabled()) {
                        log.warn("Cannot retrieve parent folder: {}", e.getMessage());
                    }
                }
            }
        }
        
        return this.parentFolder;
    }
    
}
