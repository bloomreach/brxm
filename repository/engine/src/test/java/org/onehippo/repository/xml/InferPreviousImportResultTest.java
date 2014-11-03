/*
 * Copyright 2014 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.repository.xml;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.hippoecm.repository.impl.SessionDecorator;
import org.junit.Test;
import org.onehippo.repository.testutils.RepositoryTestCase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertEquals;

public class InferPreviousImportResultTest extends RepositoryTestCase {

    public void setUp() throws Exception {
        super.setUp();
        session.getRootNode().addNode("test");
    }

    public void tearDown() throws Exception {
        removeNode("/test");
        super.tearDown();
    }

    @Test
    public void testInferPreviousImportResultFromNonDelta() throws Exception {
        final SessionDecorator session = (SessionDecorator) this.session;
        session.importEnhancedSystemViewXML("/test", getClass().getResourceAsStream("/bootstrap/foo.xml"), -1, -1, null);
        final ImportResult importResult = session.inferPreviousImportResult("/test", getClass().getResourceAsStream("/bootstrap/foo.xml"));
        final Document result = getResultDOM(importResult);
        final NodeList newnodes = result.getDocumentElement().getElementsByTagName("newnode");
        assertEquals(1, newnodes.getLength());
        assertEquals(session.getNode("/test/foo").getIdentifier(), newnodes.item(0).getAttributes().getNamedItem("id").getNodeValue());
    }

    private Document getResultDOM(final ImportResult importResult) throws RepositoryException, SAXException, IOException, ParserConfigurationException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        importResult.exportResult(out);
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        System.out.println(out.toString());
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(out.toByteArray()));
    }

    @Test
    public void testInferPreviousImportResultFromDelta() throws Exception {
        final SessionDecorator session = (SessionDecorator) this.session;
        session.getNode("/test").addNode("foo").addNode("bar");
        session.importEnhancedSystemViewXML("/test", getClass().getResourceAsStream("/bootstrap/delta.xml"), -1, -1, null);
        final ImportResult importResult = session.inferPreviousImportResult("/test", getClass().getResourceAsStream("/bootstrap/delta.xml"));
        final Document result = getResultDOM(importResult);
        final NodeList mergednodes = result.getDocumentElement().getElementsByTagName("mergenode");
        assertEquals(1, mergednodes.getLength());
        final Node mergednode = mergednodes.item(0);
        assertEquals(session.getNode("/test/foo/bar").getIdentifier(), mergednode.getAttributes().getNamedItem("id").getNodeValue());
        final NodeList newprops = mergednode.getChildNodes();
        assertEquals(1, newprops.getLength());
        assertEquals("baz", newprops.item(0).getAttributes().getNamedItem("name").getNodeValue());
        final NodeList newnodes = result.getDocumentElement().getElementsByTagName("newnode");
        assertEquals(1, newnodes.getLength());
        assertEquals(session.getNode("/test/foo/bar/baz").getIdentifier(), newnodes.item(0).getAttributes().getNamedItem("id").getNodeValue());
    }
}
