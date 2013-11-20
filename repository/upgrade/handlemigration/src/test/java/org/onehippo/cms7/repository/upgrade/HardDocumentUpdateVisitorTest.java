/*
 * Copyright 2013 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.cms7.repository.upgrade;

import javax.jcr.Node;

import org.hippoecm.repository.HippoStdNodeType;
import org.hippoecm.repository.api.HippoNodeType;
import org.junit.Before;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.repository.util.JcrConstants;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

public class HardDocumentUpdateVisitorTest extends RepositoryTestCase {

    private Node documents;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        documents = session.getNode("/content/documents");
    }

    @Test
    public void testMigrateDocument() throws Exception {
        final Node folder = documents.addNode("folder", HippoStdNodeType.NT_FOLDER);
        folder.addMixin(HippoNodeType.NT_HARDDOCUMENT);
        session.save();

        folder.checkin();

        final HardDocumentUpdateVisitor migrator = new HardDocumentUpdateVisitor();
        migrator.initialize(session);
        migrator.setLogger(log);
        migrator.doUpdate(folder);

        assertFalse(folder.isNodeType(HippoNodeType.NT_HARDDOCUMENT));
        assertTrue(folder.isNodeType(JcrConstants.MIX_REFERENCEABLE));
    }

}
