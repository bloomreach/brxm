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
import java.util.Properties;
import java.util.Set;

import org.hippoecm.frontend.service.NavAppResource;
import org.hippoecm.frontend.service.ResourceType;
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

    @Test
    public void no_resources_at_all_is_allowed() {
        final Properties properties = new Properties();
        final NavAppResourceService service = new NavAppResourceServiceImpl(properties);
        assertThat(service.getNavigationItemsResources().isEmpty(), is(true));
        assertThat(service.getLoginResources().isEmpty(), is(true));
        assertThat(service.getLogoutResources().isEmpty(), is(true));
    }

    @Test
    public void resources_from_properties() throws IOException {
        final String data = "# Sample platform.properties\n"
                + "brx.navapp.resource.login.id-1  = IFRAME, https://company-1.com/login\n"
                + "brx.navapp.resource.login.id-2  = REST, https://company-2.com/login\n"
                + "brx.navapp.resource.login.id-3  = INTERNAL_REST, https://company-3.com/login\n"
                + "brx.navapp.resource.logout.id-1 = IFRAME, https://company-1.com/logout\n"
                + "brx.navapp.resource.logout.id-2 = IFRAME, https://company-2.com/logout\n"
                + "brx.navapp.resource.logout.id-3 = IFRAME, https://company-3.com/logout\n"
                + "brx.navapp.resource.navigationitems.id-1  = IFRAME, https://company-1.com/nav-items\n"
                + "brx.navapp.resource.navigationitems.id-2  = REST, https://company-2.com/mav-items\n"
                + "brx.navapp.resource.navigationitems.id-3  = INTERNAL_REST, https://company-3.com/nav-items\n"
                + "brx.navapp.some.other.property  = some value\n"
                + "brx.navapp.resource.LOGIN.id-3  = properties are case sensitive, so this one doesn't count\n"
                + "x.y.z = just some other property\n"
                + "p.q.r = yet another one\n";
        final Properties properties = new Properties();
        properties.load(new StringReader(data));

        final NavAppResourceService service = new NavAppResourceServiceImpl(properties);

        assertThat(service.getLoginResources().size(), is(3));
        assertThat(service.getLogoutResources().size(), is(3));
        assertThat(service.getNavigationItemsResources().size(), is(3));
    }

    @Test
    public void navigationitems_without_login_logout_pair_is_allowed() {

        final Properties properties = new Properties();
        properties.setProperty(NAVIGATION_ITEMS_KEY_PREFIX + ".company-c", "INTERNAL_REST, http://company-c.com/nav-items");

        final NavAppResourceService service = new NavAppResourceServiceImpl(properties);
        final Set<NavAppResource> loginResources = service.getNavigationItemsResources();

        assertThat(loginResources.iterator().next().getResourceType(), is(ResourceType.INTERNAL_REST));
        assertThat(loginResources.iterator().next().getUrl(), is(URI.create("http://company-c.com/nav-items")));
    }

    @Test
    public void login_without_logout_not_allowed() throws IOException {
        final String data = "# Sample platform.properties\n"
                + "brx.navapp.resource.navigationitems.id-1  = IFRAME, https://company-1.com/nav-items\n"
                + "brx.navapp.resource.login.id-1  = IFRAME, https://company-1.com/login\n";
        final Properties properties = new Properties();
        properties.load(new StringReader(data));

        try {
            new NavAppResourceServiceImpl(properties);
            fail("should have thrown");
        } catch (IllegalArgumentException e) {
            System.err.println(e);
            assertThat(e.getMessage(), allOf(
                    containsString("brx.navapp.resource.logout.id-1"),
                    containsString("missing")
            ));
        }
    }

    @Test
    public void logout_without_login_not_allowed() throws IOException {
        final String data = "# Sample platform.properties\n"
                + "brx.navapp.resource.navigationitems.id-1  = IFRAME, https://company-1.com/nav-items\n"
                + "brx.navapp.resource.logout.id-1  = IFRAME, https://company-1.com/logout\n";
        final Properties properties = new Properties();
        properties.load(new StringReader(data));

        try {
            new NavAppResourceServiceImpl(properties);
            fail("should have thrown");
        } catch (IllegalArgumentException e) {
            System.err.println(e);
            assertThat(e.getMessage(), allOf(
                    containsString("brx.navapp.resource.login.id-1"),
                    containsString("missing")
            ));
        }
    }

    @Test
    public void login_logout_without_navigationitems_not_allowed() {

        final Properties properties = new Properties();
        properties.setProperty(LOGIN_KEY_PREFIX + ".company-a", "IFRAME, http://company-a.com/login");
        properties.setProperty(LOGOUT_KEY_PREFIX + ".company-a", "REST, http://company-b.com/logout");

        try {
            new NavAppResourceServiceImpl(properties);
            fail("should have thrown");
        } catch (IllegalArgumentException e) {
            System.err.println(e);
            assertThat(e.getMessage(), allOf(
                    containsString("brx.navapp.resource.navigationitems.company-a"),
                    containsString("missing")
            ));
        }
    }

    @Test
    public void values_wrong_order_not_allowed() {

        final Properties properties = new Properties();
        properties.setProperty(NAVIGATION_ITEMS_KEY_PREFIX + ".company-a", "http://company-c.com/nav-items, IFRAME");

        try {
            new NavAppResourceServiceImpl(properties);
            fail("Should have thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), allOf(
                    containsString(NAVIGATION_ITEMS_KEY_PREFIX),
                    containsString("company-a"))
            );
        }
    }

    @Test
    public void missing_resource_type_not_allowed() {

        final Properties properties = new Properties();
        properties.setProperty(NAVIGATION_ITEMS_KEY_PREFIX + ".company-a", "http://company-c.com/nav-items");

        try {
            new NavAppResourceServiceImpl(properties);
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
    public void missing_url_not_allowed() {

        final Properties properties = new Properties();
        properties.setProperty(NAVIGATION_ITEMS_KEY_PREFIX + ".company-a", "REST");

        try {
            new NavAppResourceServiceImpl(properties);
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
    public void properties_reversed_not_allowed() {

        final Properties properties = new Properties();
        properties.setProperty(NAVIGATION_ITEMS_KEY_PREFIX + ".company-a", "http://company-a.com/navitems, REST");

        try {
            new NavAppResourceServiceImpl(properties);
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
    public void duplicate_values_for_different_keys_not_allowed() throws IOException {
        final String data = "# Sample platform.properties\n"
                + "brx.navapp.resource.login.id-1  = IFRAME, https://company-1.com/login\n"
                + "brx.navapp.resource.login.id-2  = IFRAME, https://company-1.com/login\n";
        final Properties properties = new Properties();
        properties.load(new StringReader(data));

        try {
            new NavAppResourceServiceImpl(properties);
            fail("should have thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), allOf(
                    containsString("IFRAME, https://company-1.com/login"),
                    containsString("brx.navapp.resource.login.id-1"),
                    containsString("brx.navapp.resource.login.id-2")
            ));
        }
    }

}
