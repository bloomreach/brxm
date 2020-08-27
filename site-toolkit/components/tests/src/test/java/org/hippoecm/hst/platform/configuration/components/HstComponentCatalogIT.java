/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
 */
package org.hippoecm.hst.platform.configuration.components;

import java.util.List;

import javax.jcr.Session;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.hst.test.BannerComponent;
import org.hippoecm.hst.util.ParametersInfoAnnotationUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME;

public class HstComponentCatalogIT extends AbstractTestConfigurations {

    private HstManager hstManager;
    private Session session;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        session = createSession();
        createHstConfigBackup(session);
        hstManager = getComponent(HstManager.class.getName());
    }

    @Override
    @After
    public void tearDown() throws Exception {
        restoreHstConfigBackup(session);
        session.logout();
        super.tearDown();
    }

    @Test
    public void catalog_items_loading_reloading_inheritance_assertions() throws Exception {

        {
            ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");
            final HstSite hstSite = mount.getMount().getHstSite();

            final List<HstComponentConfiguration> catalogItems = hstSite.getComponentsConfiguration().getAvailableContainerItems();

            assertThat(catalogItems.size())
                    .as("Expected two catalog items bootstrapped below " +
                            "'/hst:hst/hst:configurations/unittestcommon/hst:catalog/unittestpackage'")
                    .isEqualTo(6);


            final HstComponentConfiguration bannerOldStyle = catalogItems.stream().filter(c -> c.getName().equals("unittestpackage/banner-old-style")).findFirst().get();

            assertThat(bannerOldStyle.getId())
                    .as("Even though catalog item, the 'id' is still expected to start with '/hst:components'")
                    .isEqualTo("hst:components/unittestpackage/banner-old-style");

            assertThat(bannerOldStyle.getDynamicComponentParameter("path").get().getDefaultValue())
                    .as("Expected 'banner old style' component to have dynamic parameter banner with default " +
                            "value '/some/default', see org.hippoecm.hst.test.BannerComponentInfo")
                    .isEqualTo("/some/default");

            assertThat(bannerOldStyle.getParameter("path"))
                    .as("Expected bootstrapped parameter 'document'")
                    .isEqualTo("banners/banner1");

            final HstComponentConfiguration bannerNewStyle = catalogItems.stream().filter(c -> c.getName().equals("unittestpackage/banner-new-style")).findFirst().get();

            assertThat(bannerNewStyle.getId())
                    .as("Even though catalog item, the 'id' is still expected to start with '/hst:components'")
                    .isEqualTo("hst:components/unittestpackage/banner-new-style");

            assertThat(bannerNewStyle.getDynamicComponentParameter("path").get().getDefaultValue())
                    .as("Expected 'header old style' component to have dynamic parameter header with default " +
                            "value 'some/default', see org.hippoecm.hst.test.BannerComponentInfo")
                    .isEqualTo("/some/default");

            assertThat(bannerNewStyle.getParameter("path"))
                    .as("Expected bootstrapped parameter 'document'")
                    .isEqualTo("banners/banner2");

        }

        // change items, proof reload
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:catalog/unittestpackage/banner-old-style")
                .setProperty(COMPONENT_PROPERTY_COMPONENT_CLASSNAME, "Foo");

        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:catalog/unittestpackage/banner-new-style")
                .setProperty(COMPONENT_PROPERTY_COMPONENT_CLASSNAME, "Foo");


        session.save();

        invalidator.eventPaths(new String[]{"/hst:hst/hst:configurations/unittestcommon/hst:catalog/unittestpackage/banner-old-style",
                "/hst:hst/hst:configurations/unittestcommon/hst:catalog/unittestpackage/banner-new-style"});

        {
            // assert invalidation works and 'warning' assertion on non-loadable class 'Foo'
            try (final Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(ParametersInfoAnnotationUtils.class).build()) {
                final ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");
                final HstSite hstSite = mount.getMount().getHstSite();

                final List<HstComponentConfiguration> catalogItems = hstSite.getComponentsConfiguration().getAvailableContainerItems();

                interceptor.messages().forEach(s -> assertThat(s).isEqualTo("Component class not loadable: Foo"));
            }
        }

        // move catalog items to hst:default and assert correctly reloaded

        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:catalog/unittestpackage/banner-old-style")
                .setProperty(COMPONENT_PROPERTY_COMPONENT_CLASSNAME, BannerComponent.class.getName());

        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:catalog/unittestpackage/banner-new-style")
                .setProperty(COMPONENT_PROPERTY_COMPONENT_CLASSNAME, BannerComponent.class.getName());

        session.move("/hst:hst/hst:configurations/unittestcommon/hst:catalog/unittestpackage",
                "/hst:hst/hst:configurations/hst:default/hst:catalog/unittestpackage");

        session.save();

        invalidator.eventPaths(new String[]{"/hst:hst/hst:configurations/unittestcommon/hst:catalog/unittestpackage",
                "/hst:hst/hst:configurations/hst:default/hst:catalog/unittestpackage"});

        {
            final ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");
            final HstSite hstSite = mount.getMount().getHstSite();

            final List<HstComponentConfiguration> catalogItems = hstSite.getComponentsConfiguration().getAvailableContainerItems();

            final HstComponentConfiguration bannerOldStyle = catalogItems.stream().filter(c -> c.getName().equals("unittestpackage/banner-old-style")).findFirst().get();

            assertThat(bannerOldStyle.getDynamicComponentParameter("path").get().getDefaultValue())
                    .as("Expected 'banner old style' component to have dynamic parameter banner with default " +
                            "value '/some/default', see org.hippoecm.hst.test.BannerComponentInfo")
                    .isEqualTo("/some/default");

            final HstComponentConfiguration bannerNewStyle = catalogItems.stream().filter(c -> c.getName().equals("unittestpackage/banner-new-style")).findFirst().get();

            assertThat(bannerNewStyle.getDynamicComponentParameter("path").get().getDefaultValue())
                    .as("Expected 'header new style' component to have dynamic parameter header with default " +
                            "value 'some/default', see org.hippoecm.hst.test.BannerComponentInfo")
                    .isEqualTo("/some/default");
        }
    }

}
