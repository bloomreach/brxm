/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.tools.rest;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.rest.NodeRestful;
import org.onehippo.cms7.essentials.dashboard.utils.EssentialConst;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @version "$Id$"
 */

public class FSUtilsTest extends BaseRepositoryTest {

    private static Logger log = LoggerFactory.getLogger(FSUtilsTest.class);

    @Test
    public void testTest() throws Exception {
        final Map<String,Object> placeholderData = getContext().getPlaceholderData();
        final String freemarkerRoot = (String) placeholderData.get(EssentialConst.PLACEHOLDER_SITE_FREEMARKER_ROOT);
        assertNotNull(freemarkerRoot);

    }
    @Test
    public void testWriting() throws Exception {
        final  PluginContext context = getContext();

        session = context.getSession();
        // create hst nodes
        createHstRootConfig();
        String path = "/hst:hst/hst:configurations/" + getContext().getProjectNamespacePrefix()+"/hst:templates";
        final Node templateNode = session.getNode(path);
        final Node myFirstNode = templateNode.addNode("myFirstNode", "hst:template");
        myFirstNode.setProperty("hst:script", "<#FREEMARKER>");
        final Node mySecondNode = templateNode.addNode("mySecondNode", "hst:template");
        mySecondNode.setProperty("hst:script", "<#FREEMARKER>");
        log.info("templateNode {}", templateNode);
        session.save();
        final NodeRestful scriptNodes = FSUtils.getScriptNodes(context);
        assertEquals(2, scriptNodes.getNodes().size());

        final Map<String, String> map = FSUtils.writeFreemarkerFiles(context, scriptNodes);
        assertEquals(2, map.size());

    }
}
