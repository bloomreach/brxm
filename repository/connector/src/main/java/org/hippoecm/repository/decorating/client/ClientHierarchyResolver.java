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
package org.hippoecm.repository.decorating.client;

import java.rmi.RemoteException;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.rmi.client.ClientObject;
import org.apache.jackrabbit.rmi.client.RemoteRuntimeException;
import org.apache.jackrabbit.rmi.remote.RemoteNode;

import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.decorating.remote.RemoteHierarchyResolver;

public class ClientHierarchyResolver extends ClientObject implements HierarchyResolver {

    private RemoteHierarchyResolver remote;
    private Session session;

    protected ClientHierarchyResolver(Session session, RemoteHierarchyResolver remote, LocalServicingAdapterFactory factory) {
        super(factory);
        this.remote = remote;
        this.session = session;
    }

    public Item getItem(Node ancestor, String path, boolean isProperty, Entry last) throws InvalidItemStateException, RepositoryException {
        try {
            RemoteHierarchyResolver.RemoteHierarchyResult result = remote.getItem(ancestor.getPath(), path, isProperty);
            if(last != null) {
                if(result.node != null) {
                    if(result instanceof RemoteNode) {
                        last.node = (Node) getNode(session, result.node);
                    } else {
                        last.node = (Node) getItem(session, result.node);
                    }
                } else {
                    last.node = null;
                }
                last.relPath = result.relPath;
            }
            if (result.item == null) {
                return null;
            } else {
                return getItem(session, result.item);
            }
        } catch(RemoteException ex) {
            throw new RemoteRuntimeException(ex);
        }
    }

    public Item getItem(Node node, String field) throws RepositoryException {
        return getItem(node, field, false, null);
    }

    public Property getProperty(Node node, String field) throws RepositoryException {
        return (Property) getItem(node, field, true, null);
    }

    public Property getProperty(Node node, String field, Entry last) throws RepositoryException {
        return (Property) getItem(node, field, true, last);
    }

    public Node getNode(Node node, String field) throws InvalidItemStateException, RepositoryException {
        Item item = getItem(node, field, false, null);
        if(item instanceof Node) {
            return (Node) item;
        } else {
            return null;
        }
    }
}
