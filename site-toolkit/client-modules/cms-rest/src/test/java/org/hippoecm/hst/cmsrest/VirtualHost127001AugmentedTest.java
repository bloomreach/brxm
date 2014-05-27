/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.cmsrest;

import javax.jcr.Session;

import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.hosting.CustomMountAndVirtualHostAugmenter;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.core.request.ResolvedVirtualHost;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class VirtualHost127001AugmentedTest extends AbstractCmsRestTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }


    @Test
    public void testHost127001AndCmsRestMountIsAugmentedWhenNotExisting() throws Exception {
        final VirtualHosts virtualHosts = hstManager.getVirtualHosts();
        final ResolvedVirtualHost resolvedVirtualHost = virtualHosts.matchVirtualHost("127.0.0.1");
        assertNotNull(resolvedVirtualHost);
        assertEquals("127", resolvedVirtualHost.getVirtualHost().getName());
        assertEquals("127.0.0.1", resolvedVirtualHost.getVirtualHost().getHostName());
        // when the 127.0.0.1 does not yet exist it is added at a new host group name 'CustomMountAndVirtualHostAugmenter.class.getName()'
        assertEquals(CustomMountAndVirtualHostAugmenter.class.getName(),resolvedVirtualHost.getVirtualHost().getHostGroupName());
        final ResolvedMount resolvedMount = virtualHosts.matchMount("127.0.0.1", null, "/_cmsrest");
        assertNotNull(resolvedMount);
        assertEquals("_cmsrest",resolvedMount.getMount().getName());
    }

    @Test
    public void testCmsRestMountIsAddedWhenHost127001AlreadyExisting() throws Exception {
        // Rename the 'localhost' host to 127.0.0.1 for hostgroup dev-localhost :
        // then, the _cmsrest should be added to the existing host, and thus 'live' in dev-localhost
        // instead of hostgroup with name 'CustomMountAndVirtualHostAugmenter.class.getName()'
        Session session = createSession();
        session.move("/hst:hst/hst:hosts/dev-localhost/localhost", "/hst:hst/hst:hosts/dev-localhost/127.0.0.1");
        session.save();
        final VirtualHosts virtualHosts = hstManager.getVirtualHosts();
        final ResolvedVirtualHost resolvedVirtualHost = virtualHosts.matchVirtualHost("127.0.0.1");
        assertNotNull(resolvedVirtualHost);
        assertEquals("127", resolvedVirtualHost.getVirtualHost().getName());
        assertEquals("127.0.0.1", resolvedVirtualHost.getVirtualHost().getHostName());
        // since 127.0.0.1 already exists, the _cmsrest mount should be added to 127.0.0.1 host living
        // below dev-localhost
        assertEquals("dev-localhost",resolvedVirtualHost.getVirtualHost().getHostGroupName());
        final ResolvedMount resolvedMount = virtualHosts.matchMount("127.0.0.1", null, "/_cmsrest");
        assertNotNull(resolvedMount);
        assertEquals("_cmsrest",resolvedMount.getMount().getName());

        // move 127.0.0.1 back to localhost
        session.move("/hst:hst/hst:hosts/dev-localhost/127.0.0.1", "/hst:hst/hst:hosts/dev-localhost/localhost");
        session.save();
        session.logout();
    }

}