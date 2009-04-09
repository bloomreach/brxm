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
package org.hippoecm.hst.hippo.ocm.manager.impl;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.ocm.NodeAware;
import org.hippoecm.hst.ocm.ObjectContentManagerException;
import org.hippoecm.hst.ocm.manager.ObjectConverter;
import org.hippoecm.hst.ocm.manager.ObjectConverterAware;
import org.hippoecm.hst.service.ServiceFactory;
import org.hippoecm.hst.util.DefaultKeyValue;
import org.hippoecm.hst.util.KeyValue;
import org.hippoecm.repository.api.HippoNodeType;

public class ObjectConverterImpl implements ObjectConverter {
    
    protected List<KeyValue<String, Class[]>> jcrPrimaryNodeTypeClassPairs;
    protected String [] fallBackJcrPrimaryNodeTypes;
    
    public ObjectConverterImpl(List<KeyValue<String, Class[]>> jcrPrimaryNodeTypeClassPairs, String [] fallBackJcrPrimaryNodeTypes) {
        this.jcrPrimaryNodeTypeClassPairs = jcrPrimaryNodeTypeClassPairs;
        this.fallBackJcrPrimaryNodeTypes = fallBackJcrPrimaryNodeTypes;
    }

    public Object getObject(Session session, String path) throws ObjectContentManagerException {
        Object object = null;
        
        try {
            if (!session.itemExists(path)) {
                return null;
            }
            
            Node node = (Node) session.getItem(path);

            if (node.isNodeType(HippoNodeType.NT_HANDLE)) {
                // if its a handle, we want the child node. If the child node is not present,
                // this node can be ignored
                object = getObject(session, path + "/" + node.getName());
            } else {
                object = getObject(node);
                
                if (object instanceof NodeAware) {
                    ((NodeAware) object).setNode(node);
                }
                
                if (object instanceof ObjectConverterAware) {
                    ((ObjectConverterAware) object).setObjectConverter(this);
                }
            }
        } catch (PathNotFoundException pnfe) {
            // HINT should never get here
            throw new ObjectContentManagerException("Impossible to get the object at " + path, pnfe);
        } catch (RepositoryException re) {
            throw new ObjectContentManagerException("Impossible to get the object at " + path, re);
        }
        
        return object;
    }

    public Object getObject(Node node) throws ObjectContentManagerException {
        Object object = null;
        
        try {
            String jcrPrimaryNodeType = node.getPrimaryNodeType().getName();
            Class [] proxyInterfacesOrDelegateeClass = null;
            KeyValue<String, Class[]> jcrPrimaryNodeTypePair = new DefaultKeyValue<String, Class[]>(jcrPrimaryNodeType, null, true);
            int offset = this.jcrPrimaryNodeTypeClassPairs.indexOf(jcrPrimaryNodeTypePair);
            
            if (offset != -1) {
                KeyValue<String, Class[]> pair = this.jcrPrimaryNodeTypeClassPairs.get(offset);
                proxyInterfacesOrDelegateeClass = pair.getValue();
            } else if (this.fallBackJcrPrimaryNodeTypes != null) {
                for (String fallBackJcrPrimaryNodeType : this.fallBackJcrPrimaryNodeTypes) {
                    offset = this.jcrPrimaryNodeTypeClassPairs.indexOf(new DefaultKeyValue<String, Class[]>(fallBackJcrPrimaryNodeType, null, true));
                    
                    if (offset != -1) {
                        KeyValue<String, Class[]> pair = this.jcrPrimaryNodeTypeClassPairs.get(offset);
                        proxyInterfacesOrDelegateeClass = pair.getValue();
                        break;
                    }
                }
            }
            
            if (proxyInterfacesOrDelegateeClass != null) {
                return ServiceFactory.create(node, proxyInterfacesOrDelegateeClass);
            }
        } catch (RepositoryException e) {
            throw new ObjectContentManagerException("Impossible to get the object from the repository", e);
        } catch (Exception e) {
            throw new ObjectContentManagerException("Impossible to convert the node", e);
        }
        
        return null;
    }

}
