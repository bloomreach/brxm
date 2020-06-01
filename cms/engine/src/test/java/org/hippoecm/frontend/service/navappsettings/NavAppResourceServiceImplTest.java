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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.hippoecm.frontend.service.NavAppResource;
import org.hippoecm.frontend.service.ResourceType;
import org.hippoecm.hst.core.container.ContainerConfiguration;
import org.junit.Test;

import static org.apache.commons.configuration.ConfigurationConverter.getConfiguration;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hippoecm.frontend.service.navappsettings.NavAppResourceServiceImpl.LOGIN_KEY_PREFIX;
import static org.hippoecm.frontend.service.navappsettings.NavAppResourceServiceImpl.LOGOUT_KEY_PREFIX;
import static org.hippoecm.frontend.service.navappsettings.NavAppResourceServiceImpl.NAVIGATION_ITEMS_KEY_PREFIX;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class NavAppResourceServiceImplTest {

    private final Properties properties = new Properties();
    private final ContainerConfiguration containerConfiguration = fromConfiguration(getConfiguration(properties));

    @Test
    public void no_resources_at_all_is_allowed() {
        final NavAppResourceService service = new NavAppResourceServiceImpl(containerConfiguration);
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
        properties.load(new StringReader(data));

        final NavAppResourceService service = new NavAppResourceServiceImpl(containerConfiguration);

        assertThat(service.getLoginResources().size(), is(3));
        assertThat(service.getLogoutResources().size(), is(3));
        assertThat(service.getNavigationItemsResources().size(), is(3));
        assertThat(service.getNavAppResourceUrl(), is(Optional.empty()));
    }

    @Test
    public void navigationitems_without_login_logout_pair_is_allowed() {

        properties.setProperty(NAVIGATION_ITEMS_KEY_PREFIX + ".company-c", "INTERNAL_REST, http://company-c.com/nav-items");

        final NavAppResourceService service = new NavAppResourceServiceImpl(containerConfiguration);
        final Set<NavAppResource> loginResources = service.getNavigationItemsResources();

        assertThat(loginResources.iterator().next().getResourceType(), is(ResourceType.INTERNAL_REST));
        assertThat(loginResources.iterator().next().getUrl(), is(URI.create("http://company-c.com/nav-items")));
    }

    @Test
    public void login_without_logout_not_allowed() throws IOException {
        final String data = "# Sample platform.properties\n"
                + "brx.navapp.resource.navigationitems.id-1  = IFRAME, https://company-1.com/nav-items\n"
                + "brx.navapp.resource.login.id-1  = IFRAME, https://company-1.com/login\n";
        properties.load(new StringReader(data));

        try {
            new NavAppResourceServiceImpl(containerConfiguration);
            fail("should have thrown");
        } catch (IllegalArgumentException e) {
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
        properties.load(new StringReader(data));

        try {
            new NavAppResourceServiceImpl(containerConfiguration);
            fail("should have thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), allOf(
                    containsString("brx.navapp.resource.login.id-1"),
                    containsString("missing")
            ));
        }
    }

    @Test
    public void login_logout_without_navigationitems_not_allowed() {

        properties.setProperty(LOGIN_KEY_PREFIX + ".company-a", "IFRAME, http://company-a.com/login");
        properties.setProperty(LOGOUT_KEY_PREFIX + ".company-a", "REST, http://company-b.com/logout");

        try {
            new NavAppResourceServiceImpl(containerConfiguration);
            fail("should have thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), allOf(
                    containsString("brx.navapp.resource.navigationitems.company-a"),
                    containsString("missing")
            ));
        }
    }

    @Test
    public void values_wrong_order_not_allowed() {

        properties.setProperty(NAVIGATION_ITEMS_KEY_PREFIX + ".company-a", "http://company-c.com/nav-items, IFRAME");

        try {
            new NavAppResourceServiceImpl(containerConfiguration);
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

        properties.setProperty(NAVIGATION_ITEMS_KEY_PREFIX + ".company-a", "http://company-c.com/nav-items");

        try {
            new NavAppResourceServiceImpl(containerConfiguration);
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

        properties.setProperty(NAVIGATION_ITEMS_KEY_PREFIX + ".company-a", "REST");

        try {
            new NavAppResourceServiceImpl(containerConfiguration);
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

        properties.setProperty(NAVIGATION_ITEMS_KEY_PREFIX + ".company-a", "http://company-a.com/navitems, REST");

        try {
            new NavAppResourceServiceImpl(containerConfiguration);
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
        properties.load(new StringReader(data));

        try {
            new NavAppResourceServiceImpl(containerConfiguration);
            fail("should have thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), allOf(
                    containsString("IFRAME, https://company-1.com/login"),
                    containsString("brx.navapp.resource.login.id-1"),
                    containsString("brx.navapp.resource.login.id-2")
            ));
        }
    }

    @Test
    public void navapp_resource_url_from_properties() throws IOException {
        final URI someCDN = URI.create("https://some.cdn.com/navapp");
        final String data = "# Sample platform.properties\n"
                + "brx.navapp.location  = " + someCDN + "\n";
        properties.load(new StringReader(data));

        final NavAppResourceService service = new NavAppResourceServiceImpl(containerConfiguration);

        assertThat(service.getNavAppResourceUrl(), is(Optional.of(someCDN)));
    }

    @Test
    public void navapp_resource_url_from_environment() {

        final URI someCDN = URI.create("https://some.cdn.com/navapp");
        System.setProperty(NavAppResourceServiceImpl.NAVAPP_LOCATION_KEY, someCDN.toString());

        final NavAppResourceService service = new NavAppResourceServiceImpl(containerConfiguration);

        assertThat(service.getNavAppResourceUrl(), is(Optional.of(someCDN)));
        System.getProperties().remove(NavAppResourceServiceImpl.NAVAPP_LOCATION_KEY);
    }

    @Test
    public void navapp_resource_url_null_if_not_set_as_property_or_env_var() {

        final NavAppResourceService service = new NavAppResourceServiceImpl(containerConfiguration);

        assertThat(service.getNavAppResourceUrl(), is(Optional.empty()));
    }

    private static ContainerConfiguration fromConfiguration(Configuration configuration) {
        return new ContainerConfiguration() {

            @Override
            public boolean isEmpty() {
                return configuration.isEmpty();
            }

            @Override
            public boolean containsKey(final String key) {
                return configuration.containsKey(key);
            }

            @Override
            public Iterator<String> getKeys() {
                return configuration.getKeys();
            }

            @Override
            public boolean isDevelopmentMode() {
                return true;
            }

            @Override
            public Properties toProperties() {
                // Intentionally throw this exception to prevent classes from using it.
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean getBoolean(final String key) {
                return configuration.getBoolean(key);
            }

            @Override
            public boolean getBoolean(final String key, final boolean defaultValue) {
                return configuration.getBoolean(key, defaultValue);
            }

            @Override
            public double getDouble(final String key) {
                return configuration.getDouble(key);
            }

            @Override
            public double getDouble(final String key, final double defaultValue) {
                return configuration.getDouble(key, defaultValue);
            }

            @Override
            public float getFloat(final String key) {
                return configuration.getFloat(key);
            }

            @Override
            public float getFloat(final String key, final float defaultValue) {
                return configuration.getFloat(key, defaultValue);
            }

            @Override
            public int getInt(final String key) {
                return configuration.getInt(key);
            }

            @Override
            public int getInt(final String key, final int defaultValue) {
                return configuration.getInt(key, defaultValue);
            }

            @Override
            public List<String> getList(final String key) {
                return Arrays.asList(getStringArray(key));
            }

            @Override
            public long getLong(final String key) {
                return configuration.getLong(key);
            }

            @Override
            public long getLong(final String key, final long defaultValue) {
                return configuration.getLong(key, defaultValue);
            }

            @Override
            public String getString(final String key) {
                return configuration.getString(key);
            }

            @Override
            public String getString(final String key, final String defaultValue) {
                return configuration.getString(key, defaultValue);
            }

            @Override
            public String[] getStringArray(final String key) {
                return configuration.getStringArray(key);
            }

        };
    }
}
