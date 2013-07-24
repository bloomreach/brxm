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

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.query.HstQueryManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;

/**
 * ContentBeansTool
 * <P>
 * This interface is supposed to be provided to external application frameworks and codes.
 * They can normally access this component by invoking <code>HttpServletRequest#getAttribute(ContentBeansTool.class.getName());</code>.
 * </P>
 */
public interface ContentBeansTool {

    /**
     * @return <code>HippoBean</code> belonging to the {@link org.hippoecm.hst.core.request.ResolvedSiteMapItem} or
     * <code>null</code> when bean cannot be found of when the backing
     * {@link org.hippoecm.hst.configuration.sitemap.HstSiteMapItem#getRelativeContentPath()} is <code>null</code>
     */
    public HippoBean getResolvedContentBean();

    public ObjectConverter getObjectConverter();

    public ObjectBeanManager getObjectBeanManager();

    /**
     * @return the root content path for the {@link org.hippoecm.hst.core.request.ResolvedMount} belonging to the current
     * {@link javax.servlet.http.HttpServletRequest}
     */
    public String getSiteContentBasePath();

    /**
     * @return the {@link HstQueryManager} for the {@link Session} retrieved through
     * {@link org.hippoecm.hst.core.request.HstRequestContext#getSession(boolean)}
     * @throws RepositoryException
     */
    public HstQueryManager getQueryManager() throws RepositoryException;

    /**
     * @param session
     * @return the {@link HstQueryManager} for <code>session</code>
     * @throws RepositoryException
     */
    public HstQueryManager getQueryManager(Session session) throws RepositoryException;

}
