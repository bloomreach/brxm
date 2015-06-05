/*
 *  Copyright 2009-2015 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.taxonomy.plugin.model;

import java.util.Iterator;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservationContext;
import org.hippoecm.frontend.model.ocm.JcrObject;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoSession;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.onehippo.taxonomy.plugin.ITaxonomyService;
import org.onehippo.taxonomy.plugin.api.JcrCategoryFilter;
import org.onehippo.taxonomy.plugin.api.KeyCodec;
import org.onehippo.taxonomy.plugin.api.TaxonomyException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaxonomyObject extends JcrObject {

    static final Logger log = LoggerFactory.getLogger(TaxonomyObject.class);

    protected final boolean editable;

    private final ITaxonomyService taxonomyService;

    /**
     * Constructor
     */
    public TaxonomyObject(final IModel<Node> nodeModel,
                          final boolean editable,
                          final ITaxonomyService service) {
        super(nodeModel);
        this.editable = editable;
        this.taxonomyService = service;
    }

    /**
     * Get the JCR node this model is built on.
     */
    public Node getJcrNode() throws ItemNotFoundException {
        return this.getNode();
    }

    protected void checkEditable() throws TaxonomyException {
        if (!editable) {
            throw new TaxonomyException(this + " is not editable");
        }
    }

    /**
     * Apply category filtering if applicable.
     */
    protected boolean applyCategoryFilters(final JcrCategory category) throws RepositoryException {

        // do not apply while editing
        if (editable) {
            return true;
        }

        if (taxonomyService == null) {
            return true;
        }

        if (taxonomyService.getCategoryFilters().isEmpty()) {
            return true;
        }

        final HippoSession hippoSession = (HippoSession) category.getJcrNode().getSession();

        for (JcrCategoryFilter filter : taxonomyService.getCategoryFilters()) {
            if (!filter.apply(category, hippoSession)) {
                log.debug("Skipping category '{}' because filtered by {}", category.getPath(), filter.getClass().getName());
                return false;
            }
        }
        return true;
    }

    protected Node getNode() throws ItemNotFoundException {
        return super.getNode();
    }

    protected JcrCategory createCategory(Node parent, String key, String name, String locale) throws RepositoryException, TaxonomyException {
        String encoded = KeyCodec.encode(name);

        String categoryName = encoded;
        int index = 0;
        while (parent.hasNode(categoryName)) {
            categoryName = encoded + "_" + (++index);
        }
        Node taxonomyNode = parent;
        while (!taxonomyNode.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TAXONOMY)) {
            taxonomyNode = taxonomyNode.getParent();
        }
        JcrTaxonomy taxonomy = toTaxonomy(new JcrNodeModel(taxonomyNode), true);
        if (taxonomy.getCategoryByKey(key) != null) {
            throw new TaxonomyException("Key " + key + " already exists");
        }

        Node child = parent.addNode(categoryName, TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY);
        child.setProperty(TaxonomyNodeTypes.HIPPOTAXONOMY_KEY, key);

        if (locale != null) {
            if (!JcrHelper.isNodeType(child, TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TRANSLATED)) {
                child.addMixin(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TRANSLATED);
            }
            Node translationChildNode = child.addNode(TaxonomyNodeTypes.HIPPOTAXONOMY_TRANSLATION,
                    TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TRANSLATION);
            translationChildNode.setProperty(HippoNodeType.HIPPO_LANGUAGE, locale);
            translationChildNode.setProperty(HippoNodeType.HIPPO_MESSAGE, name);
        }

        return toCategory(new JcrNodeModel(child), true);
    }

    // Factory methods to subclass, e.g. to customize node creation.

    protected JcrTaxonomy toTaxonomy(JcrNodeModel nodeModel, boolean editable) {
        return new JcrTaxonomy(nodeModel, editable, getTaxonomyService());
    }

    protected JcrCategory toCategory(JcrNodeModel nodeModel, boolean editable) throws TaxonomyException {
        return new JcrCategory(nodeModel, editable, getTaxonomyService());
    }

    protected ITaxonomyService getTaxonomyService() {
        return taxonomyService;
    }

    // Observable callback

    @SuppressWarnings("unchecked")
    @Override
    protected void processEvents(IObservationContext context, Iterator<? extends IEvent> events) {
        // TODO Auto-generated method stub
    }
}
