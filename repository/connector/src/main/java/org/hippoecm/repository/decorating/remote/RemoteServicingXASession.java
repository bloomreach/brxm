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
package org.hippoecm.repository.decorating.remote;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

import javax.jcr.RepositoryException;
import javax.jcr.security.AccessControlException;

import org.apache.jackrabbit.rmi.remote.RemoteIterator;
import org.apache.jackrabbit.rmi.remote.RemoteNode;
import org.apache.jackrabbit.rmi.remote.RemoteXASession;
import org.onehippo.repository.xml.ImportResult;

public interface RemoteServicingXASession extends RemoteXASession, Remote, Serializable {

    public RemoteNode copy(String originalPath, String absPath) throws RepositoryException, RemoteException;

    public RemoteIterator pendingChanges(String absPath, String nodeType, boolean prune) throws RepositoryException,
            RemoteException;

    public byte[] exportDereferencedView(String path, boolean binaryAsLink, boolean noRecurse) throws IOException,
            RepositoryException, RemoteException;

    public ImportResult importEnhancedSystemViewXML(String path, byte[] xml, int uuidBehavior, int referenceBehavior) throws IOException, RepositoryException, RemoteException;

    public void checkPermission(String path, String actions) throws AccessControlException, RepositoryException, RemoteException;

}
