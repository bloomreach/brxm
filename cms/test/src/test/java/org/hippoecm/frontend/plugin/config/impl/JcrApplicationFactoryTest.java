/*
 *  Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugin.config.impl;

import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.PluginPage;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.plugin.DummyPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JcrApplicationFactoryTest extends PluginTest {

    private String[] config = new String[] {
        "/config", "nt:unstructured",
            "/config/test-app", "frontend:application",
                "/config/test-app/foo", "nt:unstructured",
                "/config/test-app/default", "frontend:plugincluster",
                    "/config/test-app/default/plugin", "frontend:plugin",
                        "plugin.class", DummyPlugin.class.getName(),
                "/config/test-app/service", "frontend:plugincluster"
    };

    @Override
    @Before
    public void setUp() throws Exception {
        setConfig(config);
        super.setUp();
    }

    @Test
    public void testDefaultIsUsedAsFallbackDefaultCluster() throws Exception {
        IPluginConfigService pcs = context.getService(IPluginConfigService.class.getName(), IPluginConfigService.class);
        assertEquals("default", pcs.getDefaultCluster().getName());
        // check "foo" actually is first child and (thus) skipped
        assertEquals("foo", session.getNode("/config/test-app").getNodes().nextNode().getName());
    }

    @Test
    public void testDefaultClusterTakesPrecendence() throws Exception {
        session.getNode("/config/test-app").setProperty("frontend:defaultcluster", "service");
        session.save();
        Home home = tester.startPluginPage();
        JavaPluginConfig config = new JavaPluginConfig("dummy");
        config.put("plugin.class", DummyPlugin.class.getName());
        IPluginContext context = ((PluginPage)home).getPluginManager().start(config);
        IPluginConfigService pcs = context.getService(IPluginConfigService.class.getName(), IPluginConfigService.class);
        assertEquals("service", pcs.getDefaultCluster().getName());
    }

    @Test
    public void testInvalidDefaultClusterChild() throws Exception {
        session.getNode("/config/test-app").setProperty("frontend:defaultcluster", "foo");
        session.save();
        Home home = tester.startPluginPage();
        JavaPluginConfig config = new JavaPluginConfig("dummy");
        config.put("plugin.class", DummyPlugin.class.getName());
        IPluginContext context = ((PluginPage)home).getPluginManager().start(config);
        IPluginConfigService pcs = context.getService(IPluginConfigService.class.getName(), IPluginConfigService.class);
        assertEquals("default", pcs.getDefaultCluster().getName());
    }
    @Test
    public void testMissingDefaultClusterChild() throws Exception {
        session.getNode("/config/test-app").setProperty("frontend:defaultcluster", "bar");
        session.save();
        Home home = tester.startPluginPage();
        JavaPluginConfig config = new JavaPluginConfig("dummy");
        config.put("plugin.class", DummyPlugin.class.getName());
        IPluginContext context = ((PluginPage)home).getPluginManager().start(config);
        IPluginConfigService pcs = context.getService(IPluginConfigService.class.getName(), IPluginConfigService.class);
        assertEquals("default", pcs.getDefaultCluster().getName());
    }
}
