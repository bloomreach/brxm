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

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.util.List;
import java.util.Properties;

import org.hippoecm.frontend.service.NavAppResource;
import org.hippoecm.frontend.service.ResourceType;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.frontend.service.navappsettings.NavAppResourceServiceImpl.LOGIN_KEY_PREFIX;
import static org.hippoecm.frontend.service.navappsettings.NavAppResourceServiceImpl.LOGOUT_KEY_PREFIX;
import static org.hippoecm.frontend.service.navappsettings.NavAppResourceServiceImpl.NAVIGATION_ITEMS_KEY_PREFIX;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class NavAppResourceServiceImplTest {

    @Before
    public void setUp() {
    }

    @Test
    public void allEmpty() {
        final Properties properties = new Properties();
        final NavAppResourceService service = new NavAppResourceServiceImpl(properties);
        assertThat(service.getNavigationItemsResources().isEmpty(), is(true));
        assertThat(service.getLoginResources().isEmpty(), is(true));
        assertThat(service.getLogoutResources().isEmpty(), is(true));
    }

    @Test
    public void an_IFRAME_LoginResource() {

        final Properties properties = new Properties();
        properties.setProperty(LOGIN_KEY_PREFIX + ".company-a.", "IFRAME, http://company-a.com/nav-items");

        final NavAppResourceService service = new NavAppResourceServiceImpl(properties);
        final List<NavAppResource> loginResources = service.getLoginResources();

        assertThat(loginResources.get(0).getResourceType(), is(ResourceType.IFRAME));
        assertThat(loginResources.get(0).getUrl(), is(URI.create("http://company-a.com/nav-items")));
    }

    @Test
    public void a_REST_LogoutResource() {

        final Properties properties = new Properties();
        properties.setProperty(LOGOUT_KEY_PREFIX + ".company-b", "REST, http://company-b.com/nav-items");

        final NavAppResourceService service = new NavAppResourceServiceImpl(properties);
        final List<NavAppResource> loginResources = service.getLogoutResources();

        assertThat(loginResources.get(0).getResourceType(), is(ResourceType.REST));
        assertThat(loginResources.get(0).getUrl(), is(URI.create("http://company-b.com/nav-items")));
    }

    @Test
    public void an_INTERNAL_REST_NavigationItemsResource() {

        final Properties properties = new Properties();
        properties.setProperty(NAVIGATION_ITEMS_KEY_PREFIX + ".company-c", "INTERNAL_REST, http://company-c.com/nav-items");

        final NavAppResourceService service = new NavAppResourceServiceImpl(properties);
        final List<NavAppResource> loginResources = service.getNavigationItemsResources();

        assertThat(loginResources.get(0).getResourceType(), is(ResourceType.INTERNAL_REST));
        assertThat(loginResources.get(0).getUrl(), is(URI.create("http://company-c.com/nav-items")));
    }

    @Test
    public void values_wrong_order() {

        final Properties properties = new Properties();
        properties.setProperty(NAVIGATION_ITEMS_KEY_PREFIX + ".company-a", "http://company-c.com/nav-items, IFRAME");

        final NavAppResourceService service = new NavAppResourceServiceImpl(properties);
        try {
            service.getNavigationItemsResources();
            fail("Should have thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), allOf(
                    containsString(NAVIGATION_ITEMS_KEY_PREFIX),
                    containsString("company-a"))
            );
        }
    }

    @Test
    public void missing_ResourceType_Property() {

        final Properties properties = new Properties();
        properties.setProperty(NAVIGATION_ITEMS_KEY_PREFIX + ".company-a", "http://company-c.com/nav-items");

        final NavAppResourceService service = new NavAppResourceServiceImpl(properties);
        try {
            service.getNavigationItemsResources();
            fail("should have thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), allOf(
                    containsString(NAVIGATION_ITEMS_KEY_PREFIX),
                    containsString("http://company-c.com/nav-items"),
                    containsString("company-a"))
            );
        }
    }

    @Test
    public void missing_URL_Property() {

        final Properties properties = new Properties();
        properties.setProperty(NAVIGATION_ITEMS_KEY_PREFIX + ".company-a", "REST");

        final NavAppResourceService service = new NavAppResourceServiceImpl(properties);
        try {
            service.getNavigationItemsResources();
            fail("should have thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), allOf(
                    containsString(NAVIGATION_ITEMS_KEY_PREFIX),
                    containsString("REST"),
                    containsString("company-a"))
            );
        }
    }

    @Test
    public void properties_reversed() {

        final Properties properties = new Properties();
        properties.setProperty(NAVIGATION_ITEMS_KEY_PREFIX + ".company-a", "http://company-a.com/navitems, REST");

        final NavAppResourceService service = new NavAppResourceServiceImpl(properties);
        try {
            service.getNavigationItemsResources();
            fail("should have thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), allOf(
                    containsString(NAVIGATION_ITEMS_KEY_PREFIX),
                    containsString("REST"),
                    containsString("company-a"))
            );
        }
    }

    @Test
    public void fromPropertiesFile() throws IOException {
        final String data = "# Sample platform.properties\n"
                + "brx.navapp.resource.login.id-1  = IFRAME, https://company-1.com/login\n"
                + "brx.navapp.resource.login.id-2  = REST, https://company-2.com/login\n"
                + "brx.navapp.resource.login.id-3  = INTERNAL_REST, https://company-3.com/login\n"
                + "brx.navapp.resource.logout.id-1 = IFRAME, https://company-1.com/logout\n"
                + "brx.navapp.resource.logout.id-2 = IFRAME, https://company-2.com/logout\n"
                + "brx.navapp.some.other.property  = some value\n"
                + "brx.navapp.resource.LOGIN.id-3  = properties are case sensitive, so this one doesn't count\n"
                + "x.y.z = just some other property\n"
                + "p.q.r = yet another one\n"
                ;
        final Properties properties = new Properties();
        properties.load(new StringReader(data));

        final NavAppResourceService service = new NavAppResourceServiceImpl(properties);

        assertThat(service.getLoginResources().size(), is(3));
        assertThat(service.getLogoutResources().size(), is(2));
    }
}
