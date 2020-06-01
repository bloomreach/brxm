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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.PluginPage;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.service.ServiceTracker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PluginContextTest {

    private LogService logService;

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

    public static class NullServiceTestPlugin extends TestPlugin implements ITestService {

        public NullServiceTestPlugin(IPluginContext context, IPluginConfig config) {
            super(context, config);

            context.registerService(this, null);
        }
    }

    public static class ThrowingTestPlugin extends Plugin {

        public ThrowingTestPlugin(IPluginContext context, IPluginConfig config) {
            super(context, config);

            context.registerService(this, "service.test");

            throw new RuntimeException("Deliberate exception for testing");
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

    public static class ServicingTestPlugin extends Plugin {

        public ServicingTestPlugin(IPluginContext context, IPluginConfig config) {
            super(context, config);

            context.registerService(new IClusterable() {}, config.getString("test.id"));
        }
    }

    public static class IdService implements IClusterable {
        private final String id;

        public IdService(final String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
    
    public static class ClusterStartingPlugin extends Plugin {

        public ClusterStartingPlugin(IPluginContext context, IPluginConfig config) {
            super(context, config);
        }

        @Override
        public void start() {
            IPluginContext context = getPluginContext();

            JavaPluginConfig config = new JavaPluginConfig("test");
            config.put("plugin.class", DummyPlugin.class.getName());
            JavaClusterConfig cluster = new JavaClusterConfig();
            cluster.addReference("test.id");
            cluster.addService("test.id");
            cluster.addPlugin(config);
            JavaPluginConfig params = new JavaPluginConfig();
            params.add("test.id", "service.test");

            IClusterControl control = context.newCluster(cluster, params);

            String id = control.getClusterConfig().getString("test.id");
            IClusterable service = new IdService(id);
            context.registerService(service, id);

            control.start();
        }
    }

    static class LogService implements ILogService {

        protected List<String> messages = new LinkedList<String>();

        public void log(String message) {
            messages.add(message);
        }

    }

    protected HippoTester tester;
    private PluginPage home;
    protected IPluginContext context;

    @Before
    public void setUp() {
        tester = new HippoTester();
        home = (PluginPage) tester.startPluginPage();
        JavaPluginConfig config = new JavaPluginConfig("dummy");
        config.put("plugin.class", DummyPlugin.class.getName());
        context = home.getPluginManager().start(config);
        logService = new LogService();
        context.registerService(logService, "service.log");
    }

    @After
    public void teardown() throws Exception {
        logService = null;
        if (tester != null) {
            tester.destroy();
        }
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
    public void testCleanupServiceForwarders() {
        JavaPluginConfig config = new JavaPluginConfig("test");
        config.put("plugin.class", ServicingTestPlugin.class.getName());
        config.put("test.id", "${test.id}");
        JavaClusterConfig cluster = new JavaClusterConfig();
        cluster.addReference("test.id");
        cluster.addService("test.id");
        cluster.addPlugin(config);
        JavaPluginConfig params = new JavaPluginConfig();
        params.add("test.id", "service.test");

        IClusterable service = new IClusterable() {};
        context.registerService(service, "service.test");

        IClusterControl control = context.newCluster(cluster, params);
        control.start();

        assertEquals(2, context.getServices("service.test", IClusterable.class).size());
        String id = context.getReference(context.getServices("service.test", IClusterable.class).get(1)).getServiceId();
        assertNotNull(context.getService(id, IClusterable.class));
        assertEquals(context.getService(id, IClusterable.class), context.getServices("service.test", IClusterable.class).get(1));

        control.stop();

        assertEquals(1, context.getServices("service.test", IClusterable.class).size());
        assertEquals(service, context.getService("service.test", IClusterable.class));
        assertNull(context.getService(id, IClusterable.class));
    }

    @Test
    public void pluginThatStartsClusterWithForwardersIsCleanedUp() {
        JavaPluginConfig config = new JavaPluginConfig("test");
        config.put("plugin.class", ClusterStartingPlugin.class.getName());
        JavaClusterConfig cluster = new JavaClusterConfig();
        cluster.addPlugin(config);

        JavaPluginConfig params = new JavaPluginConfig();

        IClusterControl control = context.newCluster(cluster, params);
        control.start();

        List<IClusterable> services = context.getServices("service.test", IClusterable.class);
        assertEquals(1, services.size());
        String serviceId = null;
        for (IClusterable service : services) {
            if (service instanceof IdService) {
                serviceId = ((IdService) service).getId();
            }
        }

        String id = context.getReference(context.getServices("service.test", IClusterable.class).get(0)).getServiceId();
        control.stop();

        assertEquals(0, context.getServices("service.test", IClusterable.class).size());
        assertNull(context.getService(id, IClusterable.class));
        assertEquals(0, context.getServices(serviceId, IClusterable.class).size());
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

    @Test
    public void nullRegistrationIsCleanedUp() {
        JavaPluginConfig config = new JavaPluginConfig("test");
        config.put("plugin.class", NullServiceTestPlugin.class.getName());
        JavaClusterConfig cluster = new JavaClusterConfig();
        cluster.addPlugin(config);
        IClusterControl control = context.newCluster(cluster, new JavaPluginConfig());
        control.start();

        ITestService testService = context.getService("service.test", ITestService.class);
        assertNotNull(testService);

        String serviceId = context.getReference(testService).getServiceId();
        control.stop();

        assertNull(context.getService(serviceId, ITestService.class));
    }

    public static class DependentServicePlugin extends Plugin {

        IObservable observable;

        public DependentServicePlugin(final IPluginContext context, final IPluginConfig config) {
            super(context, config);
        }

        @Override
        public void start() {
            final IPluginContext context = getPluginContext();

            context.registerService(new IClusterable() {}, "service.test");

            final IObserver observer = new IObserver() {

                private final IPluginContext context = DependentServicePlugin.this.getPluginContext();

                @Override
                public IObservable getObservable() {
                    return observable;
                }

                @Override
                public void onEvent(final Iterator events) {
                }
            };

            context.registerTracker(new ServiceTracker<IClusterable>(IClusterable.class) {

                @Override
                protected void onServiceAdded(final IClusterable service, final String name) {
                    observable = new IObservable() {

                        @Override
                        public void setObservationContext(final IObservationContext<? extends IObservable> context) {
                        }

                        @Override
                        public void startObservation() {
                        }

                        @Override
                        public void stopObservation() {
                        }
                    };
                    context.registerService(observer, IObserver.class.getName());
                }

                @Override
                protected void onRemoveService(final IClusterable service, final String name) {
                    context.unregisterService(observer, IObserver.class.getName());
                    observable = null;
                }
            }, "service.test");

        }

        @Override
        public void stop() {
        }
    }

    @Test
    public void serviceUnregistrationIsImmediateDuringShutdown() throws IOException {
        JavaPluginConfig config = new JavaPluginConfig("test");
        config.put("plugin.class", DependentServicePlugin.class.getName());
        JavaClusterConfig cluster = new JavaClusterConfig();
        cluster.addPlugin(config);
        IClusterControl control = context.newCluster(cluster, new JavaPluginConfig());
        control.start();

        control.stop();

        ObjectOutputStream oos = new ObjectOutputStream(new ByteArrayOutputStream());
        oos.writeObject(context);
    }
}
