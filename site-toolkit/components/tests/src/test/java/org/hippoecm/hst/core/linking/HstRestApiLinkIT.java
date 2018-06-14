/*
 *  Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.linking;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class HstRestApiLinkIT extends AbstractHstLinkRewritingIT

{

    @Test
    public void test_hst__root_link_to_non_mapped_mount() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        final HstLink hstLink = linkCreator.create("/", getApiMount(requestContext));
        assertEquals("/site/api", hstLink.toUrlForm(requestContext, false));
        assertEquals("http://localhost/site/api", hstLink.toUrlForm(requestContext, true));
    }


    @Test
    public void test_hst_path_link_to_non_mapped_mount() throws Exception {
        HstRequestContext requestContext = getRequestContextWithResolvedSiteMapItemAndContainerURL("localhost", "/home");
        final HstLink hstLink = linkCreator.create("/foo", getApiMount(requestContext));
        assertEquals("/site/api/foo", hstLink.toUrlForm(requestContext, false));
        assertEquals("http://localhost/site/api/foo", hstLink.toUrlForm(requestContext, true));
    }


    private Mount getApiMount(final HstRequestContext requestContext) {
        final VirtualHost virtualHost = requestContext.getResolvedMount().getMount().getVirtualHost();
        return virtualHost.getVirtualHosts().getMountByGroupAliasAndType(virtualHost.getHostGroupName(), "api", "live");
    }
}