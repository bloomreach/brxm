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

    public HippoBean getResolvedContentBean();

    public ObjectConverter getObjectConverter();

    public ObjectBeanManager getObjectBeanManager();

    public String getSiteContentBasePath();

    public HstQueryManager getQueryManager() throws RepositoryException;

    public HstQueryManager getQueryManager(Session session) throws RepositoryException;

}
