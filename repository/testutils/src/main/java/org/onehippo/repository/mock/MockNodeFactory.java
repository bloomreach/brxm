/*
 *  Copyright 2012-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.repository.mock;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.jcr.RepositoryException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class MockNodeFactory {

    private MockNodeFactory() {
        // prevent instantiation
    }

    /**
     * Creates a {@link MockNode} from system-view XML.
     *
     * @param resourceName the classpath resource name of the XML file (e.g. "/com/example/file.xml" for a file "file.xml"
     *                     located in the test resources package "com.example")
     * @return the node representing the structure in the XML file.
     * @throws IOException when the XML file cannot be read
     * @throws JAXBException the when XML cannot be parsed
     * @throws RepositoryException when the node structure cannot be created
     */
    public static MockNode fromXml(String resourceName) throws IOException, JAXBException, RepositoryException {
        final URL resource = MockNodeFactory.class.getResource(resourceName);
        if (resource == null) {
            throw new IOException("Resource not found: '" + resourceName + "'");
        }
        return fromXml(resource);
    }

    /**
     * Creates a {@link MockNode} from system-view XML URL.
     * 
     * @param resource
     * @return
     * @throws IOException
     * @throws JAXBException
     * @throws RepositoryException
     */
    public static MockNode fromXml(URL resource)  throws IOException, JAXBException, RepositoryException {
        InputStream inputStream = null;

        try {
            inputStream = resource.openStream();
            XmlNode xmlNode = parseXml(inputStream);
            return createMockNode(xmlNode);
        } finally {
            inputStream.close();
        }
    }

    private static XmlNode parseXml(InputStream inputStream) throws JAXBException {
        final JAXBContext jaxbContext = JAXBContext.newInstance(XmlNode.class);
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (XmlNode) unmarshaller.unmarshal(inputStream);
    }

    private static MockNode createMockNode(final XmlNode xmlNode) throws RepositoryException {
        MockNode mockNode = new MockNode(xmlNode.getName());
        for (XmlProperty property : xmlNode.getProperties()) {
            setProperty(mockNode, property);
        }
        for (XmlNode xmlChild : xmlNode.getChildren()) {
            MockNode mockChild = createMockNode(xmlChild);
            mockNode.addNode(mockChild);
        }
        return mockNode;
    }

    private static void setProperty(final MockNode mockNode, final XmlProperty xmlProperty) throws RepositoryException {
        if (xmlProperty.getName().equals("jcr:primaryType")) {
            mockNode.setPrimaryType(xmlProperty.getValue());
            return;
        }

        if (xmlProperty.isMultiple()) {
            mockNode.setProperty(xmlProperty.getName(), xmlProperty.getValues(), xmlProperty.getType());
            return;
        }
        mockNode.setProperty(xmlProperty.getName(), xmlProperty.getValue(), xmlProperty.getType());
    }

}
