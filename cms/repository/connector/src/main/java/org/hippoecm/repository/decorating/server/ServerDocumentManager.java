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
package org.hippoecm.repository.decorating.server;

import java.rmi.RemoteException;

import javax.jcr.RepositoryException;

import org.apache.jackrabbit.rmi.server.ServerObject;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentManager;
import org.hippoecm.repository.decorating.remote.RemoteDocumentManager;

public class ServerDocumentManager extends ServerObject implements RemoteDocumentManager {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private DocumentManager documentManager;

    protected ServerDocumentManager(DocumentManager manager, RemoteServicingAdapterFactory factory) throws RemoteException {
        super(factory);
        this.documentManager = manager;
    }

    public Document getDocument(String category, String identifier) throws RepositoryException, RemoteException {
        try {
            return documentManager.getDocument(category, identifier);
        } catch (RepositoryException ex) {
            throw getRepositoryException(ex);
        }
    }
}
