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

/**
 * The object content manager encapsulates a JCR session. This is the main
 * component used to manage objects into the JCR repository.
 * <P>
 * This interface mimics Jackrabbit's one, but this is provided
 * to support more lightweight beans in HST.
 * </P>
 * 
 * @version $Id$
 */
public interface ObjectBeanManager {

    /**
     * Get an object from the JCR repository. <code>path</code> is the absolute object path and must start with a "/"
     *
     * @param path the absolute object path. 
     * @return the object found or null
     *
     * @throws ObjectBeanManagerException
     *             when it is not possible to retrieve the object
     */
    Object getObject(String path) throws ObjectBeanManagerException;

    /**
     * Get an object from the JCR repository
     *
     * @param uuid the object uuid
     * @return the object found or null
     *
     * @throws ObjectBeanManagerException
     *             when it is not possible to retrieve the object
     */
    Object getObjectByUuid(String uuid) throws ObjectBeanManagerException;
    
    /**
     * This method returns the JCR session. The JCR session could be used to
     * make some JCR specific calls.
     *
     * @return the associated JCR session
     */
    Session getSession();
    
}
