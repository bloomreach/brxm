/**
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.hst.content.tool;

import javax.jcr.Session;

import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQueryManager;

/**
 * ContentBeansTool
 * <P>
 * This interface is supposed to be provided to external application frameworks and codes.
 * They can normally access this component by invoking <code>HttpServletRequest#getAttribute(ContentBeansTool.class.getName());</code>.
 * </P>
 */
public interface ContentBeansTool {

    /**
     * @return <code>ObjectConverter</code> which is shareed across all threads
     */
    public ObjectConverter getObjectConverter();

    /**
     * @return a new <code>ObjectBeanManager</code> instance for the {@link Session} <code>session</code>
     * @throws IllegalStateException if the application is unable to provide a ObjectBeanManager
     * @see org.hippoecm.hst.core.request.HstRequestContext#getObjectBeanManager(Session) to re-use a cached one for
     * the current request
     */
    public ObjectBeanManager createObjectBeanManager(Session session) throws IllegalStateException;

    /**
     * @param session
     * @return the {@link HstQueryManager} for <code>session</code>
     * @throws IllegalStateException if the application is unable to provide a HstQueryManager
     * @see org.hippoecm.hst.core.request.HstRequestContext#getQueryManager(Session) to re-use a cached one for
     * the current request
     */
    public HstQueryManager createQueryManager(Session session) throws IllegalStateException;

}
