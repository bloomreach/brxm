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
package org.hippoecm.repository.jackrabbit.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import javax.jcr.Binary;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.ValueFactory;

import org.apache.poi.util.IOUtils;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class ExportPackageTest extends RepositoryTestCase {

    @Override
    public void setUp() throws Exception {
        super.setUp();
        final Node test = session.getRootNode().addNode("test");
        final ValueFactory valueFactory = session.getValueFactory();
        final Binary binary = valueFactory.createBinary(new ByteArrayInputStream("test".getBytes()));
        test.setProperty("test", valueFactory.createValue(binary));
        session.save();
    }

    @Test
    public void testExportPackage() throws Exception {
        HippoSession session = (HippoSession) this.session;
        final File file = session.exportEnhancedSystemViewPackage("/test", true);
        final EnhancedSystemViewPackage pckg = EnhancedSystemViewPackage.create(file);
        assertEquals(1, pckg.getBinaries().size());
        final File binary = pckg.getBinaries().values().iterator().next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(new FileInputStream(binary), out);
        assertEquals("test", out.toString());
//        System.out.println(file.getPath());
        session.importEnhancedSystemViewPackage("/test", file,
                ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW,
                ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW,
                ImportMergeBehavior.IMPORT_MERGE_ADD_OR_SKIP);
        assertTrue(session.nodeExists("/test/test"));
//        final Node test = session.getNode("/test/test");
//        session.exportDereferencedView(test.getPath(), System.out, false, true);
    }

}
