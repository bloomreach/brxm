/*
 * Copyright 2019 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.frontend.service.navappsettings;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Stream;

import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.Session;
import org.apache.wicket.ThreadContext;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.IRequestParameters;
import org.apache.wicket.util.string.StringValue;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.hippoecm.frontend.filter.NavAppRedirectFilter;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.AppSettings;
import org.hippoecm.frontend.service.INavAppSettingsService;
import org.hippoecm.frontend.service.NavAppResource;
import org.hippoecm.frontend.service.NavAppSettings;
import org.hippoecm.frontend.service.ResourceType;
import org.hippoecm.frontend.service.UserSettings;
import org.hippoecm.frontend.session.PluginUserSession;
import org.hippoecm.repository.api.HippoSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onehippo.repository.security.SessionUser;

import static java.util.stream.Collectors.toSet;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hippoecm.frontend.service.navappsettings.NavAppSettingsService.LOGIN_RESOURCES;
import static org.hippoecm.frontend.service.navappsettings.NavAppSettingsService.LOGOUT_RESOURCES;
import static org.hippoecm.frontend.service.navappsettings.NavAppSettingsService.NAVIGATIONITEMS_ENDPOINT;
import static org.hippoecm.frontend.service.navappsettings.NavAppSettingsService.NAV_CONFIG_RESOURCES;
import static org.hippoecm.frontend.service.navappsettings.NavAppSettingsService.RESOURCE_TYPE;
import static org.hippoecm.frontend.service.navappsettings.NavAppSettingsService.RESOURCE_URL;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(EasyMockRunner.class)
public class NavAppSettingsServiceTest {

    @Mock
    private ServletWebRequest request;
    @Mock
    private IRequestParameters parameters;
    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private PluginUserSession userSession;
    @Mock
    private IPluginContext context;
    @Mock
    private IPluginConfig config, cfg1, cfg2;

    private final String scheme = "scheme";
    private final String host = "cms.host.name";
    private final String contextPath = "/context-path";

    private INavAppSettingsService navAppSettingsService;

    @Mock
    private WebApplication webApplication;
    @Mock
    private HippoSession hippoSession;
    @Mock
    private SessionUser user;
    private SessionAttributeStore sessionAttributeStore;

    @Before
    public void setUp() throws RepositoryException {

        expect(request.getContainerRequest()).andReturn(servletRequest).anyTimes();
        expect(request.getQueryParameters()).andStubReturn(parameters);
        replay(request);
        expect(parameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.UUID_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.PATH_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOGIN_TYPE_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        replay(parameters);

        expect(servletRequest.getHeader("X-Forwarded-Proto")).andReturn(scheme);
        expect(servletRequest.getHeader("X-Forwarded-Host")).andReturn(host).times(2);
        replay(servletRequest);

        expect(userSession.getJcrSession()).andReturn(hippoSession);
        expect(userSession.getUserName()).andReturn("userName");
        expect(userSession.getLocale()).andReturn(Locale.CANADA);
        expect(userSession.getTimeZone()).andReturn(TimeZone.getDefault());
        replay(userSession);


        expect(hippoSession.getUser()).andReturn(user);
        replay(hippoSession);

        expect(user.getEmail()).andReturn("email");
        expect(user.getFirstName()).andReturn("firstname");
        expect(user.getLastName()).andReturn("lastname");
        replay(user);

        expect(config.getString(INavAppSettingsService.SERVICE_ID, INavAppSettingsService.SERVICE_ID)).andReturn(null);
        expect(config.containsKey(NAV_CONFIG_RESOURCES)).andStubReturn(true);
        expect(config.getPluginConfig(NAV_CONFIG_RESOURCES)).andReturn(config);
        expect(config.getPluginConfigSet()).andReturn(Collections.emptySet());
        expect(config.containsKey(LOGIN_RESOURCES)).andStubReturn(false);
        expect(config.containsKey(LOGOUT_RESOURCES)).andStubReturn(false);
        expect(config.getInt(NavAppSettingsService.IFRAMES_CONNECTION_TIMEOUT, 30_000)).andReturn(10_000);
        replay(config);

        sessionAttributeStore =new SessionAttributeStore(){

            final private Map<String,Serializable> attributes = new HashMap<>();
            @Override
            public Serializable getAttribute(final String name) {
                return attributes.get(name);
            }

            @Override
            public Session setAttribute(final String name, final Serializable value) {
                attributes.put(name, value);
                return null;
            }
        };
        this.navAppSettingsService = new NavAppSettingsService(context, config, () -> userSession, sessionAttributeStore);

        ThreadContext.setApplication(webApplication);
    }

    @After
    public void tearDown() {
        ThreadContext.setApplication(null);
    }

    @Test
    public void cms_serves_navapp_resources_if_system_property_not_set() {

        reset(servletRequest);
        expect(servletRequest.getHeader("X-Forwarded-Proto")).andReturn(scheme);
        expect(servletRequest.getHeader("X-Forwarded-Host")).andReturn(null).times(2);
        expect(servletRequest.getHeader("Host")).andReturn(host).times(2);
        expect(servletRequest.getContextPath()).andReturn("/context-path");
        replay(servletRequest);

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getAppSettings().isCmsServingNavAppResources(), is(true));
        assertThat(navAppSettings.getAppSettings().getNavAppResourceLocation(), is(URI.create("navapp")));

        testUserSettingsAssertions(navAppSettings.getUserSettings());
        testAppSettingsAssertions(navAppSettings.getAppSettings());
    }


    @Test
    public void cdn_serves_navapp_resources_if_system_property_set_to_external_cdn() {

        reset(servletRequest);
        expect(servletRequest.getHeader("X-Forwarded-Proto")).andReturn(scheme);
        expect(servletRequest.getHeader("X-Forwarded-Host")).andReturn(null).times(2);
        expect(servletRequest.getHeader("Host")).andReturn(host).times(2);
        expect(servletRequest.getContextPath()).andReturn("/context-path");
        replay(servletRequest);

        final URI navAppLocation = URI.create("https://www.abc.xy:1010/somewhere-far-away");
        System.setProperty(NavAppSettingsService.NAVAPP_LOCATION_SYSTEM_PROPERTY, navAppLocation.toString());

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getAppSettings().getNavAppResourceLocation(), is(navAppLocation));
        assertThat(navAppSettings.getAppSettings().isCmsServingNavAppResources(), is(false));

        testUserSettingsAssertions(navAppSettings.getUserSettings());
        testAppSettingsAssertions(navAppSettings.getAppSettings());

        System.getProperties().remove(NavAppSettingsService.NAVAPP_LOCATION_SYSTEM_PROPERTY);
    }

    @Test
    public void loads_extra_navcfg_resources_from_config() {

        reset(servletRequest);
        expect(servletRequest.getHeader("X-Forwarded-Proto")).andReturn(scheme);
        expect(servletRequest.getHeader("X-Forwarded-Host")).andReturn(null).times(2);
        expect(servletRequest.getHeader("Host")).andReturn(host).times(2);
        expect(servletRequest.getContextPath()).andReturn("/context-path");
        replay(servletRequest);

        reset(config);
        expect(config.containsKey(NAV_CONFIG_RESOURCES)).andStubReturn(true);
        expect(config.getPluginConfig(NAV_CONFIG_RESOURCES)).andReturn(config);
        expect(config.getPluginConfigSet()).andReturn(Stream.of(cfg1, cfg2).collect(toSet()));
        expect(config.containsKey(LOGIN_RESOURCES)).andStubReturn(false);
        expect(config.containsKey(LOGOUT_RESOURCES)).andStubReturn(false);
        expect(config.getInt(NavAppSettingsService.IFRAMES_CONNECTION_TIMEOUT, 30_000)).andReturn(10_000);
        expect(cfg1.getString(NavAppSettingsService.RESOURCE_TYPE)).andReturn(ResourceType.IFRAME.name());
        expect(cfg1.getString(NavAppSettingsService.RESOURCE_URL)).andReturn("some-other-url1");
        expect(cfg2.getString(NavAppSettingsService.RESOURCE_TYPE)).andReturn(ResourceType.REST.name());
        expect(cfg2.getString(NavAppSettingsService.RESOURCE_URL)).andReturn("some-other-url2");
        replay(config, cfg1, cfg2);

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        final List<NavAppResource> navConfigResources = navAppSettings.getAppSettings().getNavConfigResources();
        assertThat(navConfigResources.size(), is(3));
        testAppSettingsAssertions(navAppSettings.getAppSettings());

        final NavAppResource iframeResource = navConfigResources.stream().filter(r -> ResourceType.IFRAME == r.getResourceType()).findFirst().orElseThrow(() -> new RuntimeException("IFRAME resource not found"));
        assertThat(iframeResource.getUrl(), is(URI.create("some-other-url1")));

        verify(config, cfg1, cfg2);
    }


    @Test
    public void throws_on_invalid_navcfg_resource_uri() {

        reset(config);
        expect(config.containsKey(NAV_CONFIG_RESOURCES)).andStubReturn(true);
        expect(config.getPluginConfig(NAV_CONFIG_RESOURCES)).andReturn(config);
        expect(config.getPluginConfigSet()).andReturn(Stream.of(cfg1).collect(toSet()));
        expect(cfg1.getString(NavAppSettingsService.RESOURCE_URL)).andReturn("invalid resource url");
        replay(config, cfg1);

        try {
            navAppSettingsService.getNavAppSettings(request);
            fail("should have thrown an exception because the resource url is invalid");
        } catch (IllegalArgumentException e) {
            assertThat(e.getCause(), instanceOf(URISyntaxException.class));
        }
        verify(config, cfg1);
    }


    @Test
    public void throws_on_empty_navcfg_resource_uri() {

        reset(config);
        expect(config.containsKey(NAV_CONFIG_RESOURCES)).andStubReturn(true);
        expect(config.getPluginConfig(NAV_CONFIG_RESOURCES)).andReturn(config);
        expect(config.getPluginConfigSet()).andReturn(Stream.of(cfg1).collect(toSet()));
        expect(cfg1.getName()).andStubReturn("cfg1");
        expect(cfg1.getString(NavAppSettingsService.RESOURCE_URL)).andReturn("\t\t   \t\t");
        replay(config, cfg1);

        try {
            navAppSettingsService.getNavAppSettings(request);
            fail("should have thrown an exception because the resource url is invalid");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is(NavAppSettingsService.RESOURCE_URL + " must not be empty or null"));
        }
        verify(config, cfg1);
    }

    @Test
    public void is_resilient_to_RepositoryExceptions() throws RepositoryException {
        reset(hippoSession);
        expect(hippoSession.getUser()).andThrow(new ItemNotFoundException("can always happen"));
        replay(hippoSession);

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getUserSettings().getEmail(), is(nullValue()));

        verify(hippoSession);
    }

    @Test
    public void load_login_and_logout_resources() {

        reset(config);
        expect(config.containsKey(NAV_CONFIG_RESOURCES)).andReturn(false);

        expect(config.containsKey(LOGIN_RESOURCES)).andReturn(true);
        expect(config.getPluginConfig(LOGIN_RESOURCES)).andReturn(config);

        expect(config.containsKey(LOGOUT_RESOURCES)).andReturn(true);
        expect(config.getPluginConfig(LOGOUT_RESOURCES)).andReturn(config);
        expect(config.getInt(NavAppSettingsService.IFRAMES_CONNECTION_TIMEOUT, 30_000)).andReturn(10_000);

        expect(config.getPluginConfigSet())
                .andReturn(Stream.of(cfg1).collect(toSet()))
                .andReturn(Stream.of(cfg2).collect(toSet()));
        expect(cfg1.getString(RESOURCE_TYPE))
                .andReturn(ResourceType.IFRAME.name());
        expect(cfg1.getString(RESOURCE_URL))
                .andReturn("http://a.b/login");
        expect(cfg2.getString(RESOURCE_TYPE))
                .andReturn(ResourceType.REST.name());
        expect(cfg2.getString(RESOURCE_URL))
                .andReturn("http://p.q/logout");
        replay(config, cfg1, cfg2);

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);

        final List<NavAppResource> loginResources = navAppSettings.getAppSettings().getLoginResources();
        assertThat(loginResources.size(), is(1));
        assertThat(loginResources.get(0).getResourceType(), is(ResourceType.IFRAME));

        final List<NavAppResource> logoutResources = navAppSettings.getAppSettings().getLogoutResources();
        assertThat(loginResources.size(), is(1));
        assertThat(logoutResources.get(0).getResourceType(), is(ResourceType.REST));

        verify(config, cfg1, cfg2);
    }

    @Test
    public void initial_path_is_set() {
        reset(parameters);
        final String someInitialPath = "a/b/c?x=y&p=q";
        expect(parameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER))
                .andReturn(StringValue.valueOf(someInitialPath));
        expect(parameters.getParameterValue(NavAppSettingsService.UUID_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.PATH_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOGIN_TYPE_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        replay(parameters);
        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getAppSettings().getInitialPath(), is(someInitialPath));
    }

    @Test
    public void uuid_parameter_is_set() {
        reset(parameters);
        final String someUUID = "{mock-uuid}";
        expect(parameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.UUID_PARAM))
                .andReturn(StringValue.valueOf(someUUID));
        expect(parameters.getParameterValue(NavAppSettingsService.PATH_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOGIN_TYPE_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        replay(parameters);
        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getAppSettings().getInitialPath(), is("/content/uuid/" + someUUID));
    }

    @Test
    public void path_parameter_is_set() {
        reset(parameters);
        final String somePath = "/path/to/document";
        expect(parameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.UUID_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.PATH_PARAM))
                .andReturn(StringValue.valueOf(somePath));
        expect(parameters.getParameterValue(NavAppSettingsService.LOGIN_TYPE_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        replay(parameters);
        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getAppSettings().getInitialPath(), is("/content/path/" + somePath));
    }

    @Test
    public void loads_no_resources_for_logintype_local() {

        reset(config);
        expect(config.getInt(NavAppSettingsService.IFRAMES_CONNECTION_TIMEOUT, 30_000)).andReturn(10_000);
        replay(config);

        reset(parameters);
        expect(parameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.UUID_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.PATH_PARAM))
                .andReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOGIN_TYPE_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf("local"));
        replay(parameters);

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getAppSettings().getNavConfigResources().size(), is(1));
        verify(config, parameters);
    }

    @Test
    public void loads_no_resources_for_logintype_local_reload() {

        reset(config);
        expect(config.getInt(NavAppSettingsService.IFRAMES_CONNECTION_TIMEOUT, 30_000)).andReturn(10_000);
        replay(config);


        reset(parameters);
        expect(parameters.getParameterValue(NavAppRedirectFilter.INITIAL_PATH_QUERY_PARAMETER))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.UUID_PARAM))
                .andStubReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.PATH_PARAM))
                .andReturn(StringValue.valueOf((String) null));
        expect(parameters.getParameterValue(NavAppSettingsService.LOGIN_TYPE_QUERY_PARAMETER))
                .andReturn(StringValue.valueOf((String) null));
        replay(parameters);
        sessionAttributeStore.setAttribute(NavAppSettingsService.LOGIN_LOGIN_USER_SESSION_ATTRIBUTE_NAME,true);

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getAppSettings().getNavConfigResources().size(), is(1));
        verify(config, parameters);
    }

    private void testUserSettingsAssertions(UserSettings userSettings) {
        assertThat(userSettings.getUserName(), is("firstname lastname"));
        assertThat(userSettings.getLanguage(), is(Locale.CANADA.getLanguage()));
        assertThat(userSettings.getTimeZone(), is(TimeZone.getDefault()));
        assertThat(userSettings.getEmail(), is("email"));
    }

    private void testAppSettingsAssertions(AppSettings appSettings) {
        // First resource must always be present
        final List<NavAppResource> navConfigResources = appSettings.getNavConfigResources();
        assertThat(navConfigResources.get(0).getResourceType(), is(ResourceType.INTERNAL_REST));
        assertThat(navConfigResources.get(0).getUrl(), is(URI.create(NAVIGATIONITEMS_ENDPOINT)));
        assertThat(appSettings.getInitialPath(), is("/"));
    }

}
