package org.onehippo.cms7.essentials.dashboard.config;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.dashboard.ctx.DefaultPluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;

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
        final DefaultPluginContext context = new DefaultPluginContext(session, new DummyTestPlugin());
        PluginConfigService service = new JcrPluginConfigService(context);
        final ProjectSettingsBean document = new ProjectSettingsBean("DummyTestPlugin");
        //document.addProperty("test");
        document.setSetupDone(true);
        document.setParentPath(GlobalUtils.getParentConfigPath(DummyTestPlugin.class.getName()));
        document.setSelectedBeansPackage("beanspackage");
        document.setSelectedComponentsPackage("comppackage");
        document.setProjectNamespace("projectns");
        document.setSelectedRestPackage("rest");
        document.setSetupDone(true);
        service.write(document);
        // now read it:
        final ProjectSettingsBean copy = service.read(DummyTestPlugin.class.getName(), ProjectSettingsBean.class);
        assertEquals("DummyTestPlugin", copy.getName());
        //assertEquals(copy.getProperties().get(0), "test");
        assertTrue("Expected setup to be done", copy.getSetupDone());
        // delete:
        getContext().getSession().getNode("/essentials/plugins/").remove();
        getContext().getSession().save();
        service.write(document, DummyTestPlugin.class.getName());




    }



}
