/*
 * Copyright 2016-2020 Bloomreach
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

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.ws.rs.Path;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.hippoecm.hst.configuration.channel.ChannelException;
import org.hippoecm.hst.configuration.channel.exceptions.ChannelNotFoundException;
import org.hippoecm.hst.configuration.hosting.Mount;
import org.hippoecm.hst.configuration.hosting.VirtualHosts;
import org.hippoecm.hst.core.parameters.DropDownList;
import org.hippoecm.hst.core.parameters.EmptyValueListProvider;
import org.hippoecm.hst.core.parameters.HstValueType;
import org.hippoecm.hst.core.parameters.ValueListProvider;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.pagecomposer.jaxrs.api.BeforeChannelDeleteEvent;
import org.hippoecm.hst.pagecomposer.jaxrs.model.ChannelInfoDescription;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientError;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.ClientException;
import org.hippoecm.hst.pagecomposer.jaxrs.util.HstConfigurationUtils;
import org.hippoecm.hst.platform.api.beans.FieldGroupInfo;
import org.hippoecm.hst.platform.api.beans.HstPropertyDefinitionInfo;
import org.hippoecm.hst.platform.model.HstModel;
import org.hippoecm.hst.platform.model.HstModelRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.cms7.services.HippoServiceRegistry;
import org.onehippo.cms7.services.hst.Channel;
import org.onehippo.jaxrs.cxf.hst.HstCXFTestFixtureHelper;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static io.restassured.http.ContentType.JSON;
import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*", "com.sun.org.apache.xalan.*", "javax.activation.*", "javax.net.ssl.*"})
@PrepareForTest(HstConfigurationUtils.class)
public class RootResourceTest extends AbstractResourceTest {
    private static final String MOCK_REST_PATH = "test-rootresource/";

    private ChannelService channelService;
    private RootResourceWithMockPath rootResource;

    /**
     * Override the @Path annotation in the {@link ContainerItemComponentResource} for ease of testing
     */
    @Path(MOCK_REST_PATH)
    private static class RootResourceWithMockPath extends RootResource {
        @Override
        protected void resetSession() {
            // override to mock this method
        }

        @Override
        protected void removeRenderingMountId() {
            // nothing
        }
    }

    @Before
    public void setUp() throws RepositoryException {
        final HstRequestContext context = createMockHstContext();
        final HstCXFTestFixtureHelper helper = new HstCXFTestFixtureHelper(context);

        channelService = EasyMock.createMock(ChannelService.class);

        rootResource = EasyMock.createMockBuilder(RootResourceWithMockPath.class)
                .addMockedMethod("publishSynchronousEvent")
                .createMock();

        rootResource.setChannelService(channelService);


        final Config config = createDefaultConfig(JsonPojoMapperProvider.class)
                .addServerSingleton(rootResource)
                .addServerSingleton(helper);
        setup(config);
    }

    @Test
    public void can_get_channel_info_description() throws ChannelException, RepositoryException {
        final Map<String, String> i18nResources = new HashMap<>();
        i18nResources.put("field1", "Field 1");
        i18nResources.put("field2", "Field 2");
        final List<FieldGroupInfo> fieldGroups = new ArrayList<>();
        final String[] fieldNames = {"field1", "field2"};
        fieldGroups.add(new FieldGroupInfo(fieldNames, "fieldGroup1"));
        fieldGroups.add(new FieldGroupInfo(fieldNames, "fieldGroup2"));

        final ChannelInfoDescription channelInfoDescription
                = new ChannelInfoDescription(fieldGroups, createPropertyDefinitions(), i18nResources, "tester", true);

        expect(channelService.getChannelInfoDescription("channel-foo", "nl", "dev-localhost"))
                .andReturn(channelInfoDescription);
        replay(channelService);

        given()
                .header("hostGroup", "dev-localhost")
                .get(MOCK_REST_PATH + "channels/channel-foo/info?locale=nl")
                .then()
                .statusCode(200)
                .body("fieldGroups[0].titleKey", equalTo("fieldGroup1"),
                        "fieldGroups[1].titleKey", equalTo("fieldGroup2"),
                        "propertyDefinitions['field1'].name", equalTo("field1"),
                        "propertyDefinitions['field2'].name", equalTo("field2"),
                        "propertyDefinitions['field1'].annotations[0].type", equalTo("DropDownList"),
                        "propertyDefinitions['field1'].annotations[0].value", containsInAnyOrder("value-1", "value-2"),
                        "propertyDefinitions['field2'].annotations[0].value", containsInAnyOrder("value-3", "value-4"),
                        "i18nResources['field1']", equalTo("Field 1"),
                        "i18nResources['field2']", equalTo("Field 2"),
                        "lockedBy", equalTo("tester"),
                        "editable", equalTo(Boolean.TRUE));


        verify(channelService);
    }

    private Map<String, HstPropertyDefinitionInfo> createPropertyDefinitions() {
        final Map<String, HstPropertyDefinitionInfo> propertyDefinitions = new HashMap<>();

        final Annotation field1Annotation = createDropDownListAnnotation("value-1", "value-2");
        final Annotation field2Annotation = createDropDownListAnnotation("value-3", "value-4");

        propertyDefinitions.put("field1", createHstPropertyDefinitionInfo("field1", HstValueType.BOOLEAN, true, field1Annotation));
        propertyDefinitions.put("field2", createHstPropertyDefinitionInfo("field2", HstValueType.STRING, true, field2Annotation));
        return propertyDefinitions;
    }

    private static HstPropertyDefinitionInfo createHstPropertyDefinitionInfo(final String name,
                                                                             final HstValueType valueType,
                                                                             final boolean required,
                                                                             final Annotation annotation) {
        final HstPropertyDefinitionInfo propertyDefinitionInfo = new HstPropertyDefinitionInfo();
        propertyDefinitionInfo.setName(name);
        propertyDefinitionInfo.setValueType(valueType);
        propertyDefinitionInfo.setIsRequired(required);
        propertyDefinitionInfo.setAnnotations(Arrays.asList(annotation));
        return propertyDefinitionInfo;
    }

    @Test
    public void can_get_channel_info_description_with_default_locale() throws ChannelException, RepositoryException {
        final Map<String, String> i18nResources = new HashMap<>();
        i18nResources.put("field1", "Field 1");
        final List<FieldGroupInfo> fieldGroups = new ArrayList<>();
        fieldGroups.add(new FieldGroupInfo(null, "fieldGroup1"));

        final ChannelInfoDescription channelInfoDescription
                = new ChannelInfoDescription(fieldGroups, createPropertyDefinitions(), i18nResources, null, true);

        expect(channelService.getChannelInfoDescription("channel-foo", "en", "dev-localhost"))
                .andReturn(channelInfoDescription);
        replay(channelService);

        given()
                .header("hostGroup", "dev-localhost")
                .get(MOCK_REST_PATH + "channels/channel-foo/info")
                .then()
                .statusCode(200)
                .body("fieldGroups[0].titleKey", equalTo("fieldGroup1"),
                        "i18nResources", hasEntry("field1", "Field 1"));

        verify(channelService);
    }

    @Test
    public void cannot_get_channelsettings_information_when_sever_has_error() throws ChannelException, RepositoryException {
        expect(channelService.getChannelInfoDescription("channel-foo", "en", "dev-localhost"))
                .andThrow(new ChannelException("unknown error"));
        replay(channelService);


        given()
                .header("hostGroup", "dev-localhost")
                .get(MOCK_REST_PATH + "channels/channel-foo/info?locale=en")
                .then()
                .statusCode(500)
                .body(equalTo("Could not get channel setting information"));

        verify(channelService);
    }

    @Test
    public void can_save_channelsettings() throws ChannelException, RepositoryException {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("foo", "bah");
        final Channel channelFoo = new Channel("channel-foo");
        channelFoo.setProperties(properties);

        channelService.saveChannel(mockSession, "channel-foo", channelFoo, "dev-localhost");
        expectLastCall();
        replay(channelService);


        given()
                .header("hostGroup", "dev-localhost")
                .contentType(JSON)
                .body(channelFoo)
                .when()
                .put(MOCK_REST_PATH + "channels/channel-foo")
                .then()
                .statusCode(200)
                .body("properties.foo", equalTo("bah"));

        verify(channelService);
    }

    @Test
    public void cannot_save_channelsettings_when_server_has_error() throws ChannelException, RepositoryException {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("foo", "bah");
        final Channel channelFoo = new Channel("channel-foo");
        channelFoo.setProperties(properties);

        final Capture<Channel> capturedArgument = EasyMock.newCapture();
        channelService.saveChannel(eq(mockSession), eq("channel-foo"), capture(capturedArgument), eq("dev-localhost"));
        expectLastCall().andThrow(new IllegalStateException("something is wrong"));

        replay(channelService);

        given()
                .header("hostGroup", "dev-localhost")
                .contentType(JSON)
                .body(channelFoo)
                .when()
                .put(MOCK_REST_PATH + "channels/channel-foo")
                .then()
                .statusCode(500);

        verify(channelService);
        assertThat(capturedArgument.getValue().getProperties().get("foo"), equalTo("bah"));
    }

    @Test
    public void cannot_get_non_existent_channel() throws ChannelException {

        final VirtualHosts virtualHosts = createNiceMock(VirtualHosts.class);
        expect(virtualHosts.getChannelById(eq("dev-localhost"), eq("channel-non-existing"))).andStubReturn(null);

        final HstModel hstModel = createNiceMock(HstModel.class);

        final HstModelRegistry hstModelRegistry = createNiceMock(HstModelRegistry.class);
        expect(hstModelRegistry.getHstModel(eq("/site"))).andStubReturn(hstModel);
        expect(hstModel.getVirtualHosts()).andStubReturn(virtualHosts);

        EasyMock.replay(virtualHosts, hstModel, hstModelRegistry);

        try {
            HippoServiceRegistry.register(hstModelRegistry, HstModelRegistry.class);

            given()
                    .header("contextPath", "/site")
                    .header("hostGroup", "dev-localhost")
                    .get(MOCK_REST_PATH + "channels/channel-non-existing")
                    .then()
                    .statusCode(404);

        } finally {
            HippoServiceRegistry.unregister(hstModelRegistry, HstModelRegistry.class);
        }

        EasyMock.verify(virtualHosts, hstModel, hstModelRegistry);
    }

    @Test
    public void delete_channel_will_strip_preview_suffix_of_channelId() throws ChannelException, RepositoryException {

        EasyMock.expect(channelService.getChannel(mockSession, "channel-foi", "dev-localhost")).andThrow(new ChannelNotFoundException("channel-foi"));
        EasyMock.replay(channelService);

       given()
                .header("hostGroup", "dev-localhost")
                .contentType(JSON)
                .when()
                .delete(MOCK_REST_PATH + "channels/channel-foi-preview")
                .then()
                .statusCode(404);

       EasyMock.verify(channelService);
    }

    @Test
    public void deletes_channel_and_publishes_event() throws ChannelException, RepositoryException {
        mock_HstConfigurationUtils_persistChanges(1);
        final List<Mount> mountsOfChannel = Arrays.asList(EasyMock.<Mount>mock(Mount.class));

        final Channel channelFoo = new Channel("channel-foo");

        EasyMock.expect(channelService.getChannel(mockSession,"channel-foo", "dev-localhost")).andReturn(channelFoo);
        EasyMock.expect(channelService.findMounts(channelFoo)).andReturn(mountsOfChannel);

        channelService.preDeleteChannel(mockSession, channelFoo, mountsOfChannel);
        expectLastCall();

        channelService.deleteChannel(mockSession, channelFoo, mountsOfChannel);
        expectLastCall();

        final Capture<BeforeChannelDeleteEvent> capturedEvent = EasyMock.newCapture();
        rootResource.publishSynchronousEvent(and(capture(capturedEvent), EasyMock.isA(BeforeChannelDeleteEvent.class)));
        expectLastCall();

        replay(channelService, rootResource);

        given()
                .header("hostGroup", "dev-localhost")
                .contentType(JSON)
                .when()
                .delete(MOCK_REST_PATH + "channels/channel-foo")
                .then()
                .statusCode(200);

        verify(channelService, rootResource);
        PowerMock.verify(HstConfigurationUtils.class);

        assertThat(capturedEvent.getValue().getChannel().getId(), is("channel-foo"));
        assertThat(capturedEvent.getValue().getRequestContext().getSession(), is(mockSession));
        assertThat(capturedEvent.getValue().getMounts(), containsInAnyOrder(mountsOfChannel.toArray()));
    }

    @Test
    public void throws_exception_to_cancel_channel_delete() throws ChannelException, RepositoryException {
        // Make sure HstConfigurationUtils#persistChanges is not called
        mock_HstConfigurationUtils_persistChanges(0);

        final Mount mockMount = EasyMock.createMock(Mount.class);
        final Channel channelFoo = new Channel("channel-foo");

        EasyMock.expect(channelService.getChannel(mockSession, "channel-foo", "dev-localhost")).andReturn(channelFoo);
        channelService.preDeleteChannel(mockSession, channelFoo, Arrays.asList(mockMount));
        expectLastCall();

        EasyMock.expect(channelService.findMounts(channelFoo)).andReturn(Arrays.asList(mockMount));

        // Allow users to cancel event with a parameterized message
        final Map<String, String> parameterMap = new HashMap<>();
        parameterMap.put("userMessage", "Channel {{channel}} cannot be deleted");
        parameterMap.put("channel", "Foo");
        final ClientException userException = new ClientException("User cancel event", ClientError.UNKNOWN, parameterMap);

        final Capture<BeforeChannelDeleteEvent> capturedEvent = EasyMock.newCapture();
        rootResource.publishSynchronousEvent(and(capture(capturedEvent), EasyMock.isA(BeforeChannelDeleteEvent.class)));
        expectLastCall().andThrow(userException);
        replay(rootResource);

        replay(channelService);

        given()
                .header("hostGroup", "dev-localhost")
                .contentType(JSON)
                .when()
                .delete(MOCK_REST_PATH + "channels/channel-foo")
                .then()
                .statusCode(400)
                .body("error", equalTo("UNKNOWN"),
                        "parameterMap.userMessage", equalTo("Channel {{channel}} cannot be deleted"),
                        "parameterMap.channel", equalTo("Foo"));


        verify(channelService, rootResource);
        PowerMock.verify(HstConfigurationUtils.class);

        assertThat(capturedEvent.getValue().getChannel().getId(), is("channel-foo"));
        assertThat(capturedEvent.getValue().getRequestContext().getSession(), is(mockSession));
        assertThat(capturedEvent.getValue().getMounts(), containsInAnyOrder(mockMount));
    }

    @Test
    public void cannot_delete_non_existent_channel() throws ChannelException, RepositoryException {
        EasyMock.expect(channelService.getChannel(mockSession, "channel-foo", "dev-localhost"))
                .andThrow(new ChannelNotFoundException("channel-foo"));

        replay(channelService);

        given()
                .header("hostGroup", "dev-localhost")
                .contentType(JSON)
                .when()
                .delete(MOCK_REST_PATH + "channels/channel-foo")
                .then()
                .statusCode(404);
        verify(channelService);
    }

    private void mock_HstConfigurationUtils_persistChanges(final int count) throws RepositoryException {
        PowerMock.mockStaticPartial(HstConfigurationUtils.class, "persistChanges");
        if (count >= 1) {
            HstConfigurationUtils.persistChanges(notNull());
            PowerMock.expectLastCall().times(count);
        }
        PowerMock.replay(HstConfigurationUtils.class);
    }

    private static Annotation createDropDownListAnnotation(final String... values) {
        return new DropDownList() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return DropDownList.class;
            }

            @Override
            public String[] value() {
                return values;
            }

            @Override
            public Class<? extends ValueListProvider> valueListProvider() {
                return EmptyValueListProvider.class;
            }

            @Override
            public String valueListProviderKey() {
                return "";
            }
        };
    }
}
