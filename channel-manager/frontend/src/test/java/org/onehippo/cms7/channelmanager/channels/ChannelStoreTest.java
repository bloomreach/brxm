/*
 * Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.cms7.channelmanager.channels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hippoecm.frontend.service.IRestProxyService;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.rest.ChannelService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.wicketstuff.js.ext.data.ExtDataField;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests {ChannelStore}.
 */
public class ChannelStoreTest {

    private Map<String, IRestProxyService> mockedProxyServices = new HashMap<>();
    private ChannelService mockedChannelService;

    @Before
    public void initMocks() {
        final IRestProxyService mockedProxyService = createNiceMock(IRestProxyService.class);
        mockedProxyServices.put("/site", mockedProxyService);
        mockedChannelService = createNiceMock(ChannelService.class);
        expect(mockedProxyService.createSecureRestProxy(ChannelService.class)).andReturn(mockedChannelService);
        replay(mockedProxyService);


    }

    @Test
    public void only_channels_for_correct_contextpath_get_loaded() throws Exception {
        Channel channel1 = new Channel("testchannelid1");
        channel1.setType("testtype");
        channel1.setLocale("nl_NL");
        channel1.setHostname("host.example.com");
        // no context path

        Channel channel2 = new Channel("testchannelid2");
        channel2.setType("testtype");
        channel2.setLocale("nl_NL");
        channel2.setHostname("host.example.com");
        channel2.setContextPath("/foo");
        // incorrect context path

        Channel channel3 = new Channel("testchannelid3");
        channel3.setType("testtype");
        channel3.setLocale("nl_NL");
        channel3.setHostname("host.example.com");
        channel3.setContextPath("/site");
        // correct context path

        // although the channel service returns three channels, only the channel with the correct contextpath /site
        // should be returned
        expect(mockedChannelService.getChannels()).andReturn(Arrays.asList(channel1,channel2, channel3));
        replay(mockedChannelService);

        final List<ExtDataField> fields = Arrays.asList(new ExtDataField("id"), new ExtDataField("locale"), new ExtDataField("hostname"));
        ChannelStore store = new ChannelStore("testStoreId", fields, "dummySortName", ChannelStore.SortOrder.ascending,
                createNiceMock(LocaleResolver.class), mockedProxyServices, new BlueprintStore(mockedProxyServices));

        JSONArray json = store.getData();
        assertEquals("There should be JSON data for 1 channel which has contextpath /site", 1, json.length());

        JSONObject channelData = json.getJSONObject(0);
        assertEquals("The channel should have an ID testchannelid3", "testchannelid3", channelData.getString("id"));

    }

    @Test
    public void testChannelProperties() throws Exception {
        Channel channel = new Channel("testchannelid");
        channel.setType("testtype");
        channel.setLocale("nl_NL");
        channel.setContextPath("/site");
        channel.setHostname("host.example.com");
        expect(mockedChannelService.getChannels()).andReturn(Collections.singletonList(channel));
        replay(mockedChannelService);

        final List<ExtDataField> fields = Arrays.asList(new ExtDataField("id"), new ExtDataField("locale"), new ExtDataField("hostname"));
        ChannelStore store = new ChannelStore("testStoreId", fields, "dummySortName", ChannelStore.SortOrder.ascending,
                createNiceMock(LocaleResolver.class), mockedProxyServices, new BlueprintStore(mockedProxyServices));

        JSONArray json = store.getData();
        assertEquals("There should be JSON data for one channel", 1, json.length());

        JSONObject channelData = json.getJSONObject(0);
        assertEquals("The channel should have a type", "testtype", channelData.getString("channelType"));
        assertEquals("The channel should have a region", "nl_NL", channelData.getString("channelRegion"));
        assertEquals("The channel should have a locale since we provide a field for it", "nl_NL", channelData.getString("locale"));
        assertEquals("The channel should have an ID since we provide a field for it", "testchannelid", channelData.getString("id"));
        assertEquals("The channel should have a host name since we provide a field for it", "host.example.com", channelData.getString("hostname"));
    }

    @Test
    public void testChannelWithoutProperties() throws Exception {
        Channel channel = new Channel("testchannelid");
        channel.setContextPath("/site");
        expect(mockedChannelService.getChannels()).andReturn(Collections.singletonList(channel));
        replay(mockedChannelService);

        final List<ExtDataField> dummyFields = Collections.emptyList();
        ChannelStore store = new ChannelStore("testStoreId", dummyFields, "dummySortName", ChannelStore.SortOrder.ascending,
                createNiceMock(LocaleResolver.class), mockedProxyServices, new BlueprintStore(mockedProxyServices));

        JSONArray json = store.getData();
        assertEquals("There should be JSON data for one channel", 1, json.length());

        JSONObject channelData = json.getJSONObject(0);
        assertEquals("The channel should not have the default type", ChannelStore.DEFAULT_TYPE, channelData.getString("channelType"));
        assertFalse("The channel should not have a region", channelData.has("channelRegion"));
    }

}
