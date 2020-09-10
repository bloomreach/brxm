/*
 * Copyright 2015-2020 Hippo B.V. (http://www.onehippo.com)
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
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.ConfigurationUtils;
import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.configuration.site.HstSite;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.testutils.log4j.Log4jInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_COMPONENTDEFINITION;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_COMPONENT_CLASSNAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class HstComponentConfigurationIT extends AbstractTestConfigurations {

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
    public void localParameters_in_case_of_multiple_variants_on_component() throws Exception {

        Node contactPage = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:pages/contactpage");

        contactPage.setProperty("hst:parameternames", new String[]{"foo","foo"});
        contactPage.setProperty("hst:parametervalues", new String[]{"bar", "lux"});
        contactPage.setProperty("hst:parameternameprefixes", new String[]{"", "professional"});
        saveSession();

        ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");
        final HstSite hstSite = mount.getMount().getHstSite();
        final HstComponentConfiguration contactPageConfig = hstSite.getComponentsConfiguration().getComponentConfiguration("hst:pages/contactpage");
        assertNotNull(contactPageConfig);

        final String prefixedParameterName = ConfigurationUtils.createPrefixedParameterName("professional", "foo");

        final Map<String, String> parameters = contactPageConfig.getParameters();
        assertTrue(parameters.containsKey("foo"));
        assertTrue(parameters.containsKey(prefixedParameterName));
        assertEquals("bar", parameters.get("foo"));
        assertEquals("lux", parameters.get(prefixedParameterName));

        final Map<String, String> localParameters = contactPageConfig.getLocalParameters();
        assertTrue(localParameters.containsKey("foo"));
        assertTrue(localParameters.containsKey(prefixedParameterName));
        assertEquals("bar", localParameters.get("foo"));
        assertEquals("lux", localParameters.get(prefixedParameterName));

    }

    private void saveSession() throws RepositoryException {
        session.save();
        //TODO SS: Clarify what could be the cause of failures without delay
        try {
            Thread.sleep(100);
        } catch (InterruptedException ex) {}
    }

    @Test
    public void assertions_new_and_old_style_container_items() throws Exception {
        {
            final ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");
            final HstSite hstSite = mount.getMount().getHstSite();
            final HstComponentConfiguration container = hstSite.getComponentsConfiguration().getComponentConfiguration("hst:components/header/container");

            assertThat(container.getChildren().size()).isEqualTo(4);
            final HstComponentConfiguration oldStyleBanner = container.getChildByName("banner-old-style");
            assertThat(oldStyleBanner.getDynamicComponentParameter("path").get().getDefaultValue())
                    .as("Expected dynamic parameter to work for old style component items")
                    .isEqualTo("/some/default");

            final HstComponentConfiguration oldStyleBanner2 = container.getChildByName("banner-old-style-2");
            assertThat(oldStyleBanner2.getDynamicComponentParameter("path").get().getDefaultValue())
                    .as("Expected dynamic parameter to work for old style component items")
                    .isEqualTo("/some/default");

            final HstComponentConfiguration newStyleBanner = container.getChildByName("banner-new-style");
            assertThat(newStyleBanner.getDynamicComponentParameter("path").get().getDefaultValue())
                    .as("Expected dynamic parameter to work for new style component items")
                    .isEqualTo("/some/default");
            final HstComponentConfiguration newStyleBanner2 = container.getChildByName("banner-new-style-2");
            assertThat(newStyleBanner2.getDynamicComponentParameter("path").get().getDefaultValue())
                    .as("Expected dynamic parameter to work for old style component items")
                    .isEqualTo("/some/default");

            assertThat(newStyleBanner2 == newStyleBanner).isFalse();
            assertThat(newStyleBanner2.getDynamicComponentParameters() == newStyleBanner.getDynamicComponentParameters())
                    .as("Expected that dynamic component parameters instance is SHARED!")
                    .isTrue();

            // TODO CMS-13618 the dynamic component parameters should be shared even for old style components
            assertThat(oldStyleBanner.getDynamicComponentParameters() == oldStyleBanner2.getDynamicComponentParameters())
                    .as("Old style components dynamic component parameters are not yet shared")
                    .isTrue();
        }

        // assertions what happens for broken hst:componentdefinition (aka not pointing to existing catalog item)
        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:components/header/container/banner-new-style")
                .setProperty(COMPONENT_PROPERTY_COMPONENTDEFINITION, "not/found");
        session.save();

        invalidator.eventPaths(new String[]{"/hst:hst/hst:configurations/unittestcommon/hst:components/header/container/banner-new-style"});

        {
            try (Log4jInterceptor interceptor = Log4jInterceptor.onWarn().trap(HstComponentConfigurationService.class).build()) {
                final ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");
                final HstSite hstSite = mount.getMount().getHstSite();
                final HstComponentConfiguration container = hstSite.getComponentsConfiguration().getComponentConfiguration("hst:components/header/container");


                final HstComponentConfiguration newStyleBanner = container.getChildByName("banner-new-style");
                assertThat(newStyleBanner.getDynamicComponentParameters().size())
                        .as("Since 'hst:componentdefinition' points to non-existing catalog item, expect no " +
                                "dynamic parameters")
                        .isEqualTo(0);
                assertThat(interceptor.messages().collect(Collectors.toList()))
                        .as("Expected warning about unresolvable hst:componentdefinition")
                        .containsExactly("Invalid component '/hst:hst/hst:configurations/unittestcommon/hst:components/header/container/banner-new-style' since no catalog item found for 'hst:componentdefinition = not/found'");

            }
        }

    }

    /**
     * Setup : component A has ParameterInfo class X, and component A inherits component B which also has ParameterInfo class X.
     * In this case, Component A should end up with samen dynamic parameters instance as Component B
     */
    @Test
    public void component_referencing_component_with_same_componentClass_does_load_dynamicParameters_correct() throws Exception {

        // /hst:hst/hst:configurations/unittestcommon/hst:abstractpages/basepage/header inherits from
        // /hst:hst/hst:configurations/unittestcommon/hst:components/header which already has
        // classname org.hippoecm.hst.test.HeaderComponent : setting it now also on 'basepage/header' should result in
        // that it still works

        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:abstractpages/basepage/header")
                .setProperty(COMPONENT_PROPERTY_COMPONENT_CLASSNAME, "org.hippoecm.hst.test.HeaderComponent");
        session.save();

        invalidator.eventPaths(new String[]{"/hst:hst/hst:configurations/unittestcommon/hst:abstractpages/basepage/header"});

        final ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");
        final HstSite hstSite = mount.getMount().getHstSite();

        final HstComponentConfiguration baseHeader = hstSite.getComponentsConfiguration().getComponentConfiguration("hst:abstractpages/basepage/header");
        final HstComponentConfiguration header = hstSite.getComponentsConfiguration().getComponentConfiguration("hst:components/header");

        assertThat(baseHeader.getDynamicComponentParameters())
                .as("Expected same instance for dynamic component parameters since same ParameterInfo class " +
                        "should result in shared dynamic component parameters object")
                .isSameAs(header.getDynamicComponentParameters());
    }

    /**
     * Setup : component A has ParameterInfo class X, and component A inherits component B which has ParameterInfo class Y.
     * In this case, Component A should end up with dynamic parameters for class X, nothing from class Y (so no merging)
     */
    @Test
    public void component_referencing_component_with_other_componentClass_does_skip_dynamicParameters_inherited_component() throws Exception {
        // /hst:hst/hst:configurations/unittestcommon/hst:abstractpages/basepage/header inherits from
        // /hst:hst/hst:configurations/unittestcommon/hst:components/header which already has
        // classname org.hippoecm.hst.test.HeaderComponent : setting now on 'basepage/header' the class BannerComponent
        // should result in that the referenced hst:components/header its dynamic component parameters are skipped
        // completely (only residual parameters get merged since they are not ParameterInfo class related

        session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:abstractpages/basepage/header")
                .setProperty(COMPONENT_PROPERTY_COMPONENT_CLASSNAME, "org.hippoecm.hst.test.BannerComponent");
        session.save();

        invalidator.eventPaths(new String[]{"/hst:hst/hst:configurations/unittestcommon/hst:abstractpages/basepage/header"});

        final ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");
        final HstSite hstSite = mount.getMount().getHstSite();

        final HstComponentConfiguration baseHeader = hstSite.getComponentsConfiguration().getComponentConfiguration("hst:abstractpages/basepage/header");
        final HstComponentConfiguration header = hstSite.getComponentsConfiguration().getComponentConfiguration("hst:components/header");

        final List<DynamicParameter> baseHeaderParameters = baseHeader.getDynamicComponentParameters();
        final List<DynamicParameter> headerParameters = header.getDynamicComponentParameters();


        assertThat(baseHeaderParameters)
                .as("Expected ParameterInfo from inherited component ignored since already has own parameterInfo")
                .isNotSameAs(headerParameters);

        assertThat(baseHeaderParameters.get(0).getName())
                .as("Expected 'path' from BannerComponentInfo")
                .isEqualTo("path");

        assertThat(headerParameters.get(0).getName())
                .as("Expected 'header' from HeaderComponentInfo")
                .isEqualTo("header");
    }

    /**
     * This is not about DynamicComponentParameters but actual stored hst:parameternames/values
     */
    @Test
    public void referenced_component_parameters_are_merged() throws Exception {

        final Node baseHeader = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:abstractpages/basepage/header");
        baseHeader.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES, new String[] {"one", "two"});
        baseHeader.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES, new String[] {"val1", "val2"});

        final Node componentHeader = session.getNode("/hst:hst/hst:configurations/unittestcommon/hst:components/header");
        componentHeader.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES, new String[] {"three", "four"});
        componentHeader.setProperty(HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES, new String[] {"val3", "val4"});

        session.save();

        invalidator.eventPaths(new String[]{"/hst:hst/hst:configurations/unittestcommon/hst:abstractpages/basepage/header"});

        invalidator.eventPaths(new String[]{"/hst:hst/hst:configurations/unittestcommon/hst:components/header"});

        final ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");
        final HstSite hstSite = mount.getMount().getHstSite();

        final HstComponentConfiguration baseHeaderConfig = hstSite.getComponentsConfiguration().getComponentConfiguration("hst:abstractpages/basepage/header");

        assertThat(baseHeaderConfig.getLocalParameters().keySet())
                .containsExactly("one", "two");
        assertThat(baseHeaderConfig.getLocalParameters().values())
                .containsExactly("val1", "val2");

        assertThat(baseHeaderConfig.getParameters().keySet())
                .containsExactly("one", "two", "three", "four");
        assertThat(baseHeaderConfig.getParameters().values())
                .containsExactly("val1", "val2", "val3", "val4");


        final HstComponentConfiguration componentHeaderConfig = hstSite.getComponentsConfiguration().getComponentConfiguration("hst:components/header");


        assertThat(componentHeaderConfig.getLocalParameters().keySet())
                .containsExactly("three", "four");
        assertThat(componentHeaderConfig.getLocalParameters().values())
                .containsExactly("val3", "val4");

        assertThat(componentHeaderConfig.getParameters().keySet())
                .containsExactly("three", "four");
        assertThat(componentHeaderConfig.getParameters().values())
                .containsExactly("val3", "val4");
    }
}
