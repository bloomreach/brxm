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
     * <p>
     *     Creates a new ObjectBeanManager instance using the {@link ObjectConverter} from {@link #getObjectConverter()}.
     *     If you have a {@link org.hippoecm.hst.core.request.HstRequestContext} then use
     *     {@link org.hippoecm.hst.core.request.HstRequestContext#getObjectBeanManager(javax.jcr.Session)} or
     *     {@link org.hippoecm.hst.core.request.HstRequestContext#getObjectBeanManager()} instead : This will include
     *     some caching of beans automatically
     * </p>
     * @return a new <code>ObjectBeanManager</code> instance for the {@link javax.jcr.Session} <code>session</code>
     * @throws IllegalStateException if the application is unable to provide a ObjectBeanManager
     * @see org.hippoecm.hst.core.request.HstRequestContext#getObjectBeanManager(javax.jcr.Session) to re-use a cached one for
     * the current request
     */
    public ObjectBeanManager createObjectBeanManager(Session session) throws IllegalStateException;

    /**
     *
     * <p>
     *     Creates a new HstQueryManager instance using the {@link ObjectConverter} from {@link #getObjectConverter()}.
     *     If you have a {@link org.hippoecm.hst.core.request.HstRequestContext} then use
     *     {@link org.hippoecm.hst.core.request.HstRequestContext#getQueryManager(javax.jcr.Session)} or
     *     {@link org.hippoecm.hst.core.request.HstRequestContext#getQueryManager()} instead : This will include
     *     some caching of beans automatically
     * </p>
     * @param session
     * @return the {@link org.hippoecm.hst.content.beans.query.HstQueryManager} for <code>session</code>
     * @throws IllegalStateException if the application is unable to provide a HstQueryManager
     * @see org.hippoecm.hst.core.request.HstRequestContext#getQueryManager(Session) to re-use a cached one for
     * the current request
     */
    public HstQueryManager createQueryManager(Session session) throws IllegalStateException;


}
