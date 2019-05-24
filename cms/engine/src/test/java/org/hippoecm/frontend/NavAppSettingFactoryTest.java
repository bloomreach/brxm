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

package org.hippoecm.frontend;

import java.net.URI;
import java.util.Locale;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;

import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.util.tester.WicketTester;
import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.hippoecm.frontend.session.PluginUserSession;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@RunWith(EasyMockRunner.class)
public class NavAppSettingFactoryTest extends WicketTester {

    @Mock
    private ServletWebRequest request;
    @Mock
    private HttpServletRequest servletRequest;
    @Mock
    private PluginUserSession userSession;

    private final String scheme = "scheme";
    private final String host = "cms.host.name";

    @Before
    public void setUp() throws Exception {

        expect(request.getContainerRequest()).andReturn(servletRequest).anyTimes();
        replay(request);
        expect(servletRequest.getHeader("X-Forwarded-Proto")).andReturn(scheme);
        expect(servletRequest.getHeader("X-Forwarded-Host")).andReturn(host);
        replay(servletRequest);
        expect(userSession.getUserName()).andReturn("userName");
        expect(userSession.getLocale()).andReturn(Locale.CANADA);
        expect(userSession.getTimeZone()).andReturn(TimeZone.getDefault());
        replay(userSession);
    }

    @Test
    public void navapp_and_brxm_location_same_if_system_property_not_set() {
        final NavAppSettings navAppSettings = NavAppSettingFactory.newInstance(request, userSession);
        assertThat(navAppSettings.getAppSettings().getNavAppLocation(), is(URI.create(scheme + "://" + host)));
        assertThat(navAppSettings.getAppSettings().getBrXmLocation(), is(URI.create(scheme + "://" + host)));
    }


    @Test
    public void navapp_and_brxm_location_different_if_system_property_set() {

        final URI navAppLocation = URI.create("https://www.abc.xy:1010/somewhere-far-away");
        System.setProperty(NavAppSettingFactory.NAVAPP_LOCATION, navAppLocation.toString());

        final NavAppSettings navAppSettings = NavAppSettingFactory.newInstance(request, userSession);
        assertThat(navAppSettings.getAppSettings().getNavAppLocation(), is(navAppLocation));
        assertThat(navAppSettings.getAppSettings().getBrXmLocation(), is(URI.create(scheme + "://" + host)));

        System.getProperties().remove(NavAppSettingFactory.NAVAPP_LOCATION);
    }
}