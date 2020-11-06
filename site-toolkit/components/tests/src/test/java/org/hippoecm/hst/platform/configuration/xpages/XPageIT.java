/*
 *  Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.hippoecm.hst.platform.configuration.xpages;


import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import javax.jcr.Session;

import org.hippoecm.hst.configuration.components.HstComponentConfiguration;
import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.platform.configuration.components.HstComponentConfigurationService;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.repository.standardworkflow.JcrTemplateNode;
import org.hippoecm.repository.util.JcrUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_XPAGE_MIXIN;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class XPageIT extends AbstractTestConfigurations {

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
    public void xpage_hst_model_are_available() throws Exception {

        final ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");

        final Map<String, HstComponentConfiguration> xPages = mount.getMount().getHstSite().getComponentsConfiguration().getXPages();

        assertThat(xPages.size()).isEqualTo(1);

        final HstComponentConfiguration xPageLayout = xPages.get("xpage1");

        assertThat(xPageLayout).as("XPage should be available by node name").isNotNull();
        assertThat(xPageLayout.getLabel()).isEqualTo("XPage 1");

        assertThat(((HstComponentConfigurationService)xPageLayout).getJcrTemplateNode())
                .as("Expected non-null jcr template node for root 'xpage Layout'")
                .isNotNull();

        final JcrTemplateNode jcrTemplateNode = ((HstComponentConfigurationService) xPageLayout).getJcrTemplateNode();

        assertThat(jcrTemplateNode.getMixinNames())
                .containsExactly(MIXINTYPE_HST_XPAGE_MIXIN);


        assertThatThrownBy(() -> jcrTemplateNode.addChild("foo", "foo"))
                .as("JcrTemplateNode in HstModel should be immutable")
                .isInstanceOf(UnsupportedOperationException.class);

        assertThat(xPageLayout.getHippoIdentifier()).as("XPage is not expected to have an auto created 'hippo:identifier'").isNull();

        // TODO org.hippoecm.hst.pagecomposer.jaxrs.util.UUIDUtils and use it to common util but not now to
        // TODO avoid too many GIT changes (and possibly conflicts, will do later)

        final HstComponentConfiguration main = xPageLayout.getChildByName("main");

        assertThat(((HstComponentConfigurationService)main).getJcrTemplateNode())
                .as("Expected null jcr template node for NON-root 'xpage Layout'")
                .isNull();

        final HstComponentConfiguration container1 = main.getChildByName("container1");

        assertThat(((HstComponentConfigurationService)container1).getJcrTemplateNode())
                .as("Expected null jcr template node for NON-root 'xpage Layout'")
                .isNull();

        final HstComponentConfiguration container2 = main.getChildByName("container2");

        try {
            UUID.fromString(container1.getHippoIdentifier());
        } catch (IllegalArgumentException e) {
            fail("Expected hippo:identifier of form UUID on 'container1'");
        }
        try {
            UUID.fromString(container2.getHippoIdentifier());
        } catch (IllegalArgumentException e) {
            fail("Expected hippo:identifier of form UUID on 'container2'");
        }

    }

    /**
     * <p>
     *     XPage inheriting from abstract page
     * </p>
     * @throws Exception
     */
    @Test
    public void xpage_hst_model_inherits_config_from_abstract_page_from_common_config() throws Exception {
        final ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");
        final Map<String, HstComponentConfiguration> xPages = mount.getMount().getHstSite().getComponentsConfiguration().getXPages();
        final HstComponentConfiguration xpage1 = xPages.get("xpage1");

        assertNotNull(xpage1.getChildByName("header"));
        assertNotNull(xpage1.getChildByName("leftmenu"));
    }

    @Test
    public void xpage_hst_model_have_a_stable_hippoIdentifier_across_copy() throws Exception {

        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:xpages/xpage1",
                "/hst:hst/hst:configurations/unittestproject/hst:xpages/xpage2");
        session.save();

        invalidate("/hst:hst/hst:configurations/unittestproject/hst:xpages/xpage2");

        final ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");

        final Map<String, HstComponentConfiguration> xPages = mount.getMount().getHstSite().getComponentsConfiguration().getXPages();

        assertThat(xPages.size()).isEqualTo(2);

        final HstComponentConfiguration xpage1 = xPages.get("xpage1");
        final HstComponentConfiguration xpage2 = xPages.get("xpage2");

        // assert that autocreated 'hippo:identifier' property is intact after copy (normally important for copying between preview/live
        // or branches of hst config
        assertThat(xpage1.getChildByName("main").getChildByName("container1").getHippoIdentifier())
                .as("Expected 'hippo:identifier' property intact between copy")
                .isEqualTo(xpage2.getChildByName("main").getChildByName("container1").getHippoIdentifier());

    }

    @Test
    public void xpage_hst_model_are_reloaded() throws Exception {

        {
            final ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");
            final Map<String, HstComponentConfiguration> xPages = mount.getMount().getHstSite().getComponentsConfiguration().getXPages();
            assertThat(xPages.size()).isEqualTo(1);
        }

        JcrUtils.copy(session, "/hst:hst/hst:configurations/unittestproject/hst:xpages/xpage1",
                "/hst:hst/hst:configurations/unittestproject/hst:xpages/xpage2");
        session.save();
        invalidate("/hst:hst/hst:configurations/unittestproject/hst:xpages/xpage2");

        {
            final ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");
            final Map<String, HstComponentConfiguration> xPages = mount.getMount().getHstSite().getComponentsConfiguration().getXPages();
            assertThat(xPages.size()).isEqualTo(2);
        }
    }


    @Test
    public void xpage_hst_model_does_inherit_from_super_config_or_default_config() throws Exception {

        // move xpages to 'common' inherited config and confirm they are not inherited
        session.move("/hst:hst/hst:configurations/unittestproject/hst:xpages",
                "/hst:hst/hst:configurations/unittestcommon/hst:xpages");

        session.save();

        invalidate("/hst:hst/hst:configurations/unittestproject/hst:xpages",
                "/hst:hst/hst:configurations/unittestcommon/hst:xpages");

        {
            final ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");
            final Map<String, HstComponentConfiguration> xPages = mount.getMount().getHstSite().getComponentsConfiguration().getXPages();
            assertThat(xPages.size()).isEqualTo(1);
        }
        session.move("/hst:hst/hst:configurations/unittestcommon/hst:xpages",
                "/hst:hst/hst:configurations/hst:default/hst:xpages");

        session.save();

        invalidate("/hst:hst/hst:configurations/unittestcommon/hst:xpages",
                "/hst:hst/hst:configurations/hst:default/hst:xpages");
        {
            final ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");
            final Map<String, HstComponentConfiguration> xPages = mount.getMount().getHstSite().getComponentsConfiguration().getXPages();
            assertThat(xPages.size()).isEqualTo(1);
        }
    }

    private void invalidate(String... paths) {

        Arrays.stream(paths).forEach(s -> invalidator.eventPaths(s));

    }

}
