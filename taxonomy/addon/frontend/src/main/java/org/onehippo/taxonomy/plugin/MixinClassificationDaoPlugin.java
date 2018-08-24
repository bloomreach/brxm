/*
 * Copyright 2009-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onehippo.taxonomy.plugin;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.map.JcrValueList;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.onehippo.taxonomy.plugin.model.Classification;
import org.onehippo.taxonomy.plugin.model.ClassificationDao;
import org.onehippo.taxonomy.plugin.model.JcrHelper;

/**
 * This plugin provides a data access service to a taxonomy plugin.
 *
 * The multiple property where selected keys are stored is configurable in 'fieldPath' parameter.
 */
public class MixinClassificationDaoPlugin extends Plugin implements ClassificationDao {

    // Name of the multiple property where selected keys are stored
    public static final String CONFIG_FIELD_PATH = "fieldPath";

    /**
     * Constructor
     */
    public MixinClassificationDaoPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, config.getString(SERVICE_ID));
    }

    /**{@inheritDoc} */
    @Override
    public Classification getClassification(final IModel<Node> nodeModel) {
        List<String> values;
        String nodePath = null;
        String canonicalKeyValue = null;
        try {
            final Node node = nodeModel.getObject();
            nodePath = node.getPath();

            if (JcrHelper.isNodeType(node, TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CLASSIFIABLE)) {
                if (node.hasProperty(getFieldPath())) {
                    values = new ArrayList<>(new JcrValueList<String>(
                            new JcrPropertyModel<String>(node.getProperty(getFieldPath())), PropertyType.STRING));
                } else {
                    values = new LinkedList<>();
                }
                if (JcrHelper.isNodeType(node, TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CANONISED)) {
                    if (node.hasProperty(TaxonomyNodeTypes.HIPPOTAXONOMY_CANONICALKEY)) {
                        canonicalKeyValue = node.getProperty(TaxonomyNodeTypes.HIPPOTAXONOMY_CANONICALKEY).getString();
                    }
                }
            } else {
                throw new IllegalStateException("node with path  " + nodePath + " is not classifiable");
            }
        } catch (RepositoryException e) {
            throw new RuntimeException("RepositoryException creating classification object for node with path " + nodePath, e);
        }
        return new Classification(values, nodeModel, canonicalKeyValue);
    }

    /**{@inheritDoc} */
    @Override
    public void save(Classification classification) {
        String nodePath = null;
        final JcrNodeModel id = (JcrNodeModel) classification.getId();
        try {
            final Node node = id.getNode();
            nodePath = node.getPath();

            if (JcrHelper.isNodeType(node, TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CLASSIFIABLE)) {
                List<String> keys = classification.getKeys();
                node.setProperty(getFieldPath(), keys.toArray(new String[keys.size()]));
                if (JcrHelper.isNodeType(node, TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CANONISED)) {
                    String canonKey = classification.getCanonical();
                    // when null, save empty string because it is mandatory at JCR level (see hippotaxonomy.cnd)
                    node.setProperty(TaxonomyNodeTypes.HIPPOTAXONOMY_CANONICALKEY, (canonKey == null) ? "" : canonKey);
                }
            } else {
                throw new IllegalStateException("node with path " + node.getPath() + " is not classifiable");
            }
        } catch (RepositoryException e) {
            throw new RuntimeException("RepositoryException saving classification object for node with path " + nodePath, e);
        }
    }

    /**
     * Get a configured field path, or the default
     */
    protected String getFieldPath() {
        return getPluginConfig().getString(CONFIG_FIELD_PATH, TaxonomyNodeTypes.HIPPOTAXONOMY_KEYS);
    }
}
