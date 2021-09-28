/*
 *  Copyright 2011-2021 Hippo B.V. (http://www.onehippo.com)
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

import java.util.Arrays;
import java.util.List;

import com.google.common.collect.Lists;

import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.PortMount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.configuration.internal.ContextualizableMount;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.core.internal.MutableResolvedMount;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestPreviewDecoratorImpl {

    @Before
    public void setUp() {
        MockHstRequestContext ctx = new MockHstRequestContext();
        ctx.setChannelManagerPreviewRequest();
        ModifiableRequestContextProvider.set(ctx);
    }

    @After
    public void tearDown() {
        ModifiableRequestContextProvider.clear();
    }

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
        Mount decoratedMount = new PreviewDecoratorImpl().decorateMountAsPreview(mount);
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
        Mount decoratedMount = new PreviewDecoratorImpl().decorateMountAsPreview(mount);
        assertFalse("The decoratedMount of a mount that is already preview should still be decorated. ", decoratedMount == mount);

    }

    @Test
    public void testDoubleDecoratedMountGetsSameInstance() throws Exception {
        Mount mount = createNiceMock(Mount.class);
        expect(mount.isPreview()).andReturn(false).anyTimes();
        replay(mount);
        final PreviewDecoratorImpl mountDecorator = new PreviewDecoratorImpl();
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

        final PreviewDecoratorImpl mountDecorator = new PreviewDecoratorImpl();

        Mount decoratedMount = mountDecorator.decorateMountAsPreview(mount);

        assertTrue(decoratedMount.isPreview());

        final VirtualHost decoratedHost = decoratedMount.getVirtualHost();
        assertTrue(decoratedHost instanceof PreviewDecoratorImpl.PreviewDecoratedVirtualHost);
        final PortMount decoratedPort = decoratedHost.getPortMount(0);
        assertTrue(decoratedPort instanceof PreviewDecoratorImpl.PreviewDecoratedPortMount);

        final Mount decoratedMountViaHostPort = decoratedPort.getRootMount();
        assertTrue(decoratedMountViaHostPort instanceof PreviewDecoratorImpl.PreviewDecoratedMount);

        // via via you get a new decorated instance
        assertFalse(decoratedMountViaHostPort == mountDecorator);
    }

    @Test
    public void testDecoratedMountsViaDecoratedVirtualHosts() throws Exception {
        Mount mount1 = createNiceMock(Mount.class);
        Mount mount2 = createNiceMock(Mount.class);
        Mount mount3 = createNiceMock(Mount.class);
        expect(mount1.isPreview()).andReturn(false).anyTimes();
        expect(mount1.getType()).andReturn("live").anyTimes();
        expect(mount1.getName()).andReturn("mount1").anyTimes();
        expect(mount2.isPreview()).andReturn(false).anyTimes();
        expect(mount2.getType()).andReturn("live").anyTimes();
        expect(mount2.getName()).andReturn("mount2").anyTimes();
        expect(mount3.isPreview()).andReturn(false).anyTimes();
        expect(mount3.getType()).andReturn("live").anyTimes();
        expect(mount3.getName()).andReturn("mount3").anyTimes();
        ResolvedMount resolvedMount1 = createNiceMock(ResolvedMount.class);
        MutableResolvedMount resolvedMount2 = createNiceMock(MutableResolvedMount.class);
        ResolvedMount resolvedMount3 = createNiceMock(ResolvedMount.class);

        expect(resolvedMount1.getMount()).andReturn(mount1).anyTimes();
        expect(resolvedMount2.getMount()).andReturn(mount2).anyTimes();
        expect(resolvedMount3.getMount()).andReturn(mount3).anyTimes();


        PortMount portMount = createNiceMock(PortMount.class);
        VirtualHost virtualHost = createNiceMock(VirtualHost.class);
        expect(mount1.getVirtualHost()).andReturn(virtualHost).anyTimes();

        expect(virtualHost.getPortMount(eq(0))).andStubReturn(portMount);
        expect(portMount.getRootMount()).andStubReturn(mount1);


        VirtualHosts virtualHosts = createNiceMock(VirtualHosts.class);
        expect(virtualHost.getVirtualHosts()).andReturn(virtualHosts).anyTimes();

        expect(virtualHosts.getMountsByHostGroup("foo")).andReturn(Lists.asList(mount1, new Mount[]{mount2, mount3})).anyTimes();
        expect(virtualHosts.getMountByGroupAliasAndType("foo", "bar", "live")).andReturn(mount1).anyTimes();
        expect(virtualHosts.getMountByIdentifier("uuid")).andReturn(mount1).anyTimes();

        replay(mount1, mount2, mount3, resolvedMount1, resolvedMount2, resolvedMount3, virtualHost, virtualHosts);

        final PreviewDecoratorImpl mountDecorator = new PreviewDecoratorImpl();
        Mount decoratedMount = mountDecorator.decorateMountAsPreview(mount1);
        assertTrue(decoratedMount.isPreview());

        final VirtualHosts decoratedHosts = decoratedMount.getVirtualHost().getVirtualHosts();
        assertTrue(decoratedHosts instanceof PreviewDecoratorImpl.PreviewDecoratedVirtualHosts);

        final Mount mountByGroupAliasAndType = decoratedHosts.getMountByGroupAliasAndType("foo", "bar", "live");

        assertTrue(mountByGroupAliasAndType instanceof PreviewDecoratorImpl.PreviewDecoratedMount);
        assertTrue(mountByGroupAliasAndType.isPreview());
        assertTrue(mountByGroupAliasAndType.getType().equals(Mount.PREVIEW_NAME));

        final Mount decoratedByUUID = decoratedHosts.getMountByIdentifier("uuid");
        assertTrue(decoratedByUUID instanceof PreviewDecoratorImpl.PreviewDecoratedMount);

        final List<Mount> decoratedHostsMountsViaHosts = decoratedHosts.getMountsByHostGroup("foo");
        for (Mount decoratedHostsMountsViaHost : decoratedHostsMountsViaHosts) {
            assertTrue(decoratedHostsMountsViaHost instanceof PreviewDecoratorImpl.PreviewDecoratedMount);
        }
    }


    @Test
    public void testExplicitPreviewMountsAreSkipped() throws Exception {
        Mount mount1 = createNiceMock(Mount.class);
        Mount mount2 = createNiceMock(Mount.class);
        expect(mount1.isPreview()).andReturn(false).anyTimes();
        expect(mount1.getType()).andReturn("live").anyTimes();
        expect(mount1.getName()).andReturn("mount1").anyTimes();
        expect(mount2.isPreview()).andReturn(true).anyTimes();
        expect(mount2.getType()).andReturn("preview").anyTimes();
        expect(mount2.getName()).andReturn("mount2").anyTimes();

        ResolvedMount resolvedMount1 = createNiceMock(ResolvedMount.class);
        MutableResolvedMount resolvedMount2 = createNiceMock(MutableResolvedMount.class);

        expect(resolvedMount1.getMount()).andReturn(mount1).anyTimes();
        expect(resolvedMount2.getMount()).andReturn(mount2).anyTimes();

        PortMount portMount = createNiceMock(PortMount.class);
        VirtualHost virtualHost = createNiceMock(VirtualHost.class);
        expect(mount1.getVirtualHost()).andReturn(virtualHost).anyTimes();

        expect(virtualHost.getPortMount(eq(0))).andStubReturn(portMount);
        expect(portMount.getRootMount()).andStubReturn(mount1);


        VirtualHosts virtualHosts = createNiceMock(VirtualHosts.class);
        expect(virtualHost.getVirtualHosts()).andReturn(virtualHosts).anyTimes();

        expect(virtualHosts.getMountsByHostGroup("foo")).andReturn(Lists.asList(mount1, new Mount[]{mount2})).anyTimes();

        replay(mount1, mount2, resolvedMount1, resolvedMount2, virtualHost, virtualHosts);

        final PreviewDecoratorImpl mountDecorator = new PreviewDecoratorImpl();
        Mount decoratedMount = mountDecorator.decorateMountAsPreview(mount1);
        final VirtualHosts decoratedHosts = decoratedMount.getVirtualHost().getVirtualHosts();

        final List<Mount> decoratedHostsMountsViaHosts = decoratedHosts.getMountsByHostGroup("foo");

        // explicit preview mount should be skipped
        assertEquals(1L, decoratedHostsMountsViaHosts.size());

    }

    @Test
    public void get_parent_via_decorated_mount_returns_same_parent_instance() {
        final Mount child = createNiceMock(Mount.class);
        final Mount parent = createNiceMock(Mount.class);

        mockParentChildSetup(child, parent);
        replay(child, parent);

        final PreviewDecoratorImpl mountDecorator = new PreviewDecoratorImpl();
        final Mount decoratedChild = mountDecorator.decorateMountAsPreview(child);

        assertTrue(decoratedChild.getParent() == decoratedChild.getParent());
    }
    @Test
    public void get_decorated_mount_via_parent_returns_same_instance() {
        final Mount child = createNiceMock(Mount.class);
        final Mount parent = createNiceMock(Mount.class);

        mockParentChildSetup(child, parent);
        replay(child, parent);

        final PreviewDecoratorImpl mountDecorator = new PreviewDecoratorImpl();
        final Mount decoratedChild = mountDecorator.decorateMountAsPreview(child);

        assertTrue(decoratedChild.getParent().getChildMount("child") == decoratedChild);
    }

    private void mockParentChildSetup(final Mount child, final Mount parent) {
        expect(child.isPreview()).andReturn(false).anyTimes();
        expect(child.getName()).andReturn("child").anyTimes();
        expect(child.getParent()).andStubReturn(parent);
        expect(parent.getName()).andReturn("parent").anyTimes();
        expect(parent.getChildMount(eq("child"))).andStubReturn(child);

    }

    @Test
    public void get_virtualhost_via_decorated_mount_returns_same_instance() {

        final VirtualHost virtualHost = createNiceMock(VirtualHost.class);
        final PortMount portMount = createNiceMock(PortMount.class);

        final Mount mount = createNiceMock(Mount.class);
        expect(mount.isPreview()).andReturn(false).anyTimes();
        expect(mount.getName()).andReturn("mount").anyTimes();
        expect(mount.getVirtualHost()).andReturn(virtualHost).anyTimes();
        expect(virtualHost.getPortMount(eq(0))).andStubReturn(portMount);
        expect(portMount.getRootMount()).andStubReturn(mount);

        replay(mount, portMount, virtualHost);

        final PreviewDecoratorImpl mountDecorator = new PreviewDecoratorImpl();
        final Mount decoratedMount = mountDecorator.decorateMountAsPreview(mount);

        assertTrue(decoratedMount.getVirtualHost() == decoratedMount.getVirtualHost());
    }

    @Test
    public void get_decorated_mount_via_virtualhost_returns_same_instance() {
        final VirtualHost virtualHost = createNiceMock(VirtualHost.class);
        final PortMount portMount = createNiceMock(PortMount.class);

        final Mount mount = createNiceMock(Mount.class);
        expect(mount.isPreview()).andReturn(false).anyTimes();
        expect(mount.getName()).andReturn("mount").anyTimes();
        expect(mount.getVirtualHost()).andReturn(virtualHost).anyTimes();

        expect(virtualHost.getPortMount(eq(0))).andStubReturn(portMount);
        expect(portMount.getRootMount()).andStubReturn(mount);

        replay(virtualHost, portMount, mount);

        final PreviewDecoratorImpl mountDecorator = new PreviewDecoratorImpl();
        final Mount decoratedMount = mountDecorator.decorateMountAsPreview(mount);

        assertTrue(decoratedMount.getVirtualHost().getPortMount(0).getRootMount() == decoratedMount);
    }

    @Test
    public void get_decorated_virtualhost_instance_via_parent_or_child_mount_results_in_same_instance() {

        final VirtualHost virtualHost = createNiceMock(VirtualHost.class);
        final PortMount portMount = createNiceMock(PortMount.class);

        final Mount child = createNiceMock(Mount.class);
        final Mount parent = createNiceMock(Mount.class);

        expect(parent.getVirtualHost()).andReturn(virtualHost).anyTimes();
        expect(child.getVirtualHost()).andReturn(virtualHost).anyTimes();

        expect(virtualHost.getPortMount(eq(0))).andStubReturn(portMount);
        expect(portMount.getRootMount()).andStubReturn(parent);

        mockParentChildSetup(child, parent);
        replay(virtualHost, portMount, child, parent);

        final PreviewDecoratorImpl mountDecorator = new PreviewDecoratorImpl();
        final Mount decoratedMount = mountDecorator.decorateMountAsPreview(child);

        assertTrue(decoratedMount.getVirtualHost() == decoratedMount.getParent().getVirtualHost());
    }

    @Test
    public void traverse_through_preview_decorated_model_keeps_returning_same_decorated_instances() {
        final Mount child = createNiceMock(Mount.class);
        final Mount parent = createNiceMock(Mount.class);
        mockParentChildSetup(child, parent);

        final VirtualHosts virtualHosts = createNiceMock(VirtualHosts.class);

        expect(virtualHosts.getMountByIdentifier(eq("parentMountId"))).andStubReturn(parent);
        expect(parent.getIdentifier()).andStubReturn("parentMountId");

        final VirtualHost parentVirtualHost = createNiceMock(VirtualHost.class);
        expect(parentVirtualHost.getVirtualHosts()).andStubReturn(virtualHosts);

        final VirtualHost childVirtualHost = createNiceMock(VirtualHost.class);
        expect(childVirtualHost.getVirtualHosts()).andStubReturn(virtualHosts);
        expect(childVirtualHost.getName()).andStubReturn("childHost");

        expect(child.getVirtualHost()).andStubReturn(childVirtualHost);
        expect(parentVirtualHost.getChildHost(eq("childHost"))).andStubReturn(childVirtualHost);

        final PortMount portMount = createNiceMock(PortMount.class);

        expect(parent.getVirtualHost()).andReturn(childVirtualHost).anyTimes();

        expect(childVirtualHost.getPortMount(eq(0))).andStubReturn(portMount);
        expect(portMount.getRootMount()).andStubReturn(parent);

        replay(child, parent, portMount, childVirtualHost, virtualHosts);


        final PreviewDecoratorImpl mountDecorator = new PreviewDecoratorImpl();
        final Mount childDecoratedMount = mountDecorator.decorateMountAsPreview(child);

        VirtualHosts decoratedVirtualHosts = childDecoratedMount.getVirtualHost().getVirtualHosts();

        assertTrue(decoratedVirtualHosts == childDecoratedMount.getVirtualHost().getVirtualHosts());

        Mount decoratedMountParent = childDecoratedMount.getParent();

        Mount decoratedMountParentAgain = decoratedVirtualHosts.getMountByIdentifier("parentMountId");

        assertTrue(decoratedMountParent instanceof PreviewDecoratorImpl.PreviewDecoratedMount &&
                decoratedMountParentAgain instanceof PreviewDecoratorImpl.PreviewDecoratedMount);

        assertFalse("Getting a mount via the decoratedVirtualHosts does not result in the same decorated mount " +
                "as through which the decoratedVirtualHosts was retrieved...this can be a future improvement not yet " +
                "supported",decoratedMountParent == decoratedMountParentAgain);

        assertTrue("Fetching same mount twice via decoratedVirtualHosts results in same instance",
                decoratedMountParentAgain == decoratedVirtualHosts.getMountByIdentifier("parentMountId"));
    }
}
