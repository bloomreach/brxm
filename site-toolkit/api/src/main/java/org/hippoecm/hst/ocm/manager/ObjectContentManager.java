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
package org.hippoecm.hst.ocm.manager;

import javax.jcr.Session;

import org.hippoecm.hst.ocm.ObjectContentManagerException;

/**
 * The object content manager encapsulates a JCR session. This is the main
 * component used to manage objects into the JCR repository.
 * <P>
 * This interface mimics Jackrabbit's one, but this is provided
 * to support more lightweight OCM in HST.
 * </P>
 * 
 * @version $Id$
 */
public interface ObjectContentManager {

    /**
     * Get an object from the JCR repository
     *
     * @param path
     *            the object path
     * @return the object found or null
     *
     * @throws ObjectContentManagerException
     *             when it is not possible to retrieve the object
     */
    public Object getObject(String path) throws ObjectContentManagerException;

    /**
     * Get an object from the JCR repository
     *
     * @param the
     *            object uuid
     * @return the object found or null
     *
     * @throws ObjectContentManagerException
     *             when it is not possible to retrieve the object
     */
    public Object getObjectByUuid(String uuid) throws ObjectContentManagerException;
    
    /**
     * This method returns the JCR session. The JCR session could be used to
     * make some JCR specific calls.
     *
     * @return the associated JCR session
     */
    public Session getSession();
    
}
