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
package org.onehippo.taxonomy.plugin;

import java.util.List;

import javax.jcr.Node;

import org.apache.wicket.util.io.IClusterable;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.plugin.api.JcrCategoryFilter;

public interface ITaxonomyService extends IClusterable {


    String DEFAULT_SERVICE_TAXONOMY_ID = "service.taxonomy";

    /**
     * Name of the (configuration) property that contains the
     * name of the service that implements ITaxonomyService.
     * If this property is not present, the name to be used is
     * the default registration point (ITaxonomyService.class.getName()).
     */
    String SERVICE_ID = "taxonomy.id";
    
    /**
     * Name of the configuration property that corresponds to
     * the name of the taxonomy to use.  Use this name to achieve
     * cross-plugin consistency of configuration.
     */
    String TAXONOMY_NAME = "taxonomy.name";

    /**
     * Get {@link Taxonomy} instance by the name,
     * selecting the taxonomy document variant based on the configured 'taxonomy.state' property.
     * @param name
     * @return the taxonomy of the specified name.
     */
    Taxonomy getTaxonomy(String name);

    /**
     * Get {@link Taxonomy} instance by the name,
     * based on the given taxonomy document variant node.
     * @param taxonomyDocumentNode
     * @return the taxonomy of the specified name.
     */
    Taxonomy getTaxonomy(Node taxonomyDocumentNode);

    /**
     * @return the list of names of the available taxonomies
     */
    List<String> getTaxonomies();

    /**
     * @return unmodifiable List of categoryFilters, empty is non configured
     */
    List<JcrCategoryFilter> getCategoryFilters();

}
