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

import javax.jcr.Item;
import javax.jcr.Session;

import org.junit.Before;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseRepositoryTest;
import org.onehippo.cms7.essentials.BaseTest;
import org.onehippo.cms7.essentials.dashboard.model.hst.HstConfiguration;
import org.onehippo.cms7.essentials.dashboard.model.hst.HstTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @version "$Id$"
 */
public class JcrPersistenceWriterTest extends BaseRepositoryTest {

    private static Logger log = LoggerFactory.getLogger(JcrPersistenceWriterTest.class);

    private Session session;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        session = getContext().getSession();
        session.getRootNode().addNode("hst:hst", "hst:hst").addNode("hst:configurations", "hst:configurations");
        session.save();

    }

    @Test
    public void testWrite() throws Exception {
        JcrPersistenceWriter writer = new JcrPersistenceWriter(getContext());
        //############################################
        // POPULATE TREE:
        //############################################
        final HstConfiguration hstConfiguration = new HstConfiguration("mytestconfiguration", "/hst:hst/hst:configurations");
        hstConfiguration.addTemplate(new HstTemplate("main.test", "/JSP/somepath.jsp"));

        //############################################
        //
        //############################################
        final Item config = writer.write(hstConfiguration);

        assertNotNull("Expected saved object", config);
    }
}
