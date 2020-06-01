/*
 *  Copyright 2009-2017 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin.api;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;

import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.CategoryInfo;
import org.onehippo.taxonomy.api.TaxonomyException;

public interface EditableCategoryInfo extends CategoryInfo {

    /**
     * Sets translated category name of the {@link Category}
     * @param name
     * @throws TaxonomyException
     */
    void setName(String name) throws TaxonomyException;

    /**
     * Sets translated description of the {@link Category}
     * @param description
     * @throws TaxonomyException
     */
    void setDescription(String description) throws TaxonomyException;

    /**
     * Sets translated synonyms of the {@link Category}
     * @param synonyms
     * @throws TaxonomyException
     */
    void setSynonyms(String[] synonyms) throws TaxonomyException;

    /**
     * Returns the underlying node to which this category is attached.
     * <P>
     * <EM>Note: Please be careful in using this method. This method is provided only to allow
     * custom properties getting/setting. In normal use cases, you'd better use other setter methods.</EM>
     * </P>
     * @return
     * @throws ItemNotFoundException
     */
    Node getNode() throws ItemNotFoundException;

    /**
     * Sets string property to the underlying category node.
     * <P>
     * <EM>Note: Please be careful in using this method. This method is provided only to allow
     * custom properties setting. In normal use cases, you'd better use other setter methods.</EM>
     * </P>
     * @param property
     * @param value
     * @throws TaxonomyException
     */
    void setString(String property, String value) throws TaxonomyException;

    /**
     * Sets string array property to the underlying category node.
     * <P>
     * <EM>Note: Please be careful in using this method. This method is provided only to allow
     * custom properties setting. In normal use cases, you'd better use other setter methods.</EM>
     * </P>
     * @param property
     * @param values
     * @throws TaxonomyException
     */
    void setStringArray(String property, String[] values) throws TaxonomyException;
}
