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
package org.hippoecm.hst.content.beans.manager;

import javax.jcr.Session;

import org.hippoecm.hst.content.beans.ObjectBeanManagerException;

public class ObjectBeanManagerImpl implements ObjectBeanManager {

    protected Session session;
    protected ObjectConverter objectConverter;

    public ObjectBeanManagerImpl(Session session, ObjectConverter objectConverter) {
        this.session = session;
        this.objectConverter = objectConverter;
    }

    public Object getObject(String path) throws ObjectBeanManagerException {
        Object o = this.objectConverter.getObject(this.session, path);
        return o; 
    }
    
    public Object getObjectByUuid(String uuid) throws ObjectBeanManagerException {
        return objectConverter.getObject(uuid, session);
    }
    
    public Session getSession() {
        return this.session;
    }

}
