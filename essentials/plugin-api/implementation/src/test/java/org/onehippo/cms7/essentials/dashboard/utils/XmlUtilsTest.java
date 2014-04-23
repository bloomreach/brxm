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

package org.onehippo.cms7.essentials.dashboard.utils;

import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.onehippo.cms7.essentials.BaseResourceTest;
import org.onehippo.cms7.essentials.dashboard.utils.xml.XmlNode;
import org.onehippo.cms7.essentials.dashboard.utils.xml.XmlProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * @version "$Id$"
 */
public class XmlUtilsTest extends BaseResourceTest {

    private static Logger log = LoggerFactory.getLogger(XmlUtilsTest.class);

    @Test
    public void testParsingProperties() throws Exception {

        final InputStream resourceAsStream = getClass().getResourceAsStream("/test_document_type.xml");
        final XmlNode documentNode = XmlUtils.parseXml(resourceAsStream);
        assertNotNull(documentNode);
        final Collection<XmlNode> templates = documentNode.getTemplates();
        assertEquals(4, templates.size());

    }

    @Test
    public void testFindingDocuments() throws Exception {

        final List<XmlNode> templateDocuments = XmlUtils.findTemplateDocuments(getProjectRoot(), getContext());
        // NOTE: one ben is not mapped
        final int expected = NAMESPACES_TEST_SET.size() - 1;
        assertEquals("expected " + expected + " templates", expected, templateDocuments.size());
    }

    @Test
    public void testParseXml() throws Exception {
        final List<XmlNode> templateDocuments = XmlUtils.findTemplateDocuments(getProjectRoot(), getContext());

        final XmlNode xmlNode = findPluginNode(templateDocuments);
        final String xml = XmlUtils.xmlNodeToString(xmlNode);
        assertNotNull(xml);
        final XmlNode myXml = XmlUtils.parseXml(IOUtils.toInputStream(xml));
        assertNotNull(myXml);
    }

    @Test
    public void testConvertToString() throws Exception {
        final List<XmlNode> templateDocuments = XmlUtils.findTemplateDocuments(getProjectRoot(), getContext());

        final XmlNode xmlNode = findPluginNode(templateDocuments);
        final String xml = XmlUtils.xmlNodeToString(xmlNode);
        assertNotNull(xml);
        final Collection<XmlNode> prototypeNode = xmlNode.getTemplates();
        for (XmlNode node : prototypeNode) {

            log.info("node {}", node.getName());
        }
        final XmlProperty supertypeProperty = xmlNode.getSupertypeProperty();
        assertNotNull(supertypeProperty);
        final Collection<String> values = supertypeProperty.getValues();
        assertTrue(values.contains("hippoplugins:basedocument"));
        assertTrue(values.contains("hippostd:relaxed"));
        assertTrue(values.contains("hippotranslation:translated"));

    }

    //plugin-api/implementation/src/test/resources/project/content/namespaces/hippoplugins/plugin.xml
    private XmlNode findPluginNode(final Iterable<XmlNode> templateDocuments) {
        for (XmlNode templateDocument : templateDocuments) {
            if (templateDocument.getName().equals("plugin")) {
                return templateDocument;
            }
        }
        return null;
    }
}
