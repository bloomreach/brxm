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
 *
 */

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.ws.rs.Path;

import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.rest.beans.ChannelInfoClassInfo;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.jaxrs.cxf.hst.HstCXFTestFixtureHelper;

import static com.jayway.restassured.http.ContentType.JSON;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.equalTo;


public class RootResourceTest extends AbstractResourceTest {
    private static final String MOCK_REST_PATH = "test-rootresource/";

    private ChannelService channelService;

    /**
     * Override the @Path annotation in the {@link ContainerItemComponentResource} for ease of testing
     */
    @Path(MOCK_REST_PATH)
    private static class RootResourceWithMockPath extends RootResource {
    }

    @Before
    public void setUp() throws RepositoryException {
        final HstRequestContext context = createMockHstContext();
        final HstCXFTestFixtureHelper helper = new HstCXFTestFixtureHelper(context);

        channelService = EasyMock.createMock(ChannelService.class);

        final RootResource rootResource = new RootResourceWithMockPath();
        rootResource.setChannelService(channelService);
        rootResource.setRootPath("/hst:hst");

        Config config = createDefaultConfig()
                .addServerSingleton(rootResource)
                .addServerSingleton(helper);
        setup(config);
    }

    @Test
    public void can_get_channelsettings_information() throws ChannelException {
        final ChannelInfoClassInfo channelFoo = new ChannelInfoClassInfo();
        channelFoo.setClassName("org.onehippo.exampleproject.FooChannelInfo");

        expect(channelService.getChannelInfo("channel-foo"))
                .andReturn(channelFoo);
        replay(channelService);

        when()
            .get(MOCK_REST_PATH + "channels/channel-foo/info")
        .then()
            .statusCode(200)
            .body("className", equalTo("org.onehippo.exampleproject.FooChannelInfo"));

        verify(channelService);
    }


    @Test
    public void cannot_get_channelsettings_information_when_sever_has_error() throws ChannelException {
        expect(channelService.getChannelInfo("channel-foo"))
                .andThrow(new ChannelException("unknown error"));
        replay(channelService);

        when()
            .get(MOCK_REST_PATH + "channels/channel-foo/info")
        .then()
            .statusCode(500)
            .body(equalTo("Could not get channel setting information"));

        verify(channelService);
    }

    @Test
    public void can_save_channelsettings_properties() throws ChannelException, RepositoryException {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("foo", "bah");
        channelService.saveChannelProperties(mockSession, "channel-foo", properties);
        expectLastCall();
        replay(channelService);

        given()
            .contentType(JSON)
            .body(properties)
        .when()
            .put(MOCK_REST_PATH + "channels/channel-foo/properties")
        .then()
                .statusCode(200)
                .body("foo", equalTo("bah"));

        verify(channelService);
    }

    @Test
    public void cannot_save_channelsettings_properties_when_server_has_error() throws ChannelException, RepositoryException {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("foo", "bah");
        channelService.saveChannelProperties(mockSession, "channel-foo", properties);
        expectLastCall().andThrow(new IllegalStateException("something is wrong"));

        replay(channelService);

        given()
            .contentType(JSON)
            .body(properties)
        .when()
            .put(MOCK_REST_PATH + "channels/channel-foo/properties")
        .then()
            .statusCode(500);

        verify(channelService);
    }
}