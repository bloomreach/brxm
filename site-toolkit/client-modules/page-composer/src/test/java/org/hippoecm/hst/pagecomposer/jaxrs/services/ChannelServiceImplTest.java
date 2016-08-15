/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHost;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.container.ModifiableRequestContextProvider;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class ChannelServiceImplTest {

    private Session session;
    private MockNode rootNode;

    private VirtualHosts mockVirtualHosts;
    private Node channelsNode;
    private Node virtualHostsNode;
    private Node sitesNode;
    private Node hstRoot;

    private HstConfigurationService hstConfigurationService;

    @Before
    public void setUp() throws RepositoryException {
        hstConfigurationService = EasyMock.createMock(HstConfigurationService.class);

        rootNode = MockNode.root();
        session = new ExtendedMockSession(rootNode.getSession());

        hstRoot = rootNode.addNode("hst:hst", "hst:hst");
        channelsNode = hstRoot.addNode(HstNodeTypes.NODENAME_HST_CHANNELS, HstNodeTypes.NODETYPE_HST_CHANNELS);
        virtualHostsNode = hstRoot.addNode(HstNodeTypes.NODENAME_HST_HOSTS, HstNodeTypes.NODETYPE_HST_VIRTUALHOSTS);
        sitesNode = hstRoot.addNode("hst:sites", HstNodeTypes.NODETYPE_HST_SITES);

        mockVirtualHosts = mockVirtualHosts();
    }

    @Test
    public void can_create_mock_mount_nodes() throws RepositoryException {
        addMountNode("group1", "com/example/hst:root");
        addMountNode("group1", "com/example/hst:root/bah");
        addMountNode("group2", "com/example/hst:root/");
        addMountNode("group2", "com/example/hst:root/foo");

        assertThat(session.itemExists("/hst:hst/hst:hosts/group1/com/example/hst:root"), is(true));
        assertThat(session.itemExists("/hst:hst/hst:hosts/group1/com/example/hst:root/bah"), is(true));
        assertThat(session.itemExists("/hst:hst/hst:hosts/group2/com/example/hst:root/"), is(true));
        assertThat(session.itemExists("/hst:hst/hst:hosts/group2/com/example/hst:root/foo"), is(true));
    }

    @Test
    public void delete_channel_should_delete_all_binding_mounts() throws Exception {
        final Channel channelFoo = mockChannel("foo");
        addSiteNode("foo");
        addSiteNode("bah");

        final Node bahMountNode = addMountNode("group1", "com/example/hst:root");
        final Node fooMountNode1 = addMountNode("group1", "com/example/hst:root/foo");
        final Node fooMountNode2 = addMountNode("group2", "com/example/hst:root");

        final Map<String, List<String>> hostGroups = new HashMap<>();
        hostGroups.put("group1", Arrays.asList(fooMountNode1.getIdentifier(), bahMountNode.getIdentifier()));
        hostGroups.put("group2", Arrays.asList(fooMountNode2.getIdentifier()));

        final Map<String, String> mountId2MountPoint = new HashMap<>();
        mountId2MountPoint.put(fooMountNode1.getIdentifier(), "/hst:hst/hst:sites/foo");
        mountId2MountPoint.put(fooMountNode2.getIdentifier(), "/hst:hst/hst:sites/foo");
        mountId2MountPoint.put(bahMountNode.getIdentifier(), "/hst:hst/hst:sites/bah");
        mockHostGroups(hostGroups, mountId2MountPoint);

        ChannelServiceImpl channelService = EasyMock.createMockBuilder(ChannelServiceImpl.class)
                .addMockedMethod("getChannel")
                .createMock();
        channelService.setHstConfigurationService(hstConfigurationService);

        EasyMock.expect(channelService.getChannel("foo")).andReturn(channelFoo);
        hstConfigurationService.delete(session, "/hst:hst/hst:configurations/foo");
        EasyMock.expectLastCall();
        EasyMock.replay(channelService, hstConfigurationService);

        assertThat(session.itemExists("/hst:hst/hst:sites/foo"), is(true));
        assertThat(session.itemExists("/hst:hst/hst:channels/foo"), is(true));

        channelService.deleteChannel(session, "foo");

        EasyMock.verify(channelService, hstConfigurationService);

        assertThat(session.itemExists("/hst:hst/hst:sites/bah"), is(true));
        assertThat(session.itemExists("/hst:hst/hst:sites/foo"), is(false));
        assertThat(session.itemExists("/hst:hst/hst:channels/foo"), is(false));
        assertThat(session.itemExists("/hst:hst/hst:hosts/group1/com/example/hst:root/foo"), is(false));
        assertThat(session.itemExists("/hst:hst/hst:hosts/group1/com/example/hst:root"), is(true));

        // all hst:virtualhost nodes are also removed if they are leaf nodes
        assertThat(session.itemExists("/hst:hst/hst:hosts/group2/com/example/hst:root"), is(false));
        assertThat(session.itemExists("/hst:hst/hst:hosts/group2/com/example"), is(false));
        assertThat(session.itemExists("/hst:hst/hst:hosts/group2/com"), is(false));
        assertThat(session.itemExists("/hst:hst/hst:hosts/group2"), is(true));
    }

    // TODO add a test for the Channel Service's handling of the hstConfigurationService#delete() call to
    // throw an exception? (caught and uncaught)

    private static VirtualHosts mockVirtualHosts() {
        final MockHstRequestContext requestContext = new MockHstRequestContext();
        final VirtualHost mockVirtualHost = EasyMock.createMock(VirtualHost.class);
        final VirtualHosts virtualHosts = EasyMock.createMock(VirtualHosts.class);

        EasyMock.expect(mockVirtualHost.getVirtualHosts()).andReturn(virtualHosts).anyTimes();
        requestContext.setVirtualHost(mockVirtualHost);

        ModifiableRequestContextProvider.set(requestContext);

        EasyMock.replay(mockVirtualHost);
        return virtualHosts;
    }

    private void mockHostGroups(final Map<String, List<String>> hostGroups, final Map<String, String> mountPoints) {
        EasyMock.expect(mockVirtualHosts.getHostGroupNames()).andReturn(new ArrayList<>(hostGroups.keySet()));

        hostGroups.forEach((hostGroup, mountIds) -> {
            final List<Mount> mockMounts = mountIds.stream()
                    .map(mountId -> mockMount(mountId, mountPoints.get(mountId)))
                    .collect(Collectors.toList());

            EasyMock.expect(mockVirtualHosts.getMountsByHostGroup(hostGroup)).andReturn(mockMounts);
        });
        EasyMock.replay(mockVirtualHosts);
    }

    private Channel mockChannel(final String id) throws RepositoryException {
        final Channel channel = new Channel(id);
        channel.setHstConfigPath("/hst:hst/hst:configurations/" + id);
        channel.setChannelPath("/hst:hst/hst:channels/" + id);
        channel.setHstMountPoint("/hst:hst/hst:sites/" + id);

        channelsNode.addNode(id, HstNodeTypes.NODETYPE_HST_CHANNEL);
        return channel;
    }

    private void addSiteNode(final String name) throws RepositoryException {
        sitesNode.addNode(name, HstNodeTypes.NODETYPE_HST_SITE);
    }

    private Node addMountNode(final String hostGroup, final String hostPath) throws RepositoryException {
        if (!hostPath.contains(HstNodeTypes.MOUNT_HST_ROOTNAME)) {
            throw new IllegalArgumentException("hostPath must contain segment '/hst:root'");
        }
        final String[] segments = hostPath.split("/");
        if (segments.length < 1) {
            throw new IllegalArgumentException("hostPath must have at least 2 segments");
        }

        final Node hostGroupNode = getOrAddNode(virtualHostsNode, hostGroup, HstNodeTypes.NODETYPE_HST_VIRTUALHOSTGROUP);

        Node parentNode = hostGroupNode;
        String nodeType = HstNodeTypes.NODETYPE_HST_VIRTUALHOST;

        for (int i = 0; i < segments.length; i++) {
            if (HstNodeTypes.MOUNT_HST_ROOTNAME.equals(segments[i])) {
                nodeType = HstNodeTypes.NODETYPE_HST_MOUNT;
                parentNode = getOrAddNode(parentNode, segments[i], nodeType);
            } else {
                parentNode = getOrAddNode(parentNode, segments[i], nodeType);
            }
        }

        return parentNode;
    }

    private static Mount mockMount(final String mountId, final String mountPoint)  {
        final Mount mount = EasyMock.createMock(Mount.class);
        EasyMock.expect(mount.getMountPoint()).andReturn(mountPoint);

        EasyMock.expect(mount.getIdentifier()).andReturn(mountId).anyTimes();

        EasyMock.replay(mount);
        return mount;
    }

    private static Node getOrAddNode(final Node parentNode, final String name, final String nodeType) throws RepositoryException {
        if (parentNode.hasNode(name)) {
            return parentNode.getNode(name);
        } else {
            return parentNode.addNode(name, nodeType);
        }
    }

}