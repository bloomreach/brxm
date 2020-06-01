/*
 *  Copyright 2015 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.configuration.sitemap;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedSiteMapItem;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.hst.util.HstSiteMapUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class HstDefaultSiteMapLoginIT extends AbstractTestConfigurations {

    private HstManager hstManager;
    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        hstManager = getComponent(HstManager.class.getName());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void default_sitemap_item_login() throws Exception {
        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/site", "/login/form");
        final ResolvedSiteMapItem resolvedSiteMapItem = mount.matchSiteMapItem("/login/form");

        assertEquals("login/_any_",HstSiteMapUtils.getPath(resolvedSiteMapItem.getHstSiteMapItem()));
        assertTrue("/login/_any_ should be container resource since mountPath should not be in the URL",
                resolvedSiteMapItem.getHstSiteMapItem().isContainerResource());

        assertNull(resolvedSiteMapItem.getRelativeContentPath());
        assertEquals("login/form", resolvedSiteMapItem.getPathInfo());

        assertFalse("Although a 'container resource' sitemap item by default has 'schemeagnostic = true' (to be able " +
                "to serve resources over any scheme), the login/xyz is *not* scheme agnostic by default since we " +
                "promote https", resolvedSiteMapItem.getHstSiteMapItem().isSchemeAgnostic());
        assertEquals("https", resolvedSiteMapItem.getHstSiteMapItem().getScheme());
        assertEquals("Default pipeline for the login should be should be 'PlainFilterChainInvokingPipeline'.","PlainFilterChainInvokingPipeline", resolvedSiteMapItem.getNamedPipeline());
    }


}
