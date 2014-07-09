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

import org.apache.jackrabbit.core.query.lucene.IndexingConfiguration;
import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;

public interface ServicingIndexingConfiguration extends IndexingConfiguration {

    /**
     * Returns <code>true</code> if the property with the given name is a facet
     * according to this configuration.
     *
     * @param propertyName the QName of the property.
     * @return <code>true</code> if the property is facet; <code>false</code>
     *         otherwise.
     */
    boolean isFacet(Name propertyName);

    /**
     * Returns all the QName's of the child nodes that must be aggregated
     *
     * @return all the QName's of the child nodes that must be aggregated
     */
    Name[] getHippoAggregates();
    
    /**
     * @return QName of the hippo:handle
     */
    Name getHippoHandleName();

    /**
     * @return QName of hippo:document
     */
    Name getHippoDocumentName();

    /**
     * @return QName of hippo:translation
     */
    Name getHippoTranslationName();

    /**
     * @return QName of hippo:message
     */
    Name getHippoMessageName();

    String getTranslationMessageFieldName();

    /**
     * @return QName of hippo:translated
     */
    Name getHippoTranslatedName();

    /**
     * @return QName of the hippo:paths property
     */
    Name getHippoPathPropertyName();
    
    /**
     * @return QName of the hippo:text property
     */
    Name getHippoTextPropertyName();
    
    /**
     * Returns <code>true</code> if the property with the given name is a hippo path
     *
     * @param propertyName the QName of the property.
     * @return <code>true</code> if the property is path; <code>false</code>
     *         otherwise.
     */
    boolean isHippoPath(Name propertyName);

    /**
     * Returns whether field is excluded from indexing on node scope
     */
    boolean isExcludedFromNodeScope(String fieldName, NamePathResolver resolver);

    /**
     * Returns whether field should be be indexed as a single term
     */
    boolean isExcludedSingleIndexTerm(String fieldName, NamePathResolver resolver);

    /**
     * Evaluate if the name argument type is of a nodetype which should be aggregates as a child aggregate.
     * Unlike other aggregates, where the properties in searches appear as part of the parent node, the properties
     * are properties are still only in the child node.
     * 
     * @param childType the node type of the child node
     * @return true whether to index all properties in the parent node
     */
    boolean isChildAggregate(Name childType);

    /**
     * @return  the hippo namespace URI 
     */
    String getHippoNamespaceURI();

    Name getSkipIndexName();

}
