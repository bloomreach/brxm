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

import java.util.Set;

import org.apache.jackrabbit.core.query.lucene.IndexingConfiguration;
import org.apache.jackrabbit.spi.Name;

public interface ServicingIndexingConfiguration extends IndexingConfiguration {
    static final String SVN_ID = "$Id$";

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
     * Returns QName of the hippo:handle
     * @return
     */
    Name getHippoHandleName();
    
    /**
     * Returns QName of the hippo:request
     * @return
     */
    Name getHippoRequestName();
    
    /**
     * Returns QName of the hippo:paths property
     * @return
     */
    Name getHippoPathPropertyName();
    
    /**
     * Returns QName of the hippo:text property
     * @return
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
     * Returns the set of all nodescope exluded property names
     *
     */
    public Set<Name> getExcludedFromNodeScope();

    /**
     * Returns the set of all properties which should not be indexed as a single term
     *
     */
    public Set<Name> getExcludePropertiesSingleIndexTerm();

    /**
     * Evaluate if the name argument type is of a nodetype which should be aggregates as a child aggregate.
     * Unlike other aggregates, where the properties in searches appear as part of the parent node, the properties
     * are properties are still only in the child node.
     * 
     * @param childType the node type of the child node
     * @return true whether to index all properties in the parent node
     */
    public boolean isChildAggregate(Name childType);

    /**
     * @return  the hippo namespace URI 
     */
    String getHippoNamespaceURI();

}
