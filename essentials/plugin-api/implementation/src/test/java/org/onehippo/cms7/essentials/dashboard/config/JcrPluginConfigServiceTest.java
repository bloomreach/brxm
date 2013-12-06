package org.onehippo.cms7.essentials.dashboard.config;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.xmlbeans.impl.xb.xmlconfig.ConfigDocument;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.MemoryRepository;
import org.onehippo.cms7.essentials.dashboard.ctx.DashboardPluginContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @version "$Id: JcrPluginConfigServiceTest.java 174288 2013-08-19 16:21:19Z mmilicevic $"
 */
public class JcrPluginConfigServiceTest extends BaseRepositoryTest{


    private Session mySession;

    @Test
    public void testConfigReadingWriting() throws Exception {

        final Node root = session.getRootNode();
        root.addNode("essentials", "essentials:folder");
        final Node dashboard = root.getNode("essentials");
        assertNotNull(dashboard);
        session.save();
        final DashboardPluginContext context = new DashboardPluginContext(session, new DummyTestPlugin());
        PluginConfigService service = new JcrPluginConfigService(context);
        final ProjectSettingsBean document = new ProjectSettingsBean("test");
        document.addProperty("test");
        document.setSetupDone(true);
        document.setSelectedBeansPackage("beanspackage");
        document.setSelectedComponentsPackage("comppackage");
        document.setProjectNamespace("projectns");
        document.setSelectedRestPackage("rest");
        document.setSetupDone(true);
        document.setSetupDone(true);
        service.write(document);
        // now read it:
        final ProjectSettingsBean copy = service.read();
        assertEquals(copy.getName(), document.getName());
        assertEquals(copy.getProperties().get(0), "test");
        assertTrue("Expected setup to be done", copy.getSetupDone());



    }



}
