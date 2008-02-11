/*
 * Copyright 2007 Hippo
 *
 * Licensed under the Apache License, Version 2.0 (the  "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hippoecm.repository.query.lucene;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.jackrabbit.core.nodetype.xml.AdditionalNamespaceResolver;
import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.query.lucene.IndexingConfigurationImpl;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingNameResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;
import org.hippoecm.repository.api.HippoNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class ServicingIndexingConfigurationImpl extends IndexingConfigurationImpl implements ServicingIndexingConfiguration {

    /**
     * The logger instance for this class
     */
    private static final Logger log = LoggerFactory.getLogger(IndexingConfigurationImpl.class);


    /**
     * Set of properties that are configured to be facets
     */
    private Set facetProperties = new HashSet();

    /**
     * QName Hippo Path qualified name
     */
    private Name hippoPath;

    @Override
    public void init(Element config, QueryHandlerContext context, NamespaceMappings nsMappings) throws Exception {
        super.init(config, context, nsMappings);
        NamespaceResolver nsResolver = new AdditionalNamespaceResolver(getNamespaces(config));
        NameResolver resolver = new ParsingNameResolver(NameFactoryImpl.getInstance(), nsResolver);
        NodeList indexingConfigs = config.getChildNodes();
        for (int i = 0; i < indexingConfigs.getLength(); i++) {
            Node configNode = indexingConfigs.item(i);
            if (configNode.getNodeName().equals("facets")) {
                NodeList propertyChildNodes = configNode.getChildNodes();
                for (int k = 0; k < propertyChildNodes.getLength(); k++) {
                    Node propertyNode = propertyChildNodes.item(k);
                    if (propertyNode.getNodeName().equals("property")) {
                        // get property name
                        Name propName = resolver.getQName(getTextContent(propertyNode));
                        facetProperties.add(propName);
                        log.debug("Added property '"+propName.getNamespaceURI()+":"+propName.getLocalName()+"' to be indexed as facet.");
                    }
                }
            }
        }
        hippoPath = resolver.getQName(HippoNodeType.HIPPO_PATHS);
    }

    public boolean isFacet(Name propertyName) {
        if(facetProperties.contains(propertyName)){
          return true;
        }
        // TODO for now, all fields that are possible to index as a facet are index
        // as a facet by the '|| true' part. When the indexing_configuration is maintainable
        // through the repository, we can change this to only index facet properties as facets
        return true;
        //return false;
    }

    public boolean isHippoPath(Name propertyName) {
        if(this.hippoPath.equals(propertyName)){
            return true;
        }
        return false;
    }

    /**
     * Returns the namespaces declared on the <code>node</code>.
     *
     * @param node a DOM node.
     * @return the namespaces
     */
    private Properties getNamespaces(Node node) {
        Properties namespaces = new Properties();
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            if (attribute.getName().startsWith("xmlns:")) {
                namespaces.setProperty(
                        attribute.getName().substring(6), attribute.getValue());
            }
        }
        return namespaces;
    }

    /**
     * @param node a node.
     * @return the text content of the <code>node</code>.
     */
    private static String getTextContent(Node node) {
        StringBuffer content = new StringBuffer();
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n.getNodeType() == Node.TEXT_NODE) {
                content.append(((CharacterData) n).getData());
            }
        }
        return content.toString();
    }

}
