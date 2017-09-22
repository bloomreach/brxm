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
package org.onehippo.taxonomy.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.hippoecm.hst.service.AbstractJCRService;
import org.hippoecm.hst.service.Service;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyException;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.onehippo.taxonomy.util.TaxonomyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class TaxonomyImpl extends AbstractJCRService implements Taxonomy {

    static Logger log = LoggerFactory.getLogger(CategoryImpl.class);

    private String name;
    private String path;
    private List<Locale> locales;
    private List<Category> childCategories = new ArrayList<>();
    private List<Category> descendantCategories = new ArrayList<>();


    private Map<String, Category> descendantsByRelPath = new HashMap<>();
    private Map<String, Category> descendantsByKey = new HashMap<>();

    public TaxonomyImpl(Node taxonomy) throws RepositoryException, TaxonomyException {
        super(taxonomy);
        this.name = this.getValueProvider().getName();
        this.path = this.getValueProvider().getPath();
        this.locales = TaxonomyUtil.getLocalesList(this.getValueProvider().getStrings(TaxonomyNodeTypes.HIPPOTAXONOMY_LOCALES));

        NodeIterator nodes = taxonomy.getNodes();
        while(nodes.hasNext()) {
            Node childItem = nodes.nextNode();
            if(childItem != null) {
                if(childItem.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY)) {
                    Category taxonomyItem = new CategoryImpl(childItem, null, this);
                    this.childCategories.add(taxonomyItem);
                } else {
                    log.warn("Skipping child node below '{}' that is not of type '{}'",
                            this.path, TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY);
                }
                // the descendant items are added in the constructor of TaxonomyItemImpl
            }
        }
        populateMaps();
    }

    private void populateMaps() {
        for(Category descendant : this.descendantCategories) {
            this.descendantsByRelPath.put(descendant.getPath(), descendant);
            this.descendantsByKey.put(descendant.getKey(), descendant);
        }
    }

    public List<Category> getCategories() {
        return Collections.unmodifiableList(childCategories);
    }

    public List<Category> getDescendants() {
        return Collections.unmodifiableList(descendantCategories);
    }

    public void addDescendantItem(Category descendant) {
        this.descendantCategories.add(descendant);
    }

    public String getName() {
        return this.name;
    }

    public String getPath() {
        return this.path;
    }

    /**
     * @deprecated use {@link #getLocaleObjects()} instead
     */
    @Deprecated
    public String [] getLocales() {
        return getLocaleObjects().stream().map(Locale::getLanguage).toArray(String[]::new);
    }

    @Override
    public List<Locale> getLocaleObjects() {
        return locales;
    }

    public Category getCategory(String relPath) {
        return this.descendantsByRelPath.get(relPath);
    }

    public Category getCategoryByKey(String uuid) {
        return this.descendantsByKey.get(uuid);
    }

    public Service[] getChildServices() {
        return childCategories.toArray(new Service[childCategories.size()]);
    }


}
