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

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.hippoecm.hst.ocm.ObjectContentManagerException;
import org.hippoecm.hst.ocm.manager.ObjectContentManager;
import org.hippoecm.hst.ocm.manager.ObjectConverter;

public class ObjectContentManagerImpl implements ObjectContentManager {

    protected Session session;
    protected ObjectConverter objectConverter;

    public ObjectContentManagerImpl(Session session, ObjectConverter objectConverter) {
        this.session = session;
        this.objectConverter = objectConverter;
    }

    public Object getObject(String path) throws ObjectContentManagerException {
        return this.objectConverter.getObject(this.session, path);
    }

    public Session getSession() {
        return this.session;
    }

    public void save() throws ObjectContentManagerException {
        try {
            this.session.save();
        } catch (NoSuchNodeTypeException nsnte) {
            throw new ObjectContentManagerException(
                    "Cannot persist current session changes. An unknown node type was used.", nsnte);
        } catch (LockException le) {
            throw new ObjectContentManagerException(
                    "Cannot persist current session changes. Violation of a lock detected", le);
        } catch (RepositoryException e) {
            throw new ObjectContentManagerException("Cannot persist current session changes.", e);
        }
    }

    /**
    *
    * @see org.apache.jackrabbit.ocm.manager.ObjectContentManager#logout()
    */
    public void logout() throws ObjectContentManagerException {
        try {
            this.session.save();
            this.session.logout();
        } catch (NoSuchNodeTypeException nsnte) {
            throw new ObjectContentManagerException(
                    "Cannot persist current session changes. An unknown node type was used.", nsnte);
        } catch (LockException le) {
            throw new ObjectContentManagerException(
                    "Cannot persist current session changes. Violation of a lock detected", le);
        } catch (RepositoryException e) {
            throw new ObjectContentManagerException("Cannot persist current session changes.", e);
        }
    }

}
