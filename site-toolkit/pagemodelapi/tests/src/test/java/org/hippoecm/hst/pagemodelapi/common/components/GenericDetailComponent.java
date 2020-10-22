/*
 * Copyright 2019-2020 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.pagemodelapi.common.components;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.component.pagination.HippoBeanPaginationUtils;
import org.hippoecm.hst.component.pagination.Pagination;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstComponentException;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagemodelapi.common.context.ApiVersionProvider;
import org.hippoecm.hst.platform.linking.DefaultHstLinkCreator;
import org.hippoecm.hst.platform.linking.HstLinkImpl;

@ParametersInfo(type = GenericDetailComponent.GenericDetailComponentInfo.class)
public class GenericDetailComponent extends GenericHstComponent {

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);

        HstRequestContext requestContext = request.getRequestContext();

        request.setModel("document", requestContext.getContentBean());

        // make sure that same document twice in same page model gets serialized only once
        request.setModel("document-again", requestContext.getContentBean());

        // trigger some HstLinks on the model to make sure HstLinkMixin is triggered and that we do not make
        // BC incompatible changes to the Page Model API

        final HstLinkCreator hstLinkCreator = requestContext.getHstLinkCreator();
        final HippoBean requestContentBean = requestContext.getContentBean();

        request.setModel("menu", requestContext.getHstSiteMenus().getSiteMenu("main"));

        // make sure that same menu twice in same page model gets serialized only once
        request.setModel("menu-again", requestContext.getHstSiteMenus().getSiteMenu("main"));


        // make sure that the 'menu' within 'menus' get serialized via $ref and is the same $ref as 'menu' above
        request.setModel("menus", requestContext.getHstSiteMenus());

        if (requestContentBean != null) {
            request.setModel("currentLink", hstLinkCreator.create(requestContentBean, requestContext));
        }

        // include link not found
        HstLink notFoundLink = new HstLinkImpl(DefaultHstLinkCreator.DEFAULT_PAGE_NOT_FOUND_PATH, requestContext.getResolvedMount().getMount());
        notFoundLink.setNotFound(true);

        // assert that notFoundLink results in something like in PMA (with type = unknown and no href) !!
        //       "notFoundLink":{
        //        "type":"unknown"
        //    },
        request.setModel("notFoundLink", notFoundLink);

        // include an object of type IdentifiableLinkableMetadataBaseModel to make sure that even if 'links' or
        // 'meta' is null, the fields are still present in the PMA output
        ApiVersionProvider.ApiVersion apiVersion = ApiVersionProvider.get();
        if (apiVersion == null) {
            apiVersion = ApiVersionProvider.ApiVersion.V09;
        }
        switch (apiVersion) {
            case V09:
                    request.setModel("testLinkAndMetaNull", new org.hippoecm.hst.pagemodelapi.v09.core.model.IdentifiableLinkableMetadataBaseModel("some-id"));
                    break;
            case V10:
                request.setModel("testLinkAndMetaNull", new org.hippoecm.hst.pagemodelapi.v10.core.model.IdentifiableLinkableMetadataBaseModel("some-id"));

                // verify that the pageable object includes pages with internal and external links
                final int itemCount = 105;
                List<HippoBean> beans = new ArrayList<>(itemCount);
                IntStream.rangeClosed(1, itemCount).forEach(r -> beans.add(requestContext.getContentBean()));

                final int currentPage;
                if (request.getParameter("page") != null) {
                    currentPage = Integer.valueOf(request.getParameter("page"));
                } else {
                    // just a random default
                    currentPage = 6;
                }

                Pagination<HippoBean> pagination = HippoBeanPaginationUtils.createPagination(beans, currentPage, 10);
                request.setModel("pageable", pagination);

                break;
        }

        try {
            final Node sameChannelNews = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
            final Node otherChannelNews = requestContext.getSession().getNode("/unittestcontent/documents/unittestsubproject/News/2008/SubNews1");

            final HstLink sameChannelNewsLink = hstLinkCreator.create(sameChannelNews, requestContext);

            request.setModel("sameChannelNewsLink", sameChannelNewsLink);

            final HstLink otherChannelNewsLink = hstLinkCreator.create(otherChannelNews, requestContext);

            request.setModel("otherChannelNewsLink", otherChannelNewsLink);

        } catch (RepositoryException e) {
            throw new HstComponentException(e);
        }

    }

    public interface GenericDetailComponentInfo {

        @Parameter(name = "parameterOne", defaultValue = "valueTwo")
        String getParameterOne();

        @Parameter(name = "parameterTwo", defaultValue = "valueTwo")
        String getParameterTwo();

        @Parameter(name =" test", defaultValue = "1")
        long getValue();
    }

}
