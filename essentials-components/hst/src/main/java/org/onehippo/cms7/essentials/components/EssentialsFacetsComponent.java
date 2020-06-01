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

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.util.ContentBeanUtils;
import org.hippoecm.hst.util.PathUtils;
import org.onehippo.cms7.essentials.components.info.EssentialsFacetsComponentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

/**
 * @version "$Id$"
 */
@ParametersInfo(type = EssentialsFacetsComponentInfo.class)
public class EssentialsFacetsComponent extends CommonComponent {

    private static Logger log = LoggerFactory.getLogger(EssentialsFacetsComponent.class);

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);
        final EssentialsFacetsComponentInfo paramInfo = getComponentParametersInfo(request);
        final HstRequestContext context = request.getRequestContext();
        String facetPath = paramInfo.getFacetPath();
        // check if component is used through sitemap mapping:
        if (Strings.isNullOrEmpty(facetPath)) {
            final HippoBean bean = context.getContentBean();
            if (bean != null) {
                facetPath = bean.getPath();
            }
        }
        final String queryParam = cleanupSearchQuery(getAnyParameter(request, REQUEST_PARAM_QUERY));
        final HippoFacetNavigationBean hippoFacetNavigationBean = getFacetNavigationBean(context, facetPath, queryParam);

        request.setAttribute(REQUEST_ATTR_QUERY, queryParam);
        request.setModel(REQUEST_ATTR_FACETS, hippoFacetNavigationBean);
        request.setAttribute(REQUEST_ATTR_PARAM_INFO, paramInfo);
    }

    protected HippoFacetNavigationBean getFacetNavigationBean(final HstRequestContext context, String path, String query) {
        if (Strings.isNullOrEmpty(path)) {
            log.warn("Facetpath was empty {}", path);
            return null;
        }
        ResolvedSiteMapItem resolvedSiteMapItem = context.getResolvedSiteMapItem();
        String resolvedContentPath = PathUtils.normalizePath(resolvedSiteMapItem.getRelativeContentPath());
        String parsedQuery = cleanupSearchQuery(query);
        HippoFacetNavigationBean facNavBean;
        if (!StringUtils.isBlank(resolvedContentPath)
                && !resolvedContentPath.startsWith("/")
                && context.getSiteContentBaseBean().getBean(resolvedContentPath, HippoFacetNavigationBean.class) != null) {
            facNavBean = ContentBeanUtils.getFacetNavigationBean(resolvedContentPath, parsedQuery);
        } else {
            facNavBean = ContentBeanUtils.getFacetNavigationBean(path, parsedQuery);
        }
        return facNavBean;
    }
}
