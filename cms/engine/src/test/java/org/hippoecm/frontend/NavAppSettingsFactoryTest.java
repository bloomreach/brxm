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

import java.io.IOException;
import java.util.Locale;
import java.util.TimeZone;

import org.easymock.EasyMockRunner;
import org.easymock.Mock;
import org.hippoecm.frontend.session.PluginUserSession;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(EasyMockRunner.class)
public class NavAppSettingsFactoryTest {

    private final NavAppSettingsFactory factory = new NavAppSettingsFactory();

    @Mock
    private PluginUserSession userSession;

    @Test
    public void testGenerate() {

        final String name = "Frank Zappa";
        expect(userSession.getUserName()).andReturn(name);

        final Locale locale = Locale.CANADA;
        expect(userSession.getLocale()).andReturn(locale);

        final String id = "Europe/Amsterdam";
        expect(userSession.getTimeZone()).andReturn(TimeZone.getTimeZone(id));
        replay(userSession);

        final NavAppSettings result = factory.getNavAppSettings(userSession);
        assertThat(result.getUserSettings().getUserName(), is(name));
        assertThat(result.getUserSettings().getLanguage(), is(locale.getLanguage()));
        assertThat(result.getUserSettings().getTimeZone(), is(TimeZone.getTimeZone(id)));

        assertThat(result.getAppSettings().getNavConfigResources().size(), is(1));
        assertThat(result.getAppSettings().getNavConfigResources().get(0).getResourceType(), is(NavAppSettings.ResourceType.REST));
        assertThat(result.getAppSettings().getNavConfigResources().get(0).getUrl(), is(nullValue()));
    }

}