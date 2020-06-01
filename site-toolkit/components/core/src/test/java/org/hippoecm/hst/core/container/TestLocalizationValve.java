/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.core.container;

import static org.junit.Assert.assertArrayEquals;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.ChannelInfo;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.mock.core.request.MockHstRequestContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestLocalizationValve {

    private static final String MOUNT_DEFAULT_RESOURCE_BUNDLE_ID = "com.example.mount.Messages1,com.example.mount.Messages2";
    private static final String[] MOUNT_DEFAULT_RESOURCE_BUNDLE_ID_ARRAY = StringUtils
            .split(MOUNT_DEFAULT_RESOURCE_BUNDLE_ID, "\t\r\n, ");

    private static final String CHANNEL_INFO_DEFAULT_RESOURCE_BUNDLE_ID = "com.example.channel.Messages1,com.example.channel.Messages2";
    private static final String[] CHANNEL_INFO_DEFAULT_RESOURCE_BUNDLE_ID_ARRAY = StringUtils
            .split(CHANNEL_INFO_DEFAULT_RESOURCE_BUNDLE_ID, "\t\r\n, ");

    private LocalizationValve localizationValve;

    private MockHstRequestContext requestContext;
    private ResolvedMount resolvedMount;
    private Mount mount;
    private ChannelInfo channelInfo;
    private Map<String, Object> channelInfoProps = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        localizationValve = new LocalizationValve();

        requestContext = new MockHstRequestContext();

        channelInfo = EasyMock.createNiceMock(ChannelInfo.class);
        EasyMock.expect(channelInfo.getProperties()).andReturn(channelInfoProps).anyTimes();

        mount = EasyMock.createNiceMock(Mount.class);
        EasyMock.expect(mount.getDefaultResourceBundleIds()).andReturn(MOUNT_DEFAULT_RESOURCE_BUNDLE_ID_ARRAY).anyTimes();
        EasyMock.expect(mount.getChannelInfo()).andReturn(channelInfo).anyTimes();

        resolvedMount = EasyMock.createNiceMock(ResolvedMount.class);
        EasyMock.expect(resolvedMount.getMount()).andReturn(mount).anyTimes();

        requestContext.setResolvedMount(resolvedMount);

        EasyMock.replay(channelInfo);
        EasyMock.replay(mount);
        EasyMock.replay(resolvedMount);
    }

    @After
    public void tearDown() throws Exception {
        channelInfoProps.clear();
    }

    @Test
    public void testFindResourceBundleIdsFromMount() throws Exception {
        String [] bundleIds = localizationValve.findResourceBundleIds(requestContext);
        assertArrayEquals(MOUNT_DEFAULT_RESOURCE_BUNDLE_ID_ARRAY, bundleIds);
    }

    @Test
    public void testFindResourceBundleIdsOverriddenInChannel() throws Exception {
        channelInfoProps.put(HstNodeTypes.GENERAL_PROPERTY_DEFAULT_RESOURCE_BUNDLE_ID, CHANNEL_INFO_DEFAULT_RESOURCE_BUNDLE_ID);
        String [] bundleIds = localizationValve.findResourceBundleIds(requestContext);
        assertArrayEquals(CHANNEL_INFO_DEFAULT_RESOURCE_BUNDLE_ID_ARRAY, bundleIds);
    }
}
