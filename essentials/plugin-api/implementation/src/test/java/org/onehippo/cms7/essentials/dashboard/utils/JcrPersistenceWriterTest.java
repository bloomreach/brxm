/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.TestPluginContext;
import org.onehippo.cms7.essentials.dashboard.ctx.PluginContext;
import org.onehippo.cms7.essentials.dashboard.model.hst.HstConfiguration;
import org.onehippo.cms7.essentials.dashboard.model.hst.HstSiteMenu;
import org.onehippo.cms7.essentials.dashboard.model.hst.HstSiteMenuItem;
import org.onehippo.cms7.essentials.dashboard.model.hst.HstTemplate;

import static org.junit.Assert.assertNotNull;

/**
 * @version "$Id$"
 */
public class JcrPersistenceWriterTest extends BaseRepositoryTest {


    private Session session;

    private final boolean useHippoSesson = false;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        createHstRootConfig();


    }

    @Test
    public void testWrite() throws Exception {
        final PluginContext context = getContext();

        if (useHippoSesson) {
            ((TestPluginContext) context).setSession(getHippoSession());
        }
        JcrPersistenceWriter writer = new JcrPersistenceWriter(context);
        //############################################
        // POPULATE TREE:
        //############################################
        final HstConfiguration config = new HstConfiguration("mytestconfiguration", "/hst:hst/hst:configurations");
        // template
        final HstTemplate template = config.addTemplate("main.test", "/JSP/somepath.jsp");
        final List<String> containers = new ArrayList<>();
        containers.add("foo");
        containers.add("bar");
        template.setContainers(containers);

        // menu
        final HstSiteMenu myMenu = config.addMenu("myMenu");
        final HstSiteMenuItem menuItem = new HstSiteMenuItem("HOME", "home");
        myMenu.addMenuItem(menuItem);


        //############################################
        //
        //############################################
        final Item item = writer.write(config);
        assertNotNull("Expected saved object", item);
    }
}
