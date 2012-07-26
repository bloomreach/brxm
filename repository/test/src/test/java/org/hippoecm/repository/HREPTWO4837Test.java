/*
 *  Copyright 2011 Hippo.
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
package org.hippoecm.repository;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Calendar;
import javax.jcr.Binary;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import org.hippoecm.repository.api.HippoSession;
import org.hippoecm.repository.api.ImportMergeBehavior;
import org.hippoecm.repository.api.ImportReferenceBehavior;
import org.hippoecm.repository.util.Utilities;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class HREPTWO4837Test extends TestCase {

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        while (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        session.getRootNode().addNode("test", "nt:unstructured");
    }

    @After
    @Override
    public void tearDown() throws Exception {
        while (session.getRootNode().hasNode("test")) {
            session.getRootNode().getNode("test").remove();
        }
        super.tearDown();
    }

    @Test
    public void testRecursiveOverride() throws Exception {
        session.importXML("/test", getClass().getResourceAsStream("HREPTWO4837Test.xml"), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
        session.save();
        session.refresh(false);
        assertTrue(session.getRootNode().hasNode("test/aap/noot/noot/noot"));
        ((HippoSession)session).importDereferencedXML("/test", getClass().getResourceAsStream("HREPTWO4837Test.xml"),
                ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, ImportReferenceBehavior.IMPORT_REFERENCE_NOT_FOUND_THROW, ImportMergeBehavior.IMPORT_MERGE_OVERWRITE);
        session.save();
        session.refresh(false);
        assertTrue(session.getRootNode().hasNode("test/aap/noot/noot/noot"));
    }
}
