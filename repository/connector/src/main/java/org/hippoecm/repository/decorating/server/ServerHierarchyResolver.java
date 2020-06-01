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
package org.hippoecm.repository.decorating.server;

import java.rmi.RemoteException;

import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.rmi.server.ServerAdapterFactory;
import org.apache.jackrabbit.rmi.server.ServerObject;
import org.hippoecm.repository.api.HierarchyResolver;
import org.hippoecm.repository.decorating.remote.RemoteHierarchyResolver;

public class ServerHierarchyResolver extends ServerObject implements RemoteHierarchyResolver
{

    protected Session session;
    protected HierarchyResolver resolver;

    protected ServerHierarchyResolver(HierarchyResolver resolver, ServerAdapterFactory factory, Session session)
        throws RemoteException {
        super(factory);
        this.session = session;
        this.resolver = resolver;
    }

    public RemoteHierarchyResolver.RemoteHierarchyResult getItem(String ancestor, String path, boolean isProperty)
        throws InvalidItemStateException, RepositoryException, RemoteException {
        try {
            Node node;
            if ("/".equals(ancestor)) {
                node = session.getRootNode();
            } else {
                node = session.getRootNode().getNode(ancestor.substring(1));
            }
            RemoteHierarchyResult result = new RemoteHierarchyResult();
            HierarchyResolver.Entry last = new HierarchyResolver.Entry();
            Item item = resolver.getItem(node, path, isProperty, last);
            if(item != null) {
                if(item.isNode()) {
                    result.item = getFactory().getRemoteNode((Node)item);
                } else {
                    result.item = getFactory().getRemoteProperty((Property)item);
                }
            } else {
                result.item = null;
            }
            result.node = getFactory().getRemoteNode(node);
            result.relPath = last.relPath;
            return result;
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
}
