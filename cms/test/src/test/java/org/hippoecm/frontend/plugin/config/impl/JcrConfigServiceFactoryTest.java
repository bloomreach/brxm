/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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

import org.apache.wicket.protocol.http.mock.MockServletContext;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.PluginPage;
import org.hippoecm.frontend.PluginTest;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.DummyPlugin;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IClusterConfig;
import org.hippoecm.frontend.plugin.config.IPluginConfigService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JcrConfigServiceFactoryTest extends PluginTest {

    private String[] config = new String[] {
        "/config", "nt:unstructured",
            "/config/test-app", "frontend:application",
                "/config/test-app/default", "frontend:plugincluster",
                    "/config/test-app/default/plugin", "frontend:plugin",
                        "plugin.class", DummyPlugin.class.getName(),
                "/config/test-app/service", "frontend:plugincluster",
            "/config/second-app", "frontend:application",
                "/config/second-app/cluster", "frontend:plugincluster",
    };

    @Override
    @Before
    public void setUp() throws Exception {
        setConfig(config);
        super.setUp();
    }

    @Test
    public void testFirstApplicationIsUsedAsFallback() throws Exception {
        PluginTestApplication secondApp = new PluginTestApplication();
        MockServletContext servletContext = new MockServletContext(secondApp, null);
        servletContext.addInitParameter("config", "second-app");
        HippoTester second = new HippoTester(secondApp, servletContext);

        try {
            PluginPage home = (PluginPage) second.startPluginPage();
            JavaPluginConfig config = new JavaPluginConfig("dummy");
            config.put("plugin.class", DummyPlugin.class.getName());
            IPluginContext context = home.getPluginManager().start(config);
            IPluginConfigService pcs = context.getService(IPluginConfigService.class.getName(), IPluginConfigService.class);
            IClusterConfig cluster = pcs.getCluster("service");
            assertEquals(new JcrClusterConfig(new JcrNodeModel("/config/test-app/service")), cluster);
        } finally {
            second.destroy();
        }
    }
}
