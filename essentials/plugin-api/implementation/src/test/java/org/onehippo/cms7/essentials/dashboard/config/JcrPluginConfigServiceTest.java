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

package org.onehippo.cms7.essentials.dashboard.config;

import javax.jcr.Node;
import javax.jcr.Session;

import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.TestPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.DefaultPluginContext;
import org.onehippo.cms7.essentials.dashboard.utils.GlobalUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @version "$Id: JcrPluginConfigServiceTest.java 174288 2013-08-19 16:21:19Z mmilicevic $"
 */
public class JcrPluginConfigServiceTest extends BaseRepositoryTest {


    private Session mySession;

    @Test
    public void testConfigReadingWriting() throws Exception {

        final Session session = getSession();
        final Node root = session.getRootNode();
        root.addNode("essentials", "essentials:folder");
        final Node dashboard = root.getNode("essentials");
        assertNotNull(dashboard);
        session.save();
        final DefaultPluginContext context = new TestPluginContext(repository, new DummyTestPlugin());
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
        final Session session1 = getContext().createSession();
        session1.getNode("/essentials/plugins/").remove();
        session1.save();
        GlobalUtils.cleanupSession(session1);
        GlobalUtils.cleanupSession(session);
        service.write(document, DummyTestPlugin.class.getName());



    }


}
