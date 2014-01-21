/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.server.ServerNode;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.Localized;
import org.hippoecm.repository.decorating.remote.RemoteServicingNode;

public class ServerServicingNode extends ServerNode implements RemoteServicingNode {

    private HippoNode node;

    public ServerServicingNode(HippoNode node, RemoteServicingAdapterFactory factory) throws RemoteException {
        super(node, factory);
        this.node = node;
    }

    public RemoteNode getCanonicalNode() throws RepositoryException, RemoteException {
        try {
            Node result = node.getCanonicalNode();
            return (result == null ? null : getRemoteNode(result));
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    public String getLocalizedName(Localized localized) throws RepositoryException, RemoteException {
        try {
            return node.getLocalizedName(localized);
        } catch(RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    @Override
    public Map<Localized, String> getLocalizedNames() throws RepositoryException, RemoteException {
        try {
            return node.getLocalizedNames();
        } catch(RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }

    @Override
    public boolean recomputeDerivedData() throws RepositoryException, RemoteException {
        try {
            return node.recomputeDerivedData();
        } catch(RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
}
