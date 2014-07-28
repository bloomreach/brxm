/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.core.query.QueryHandlerContext;
import org.apache.jackrabbit.core.query.lucene.AggregateRule;
import org.apache.jackrabbit.core.query.lucene.IndexingConfigurationImpl;
import org.apache.jackrabbit.core.query.lucene.NamespaceMappings;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
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


    private static final Logger log = LoggerFactory.getLogger(ServicingIndexingConfigurationImpl.class);

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


    private Name hippoDocument;

    private Name hippoTranslation;

    private Name hippoMessage;

    private Name hippoTranslated;

    private String translationMessageFieldName;

    /**
     * QName's of all the child node that should be aggregated
     */
    private Name[] hippoAggregates;

    private AggregateRule[] aggregateRules;

    /**
     * The child node types which properties should be indexed within the parent lucene document,
     * but which will not be actual part of the document (i.e they still get their relative path
     * embedded in their name)
     */
    private final Set<Name> childAggregates = new HashSet<Name>();

    /**
     * Set of property names that should not be node scoped indexed and not tokenized (for example hippo:path)
     */
    private final Set<Name> excludedFromNodeScope = new HashSet<Name>();

    /**
     * Cache of property names that are excluded from indexing on node scope
     */
    private final Map<String, Boolean> isExcludedFromNodeScope = new HashMap<String, Boolean>();

    /**
     * Set of property names that are not allowed to be indexed as a single term (for example hippostd:content)
     */
    private final Set<Name> excludedSingleIndexTerms = new HashSet<Name>();

    /**
     * Cache of property names that are excluded from being indexed as a single term
     */
    private final Map<String, Boolean> isExcludedSingleIndexTerm = new HashMap<String, Boolean>();

    private Name skipIndex;


    @Override
    public void init(Element config, QueryHandlerContext context, NamespaceMappings nsMappings) throws Exception {
        super.init(config, context, nsMappings);
        NamespaceResolver nsResolver = new OverlayNamespaceResolver(context.getNamespaceRegistry(), getNamespaces(config));
        NameResolver nameResolver = new ParsingNameResolver(NameFactoryImpl.getInstance(), nsResolver);
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
                        Name propName = nameResolver.getQName(getTextContent(propertyNode));
                        excludePropertiesForFacet.add(propName);
                        log.debug("Added property '{}:{}' to be excluded from facet index.",
                                propName.getNamespaceURI(), propName.getLocalName());
                    }
                }
            }
            if (configNode.getNodeName().equals("aggregates")) {
                NodeList nameChildNodes = configNode.getChildNodes();
                for (int k = 0; k < nameChildNodes.getLength(); k++) {
                    Node nodeTypeNode = nameChildNodes.item(k);
                    if (nodeTypeNode.getNodeName().equals("nodetype")) {
                        // get property name
                        Name nodeTypeName = nameResolver.getQName(getTextContent(nodeTypeNode));
                        idxHippoAggregates.add(nodeTypeName);
                        log.debug("Added nodetype '{}:{}' to be indexed as a hippo aggregate.",
                                nodeTypeName.getNamespaceURI(), nodeTypeName.getLocalName());
                    } else if (nodeTypeNode.getNodeName().equals("childtype")) {
                        // get property name
                        Name nodeTypeName = nameResolver.getQName(getTextContent(nodeTypeNode));
                        childAggregates.add(nodeTypeName);
                        log.debug("Added childtype '{}:{}' to be indexed as a hippo aggregate.",
                                nodeTypeName.getNamespaceURI(), nodeTypeName.getLocalName());
                    }
                }
            }

            if (configNode.getNodeName().equals("excludefromnodescope")) {
                NodeList nameChildNodes = configNode.getChildNodes();
                for (int k = 0; k < nameChildNodes.getLength(); k++) {
                    Node nodeTypeNode = nameChildNodes.item(k);
                    if (nodeTypeNode.getNodeName().equals("nodetype")) {
                        Name nodeTypeName = nameResolver.getQName(getTextContent(nodeTypeNode));
                        excludedFromNodeScope.add(nodeTypeName);
                        log.debug("nodetype '{}' will not be indexed in the nodescope.", nodeTypeName);
                    }
                }
            }
            if (configNode.getNodeName().equals("nosingleindexterm")) {
                NodeList nameChildNodes = configNode.getChildNodes();
                for (int k = 0; k < nameChildNodes.getLength(); k++) {
                    Node nodeTypeProperty = nameChildNodes.item(k);
                    if (nodeTypeProperty.getNodeName().equals("property")) {
                        Name nodeTypePropertyName = nameResolver.getQName(getTextContent(nodeTypeProperty));
                        excludedSingleIndexTerms.add(nodeTypePropertyName);
                        log.debug("property '{}' will not be indexed as a single term.", nodeTypePropertyName);
                    }
                }
            }

            if(configNode.getNodeName().equals("indexnodename")) {
                log.warn("Indexing of node names through 'indexnodename' is deprecated, not used any more" +
                        " and not needed to configure any more ");
            }

        }
        hippoPath = nameResolver.getQName(HippoNodeType.HIPPO_PATHS);
        hippoText = nameResolver.getQName(HippoNodeType.HIPPO_TEXT);
        hippoHandle = nameResolver.getQName(HippoNodeType.NT_HANDLE);
        hippoDocument = nameResolver.getQName(HippoNodeType.NT_DOCUMENT);
        hippoTranslation = nameResolver.getQName(HippoNodeType.HIPPO_TRANSLATION);
        hippoMessage = nameResolver.getQName(HippoNodeType.HIPPO_MESSAGE);
        hippoTranslated = nameResolver.getQName(HippoNodeType.NT_TRANSLATED);
        skipIndex = nameResolver.getQName(HippoNodeType.NT_SKIPINDEX);
        hippoAggregates = idxHippoAggregates.toArray(new Name[idxHippoAggregates.size()]);
        translationMessageFieldName = nameResolver.getJCRName(hippoTranslation) + "/" + nameResolver.getJCRName(hippoMessage);
        aggregateRules = super.getAggregateRules();
        if (aggregateRules == null) {
            aggregateRules = new AggregateRule[0];
        }

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
        StringBuilder content = new StringBuilder();
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

    @Override
    public boolean isExcludedFromNodeScope(final String fieldName, final NamePathResolver resolver) {
        Boolean result = isExcludedFromNodeScope.get(fieldName);
        if (result == null) {
            result = false;
            for (Name name : excludedFromNodeScope) {
                try {
                    if (resolver.getJCRName(name).equals(fieldName)) {
                        result = true;
                    }
                } catch (NamespaceException e) {
                    log.debug("Failed to resolve jcr name {}", name);
                }
            }
            isExcludedFromNodeScope.put(fieldName, result);
        }
        return result;
    }

    @Override
    public boolean isExcludedSingleIndexTerm(final String fieldName, final NamePathResolver resolver) {
        Boolean result = isExcludedSingleIndexTerm.get(fieldName);
        if (result == null) {
            result = false;
            for (Name name : excludedSingleIndexTerms) {
                try {
                    if (resolver.getJCRName(name).equals(fieldName)) {
                        result = true;
                        break;
                    }
                } catch (NamespaceException e) {
                    log.debug("Failed to resolve jcr name {}", name);
                }
            }
            isExcludedSingleIndexTerm.put(fieldName, result);
        }
        return result;
    }

    public Name getHippoHandleName() {
        return this.hippoHandle;
    }

    @Override
    public Name getHippoDocumentName() {
        return hippoDocument;
    }

    @Override
    public Name getHippoTranslationName() {
        return hippoTranslation;
    }

    @Override
    public Name getHippoMessageName() {
        return hippoMessage;
    }

    @Override
    public Name getHippoTranslatedName() {
        return hippoTranslated;
    }

    @Override
    public String getTranslationMessageFieldName() {
        return translationMessageFieldName;
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

    @Override
    public Name getSkipIndexName() {
        return skipIndex;
    }

    @Override
    public AggregateRule[] getAggregateRules() {
        return aggregateRules;
    }

}
