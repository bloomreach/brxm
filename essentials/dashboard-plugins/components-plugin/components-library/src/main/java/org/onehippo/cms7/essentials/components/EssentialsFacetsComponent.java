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

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoDocumentIterator;
import org.hippoecm.hst.content.beans.standard.HippoFacetNavigationBean;
import org.hippoecm.hst.content.beans.standard.HippoResultSetBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.utils.BeanUtils;
import org.onehippo.cms7.essentials.components.info.EssentialsFacetsComponentInfo;
import org.onehippo.cms7.essentials.components.paging.DefaultPagination;
import org.onehippo.cms7.essentials.components.paging.IterablePagination;
import org.onehippo.cms7.essentials.components.paging.Pageable;
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
        log.info("**** FACET COMPONENT **** ");
        final EssentialsFacetsComponentInfo componentInfo = getComponentParametersInfo(request);
        final String facetPath = componentInfo.getFacetPath();
        if (Strings.isNullOrEmpty(facetPath)) {
            log.warn("Facetpath was empty {}", facetPath);
            request.setAttribute(REQUEST_PARAM_PAGEABLE, DefaultPagination.emptyCollection());
            return;
        }
        final String query = cleanupSearchQuery(getAnyParameter(request, REQUEST_PARAM_QUERY));
        // NOTE: query may be null in this case
        final HippoFacetNavigationBean hippoFacetNavigationBean = BeanUtils.getFacetNavigationBean(request, facetPath, query, getObjectConverter());
        if(hippoFacetNavigationBean==null){
            log.warn("Facet navigation bean for facet path: {} was null", facetPath);
            request.setAttribute(REQUEST_PARAM_PAGEABLE, DefaultPagination.emptyCollection());
            return;
        }
        final HippoResultSetBean hippoResultSetBean = hippoFacetNavigationBean.getResultSet();
        final int pageSize = componentInfo.getPageSize();
        final int page = getAnyIntParameter(request, REQUEST_PARAM_PAGE, 1);
        final Pageable<HippoBean> results;
        if (hippoResultSetBean != null) {
            final HippoDocumentIterator<HippoBean> hippoDocumentIterator = hippoResultSetBean.getDocumentIterator(HippoBean.class);
            final int facetCount = hippoFacetNavigationBean.getCount().intValue();
            results = new IterablePagination<>(hippoDocumentIterator, facetCount, pageSize, page);
        } else {
            results = DefaultPagination.emptyCollection();
        }
        request.setAttribute(REQUEST_PARAM_PAGEABLE, results);
    }
}
