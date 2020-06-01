/*
 * Copyright 2015-2019 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.core.channelmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstURL;
import org.hippoecm.hst.core.component.HstURLFactory;
import org.hippoecm.hst.core.container.HstComponentWindow;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.hst.Channel;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.hst.configuration.components.HstComponentConfiguration.Type.CONTAINER_COMPONENT;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_INHERITED;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_TYPE;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_XTYPE;
import static org.hippoecm.hst.core.channelmanager.ChannelManagerConstants.HST_END_MARKER;
import static org.hippoecm.hst.core.container.ContainerConstants.PAGE_MODEL_PIPELINE_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class CmsComponentComponentWindowAttributeContributorTest {

    private List<Object> mocks;
    private HstComponentWindow window;
    private HstRequest request;
    private HstRequestContext requestContext;
    private Channel channel;
    private Mount mount;

    private CmsComponentComponentWindowAttributeContributor contributor;

    @Before
    public void setUp() {
        mocks = new ArrayList<>();
        window = mock(HstComponentWindow.class);
        request = mock(HstRequest.class);

        final ResolvedMount resolvedMount = mock(ResolvedMount.class);
        mount = mock(Mount.class);
        expect(resolvedMount.getMount()).andStubReturn(mount);
        channel = mock(Channel.class);
        expect(mount.getChannel()).andStubReturn(channel);
        expect(channel.isConfigurationLocked()).andStubReturn(false);
        contributor = new CmsComponentComponentWindowAttributeContributor();
        requestContext= mock(HstRequestContext.class);
        expect(requestContext.getResolvedMount()).andStubReturn(resolvedMount);
        final HstComponentConfiguration config = mock(HstComponentConfiguration.class);
        expect(window.getComponentInfo()).andStubReturn(config);
        expect(window.getReferenceNamespace()).andStubReturn("reference-namespace");
        expect(config.getComponentType()).andStubReturn(CONTAINER_COMPONENT);

        expect(request.getRequestContext()).andStubReturn(requestContext);
        final HstURLFactory urlFactory = mock(HstURLFactory.class);
        expect(requestContext.getURLFactory()).andStubReturn(urlFactory);
        final HstURL url = mock(HstURL.class);
        expect(urlFactory.createURL(isA(String.class), isA(String.class), eq(null), eq(requestContext))).andReturn(url);
    }

    @Test
    public void testContributePreamble() {

        replay(mocks.toArray());

        final Map<String, String> map = new HashMap<>();
        contributor.contributePreamble(window, request, map);

        assertThat(map.containsKey(HST_XTYPE), is(false));
        assertThat(map.containsKey(HST_INHERITED), is(false));

        assertThat(map.containsKey("uuid"), is(true));
        assertThat(map.containsKey(HST_TYPE), is(true));
        assertThat(map.containsKey("refNS"), is(true));
        assertThat(map.containsKey("url"), is(true));
    }

    @Test
    public void testContributePreambleChannelLocked() {
        reset(channel);
        expect(channel.isConfigurationLocked()).andStubReturn(true);

        replay(mocks.toArray());
        final Map<String, String> map = new HashMap<>();
        contributor.contributePreamble(window, request, map);
        assertEquals("Since hst channel configuration is locked we expect the preamble to " +
                "indicate that 'system' has the lock",map.get("HST-LockedBy"), "system");
        assertEquals("false", map.get("HST-LockedBy-Current-User"));
    }

    @Test
    public void testContributePreambleChannelLockedPageModelApi() {
        reset(requestContext, channel);

        final ResolvedMount apiResolvedMount = mock(ResolvedMount.class);
        final Mount apiMount = mock(Mount.class);
        expect(apiResolvedMount.getMount()).andStubReturn(apiMount);
        expect(apiMount.getNamedPipeline()).andStubReturn(PAGE_MODEL_PIPELINE_NAME);
        // also return same channel object as parent mount
        expect(apiMount.getChannel()).andStubReturn(channel);
        expect(apiMount.getParent()).andStubReturn(mount);

        expect(requestContext.getResolvedMount()).andStubReturn(apiResolvedMount);

        final HstURLFactory urlFactory = mock(HstURLFactory.class);
        expect(requestContext.getURLFactory()).andReturn(urlFactory);
        final HstURL url = mock(HstURL.class);
        expect(urlFactory.createURL(isA(String.class), isA(String.class), eq(null), eq(requestContext))).andReturn(url);

        expect(channel.isConfigurationLocked()).andStubReturn(true);

        replay(mocks.toArray());

        assertThat(requestContext.getResolvedMount(), is(apiResolvedMount));
        final Map<String, String> map = new HashMap<>();


        contributor.contributePreamble(window, request, map);

        assertEquals("Since hst channel configuration is locked we expect the preamble to " +
                "indicate that 'system' has the lock",map.get("HST-LockedBy"), "system");
        assertEquals("false", map.get("HST-LockedBy-Current-User"));

    }

    @Test
    public void testContributeEpilogue() {
        final HstComponentConfiguration config = mock(HstComponentConfiguration.class);
        expect(window.getComponentInfo()).andReturn(config);
        expect(config.getComponentType()).andReturn(CONTAINER_COMPONENT);
        replay(mocks.toArray());

        final Map<String, String> map = new HashMap<>();
        contributor.contributeEpilogue(window, request, map);

        assertThat(map.containsKey("uuid"), is(true));
        assertThat(map.containsKey(HST_END_MARKER), is(true));
    }

    private <T> T mock(final Class<T> type) {
        final T mock = createNiceMock(type);
        mocks.add(mock);
        return mock;
    }
}
