/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

package org.hippoecm.hst.pagecomposer.jaxrs.services;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.pagecomposer.jaxrs.services.exceptions.HstConfigurationException;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.mock.MockNode;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_ABSTRACTPAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_CONTAINERS;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_PAGES;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODENAME_HST_WORKSPACE;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_COMPONENTS;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONTAINERCOMPONENTSFOLDER;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class HstConfigurationServiceImplTest {

    private HstConfigurationService hstConfigurationService;
    private Node configurationsNode;
    private Session session;

    @Before
    public void setUp() throws RepositoryException {
        final MockNode rootNode = MockNode.root();
        session = rootNode.getSession();

        final Node hstRoot = rootNode.addNode("hst:hst", "hst:hst");
        configurationsNode = hstRoot.addNode(HstNodeTypes.NODENAME_HST_CONFIGURATIONS, HstNodeTypes.NODETYPE_HST_CONFIGURATIONS);

        hstConfigurationService = new HstConfigurationServiceImpl("/hst:hst/hst:configurations");
    }

    @Test
    public void delete_configuration_node() throws RepositoryException, HstConfigurationException {
        addConfigurationNode("foo");
        addConfigurationNode("bah");

        assertThat(session.itemExists("/hst:hst/hst:configurations/foo"), is(true));
        assertThat(session.itemExists("/hst:hst/hst:configurations/bah"), is(true));

        hstConfigurationService.delete(session, "/hst:hst/hst:configurations/foo");

        assertThat(session.itemExists("/hst:hst/hst:configurations/foo"), is(false));
        assertThat(session.itemExists("/hst:hst/hst:configurations/bah"), is(true));
    }

    @Test
    public void delete_both_live_and_preview_config_nodes() throws RepositoryException, HstConfigurationException {
        addConfigurationNode("foo");
        addConfigurationNode("foo-preview");

        assertThat(session.itemExists("/hst:hst/hst:configurations/foo"), is(true));
        assertThat(session.itemExists("/hst:hst/hst:configurations/foo-preview"), is(true));

        hstConfigurationService.delete(session, "/hst:hst/hst:configurations/foo");

        assertThat(session.itemExists("/hst:hst/hst:configurations/foo"), is(false));
        assertThat(session.itemExists("/hst:hst/hst:configurations/foo-preview"), is(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannot_delete_null_config_path() throws HstConfigurationException, RepositoryException {
        hstConfigurationService.delete(session, (String)null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannot_delete_blank_config_path() throws HstConfigurationException, RepositoryException {
        hstConfigurationService.delete(session, " ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannot_delete_preview_config_path() throws HstConfigurationException, RepositoryException {
        addConfigurationNode("foo");
        addConfigurationNode("foo-preview");

        hstConfigurationService.delete(session, "/hst:hst/hst:configurations/foo-preview");
    }

    @Test
    public void cannot_delete_configuration_node_if_a_descendant_config_exists() throws RepositoryException {
        addConfigurationNode("foo");
        addConfigurationNode("bah");
        final Node alphaNode = addConfigurationNode("alpha");
        final Node betaNode = addConfigurationNode("beta");

        setInheritance(alphaNode, "../foo");
        setInheritance(betaNode, "../foo", "../bah");

        try {
            hstConfigurationService.delete(session, "/hst:hst/hst:configurations/foo");
            fail("foo config should not be deleted");
        } catch (HstConfigurationException e) {
            assertThat(e.getMessage(), is("The configuration node is inherited by others"));
        }

        try {
            hstConfigurationService.delete(session, "/hst:hst/hst:configurations/bah");
            fail("bah config should not be deleted");
        } catch (HstConfigurationException e) {
            assertThat(e.getMessage(), is("The configuration node is inherited by others"));
        }
    }

    @Test
    public void get_all_container_nodes() throws Exception {
        final Node fooConfigNode = addConfigurationNode("foo");
        final List<Node> mockContainerNodes = mockContainerNodes(fooConfigNode);

        final List<Node> containerNodes = hstConfigurationService.getContainerNodes(session, "/hst:hst/hst:configurations/foo");

        assertThat(containerNodes, containsInAnyOrder(mockContainerNodes.toArray(new Node[mockContainerNodes.size()])));
    }

    private List<Node> mockContainerNodes(final Node configurationNode) throws RepositoryException {
        final List<Node> containerNodes = new ArrayList<>();
        final Node pagesNode = configurationNode.addNode(NODENAME_HST_PAGES, NODENAME_HST_PAGES);
        containerNodes.add(pagesNode.addNode("page-container-1", NODETYPE_HST_CONTAINERCOMPONENT));

        final Node componentsNode = configurationNode.addNode(NODETYPE_HST_COMPONENTS, NODETYPE_HST_COMPONENTS);
        containerNodes.add(componentsNode.addNode("component-container-1", NODETYPE_HST_CONTAINERCOMPONENT));
        containerNodes.add(componentsNode.addNode("component-container-2", NODETYPE_HST_CONTAINERCOMPONENT));

        final Node abstractPagesNode = configurationNode.addNode(NODENAME_HST_ABSTRACTPAGES, NODENAME_HST_ABSTRACTPAGES);
        containerNodes.add(abstractPagesNode.addNode("abstractpage-container-1", NODETYPE_HST_CONTAINERCOMPONENT));
        containerNodes.add(abstractPagesNode.addNode("abstractpage-container-2", NODETYPE_HST_CONTAINERCOMPONENT));

        final Node workspaceNode = configurationNode.addNode(NODENAME_HST_WORKSPACE, NODENAME_HST_WORKSPACE);
        final Node hstWorkspaceContainersNode = workspaceNode.addNode(NODENAME_HST_CONTAINERS, NODETYPE_HST_CONTAINERCOMPONENTSFOLDER);
        containerNodes.add(hstWorkspaceContainersNode.addNode("ws-container-1", NODETYPE_HST_CONTAINERCOMPONENT));
        containerNodes.add(hstWorkspaceContainersNode.addNode("ws-container-2", NODETYPE_HST_CONTAINERCOMPONENT));

        return containerNodes;
    }

    private void setInheritance(final Node configNode, final String ... inheritsFrom) throws RepositoryException {
        configNode.setProperty(HstNodeTypes.GENERAL_PROPERTY_INHERITS_FROM, inheritsFrom);
    }

    private Node addConfigurationNode(final String configId) throws RepositoryException {
        return configurationsNode.addNode(configId, HstNodeTypes.NODETYPE_HST_CONFIGURATION);
    }
}