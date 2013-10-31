package org.onehippo.cms7.essentials.dashboard.config;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.MemoryRepository;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


/**
 * @version "$Id: JcrPluginConfigServiceTest.java 174288 2013-08-19 16:21:19Z mmilicevic $"
 */
public class JcrPluginConfigServiceTest {

    public static final Credentials CREDENTIALS = new SimpleCredentials("admin", "admin".toCharArray());
    private static Logger log = LoggerFactory.getLogger(JcrPluginConfigServiceTest.class);
    private MemoryRepository repository;
    private Session session;

    @Test
    public void testConfigReadingWriting() throws Exception {
        session = repository.getSession();
        final Node root = session.getRootNode();
        root.addNode("dashboard", "dashboard:folder");
        final Node dashboard = root.getNode("dashboard");
        assertNotNull(dashboard);
        session.save();
        final DashboardPluginContext context = new DashboardPluginContext(session, new DummyTestPlugin());
        PluginConfigService service = new JcrPluginConfigService(context);
        final ConfigDocument document = new PluginConfigDocument("test");
        final ConfigProperty property = new PluginConfigProperty("test");
        property.addValue("test value");
        property.addValue("test value 2");
        document.addProperty(property);
        service.write(document);
        // now read it:
        final ConfigDocument copy = service.read();
        assertEquals(copy.getName(), document.getName());
        assertEquals(copy.getValue("test"), document.getValue("test"));


    }


    @Before
    public void setUp() throws Exception {
        repository = new MemoryRepository();
    }

    @After
    public void tearDown() throws Exception {
        if (repository != null) {
            repository.shutDown();
        }
    }

}
