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
package org.hippoecm.repository.decorating.client;

import java.rmi.RemoteException;
import java.util.Locale;
import java.util.Map;

import javax.jcr.NamespaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.jackrabbit.rmi.client.ClientNode;
import org.apache.jackrabbit.rmi.client.RemoteRepositoryException;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.hippoecm.repository.api.HippoNode;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.Localized;
import org.hippoecm.repository.decorating.remote.RemoteServicingNode;

public class ClientServicingNode extends ClientNode implements HippoNode {

    private RemoteServicingNode remote;

    protected ClientServicingNode(Session session, RemoteServicingNode remote, LocalServicingAdapterFactory factory) {
        super(session, remote, factory);
        this.remote = remote;
    }

    public Node getCanonicalNode() throws RepositoryException {
        try {
            RemoteNode remoteCanonical = remote.getCanonicalNode();
            return ( remoteCanonical == null ? null : getNode(getSession(), remoteCanonical));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    @Override
    public boolean isVirtual() throws RepositoryException {
        return getIdentifier().startsWith("cafeface");
    }

    @Override
    public boolean recomputeDerivedData() throws RepositoryException {
        try {
            return remote.recomputeDerivedData();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public String getLocalizedName() throws RepositoryException {
        try {
            return remote.getLocalizedName(Localized.getInstance(Locale.getDefault()));
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public String getLocalizedName(Localized localized) throws RepositoryException {
        try {
            return remote.getLocalizedName(localized);
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    @Override
    public Map<Localized, String> getLocalizedNames() throws RepositoryException {
        try {
            return remote.getLocalizedNames();
        } catch (RemoteException ex) {
            throw new RemoteRepositoryException(ex);
        }
    }

    public NodeIterator pendingChanges(String nodeType, boolean prune) throws NamespaceException, NoSuchNodeTypeException,
                                                                              RepositoryException {
        return ((HippoSession)getSession()).pendingChanges(this, nodeType, prune);
    }

    public NodeIterator pendingChanges(String nodeType) throws NamespaceException, NoSuchNodeTypeException, RepositoryException {
        return pendingChanges(nodeType, false);
    }

    public NodeIterator pendingChanges() throws RepositoryException {
        return pendingChanges(null, false);
    }

}
