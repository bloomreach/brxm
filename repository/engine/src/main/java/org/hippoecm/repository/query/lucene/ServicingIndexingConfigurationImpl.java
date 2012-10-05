/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.repository.query.lucene;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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
    private Set<Name> excludePropertiesForFacet = new HashSet<Name>();

    /**
     * QName Hippo Path qualified name
     */
    private Name hippoPath;
    
    /**
     * QName Hippo Text qualified name
     */
    private Name hippoText;

    /**
     * QName Hippo Handle qualified name
     */
    private Name hippoHandle;

    /**
     * QName Hippo Request qualified name
     */
    private Name hippoRequest;

    
    /**
     * QName's of all the child node that should be aggregated
     */
    private Name[] hippoAggregates;

    /**
     * The child node types which properties should be indexed within the parent lucene document,
     * but which will not be actual part of the document (i.e they still get their relative path
     * embedded in their name)
     */
    Set<Name> childAggregates = new HashSet<Name>();

    /**
     * set of property names that should not be nodescoped indexed and not tokenized (for example hippo:path)
     */
    private Set<String> excludedFromNodeScope = new HashSet<String>();

    /**
     * set of property names that are not allowed to be indexed as a single term (for example hippostd:content)
     */
    private Set<String> excludedSingleIndexTerms = new HashSet<String>();

    @Override
    public void init(Element config, QueryHandlerContext context, NamespaceMappings nsMappings) throws Exception {
        super.init(config, context, nsMappings);
        NamespaceResolver nsResolver = new OverlayNamespaceResolver(context.getNamespaceRegistry(), getNamespaces(config));
        NameResolver resolver = new ParsingNameResolver(NameFactoryImpl.getInstance(), nsResolver);
        NodeList indexingConfigs = config.getChildNodes();

        List<Name> idxHippoAggregates = new ArrayList<Name>();

        for (int i = 0; i < indexingConfigs.getLength(); i++) {
            Node configNode = indexingConfigs.item(i);
            if (configNode.getNodeName().equals("facets")) {
                NodeList propertyChildNodes = configNode.getChildNodes();
                for (int k = 0; k < propertyChildNodes.getLength(); k++) {
                    Node propertyNode = propertyChildNodes.item(k);
                    if (propertyNode.getNodeName().equals("excludeproperty")) {
                        // get property name
                        Name propName = resolver.getQName(getTextContent(propertyNode));
                        excludePropertiesForFacet.add(propName);
                        log.debug("Added property '" + propName.getNamespaceURI() + ":" + propName.getLocalName() + "' to be indexed as facet.");
                    }
                }
            }
            if (configNode.getNodeName().equals("aggregates")) {
                NodeList nameChildNodes = configNode.getChildNodes();
                for (int k = 0; k < nameChildNodes.getLength(); k++) {
                    Node nodeTypeNode = nameChildNodes.item(k);
                    if (nodeTypeNode.getNodeName().equals("nodetype")) {
                        // get property name
                        Name nodeTypeName = resolver.getQName(getTextContent(nodeTypeNode));
                        idxHippoAggregates.add(nodeTypeName);
                        log.debug("Added nodetype '" + nodeTypeName.getNamespaceURI() + ":" + nodeTypeName.getLocalName() + "' to be indexed as a hippo aggregate.");
                    } else if (nodeTypeNode.getNodeName().equals("childtype")) {
                        // get property name
                        Name nodeTypeName = resolver.getQName(getTextContent(nodeTypeNode));
                        childAggregates.add(nodeTypeName);
                        log.debug("Added childtype '" + nodeTypeName.getNamespaceURI() + ":" + nodeTypeName.getLocalName() + "' to be indexed as a hippo aggregate.");
                    }
                }
            }

            if (configNode.getNodeName().equals("excludefromnodescope")) {
                NodeList nameChildNodes = configNode.getChildNodes();
                for (int k = 0; k < nameChildNodes.getLength(); k++) {
                    Node nodeTypeNode = nameChildNodes.item(k);
                    if (nodeTypeNode.getNodeName().equals("nodetype")) {
                        String nodeTypeName = getTextContent(nodeTypeNode);
                        excludedFromNodeScope.add(nodeTypeName);
                        log.debug("nodetype '" + nodeTypeName + "' will not be indexed in the nodescope.");
                    }
                }
            }
            if (configNode.getNodeName().equals("nosingleindexterm")) {
                NodeList nameChildNodes = configNode.getChildNodes();
                for (int k = 0; k < nameChildNodes.getLength(); k++) {
                    Node nodeTypeProperty = nameChildNodes.item(k);
                    if (nodeTypeProperty.getNodeName().equals("property")) {
                        // get property name
                        String nodeTypePropertyName = getTextContent(nodeTypeProperty);
                        excludedSingleIndexTerms.add(nodeTypePropertyName);
                        log.debug("property '" + nodeTypePropertyName + "' will not be indexed as a single term.");
                    }
                }
            }

            if(configNode.getNodeName().equals("indexnodename")) {
                log.warn("Indexing of node names through 'indexnodename' is deprecated, not used any more" +
                        " and not needed to configure any more ");
            }

        }
        hippoPath = resolver.getQName(HippoNodeType.HIPPO_PATHS);
        hippoText = resolver.getQName(HippoNodeType.HIPPO_TEXT);
        hippoHandle = resolver.getQName(HippoNodeType.NT_HANDLE);
        hippoRequest = resolver.getQName(HippoNodeType.NT_REQUEST);
        
        hippoAggregates = idxHippoAggregates.toArray(new Name[idxHippoAggregates.size()]);
    }

    public boolean isChildAggregate(Name childType) {
        return childAggregates.contains(childType);
    }

    public boolean isFacet(Name propertyName) {
        return !excludePropertiesForFacet.contains(propertyName);
    }

    public boolean isHippoPath(Name propertyName) {
        return hippoPath.equals(propertyName);
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
                namespaces.setProperty(attribute.getName().substring(6), attribute.getValue());
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

    public Name[] getHippoAggregates() {
        return hippoAggregates;
    }

    public Set<String> getExcludedFromNodeScope() {
        return this.excludedFromNodeScope;
    }

    public Set<String> getExcludedSingleIndexTerms() {
        return this.excludedSingleIndexTerms;
    }

    public Name getHippoHandleName() {
        return this.hippoHandle;
    }

    public Name getHippoRequestName() {
        return this.hippoRequest;
    }
    
    public Name getHippoPathPropertyName() {
        return this.hippoPath;
    }
    
    public Name getHippoTextPropertyName() {
        return this.hippoText;
    }

    public String getHippoNamespaceURI() {
        // we know the hippo:handle is of namespace hippo, hence we can take the hippo namespace from here.
        return getHippoHandleName().getNamespaceURI();
    }
}
