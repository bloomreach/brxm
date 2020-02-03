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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.GenericHstComponent;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.linking.HstLink;
import org.hippoecm.hst.core.linking.HstLinkCreator;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.platform.linking.DefaultHstLinkCreator;
import org.hippoecm.hst.platform.linking.HstLinkImpl;

public class GenericDetailComponent extends GenericHstComponent {

    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);

        HstRequestContext requestContext = request.getRequestContext();

        request.setModel("document", requestContext.getContentBean());

        // trigger some HstLinks on the model to make sure HstLinkMixin is triggered and that we do not make
        // BC incompatible changes to the Page Model API

        final HstLinkCreator hstLinkCreator = requestContext.getHstLinkCreator();
        final HippoBean requestContentBean = requestContext.getContentBean();

        request.setModel("menu", requestContext.getHstSiteMenus().getSiteMenu("main"));

        request.setModel("currentLink", hstLinkCreator.create(requestContentBean, requestContext));

        // include link not found
        HstLink notFoundLink = new HstLinkImpl(DefaultHstLinkCreator.DEFAULT_PAGE_NOT_FOUND_PATH, requestContext.getResolvedMount().getMount());
        notFoundLink.setNotFound(true);

        // assert that notFoundLink results in something like in PMA (with type = internal) !!
        //       "notFoundLink":{
        //        "href":"/site/spa/pagenotfound",
        //        "type":"internal"
        //    },
        request.setModel("notFoundLink", notFoundLink);

        try {
            final Node sameChannelNews = requestContext.getSession().getNode("/unittestcontent/documents/unittestproject/News/News1");
            final Node otherChannelNews = requestContext.getSession().getNode("/unittestcontent/documents/unittestsubproject/News/2008/SubNews1");

            final HstLink sameChannelNewsLink = hstLinkCreator.create(sameChannelNews, requestContext);

            request.setModel("sameChannelNewsLink", sameChannelNewsLink);

            final HstLink otherChannelNewsLink = hstLinkCreator.create(otherChannelNews, requestContext);

            request.setModel("otherChannelNewsLink", otherChannelNewsLink);


        } catch (RepositoryException e) {
            e.printStackTrace();
        }


    }

}
