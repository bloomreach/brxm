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
package org.hippoecm.hst.pagecomposer.jaxrs.services.repositorytests.sitemapresource;


import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.Response;

import org.hippoecm.hst.pagecomposer.jaxrs.model.SiteMapItemRepresentation;
import org.hippoecm.hst.pagecomposer.jaxrs.services.PageComposerContextService;
import org.hippoecm.hst.pagecomposer.jaxrs.services.SiteMapResource;
import org.hippoecm.hst.pagecomposer.jaxrs.services.helpers.LockHelper;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * tests to primarily confirm correct page creation / publication after adding a new sitemap item
 */
public class CreateAndPublicationTest extends AbstractSiteMapResourceTest {


    private final LockHelper helper = new LockHelper();

    private void initContext() throws Exception {
        // call below will init request context
        final SiteMapItemRepresentation home = getSiteMapItemRepresentation(session, "home");
    }

    @Test
    public void test_create() throws Exception {
        initContext();
        final SiteMapItemRepresentation newFoo = new SiteMapItemRepresentation();
        newFoo.setName("foo");
        String previewConfigurationPath = mountResource.getPageComposerContextService().getEditingPreviewSite().getConfigurationPath();
        String pageUuid = session.getNode(previewConfigurationPath).getNode("hst:pages/homepage").getIdentifier();
        newFoo.setComponentConfigurationId(pageUuid);
        final SiteMapResource siteMapResource = createResource();
        final Response response = siteMapResource.create(newFoo);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    }


}
