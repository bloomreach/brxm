/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 */

package org.onehippo.cms7.essentials.components;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletResponse;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocument;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * Base HST component, containing default values and methods
 *
 * @version "$Id$"
 */
public class CommonComponent extends BaseHstComponent {

    /**
     * Name of the content not found (404) redirect page
     */
    public static final String PAGE_404 = "404page";
    /**
     * Attributes  names used within Essentials
     */
    public static final String ATTRIBUTE_DOCUMENTS = "documents";
    private static Logger log = LoggerFactory.getLogger(CommonComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        // do nothing
    }

    public <T extends HippoBean> T getHippoBeanForPath(final String documentPath, Class<T> beanMappingClass, final HstRequest request) {
        if (!Strings.isNullOrEmpty(documentPath)) {
            final HippoBean root = getSiteContentBaseBean(request);
            return root.getBean(documentPath, beanMappingClass);
        }
        return null;
    }

    /**
     * Sets content bean onto request. If no bean is found, 404 response will be set.
     * NOTE: we first check if document is set through component interface,
     * otherwise we try o fetch mapped (sitemap) bean
     *
     * @param documentPath
     * @param request      HstRequest
     * @param response     HstResponse
     * @see #pageNotFound(org.hippoecm.hst.core.component.HstRequest, org.hippoecm.hst.core.component.HstResponse)
     */
    public void setContentBean(final String documentPath, HstRequest request, final HstResponse response) {

        HippoBean bean;
        if (!Strings.isNullOrEmpty(documentPath)) {
            final HippoBean root = getSiteContentBaseBean(request);
            bean = root.getBean(documentPath);
            request.setAttribute("document", bean);
            return;
        }
        bean = getContentBean(request);
        request.setAttribute("document", bean);
        if (bean == null) {
            pageNotFound(request, response);
        }
    }

    /**
     * Sets {@code HttpServletResponse.SC_NOT_FOUND} error code onto request.
     *
     * @param request
     * @param response
     * @see javax.servlet.http.HttpServletResponse#SC_NOT_FOUND
     */
    public void pageNotFound(HstRequest request, HstResponse response) {
        String pageNotFoundPath = getComponentParameter(PAGE_404);
        if (Strings.isNullOrEmpty(pageNotFoundPath)) {
            pageNotFoundPath = PAGE_404;
        }
        final HippoBean bean = this.getSiteContentBaseBean(request).getBean(pageNotFoundPath, HippoBean.class);
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        if (bean != null) {
            final HstLinkCreator hstLinkCreator = request.getRequestContext().getHstLinkCreator();
            final HstLink hstLink = hstLinkCreator.create(bean.getNode(), request.getRequestContext());
            try {
                response.sendRedirect(hstLink.toUrlForm(request.getRequestContext(), false));
            } catch (IOException e) {
                log.warn("Error redirecting to 404 page: [{}]", PAGE_404);
            }
        }
    }

    //############################################
    // UTILS
    //############################################

    /**
     * Find HippoBean for given path. If path is null or empty, site root bean will be returned
     *
     * @param request HstRequest
     * @param path    document (or folder) path
     * @return null if document of folder exists
     */
    public HippoBean getScopeBean(final HstRequest request, final String path) {
        HippoBean scope;
        final HippoBean siteBean = getSiteContentBaseBean(request);
        if (Strings.isNullOrEmpty(path)) {
            scope = siteBean;
        } else {
            final String myPath = PathUtils.normalizePath(path);
            log.debug("Looking for bean {}", myPath);
            scope = siteBean.getBean(myPath);
            if (scope == null) {
                log.warn("Bean was null for selected path:  {}", myPath);
                scope = siteBean;
            }
        }
        return scope;
    }

    public int getIntParameter(HstRequest request, String parameter, int defaultValue) {
        final String p = getAnyParameter(request, parameter);
        if (p == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(p);
        } catch (NumberFormatException ignore) {
            // ignore exception
        }
        return defaultValue;
    }

    public boolean getBooleanParam(HstRequest request, String parameter, boolean defaultValue) {
        final String p = getAnyParameter(request, parameter);
        if (p == null) {
            return defaultValue;
        }
        return Boolean.valueOf(p);
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

        String p = request.getParameter(parameter);
        if (Strings.isNullOrEmpty(p)) {
            p = getPublicRequestParameter(request, parameter);
        }

        if (Strings.isNullOrEmpty(p)) {
            p = getComponentParameter(parameter);
        }
        return p;
    }


    /**
     * Adds beans to collection for given path
     *
     * @param request
     * @param path
     * @param beans
     */
    public void addBeanForPath(final HstRequest request, final String path, final Collection<HippoDocument> beans) {
        if (Strings.isNullOrEmpty(path)) {
            return;
        }

        log.debug("Fetching carousel item for path: [{}]", path);
        final HippoDocument bean = getHippoBeanForPath(path, HippoDocument.class, request);
        if (bean != null) {
            beans.add(bean);
        } else {
            log.debug("Couldn't find bean for path: {}", path);
        }
    }
}
