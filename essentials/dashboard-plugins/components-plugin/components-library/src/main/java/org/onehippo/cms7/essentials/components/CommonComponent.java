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

package org.onehippo.cms7.essentials.components;

import java.io.IOException;
import java.util.Collection;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.hst.util.SearchInputParsingUtils;
import org.onehippo.cms7.essentials.components.utils.SiteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * Base HST component, containing default values and utility methods
 *
 * @version "$Id$"
 */
public abstract class CommonComponent extends BaseHstComponent {

    /**
     * Name of the content not found (404) redirect page
     */
    public static final String PAGE_404 = "404page";

    /**
     * Attribute names used within Essentials
     */
    protected static final String REQUEST_ATTR_DOCUMENT = "document";
    protected static final String REQUEST_ATTR_FACETS = "facets";
    protected static final String REQUEST_ATTR_PAGE = "page"; // current page
    protected static final String REQUEST_ATTR_PAGEABLE = "pageable";
    protected static final String REQUEST_ATTR_PARAM_INFO = "cparam"; // (annotated) Component Parameters
    protected static final String REQUEST_ATTR_QUERY = "query"; // free-text query string
    protected static final String REQUEST_ATTR_CMS_EDIT = "editMode"; // CMS edit mode

    /**
     * Request parameters (as submitted in HTTP-GET request).
     */
    protected static final String REQUEST_PARAM_QUERY = REQUEST_ATTR_QUERY;
    protected static final String REQUEST_PARAM_PAGE = REQUEST_ATTR_PAGE;


    private static Logger log = LoggerFactory.getLogger(CommonComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        request.setAttribute(REQUEST_ATTR_CMS_EDIT, RequestContextProvider.get().isCmsRequest());
    }

    public <T extends HippoBean> T getHippoBeanForPath(final String documentPath, Class<T> beanMappingClass) {
        if (!Strings.isNullOrEmpty(documentPath)) {
            final HstRequestContext context = RequestContextProvider.get();
            final HippoBean root = context.getSiteContentBaseBean();
            return root.getBean(documentPath, beanMappingClass);
        }

        return null;
    }

    /**
     * Sets content bean onto request. If no bean is found, *no* 404 response will be set.
     * NOTE: we first check if document is set through component interface,
     * otherwise we try o fetch mapped (sitemap) bean
     *
     * @param documentPath
     * @param request      HstRequest
     * @param response     HstResponse
     * @see #pageNotFound(org.hippoecm.hst.core.component.HstResponse)
     */
    public void setContentBean(final String documentPath, HstRequest request, final HstResponse response) {
        final HstRequestContext context = request.getRequestContext();
        HippoBean bean;

        if (!Strings.isNullOrEmpty(documentPath)) {
            final HippoBean root = context.getSiteContentBaseBean();
            bean = root.getBean(documentPath);
        } else {
            bean = context.getContentBean();
        }

        request.setAttribute(REQUEST_ATTR_DOCUMENT, bean);
    }



    /**
     * Sets content bean onto request. If no bean is found, 404 response will be set.
     *
     * @param request  HstRequest
     * @param response HstResponse
     */
    public void setContentBeanWith404(final HstRequest request, final HstResponse response) {
        final HstRequestContext context = request.getRequestContext();
        final HippoBean bean = context.getContentBean();

        if (bean == null) {
            pageNotFound(response);
            return;
        }

        request.setAttribute(REQUEST_ATTR_DOCUMENT, bean);
    }

    /**
     * Sets {@code HttpServletResponse.SC_NOT_FOUND} error code onto request.
     *
     * @param response
     * @see javax.servlet.http.HttpServletResponse#SC_NOT_FOUND
     */
    public void pageNotFound(HstResponse response) {
        final HstRequestContext context = RequestContextProvider.get();
        String pageNotFoundPath = getComponentParameter(PAGE_404);
        if (Strings.isNullOrEmpty(pageNotFoundPath)) {
            pageNotFoundPath = PAGE_404;
        }
        final HippoBean bean = context.getSiteContentBaseBean().getBean(pageNotFoundPath, HippoBean.class);
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        if (bean != null) {
            final HstLinkCreator hstLinkCreator = context.getHstLinkCreator();
            final HstLink hstLink = hstLinkCreator.create(bean.getNode(), context);
            try {
                response.sendRedirect(hstLink.toUrlForm(context, false));
            } catch (IOException e) {
                log.warn("Error redirecting to 404 page: [{}]", PAGE_404);
            }
        }
    }

    /**
     * Find HippoBean for given path. If path is null or empty, site root bean will be returned
     *
     * @param path    document (or folder) path relative to site-root
     * @return bean identified by path. Site root bean if path empty or no corresponding bean.
     */
    public static HippoBean getScopeBean(final String path) {
        final HstRequestContext context = RequestContextProvider.get();
        final HippoBean siteBean = context.getSiteContentBaseBean();

        if (!Strings.isNullOrEmpty(path)) {
            final String myPath = PathUtils.normalizePath(path);
            log.debug("Looking for bean {}", myPath);
            HippoBean scope = siteBean.getBean(myPath);
            if (scope != null) {
                return scope;
            }
            log.warn("Bean was null for selected path:  {}", myPath);
        }
        return siteBean;
    }

    public int getAnyIntParameter(HstRequest request, String parameter, int defaultValue) {
        return SiteUtils.getAnyIntParameter(request, parameter, defaultValue, this);
    }

    public boolean getAnyBooleanParam(HstRequest request, String parameter, boolean defaultValue) {
        return SiteUtils.getAnyBooleanParam(request, parameter, defaultValue, this);
    }

    /**
     * Retrieves  parameter, *any* means:
     * <p>- first try to fetch namespaced  parameter</p>
     * <p>- otherwise a public one</p>
     * <p>- otherwise component one</p>
     *
     * @param request   hst request instance
     * @param parameter name of the parameter
     * @return null if empty or undefined
     */
    public String getAnyParameter(HstRequest request, String parameter) {
        return SiteUtils.getAnyParameter(parameter, request, this);
    }


    /**
     * Adds beans to collection for given path
     *
     * @param path
     * @param beans
     */
    public void addBeanForPath(final String path, final Collection<HippoDocument> beans) {
        if (Strings.isNullOrEmpty(path)) {
            return;
        }
        log.debug("Fetching item for path: [{}]", path);
        final HippoDocument bean = getHippoBeanForPath(path, HippoDocument.class);
        if (bean != null) {
            beans.add(bean);
        } else {
            log.debug("Couldn't find bean for path: {}", path);
        }
    }

    @Nullable
    public String cleanupSearchQuery(final String query) {
        return SearchInputParsingUtils.parse(query, false);
    }

}
