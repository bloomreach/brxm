package org.onehippo.cms7.channelmanager.channels;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hippoecm.frontend.service.IRestProxyService;
import org.hippoecm.hst.configuration.channel.Channel;
import org.hippoecm.hst.rest.ChannelService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.wicketstuff.js.ext.data.ExtField;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Tests {ChannelStore}.
 */
public class ChannelStoreTest {

    private IRestProxyService mockedProxyService;
    private ChannelService mockedChannelService;

    @Before
    public void initMocks() {
        mockedProxyService = createNiceMock(IRestProxyService.class);
        mockedChannelService = createNiceMock(ChannelService.class);
        expect(mockedProxyService.createRestProxy(ChannelService.class)).andReturn(mockedChannelService);
        replay(mockedProxyService);
    }

    @Test
    public void testChannelProperties() throws Exception {
        Channel channel = new Channel("testchannelid");
        channel.setType("testtype");
        channel.setLocale("nl_NL");
        channel.setHostname("host.example.com");
        expect(mockedChannelService.getChannels()).andReturn(Collections.singletonList(channel));
        replay(mockedChannelService);

        final List<ExtField> fields = Arrays.asList(new ExtField("id"), new ExtField("locale"), new ExtField("hostname"));
        ChannelStore store = new ChannelStore("testStoreId", fields, "dummySortName", ChannelStore.SortOrder.ascending,
                createNiceMock(LocaleResolver.class), mockedProxyService);

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
        expect(mockedChannelService.getChannels()).andReturn(Collections.singletonList(channel));
        replay(mockedChannelService);

        final List<ExtField> dummyFields = Collections.emptyList();
        ChannelStore store = new ChannelStore("testStoreId", dummyFields, "dummySortName", ChannelStore.SortOrder.ascending,
                createNiceMock(LocaleResolver.class), mockedProxyService);

        JSONArray json = store.getData();
        assertEquals("There should be JSON data for one channel", 1, json.length());

        JSONObject channelData = json.getJSONObject(0);
        assertEquals("The channel should not have the default type", ChannelStore.DEFAULT_TYPE, channelData.getString("channelType"));
        assertFalse("The channel should not have a region", channelData.has("channelRegion"));
    }

}
