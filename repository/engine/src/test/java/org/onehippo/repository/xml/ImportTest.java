/*
 *  Copyright 2010-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.sql.Ref;
import java.util.Arrays;
import java.util.Calendar;

import javax.jcr.Binary;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.onehippo.repository.util.JcrConstants;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import static javax.jcr.ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;
import static org.hippoecm.repository.api.ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onehippo.repository.util.JcrConstants.NT_UNSTRUCTURED;

public class ImportTest extends RepositoryTestCase {

    @Rule public final TemporaryFolder testFolder = new TemporaryFolder();
    private Node test;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        test = session.getRootNode().addNode("test", NT_UNSTRUCTURED);
    }

    @Test
    public void testWhiteSpacesInSmallBinary() throws Exception {
        testWhiteSpacesInBinary(new byte[32]);
    }

    @Test
    public void testWhiteSpacesInLargeBinary() throws Exception {
        testWhiteSpacesInBinary(new byte[1024*125]);
    }

    private void testWhiteSpacesInBinary(byte[] data) throws Exception {
        if(data.length > 255) {
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte)(i &0xff);
                data[i] = (byte)((i>>8) & 0xff);
            }
        } else {
            for (byte i = 0; i < data.length; i++)
                data[i] = i;
        }

        Node resource = test.addNode("resource", "nt:resource");
        Binary binary = session.getValueFactory().createBinary(new ByteArrayInputStream(data));
        resource.setProperty("jcr:data", session.getValueFactory().createValue(binary));
        resource.setProperty("jcr:lastModified", Calendar.getInstance());

        final File file = testFolder.newFile("import.xml");
        final FileOutputStream out = new FileOutputStream(file);
        session.exportSystemView("/test/resource", out, false, false);
        out.close();
        resource.remove();

        FileInputStream in = new FileInputStream(file);
        ((HippoSession)session).importEnhancedSystemViewXML(test.getPath(), in, IMPORT_UUID_CREATE_NEW, IMPORT_REFERENCE_NOT_FOUND_THROW, null);
        in.close();

        assertTrue(test.hasNode("resource"));
        binary = test.getNode("resource").getProperty("jcr:data").getBinary();
        assertEquals(data.length, binary.getSize());
        byte[] compareData = new byte[data.length];
        assertEquals(data.length, binary.read(compareData, 0));
        assertTrue(Arrays.equals(data, compareData));
    }

    @Test
    public void testNoFailureWithFaultyNamespaceDeclaration() throws Exception {
        InputStream in = getClass().getClassLoader().getResourceAsStream("import/faulty-namespace.xml");
        ((HippoSession)session).importEnhancedSystemViewXML("/test", in, IMPORT_UUID_CREATE_NEW, IMPORT_REFERENCE_NOT_FOUND_THROW, null);
    }
}
