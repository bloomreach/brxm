/*
 *  Copyright 2009 Hippo.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.IClusterable;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.PluginRequestTarget;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.ServiceTracker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PluginContextTest {

    public interface ITestService extends IClusterable {

        void test();
    }
    
    public interface ILogService extends IClusterable {

        void log(String message);
    }
    
    public static class TestPlugin extends Plugin implements ITestService {

        public TestPlugin(IPluginContext context, IPluginConfig config) {
            super(context, config);

            context.registerService(this, "service.test");
        }

        public void test() {
            getPluginContext().getService("service.log", ILogService.class).log("TestPlugin");
        }
    }

    public static class ThrowingTestPlugin extends Plugin {

        public ThrowingTestPlugin(IPluginContext context, IPluginConfig config) {
            super(context, config);

            context.registerService(this, "service.test");
            
            throw new RuntimeException("exception");
        }
    }

    public static class TestSubclassingPlugin extends TestPlugin {

        boolean initialized = false;
        
        public TestSubclassingPlugin(IPluginContext context, IPluginConfig config) {
            super(context, config);
            initialized = true;
        }

        @Override
        public void test() {
            if (!initialized) {
                throw new RuntimeException("not yet initialized");
            }
        }
    }

    protected HippoTester tester;
    private Home home;
    protected IPluginContext context;
    protected List<String> messages;

    @Before
    public void setUp() {
        tester = new HippoTester();
        home = tester.startPluginPage();
        JavaPluginConfig config = new JavaPluginConfig("dummy");
        config.put("plugin.class", DummyPlugin.class.getName());
        context = home.getPluginManager().start(config);
        messages = new LinkedList<String>();
        context.registerService(new ILogService() {

            public void log(String message) {
                messages.add(message);
            }
            
        }, "service.log");
    }

    @After
    public void teardown() throws Exception {
        messages = null;
        if (tester != null) {
            tester.destroy();
        }
    }

    protected void refreshPage() {
        WebRequestCycle requestCycle = tester.setupRequestAndResponse(true);
        ;
        HippoTester.callOnBeginRequest(requestCycle);
        AjaxRequestTarget target = new PluginRequestTarget(home);
        requestCycle.setRequestTarget(target);

        // process the request target
        tester.processRequestCycle(requestCycle);
    }

    @Test
    public void testCleanupOnStop() {
        JavaPluginConfig config = new JavaPluginConfig("test");
        config.put("plugin.class", TestPlugin.class.getName());
        JavaClusterConfig cluster = new JavaClusterConfig();
        cluster.addPlugin(config);
        IClusterControl control = context.newCluster(cluster, new JavaPluginConfig());
        control.start();
        
        assertNotNull(context.getService("service.test", IClusterable.class));
        String id = context.getReference(context.getService("service.test", IClusterable.class)).getServiceId();
        assertNotNull(context.getService(id, IClusterable.class));
        assertEquals(context.getService(id, IClusterable.class), context.getService("service.test", IClusterable.class));
        
        control.stop();

        assertNull(context.getService("service.test", IClusterable.class));
        assertNull(context.getService(id, IClusterable.class));
    }

    @Test
    public void testCleanupOnThrow() {
        JavaPluginConfig config = new JavaPluginConfig("test");
        config.put("plugin.class", ThrowingTestPlugin.class.getName());
        JavaClusterConfig cluster = new JavaClusterConfig();
        cluster.addPlugin(config);
        IClusterControl control = context.newCluster(cluster, new JavaPluginConfig());
        control.start();
        
        assertNull(context.getService("service.test", IClusterable.class));
        
        control.stop();

        assertNull(context.getService("service.test", IClusterable.class));
    }

    @Test
    public void testServiceIsInvisibleDuringConstruction() {
        ServiceTracker<ITestService> tracker = new ServiceTracker<ITestService>(ITestService.class) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onServiceAdded(ITestService service, String name) {
                service.test();
            }
        };
        context.registerTracker(tracker, "service.test");

        JavaPluginConfig config = new JavaPluginConfig("test");
        config.put("plugin.class", TestSubclassingPlugin.class.getName());
        JavaClusterConfig cluster = new JavaClusterConfig();
        cluster.addPlugin(config);
        IClusterControl control = context.newCluster(cluster, new JavaPluginConfig());
        control.start();

        ITestService testService = context.getService("service.test", ITestService.class);
        assertNotNull(testService);
        testService.test();
    }

}
