/*
 *  Copyright 2011-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.hst.platform.configuration.channel;

import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;

import org.hippoecm.hst.configuration.HstNodeTypes;
import org.hippoecm.hst.configuration.channel.Blueprint;
import org.hippoecm.hst.core.jcr.AbstractRepositoryTestCase;
import org.hippoecm.hst.platform.configuration.cache.HstNodeLoadingCache;
import org.hippoecm.hst.configuration.model.HstNode;
import org.hippoecm.hst.platform.configuration.channel.BlueprintHandler;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.services.hst.Channel;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_BLUEPRINT;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CHANNEL;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CHANNELINFO;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_CONFIGURATION;
import static org.hippoecm.hst.configuration.HstNodeTypes.NODETYPE_HST_MOUNT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BlueprintHandlerIT extends AbstractRepositoryTestCase {

    private HstNodeLoadingCache hstNodeLoadingCache;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        RepositoryTestCase.build(new String[]{
                "/test", "nt:unstructured",
                "/test/hst:blueprints", HstNodeTypes.NODENAME_HST_BLUEPRINTS
        }, session);

        hstNodeLoadingCache = new HstNodeLoadingCache(server.getRepository(), new SimpleCredentials("admin", "admin".toCharArray()), "/test");
    }

    /**
     * Tests all getters.
     */
    @Test
    public void getters() throws RepositoryException {
        RepositoryTestCase.build(new String[]{
                "/test/hst:blueprints/test", NODETYPE_HST_BLUEPRINT,
                HstNodeTypes.BLUEPRINT_PROPERTY_NAME, "Test Blueprint",
                HstNodeTypes.BLUEPRINT_PROPERTY_DESCRIPTION, "Description of Test Blueprint",
                "/test/hst:blueprints/test/hst:configuration", NODETYPE_HST_CONFIGURATION
        }, session);
        session.save();

        HstNode blueprintNode = hstNodeLoadingCache.getNode("/test/hst:blueprints/test");
        Blueprint blueprint = BlueprintHandler.buildBlueprint(blueprintNode, "/site");

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

        RepositoryTestCase.build(new String[]{
                "/test/hst:blueprints/test", NODETYPE_HST_BLUEPRINT,
                "/test/hst:blueprints/test/hst:configuration", NODETYPE_HST_CONFIGURATION,
                "/test/hst:blueprints/test/hst:configuration/hst:channel", NODETYPE_HST_CHANNEL,
                "/test/hst:blueprints/test/hst:configuration/hst:channel/hst:channelinfo", NODETYPE_HST_CHANNELINFO,
                "/test/hst:blueprints/test/hst:mount", NODETYPE_HST_MOUNT,
                "hst:mountpoint", "/hst:hst/hst:sites/blueprint-site"
        }, session);
        session.save();

        HstNode blueprintNode = hstNodeLoadingCache.getNode("/test/hst:blueprints/test");
        Blueprint blueprint = BlueprintHandler.buildBlueprint(blueprintNode, "/site");

        Channel channel = blueprint.getPrototypeChannel();
        assertEquals("/hst:hst/hst:sites/blueprint-site", channel.getHstMountPoint());
        assertNull(channel.getHstConfigPath());
        assertNull(channel.getContentRoot());
    }

}
