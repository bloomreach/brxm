package org.onehippo.cms7.essentials.site.components;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.content.beans.standard.HippoFacetChildNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.util.PathUtils;
import org.hippoecm.hst.util.SearchInputParsingUtils;
import org.hippoecm.hst.utils.BeanUtils;

import com.google.common.base.Strings;

public abstract class BaseComponent extends BaseHstComponent {

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {

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


    /**
     * Retrieves  parameter, any means: first component (namespaced parameter)
     * will be fetched, otherwise a public one
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
            return null;
        }
        return p;
    }

    protected HippoFacetNavigationBean getFacetNavigationBean(HstRequest request, String path) {
        final ResolvedSiteMapItem resolvedSiteMapItem = request.getRequestContext().getResolvedSiteMapItem();
        final String resolvedContentPath = PathUtils.normalizePath(resolvedSiteMapItem.getRelativeContentPath());
        final HippoFacetChildNavigationBean resolvedContentBean = getSiteContentBaseBean(request).getBean(resolvedContentPath, HippoFacetChildNavigationBean.class);
        final String query = getAnyParameter(request, "query");
        final String parsedQuery = SearchInputParsingUtils.parse(query, false);
        HippoFacetNavigationBean facNavBean;
        if (resolvedContentBean != null) {
            facNavBean = BeanUtils.getFacetNavigationBean(request, resolvedContentPath, parsedQuery, getObjectConverter());
        } else {
            facNavBean = BeanUtils.getFacetNavigationBean(request, path, parsedQuery, getObjectConverter());
        }
        return facNavBean;
    }

}