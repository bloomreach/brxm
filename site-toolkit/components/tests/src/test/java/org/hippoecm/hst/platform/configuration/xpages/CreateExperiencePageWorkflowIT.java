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

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.model.HstManager;
import org.hippoecm.hst.core.request.ResolvedMount;
import org.hippoecm.hst.platform.api.ChannelService;
import org.hippoecm.hst.platform.api.PlatformServices;
import org.hippoecm.hst.platform.api.experiencepages.XPageLayout;
import org.hippoecm.hst.site.HstServices;
import org.hippoecm.hst.test.AbstractTestConfigurations;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.standardworkflow.FolderWorkflow;
import org.hippoecm.repository.standardworkflow.JcrTemplateNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.HippoServiceRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hippoecm.hst.configuration.HstNodeTypes.COMPONENT_PROPERTY_LABEL;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES;
import static org.hippoecm.hst.configuration.HstNodeTypes.MIXINTYPE_HST_XPAGE_MIXIN;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_XPAGE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_TEMPLATE;

public class CreateExperiencePageWorkflowIT extends AbstractTestConfigurations {


    private Repository repository;
    private HippoSession adminSession;
    private HstManager hstManager;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        repository = HstServices.getComponentManager().getComponent(Repository.class.getName() + ".delegating");
        adminSession = (HippoSession) repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
        hstManager = getComponent(HstManager.class.getName());
    }

    @After
    public void tearDown() throws Exception {
        adminSession.logout();
        super.tearDown();
    }

    @Test
    public void assert_XPageLayout_for_channel_is_available_via_Channel_Service() throws Exception {
        final ResolvedMount mount = hstManager.getVirtualHosts().matchMount("localhost", "/");

        final String channelId = mount.getMount().getChannel().getId();

        final ChannelService channelService = HippoServiceRegistry.getService(PlatformServices.class).getChannelService();

        final Map<String, XPageLayout> xPageLayouts = channelService.getXPageLayouts(channelId);
        assertThat(xPageLayouts.size())
                .as("Expected xpage1 to be found")
                .isEqualTo(1);

        assertThat(xPageLayouts.containsKey("hst:xpages/xpage1"))
                .isTrue();

        final XPageLayout xPageLayout = xPageLayouts.get("hst:xpages/xpage1");

        assertThat(xPageLayout.getLabel())
                .isEqualTo("XPage 1");

        final JcrTemplateNode jcrTemplateNode = xPageLayout.getJcrTemplateNode();

        assertThat(jcrTemplateNode)
                .isNotNull();

        assertThat(jcrTemplateNode.getMixinNames())
                .containsExactly(MIXINTYPE_HST_XPAGE_MIXIN);

        assertThatThrownBy(() -> jcrTemplateNode.addChild("foo", "foo"))
                .as("JcrTemplateNode in HstModel should be immutable")
                .isInstanceOf(UnsupportedOperationException.class);

        assertThatThrownBy(() -> jcrTemplateNode.getMixinNames().add("test"))
                .as("JcrTemplateNode in HstModel should be immutable")
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void folder_workflow_create_new_XPage_document_as_admin() throws Exception {
        // unfortunately cannot test as 'author' since access for group author to
        // '/hippo:configuration/hippo:queries/hippo:templates' is bootstrapped by cms and read access is required by document
        // workflow
        createNewDocumentAs(adminSession);
    }


    private void createNewDocumentAs(final HippoSession userSession) throws Exception {

        final String folder = "/unittestcontent/documents/unittestproject/News";

        try {

            final Map<String, XPageLayout> xPageLayouts = HippoServiceRegistry.getService(PlatformServices.class).getChannelService()
                    .getXPageLayouts("unittestproject");

            final XPageLayout xPageLayout = xPageLayouts.get("hst:xpages/xpage1");

            final Node folderNode = userSession.getNode(folder);

            final FolderWorkflow workflow = (FolderWorkflow) userSession.getWorkspace().getWorkflowManager().getWorkflow("internal", folderNode);



            final String add = workflow.add("simple", "new-document", "newDoc", xPageLayout.getJcrTemplateNode());

            final Node node = userSession.getNode(add);

            assertThat(node.hasNode(NODENAME_HST_XPAGE))
                    .as("Expected Experience hst:xpage added below document variant")
                    .isTrue();

            final Node xpage = node.getNode(NODENAME_HST_XPAGE);

            assertThat(xpage.hasNode("430df2da-3dc8-40b5-bed5-bdc44b8445c6"))
                    .as("Expected the hippo:identifier from the 'xpage1' container to be added as child")
                    .isTrue();
            assertThat(xpage.hasNode("430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner"))
                    .as("Expected the banner component item copied as prototpye")
                    .isTrue();

            final Node banner = xpage.getNode("430df2da-3dc8-40b5-bed5-bdc44b8445c6/banner");
            assertThat(banner.hasProperty(GENERAL_PROPERTY_PARAMETER_NAMES)).isTrue();
            assertThat(banner.hasProperty(GENERAL_PROPERTY_PARAMETER_VALUES)).isTrue();
            assertThat(banner.hasProperty(NODETYPE_HST_TEMPLATE)).isTrue();
            assertThat(banner.hasProperty(COMPONENT_PROPERTY_LABEL)).isTrue();


            assertThat(xpage.hasNode("430df2da-3dc8-40b5-bed5-bdc44b8445c7"))
                    .as("Expected the hippo:identifier from the 'xpage1' container 2 to be added as child")
                    .isTrue();

        } finally {
            if (userSession.nodeExists("/unittestcontent/documents/unittestproject/News/newDoc")) {
                userSession.getNode("/unittestcontent/documents/unittestproject/News/newDoc").remove();
                userSession.save();
            }
        }
    }
}
