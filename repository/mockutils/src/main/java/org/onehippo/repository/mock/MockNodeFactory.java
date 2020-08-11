/*
 *  Copyright 2012-2019 Hippo B.V. (http://www.onehippo.com)
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

import org.onehippo.cm.model.impl.GroupImpl;
import org.onehippo.cm.model.impl.ModuleImpl;
import org.onehippo.cm.model.impl.ProjectImpl;
import org.onehippo.cm.model.impl.definition.ContentDefinitionImpl;
import org.onehippo.cm.model.parser.ContentSourceParser;
import org.onehippo.cm.model.source.ResourceInputProvider;
import org.onehippo.cm.model.source.Source;

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
        try (InputStream inputStream = resource.openStream()) {
            XmlNode xmlNode = parseXml(inputStream);
            return createMockNode(xmlNode);
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
        switch(xmlProperty.getName()) {
            case "jcr:primaryType":
                mockNode.setPrimaryType(xmlProperty.getValue());
                return;
            case "jcr:uuid":
                mockNode.setIdentifier(xmlProperty.getValue());
                return;
        }

        if (xmlProperty.isMultiple()) {
            mockNode.setProperty(xmlProperty.getName(), xmlProperty.getValues(), xmlProperty.getType());
            return;
        }
        mockNode.setProperty(xmlProperty.getName(), xmlProperty.getValue(), xmlProperty.getType());
    }

    /**
     * Creates a {@link MockNode} from YAML.
     *
     * @param resourceName the classpath resource name of the YAML file (e.g. "/com/example/file.yaml" for a file "file.yaml"
     *                     located in the test resources package "com.example")
     * @return the node representing the structure in the YAML file.
     * @throws IOException when the YAML file cannot be read
     * @throws RepositoryException when the node structure cannot be created
     */
    public static MockNode fromYaml(final String resourceName) throws IOException, RepositoryException {
        final MockNode root = MockNode.root();
        importYaml(resourceName, root);
        return root;
    }

    public static MockNode fromYaml(final URL url) throws IOException, RepositoryException {
        final MockNode root = MockNode.root();
        importYaml(url, root);
        return root;
    }

    /**
     * Imports a {@link MockNode} defined in YAML below an existing mock node.
     *
     * @param resourceName the classpath resource name of the YAML file (e.g. "/com/example/file.yaml" for a file "file.yaml"
     *                     located in the test resources package "com.example")
     * @param parentNode the node to import the YAML nodes at
     * @return the node representing the structure in the YAML file.
     * @throws IOException when the YAML file cannot be read
     * @throws RepositoryException when the node structure cannot be created
     */
    public static void importYaml(final String resourceName, final MockNode parentNode) throws IOException, RepositoryException {
        final URL resource = MockNodeFactory.class.getResource(resourceName);
        if (resource == null) {
            throw new IOException("Resource not found: '" + resourceName + "'");
        }
        importYaml(resource, parentNode);
    }

    public static void importYaml(final URL resource, final MockNode parentNode) throws IOException, RepositoryException {
        try (InputStream inputStream = resource.openStream()) {
            final ContentSourceParser sourceParser = new ContentSourceParser(new MockResourceInputProvider());
            final ModuleImpl module = new ModuleImpl("mock-module", new ProjectImpl("mock-project", new GroupImpl("mock-group")));
            sourceParser.parse(inputStream, "/import", resource.toString(), module);
            final ContentDefinitionImpl contentDefinition = module.getContentSources().iterator().next().getContentDefinition();
            MockNodeImporter.importNode(contentDefinition.getNode(), parentNode);
        }
    }

    private static class MockResourceInputProvider implements ResourceInputProvider {

        @Override
        public boolean hasResource(final Source source, final String resourcePath) {
            return false;
        }

        @Override
        public InputStream getResourceInputStream(final Source source, final String resourcePath) throws IOException {
            throw new IOException("Mock YAML does not support links to resources");
        }
    }
}
