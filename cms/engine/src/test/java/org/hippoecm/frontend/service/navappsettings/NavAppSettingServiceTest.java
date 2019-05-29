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

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.util.tester.WicketTester;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.AppSettings;
import org.hippoecm.frontend.service.INavAppSettingsService;
import org.hippoecm.frontend.service.NavAppSettings;
import org.hippoecm.frontend.service.NavConfigResource;
import org.hippoecm.frontend.service.ResourceType;
import org.hippoecm.frontend.service.UserSettings;
import org.hippoecm.frontend.session.PluginUserSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hippoecm.frontend.service.navappsettings.NavAppSettingsService.NAVIGATIONITEMS_ENDPOINT;
import static org.junit.Assert.assertThat;

@RunWith(EasyMockRunner.class)
public class NavAppSettingServiceTest extends WicketTester {

    @Mock
    private ServletWebRequest request;
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

    private INavAppSettingsService navAppSettingsService;

    @Before
    public void setUp() {

        expect(request.getContainerRequest()).andReturn(servletRequest).anyTimes();
        replay(request);

        expect(servletRequest.getHeader("X-Forwarded-Proto")).andReturn(scheme);
        expect(servletRequest.getHeader("X-Forwarded-Host")).andReturn(host);
        replay(servletRequest);

        expect(userSession.getUserName()).andReturn("userName");
        expect(userSession.getLocale()).andReturn(Locale.CANADA);
        expect(userSession.getTimeZone()).andReturn(TimeZone.getDefault());
        replay(userSession);

        expect(config.getString(INavAppSettingsService.SERVICE_ID, INavAppSettingsService.SERVICE_ID)).andReturn(null);
        expect(config.getPluginConfig(NavAppSettingsService.NAV_CONFIG_RESOURCES)).andReturn(config);
        expect(config.getPluginConfigSet()).andReturn(Collections.emptySet());
        replay(config);

        this.navAppSettingsService = new NavAppSettingsService(context, config, () -> userSession);


    }

    @Test
    public void navapp_and_brxm_location_same_if_system_property_not_set() {

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getAppSettings().getNavAppLocation(), is(URI.create(scheme + "://" + host)));
        assertThat(navAppSettings.getAppSettings().getBrXmLocation(), is(URI.create(scheme + "://" + host)));

        testUserSettingsAssertions(navAppSettings.getUserSettings());
        testAppSettingsAssertions(navAppSettings.getAppSettings());
    }


    @Test
    public void navapp_and_brxm_location_different_if_system_property_set() {

        final URI navAppLocation = URI.create("https://www.abc.xy:1010/somewhere-far-away");
        System.setProperty(NavAppSettingsService.NAVAPP_LOCATION_SYSTEM_PROPERTY, navAppLocation.toString());

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        assertThat(navAppSettings.getAppSettings().getNavAppLocation(), is(navAppLocation));
        assertThat(navAppSettings.getAppSettings().getBrXmLocation(), is(URI.create(scheme + "://" + host)));

        testUserSettingsAssertions(navAppSettings.getUserSettings());
        testAppSettingsAssertions(navAppSettings.getAppSettings());

        System.getProperties().remove(NavAppSettingsService.NAVAPP_LOCATION_SYSTEM_PROPERTY);
    }

    @Test
    public void loads_extra_navcfg_resources_from_config() {

        reset(config);
        expect(config.getString(INavAppSettingsService.SERVICE_ID, INavAppSettingsService.SERVICE_ID)).andReturn(null);
        expect(config.getPluginConfig(NavAppSettingsService.NAV_CONFIG_RESOURCES)).andReturn(config);
        expect(config.getPluginConfigSet()).andReturn(Stream.of(cfg1, cfg2).collect(Collectors.toSet()));
        expect(cfg1.getString(NavAppSettingsService.RESOURCE_TYPE)).andReturn(ResourceType.IFRAME.name());
        expect(cfg1.getString(NavAppSettingsService.RESOURCE_URL)).andReturn("some-other-url1");
        expect(cfg2.getString(NavAppSettingsService.RESOURCE_TYPE)).andReturn(ResourceType.REST.name());
        expect(cfg2.getString(NavAppSettingsService.RESOURCE_URL)).andReturn("some-other-url2");
        replay(config, cfg1, cfg2);

        final NavAppSettings navAppSettings = navAppSettingsService.getNavAppSettings(request);
        final List<NavConfigResource> navConfigResources = navAppSettings.getAppSettings().getNavConfigResources();
        assertThat(navConfigResources.size(), is(3));
        testAppSettingsAssertions(navAppSettings.getAppSettings());

        final NavConfigResource iframeResource = navConfigResources.stream().filter(r -> ResourceType.IFRAME == r.getResourceType()).findFirst().orElseThrow(() -> new RuntimeException("IFRAME resource not found"));
        assertThat(iframeResource.getUrl(), is("some-other-url1/?parent=" + navAppSettings.getAppSettings().getBrXmLocation()));
    }


    private void testUserSettingsAssertions(UserSettings userSettings) {
        assertThat(userSettings.getUserName(), is("userName"));
        assertThat(userSettings.getLanguage(), is(Locale.CANADA.getLanguage()));
        assertThat(userSettings.getTimeZone(), is(TimeZone.getDefault()));
        assertThat(userSettings.getEmail(), is(nullValue()));
    }

    private void testAppSettingsAssertions(AppSettings appSettings) {
        // I would like to mock the servlet context, but I don't see an easy way to do that with the WicketTester
        assertThat(appSettings.getContextPath(), is(""));

        final List<NavConfigResource> navConfigResources = appSettings.getNavConfigResources();
        assertThat(navConfigResources.get(0).getResourceType(), is(ResourceType.REST));
        assertThat(navConfigResources.get(0).getUrl(), is(scheme + "://" + host + NAVIGATIONITEMS_ENDPOINT));
    }

}