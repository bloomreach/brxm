package org.hippoecm.frontend.plugin;

import static org.junit.Assert.*;

import org.apache.wicket.IClusterable;
import org.hippoecm.frontend.HippoTester;
import org.hippoecm.frontend.Home;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaClusterConfig;
import org.hippoecm.frontend.plugin.config.impl.JavaPluginConfig;
import org.hippoecm.frontend.plugin.impl.PluginContext;
import org.junit.Before;
import org.junit.Test;

public class ServiceFactoryTest {

    HippoTester tester;
    Home home;
    IPluginContext context;

    @Before
    public void setUp() {
        tester = new HippoTester();
        home = (Home) tester.startPage(Home.class);
        context = new PluginContext(home.getPluginManager(), new JavaPluginConfig("test"));
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

    public static class TestPlugin implements IPlugin {

        public TestPlugin(IPluginContext context, IPluginConfig config) {
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
