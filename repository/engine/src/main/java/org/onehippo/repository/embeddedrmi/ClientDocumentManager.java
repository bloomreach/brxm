/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *  http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.embeddedrmi;

import java.rmi.RemoteException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.hippoecm.repository.api.Document;
import org.hippoecm.repository.api.DocumentManager;

class ClientDocumentManager implements DocumentManager {
    Session session;
    RemoteDocumentManager remote;

    public ClientDocumentManager(Session session, RemoteDocumentManager remote) {
        this.session = session;
        this.remote = remote;
    }

    public Session getSession() throws RepositoryException {
        return session;
    }

    public Document getDocument(String category, String identifier) throws RepositoryException {
        try {
            return remote.getDocument(category, identifier);
        } catch (RemoteException ex) {
            throw new RepositoryException(ex);
        }
    }
    
}
