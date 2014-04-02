/*
 *  Copyright 2011-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.site.request;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.PortMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.core.internal.MutableResolvedMount;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.junit.Test;

public class TestMountDecoratorImpl {
    @Test
    public void testDecorationsOfLiveToPreviewMount() throws Exception {
        ContextualizableMount mount = createNiceMock(ContextualizableMount.class);

        expect(mount.isPreview()).andReturn(false).anyTimes();
        expect(mount.getMountPoint()).andReturn("/hst:hst/hst:sites/myproject").anyTimes();
        expect(mount.getType()).andReturn("live").anyTimes();
        // getType is always part of 'types' hence below also add 'live'
        String[] arr = {"foo", "bar", "lux", "live"};
        List<String> types = Arrays.asList(arr);
        expect(mount.getTypes()).andReturn(types).anyTimes();
        
        replay(mount);
        Mount decoratedMount = new MountDecoratorImpl().decorateMountAsPreview(mount);
        assertTrue("The decorated mount is expected to be a preview. ", decoratedMount.isPreview());
        assertEquals("The decorated mount should have a mountPoint '/hst:hst/hst:sites/myproject'. ","/hst:hst/hst:sites/myproject", decoratedMount.getMountPoint());

        assertEquals("The decorated mount should change the type from live to preview. ",decoratedMount.getType(), "preview");
        assertFalse("The decorated mount should not have live ", decoratedMount.getTypes().contains("live"));
        assertTrue("The decorated mount should  have preview ", decoratedMount.getTypes().contains("preview"));
        assertTrue("The decorated mount should  have foo, bar and lux ",
                decoratedMount.getTypes().contains("foo-preview") &&
                        decoratedMount.getTypes().contains("bar-preview") &&
                        decoratedMount.getTypes().contains("lux-preview"));

        assertFalse("The decorated mount should  have foo, bar and lux ",
                decoratedMount.getTypes().contains("foo") ||
                        decoratedMount.getTypes().contains("bar") ||
                        decoratedMount.getTypes().contains("lux"));
        
    }

    @Test
    public void testDecorationsOfPreviewMountStillGetsDecorated() throws Exception {
        Mount mount = createNiceMock(Mount.class);

        expect(mount.isPreview()).andReturn(true).anyTimes();
        replay(mount);
        Mount decoratedMount = new MountDecoratorImpl().decorateMountAsPreview(mount);
        assertFalse("The decoratedMount of a mount that is already preview should still be decorated. ", decoratedMount == mount);

    }

    @Test
    public void testDoubleDecoratedMountGetsSameInstance() throws Exception {
        Mount mount = createNiceMock(Mount.class);
        expect(mount.isPreview()).andReturn(false).anyTimes();
        replay(mount);
        final MountDecoratorImpl mountDecorator = new MountDecoratorImpl();
        Mount decoratedMount = mountDecorator.decorateMountAsPreview(mount);

        assertTrue(decoratedMount.isPreview());

        Mount doubleDecorated =  mountDecorator.decorateMountAsPreview(decoratedMount);
        assertTrue(decoratedMount == doubleDecorated);
    }


    @Test
    public void testDecoratedMountsViaDecoratedVirtualHost() throws Exception {
        Mount mount = createNiceMock(Mount.class);
        expect(mount.isPreview()).andReturn(false).anyTimes();

        VirtualHost virtualHost = createNiceMock(VirtualHost.class);
        PortMount portMount = createNiceMock(PortMount.class);
        expect(mount.getVirtualHost()).andReturn(virtualHost).anyTimes();
        expect(virtualHost.getPortMount(0)).andReturn(portMount).anyTimes();
        expect(portMount.getRootMount()).andReturn(mount).anyTimes();

        replay(mount, virtualHost, portMount);

        final MountDecoratorImpl mountDecorator = new MountDecoratorImpl();

        Mount decoratedMount = mountDecorator.decorateMountAsPreview(mount);

        assertTrue(decoratedMount.isPreview());

        final VirtualHost decoratedHost = decoratedMount.getVirtualHost();
        assertTrue(decoratedHost instanceof MountDecoratorImpl.VirtualHostAsPreviewDecorator);
        final PortMount decoratedPort = decoratedHost.getPortMount(0);
        assertTrue(decoratedPort instanceof MountDecoratorImpl.PortMountAsPreviewDecorator);

        final Mount decoratedMountViaHostPort = decoratedPort.getRootMount();
        assertTrue(decoratedMountViaHostPort instanceof MountDecoratorImpl.MountAsPreviewDecorator);

        // via via you get a new decorated instance
        assertFalse(decoratedMountViaHostPort == mountDecorator);
    }

    @Test
    public void testDecoratedMountsViaDecoratedVirtualHosts() throws Exception {
        Mount mount1 = createNiceMock(Mount.class);
        Mount mount2 = createNiceMock(Mount.class);
        Mount mount3 = createNiceMock(Mount.class);
        expect(mount1.isPreview()).andReturn(false).anyTimes();
        expect(mount1.getName()).andReturn("mount1").anyTimes();
        expect(mount2.isPreview()).andReturn(false).anyTimes();
        expect(mount2.getName()).andReturn("mount2").anyTimes();
        expect(mount3.isPreview()).andReturn(false).anyTimes();
        expect(mount3.getName()).andReturn("mount3").anyTimes();
        ResolvedMount resolvedMount1 = createNiceMock(ResolvedMount.class);
        MutableResolvedMount resolvedMount2 = createNiceMock(MutableResolvedMount.class);
        ResolvedMount resolvedMount3 = createNiceMock(ResolvedMount.class);

        expect(resolvedMount1.getMount()).andReturn(mount1).anyTimes();
        expect(resolvedMount2.getMount()).andReturn(mount2).anyTimes();
        expect(resolvedMount3.getMount()).andReturn(mount3).anyTimes();

        VirtualHost virtualHost = createNiceMock(VirtualHost.class);
        expect(mount1.getVirtualHost()).andReturn(virtualHost).anyTimes();

        VirtualHosts virtualHosts = createNiceMock(VirtualHosts.class);
        expect(virtualHost.getVirtualHosts()).andReturn(virtualHosts).anyTimes();

        expect(virtualHosts.getMountsByHostGroup("foo")).andReturn(Lists.asList(mount1, new Mount[]{mount2, mount3})).anyTimes();

        replay(mount1, mount2, mount3, resolvedMount1, resolvedMount2, resolvedMount3, virtualHost, virtualHosts);

        final MountDecoratorImpl mountDecorator = new MountDecoratorImpl();
        Mount decoratedMount = mountDecorator.decorateMountAsPreview(mount1);
        assertTrue(decoratedMount.isPreview());

        final VirtualHosts decoratedHosts = decoratedMount.getVirtualHost().getVirtualHosts();
        assertTrue(decoratedHosts instanceof MountDecoratorImpl.VirtualHostsAsPreviewDecorator);

        final List<Mount> decoratedHostsMountsViaHosts = decoratedHosts.getMountsByHostGroup("foo");
        for (Mount decoratedHostsMountsViaHost : decoratedHostsMountsViaHosts) {
            assertTrue(decoratedHostsMountsViaHost instanceof MountDecoratorImpl.MountAsPreviewDecorator);
        }
    }

}
