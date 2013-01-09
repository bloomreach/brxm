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
package org.hippoecm.hst.jackrabbit.ocm.jndi;

import java.lang.reflect.Method;

import javax.jcr.Session;

import org.apache.jackrabbit.ocm.manager.ObjectContentManager;

/**
 * DelegatingObjectContentManagerProvider
 * 
 * @version $Id$
 */
public class DelegatingObjectContentManagerProvider implements ObjectContentManagerProvider
{
    private Object delegatee;
    private Method getObjectContentManagerMethod;
    
    public DelegatingObjectContentManagerProvider(Object delegatee) throws Exception
    {
        this.delegatee = delegatee;
        this.getObjectContentManagerMethod = delegatee.getClass().getMethod("getObjectContentManager", new Class [] { Session.class });
    }
    
    public ObjectContentManager getObjectContentManager(Session session)
    {
        try
        {
            return (ObjectContentManager) getObjectContentManagerMethod.invoke(delegatee, new Object [] { session });
        }
        catch (Exception e)
        {
            throw new RuntimeException(e.toString(), e);
        }
    }
}
