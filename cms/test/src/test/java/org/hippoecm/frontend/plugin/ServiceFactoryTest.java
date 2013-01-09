/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugin;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.PluginPage;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.junit.Before;
import org.junit.Test;

public class ServiceFactoryTest {

    HippoTester tester;
    PluginPage home;
    IPluginContext context;

    @Before
    public void setUp() {
        tester = new HippoTester();
        home = (PluginPage) tester.startPage(PluginPage.class);
        JavaPluginConfig config = new JavaPluginConfig("dummy");
        config.put("plugin.class", DummyPlugin.class.getName());
        context = home.getPluginManager().start(config);
    }

    static class TestService implements IClusterable {

        boolean destroyed = false;
        IPluginContext context;

        TestService(IPluginContext context) {
            this.context = context;
        }

        void destroy() {
            destroyed = true;
        }
    }

    static class TestFactory implements IServiceFactory<TestService> {

        int count = 0;

        public TestService getService(IPluginContext context) {
            count++;
            return new TestService(context);
        }

        public Class<TestService> getServiceClass() {
            return TestService.class;
        }

        public void releaseService(IPluginContext context, TestService service) {
            service.destroy();
            count--;
        }

    }

    @Test
    public void testFactoryCreatesService() throws Exception {
        context.registerService(new TestFactory(), "service.test");
        TestService testService = context.getService("service.test", TestService.class);
        assertNotNull(testService);
    }

    public static class TestPlugin extends Plugin {

        public TestPlugin(IPluginContext context, IPluginConfig config) {
            super(context, config);

            TestService testService = context.getService("service.test", TestService.class);
            assertNotNull(testService);
        }
    }

    @Test
    public void testServiceCleanup() throws Exception {
        TestFactory factory = new TestFactory();
        context.registerService(factory, "service.test");

        JavaClusterConfig clusterConfig = new JavaClusterConfig();
        JavaPluginConfig pluginConfig = new JavaPluginConfig("plugin");
        pluginConfig.put("plugin.class", TestPlugin.class.getName());
        clusterConfig.addPlugin(pluginConfig);

        IClusterControl control = context.newCluster(clusterConfig, new JavaPluginConfig("cluster"));
        control.start();

        assertTrue(factory.count == 1);

        control.stop();

        assertTrue(factory.count == 0);
    }

}
