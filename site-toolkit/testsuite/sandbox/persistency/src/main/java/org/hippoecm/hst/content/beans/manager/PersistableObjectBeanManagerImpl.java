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
package org.hippoecm.hst.content.beans.manager;

import javax.jcr.Session;

import org.hippoecm.hst.persistence.ContentPersistenceException;
import org.hippoecm.hst.persistence.ContentPersistenceManager;

public class PersistableObjectBeanManagerImpl extends ObjectBeanManagerImpl implements ContentPersistenceManager {

    public PersistableObjectBeanManagerImpl(Session session, ObjectConverter objectConverter) {
        super(session, objectConverter);
    }

    public void create(String absPath, String nodeTypeName, String name) throws ContentPersistenceException {
        // TODO Auto-generated method stub

    }

    public void remove(Object content) throws ContentPersistenceException {
        // TODO Auto-generated method stub

    }

    public void reset() throws ContentPersistenceException {
        // TODO Auto-generated method stub

    }

    public void save() throws ContentPersistenceException {
        // TODO Auto-generated method stub

    }

    public void update(Object content) throws ContentPersistenceException {
        // TODO Auto-generated method stub

    }

}
