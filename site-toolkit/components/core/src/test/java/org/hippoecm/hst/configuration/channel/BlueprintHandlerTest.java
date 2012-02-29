/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.hst.configuration.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.test.AbstractHstTestCase;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests {@link BlueprintHandler}.
 */
public class BlueprintHandlerTest extends AbstractHstTestCase {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        build(getSession(), new String[] {
                "/test", "nt:unstructured",
                    "/test/hst:blueprints", HstNodeTypes.NODENAME_HST_BLUEPRINTS
        });
    }

    /**
     * Tests all getters.
     */
    @Test
    public void getters() throws RepositoryException {
        build(getSession(), new String[] {
                "/test/hst:blueprints/test", HstNodeTypes.NODETYPE_HST_BLUEPRINT,
                    HstNodeTypes.BLUEPRINT_PROPERTY_NAME, "Test Blueprint",
                    HstNodeTypes.BLUEPRINT_PROPERTY_DESCRIPTION, "Description of Test Blueprint",
                    "/test/hst:blueprints/test/hst:configuration", HstNodeTypes.NODETYPE_HST_CONFIGURATION
        });
        getSession().save();

        Node blueprintNode = getSession().getNode("/test/hst:blueprints/test");
        Blueprint blueprint = BlueprintHandler.buildBlueprint(blueprintNode);

        assertEquals("test", blueprint.getId());
        assertEquals("Test Blueprint", blueprint.getName());
        assertEquals("Description of Test Blueprint", blueprint.getDescription());
        assertEquals("/test/hst:blueprints/test", blueprint.getPath());
    }

    /**
     * Tests createChannel()
     */
    @Test
    public void createChannelWithFixedMountPoint() throws RepositoryException {
        build(getSession(), new String[] {
                "/test/hst:blueprints/test", HstNodeTypes.NODETYPE_HST_BLUEPRINT,
                    "/test/hst:blueprints/test/hst:configuration", HstNodeTypes.NODETYPE_HST_CONFIGURATION,
                    "/test/hst:blueprints/test/hst:channel", HstNodeTypes.NODETYPE_HST_CHANNEL,
                        "/test/hst:blueprints/test/hst:channel/hst:channelinfo", HstNodeTypes.NODETYPE_HST_CHANNELINFO,
                    "/test/hst:blueprints/test/hst:mount", HstNodeTypes.NODETYPE_HST_MOUNT,
                        "hst:mountpoint", "/hst:hst/hst:sites/blueprint-site"
        });
        getSession().save();

        Node blueprintNode = getSession().getNode("/test/hst:blueprints/test");
        Blueprint blueprint = BlueprintHandler.buildBlueprint(blueprintNode);

        Channel channel = blueprint.getPrototypeChannel();
        assertEquals("/hst:hst/hst:sites/blueprint-site", channel.getHstMountPoint());
        assertNull(channel.getHstConfigPath());
        assertNull(channel.getContentRoot());
    }

    /**
     * Tests createChannel()
     */
    @Test
    public void createChannelWithPrototypeSite() throws RepositoryException {
        build(getSession(), new String[] {
                "/test/hst:blueprints/test", HstNodeTypes.NODETYPE_HST_BLUEPRINT,
                    "/test/hst:blueprints/test/hst:configuration", HstNodeTypes.NODETYPE_HST_CONFIGURATION,
                    "/test/hst:blueprints/test/hst:channel", HstNodeTypes.NODETYPE_HST_CHANNEL,
                        "/test/hst:blueprints/test/hst:channel/hst:channelinfo", HstNodeTypes.NODETYPE_HST_CHANNELINFO,
                    "/test/hst:blueprints/test/hst:mount", HstNodeTypes.NODETYPE_HST_MOUNT,
                        // the mountpoint should be ignored by the blueprint because there is also a hst:site node
                        "hst:mountpoint", "/hst:hst/hst:sites/blueprint-site",
                    "/test/hst:blueprints/test/hst:site", HstNodeTypes.NODETYPE_HST_SITE,
                        "hst:configurationpath", "/hst:hst/hst:configurations/blueprint-configuration"
        });
        Node contentRoot = getSession().getRootNode().addNode("test/content");
        build(getSession(), new String[] {
            "/test/hst:blueprints/test/hst:site/hst:content", "hippo:facetselect",
            "hippo:docbase", contentRoot.getIdentifier(),
            "hippo:facets", "hippo:availability",
            "hippo:modes", "single",
            "hippo:values", "live",
        });
        getSession().save();

        Node blueprintNode = getSession().getNode("/test/hst:blueprints/test");
        Blueprint blueprint = BlueprintHandler.buildBlueprint(blueprintNode);

        Channel channel = blueprint.getPrototypeChannel();
        assertNull(channel.getHstMountPoint());
        assertEquals("/hst:hst/hst:configurations/blueprint-configuration", channel.getHstConfigPath());
        assertEquals("/test/content", channel.getContentRoot());
    }

}
