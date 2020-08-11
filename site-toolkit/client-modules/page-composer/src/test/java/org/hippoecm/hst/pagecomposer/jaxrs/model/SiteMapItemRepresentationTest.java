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
package org.hippoecm.hst.pagecomposer.jaxrs.model;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.sitemap.HstSiteMapItem;
import org.hippoecm.hst.mock.configuration.MockSiteMapItem;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SiteMapItemRepresentationTest {

    @Test
    public void assert_parentLocation_when_no_parent() throws Exception {
        VirtualHost host = createNiceMock(VirtualHost.class);
        expect(host.getHostName()).andReturn("localhost").anyTimes();
        Mount mount = createNiceMock(Mount.class);
        expect(mount.getVirtualHost()).andReturn(host).anyTimes();
        expect(mount.getMountPath()).andReturn("/sub").anyTimes();
        HstSiteMapItem item = createNiceMock(HstSiteMapItem.class);
        expect(item.getParentItem()).andReturn(null);
        replay(host, mount, item);
        Location location = new SiteMapItemRepresentation().findParentLocation(mount, item);
        assertNull(location.getId());
        assertEquals("localhost/sub/", location.getLocation());
    }

    @Test
    public void assert_parentLocation_with_parent() throws Exception {
        VirtualHost host = createNiceMock(VirtualHost.class);
        expect(host.getHostName()).andReturn("localhost").anyTimes();
        Mount mount = createNiceMock(Mount.class);
        expect(mount.getVirtualHost()).andReturn(host).anyTimes();
        expect(mount.getMountPath()).andReturn("/sub").anyTimes();
        MockSiteMapItem parent = createNiceMock(MockSiteMapItem.class);
        expect(parent.getValue()).andReturn("foo").anyTimes();
        expect(parent.getCanonicalIdentifier()).andReturn("parent-id");
        HstSiteMapItem item = createNiceMock(HstSiteMapItem.class);
        expect(item.getParentItem()).andReturn(parent).anyTimes();
        replay(host, mount, item, parent);
        Location location = new SiteMapItemRepresentation().findParentLocation(mount, item);
        assertEquals("parent-id", location.getId());
        assertEquals("localhost/sub/foo/", location.getLocation());
    }

}
