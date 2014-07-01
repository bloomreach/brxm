/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onehippo.cms7.essentials.components.rest.ctx;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectConverter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFolderBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.essentials.components.rest.BaseRestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version "$Id: DefaultRestContext.java 174726 2013-08-22 14:24:50Z mmilicevic $"
 */
public class DefaultRestContext implements RestContext {

    public static final int PAGE_SIZE = 10;
    private static final Logger log = LoggerFactory.getLogger(DefaultRestContext.class);
    private final HttpServletRequest request;
    private final HstRequestContext context;
    private final BaseRestResource resource;
    private Map<String, String> contextParams = new HashMap<String, String>();
    private int pageSize;
    private int page;
    private boolean absolutePath;
    private String scope;


    public DefaultRestContext(final BaseRestResource resource, final HttpServletRequest request) {
        this.request = request;
        this.context = RequestContextProvider.get();
        this.pageSize = PAGE_SIZE;
        this.resource = resource;
        this.page = 1;


    }

    public DefaultRestContext(final BaseRestResource resource, final HttpServletRequest request, final int page, final int pageSize) {
        this(resource, request);
        this.pageSize = pageSize;
        this.page = page;
    }


    @Override
    public int getPage() {
        if (page < 1) {
            return 1;
        }
        return page;
    }

    @Override
    public void setPage(final int page) {
        this.page = page;
    }

    @Override
    public int getPageSize() {
        if (page < 1) {
            return PAGE_SIZE;
        }
        return pageSize;
    }

    @Override
    public void setPageSize(final int pageSize) {
        this.pageSize = pageSize;
    }

    @Override
    public HttpServletRequest getRequest() {
        return request;
    }

    @Override
    public HstRequestContext getRequestContext() {
        return context;
    }

    @Override
    public String getScope() {
        return scope;
    }

    @Override
    public void setScope(String scopePath) {
        this.scope = scopePath;
    }

    @Override
    public boolean isAbsolutePath() {
        return absolutePath;
    }

    @Override
    public void setAbsolutePath(final boolean absolutePath) {
        this.absolutePath = absolutePath;
    }

    @Override
    public Map<String, String> getContextParams() {
        return this.contextParams;
    }

    @Override
    public BaseRestResource getResource() {
        return resource;
    }

    public HippoFolderBean getGalleryFolder() {
        try {
            final ObjectConverter objectConverter = resource.getObjectConverter(context);
            HippoBean gallery = (HippoBean) objectConverter.getObject(context.getSession(), "/content/gallery");
            if (gallery instanceof HippoFolderBean) {
                return (HippoFolderBean) gallery;
            } else {
                log.warn("Gallery base folder not of type folder. Cannot return folder bean for it. Return null");
            }
        } catch (ObjectBeanManagerException e) {
            log.warn("Cannot find the root Gallery folder. Return null");
        } catch (RepositoryException e) {
            log.error("Error obtaining session", e);
        }

        return null;
    }
}
