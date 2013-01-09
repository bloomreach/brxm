/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.jackrabbit.ocm.manager.impl;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.ocm.exception.IncorrectPersistentClassException;
import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.atomictypeconverter.AtomicTypeConverterProvider;
import org.apache.jackrabbit.ocm.manager.cache.ObjectCache;
import org.apache.jackrabbit.ocm.manager.objectconverter.ProxyManager;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.ObjectConverterImpl;
import org.apache.jackrabbit.ocm.manager.objectconverter.impl.SimpleFieldsHelper;
import org.apache.jackrabbit.ocm.mapper.Mapper;
import org.hippoecm.hst.content.beans.NodeAware;
import org.hippoecm.hst.content.beans.SimpleObjectConverter;
import org.hippoecm.hst.content.beans.SimpleObjectConverterAware;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HstObjectConverterImpl extends ObjectConverterImpl implements SimpleObjectConverter {

    private static Logger log = LoggerFactory.getLogger(HstObjectConverterImpl.class);

    protected Mapper mapper;
    protected AtomicTypeConverterProvider converterProvider;
    protected ProxyManager proxyManager;
    protected ObjectCache requestObjectCache;
    protected SimpleFieldsHelper simpleFieldsHelp;

    public HstObjectConverterImpl(Mapper mapper, AtomicTypeConverterProvider converterProvider,
            ProxyManager proxyManager, ObjectCache requestObjectCache) {
        super(mapper, converterProvider, proxyManager, requestObjectCache);

        this.mapper = mapper;
        this.converterProvider = converterProvider;
        this.proxyManager = proxyManager;
        this.requestObjectCache = requestObjectCache;
        this.simpleFieldsHelp = new SimpleFieldsHelper(converterProvider);
    }

    @Override
    public void insert(Session session, Object object) {
        super.insert(session, object);
    }

    @Override
    public void insert(Session session, Node parentNode, String nodeName, Object object) {
        super.insert(session, parentNode, nodeName, object);
    }

    @Override
    public void update(Session session, Object object) {
        super.update(session, object);
    }

    @Override
    public void update(Session session, String uuId, Object object) {
        super.update(session, uuId, object);
    }

    @Override
    public void update(Session session, Node objectNode, Object object) {
        super.update(session, objectNode, object);
    }

    @Override
    public void update(Session session, Node parentNode, String nodeName, Object object) {
        super.update(session, parentNode, nodeName, object);
    }

    @Override
    public Object getObject(Session session, String path) {
        Object object = null;
        Node node = null;
        
        try {
            if (!session.itemExists(path)) {
                log.debug("Cannot load object for path '{}' because node does not exist", path);
                return null;
            }
            
            Item item = session.getItem(path);
            
            if (!item.isNode()) {
                log.warn("The object is not a node: {}", path);
            } else {
                node = (Node) item;
                
                if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    // if its a handle, we want the child node. If the child node is not present,
                    // this node can be ignored
                    object = getObject(session, path + "/" + node.getName());
                } else {
                    object = super.getObject(session, path);
                    
                    if (object instanceof NodeAware) {
                        ((NodeAware) object).setNode(node);
                    }
                    
                    if (object instanceof SimpleObjectConverterAware) {
                        ((SimpleObjectConverterAware) object).setSimpleObjectConverter(this);
                    }
                }
            }
        } catch(IncorrectPersistentClassException e) {
            if (log.isWarnEnabled()) {
                String nodeType = "";
                try { nodeType = node.getPrimaryNodeType().getName(); } catch (Exception re) {}
                log.warn("Cannot find the class descriptor for {}: {}", nodeType, path);
            }
        } catch (PathNotFoundException pnfe) {
            throw new ObjectContentManagerException("Impossible to get the object at " + path, pnfe);
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException("Impossible to get the object at " + path, re);
        }
        
        return object;
    }

    @Override
    public Object getObject(Session session, Class clazz, String path) {
        Object object = null;
        Node node = null;

        try {
            if (!session.itemExists(path)) {
                return null;
            }

            Item item = session.getItem(path);

            if (!item.isNode()) {
                log.warn("The object is not a node: {}", path);
            } else {
                node = (Node) item;

                if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                    object = getObject(session, clazz, path + "/" + node.getName());
                } else {
                    object = super.getObject(session, clazz, path);

                    if (object instanceof NodeAware) {
                        ((NodeAware) object).setNode(node);
                    }

                    if (object instanceof SimpleObjectConverterAware) {
                        ((SimpleObjectConverterAware) object).setSimpleObjectConverter(this);
                    }
                }
            }
        } catch (PathNotFoundException pnfe) {
            throw new ObjectContentManagerException("Impossible to get the object at " + path, pnfe);
        } catch (RepositoryException re) {
            throw new org.apache.jackrabbit.ocm.exception.RepositoryException(
                    "Impossible to get the object at " + path, re);
        }

        return object;
    }

    @Override
    public void retrieveAllMappedAttributes(Session session, Object object) {
        super.retrieveAllMappedAttributes(session, object);
    }

    @Override
    public void retrieveMappedAttribute(Session session, Object object, String attributeName) {
        super.retrieveMappedAttribute(session, object, attributeName);
    }

    @Override
    public String getPath(Session session, Object object) {
        return super.getPath(session, object);
    }

}
