/*
 *  Copyright 2009-2013 Hippo B.V. (http://www.onehippo.com)
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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.map.LazyMap;
import org.apache.wicket.model.IModel;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.onehippo.taxonomy.plugin.api.EditableCategoryInfo;
import org.onehippo.taxonomy.plugin.api.KeyCodec;
import org.onehippo.taxonomy.plugin.api.TaxonomyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JcrCategoryInfo extends TaxonomyObject implements EditableCategoryInfo {
    private static final long serialVersionUID = 1L;

    static final Logger log = LoggerFactory.getLogger(JcrCategoryInfo.class);

    private transient String name;

    public JcrCategoryInfo(IModel<Node> nodeModel, boolean editable) {
        super(nodeModel, editable, null);
    }

    public String getName() {
        if (name == null) {
            name = getString(HippoNodeType.HIPPO_MESSAGE);
        }
        return name;
    }

    public void setName(String name) throws TaxonomyException {
        checkEditable();

        this.name = name;
        setString(HippoNodeType.HIPPO_MESSAGE, name);
        String encoded = KeyCodec.encode(name);
        try {
            Node categoryNode = getNode().getParent();
            if (categoryNode.isNew()) {
                categoryNode.getSession().move(categoryNode.getPath(),
                        categoryNode.getParent().getPath() + "/" + encoded);
            }
        } catch (ItemExistsException ex) {
            log.debug("Could not rename category node; a sibling with name " + encoded + " already exists");
        } catch (RepositoryException ex) {
            log.warn("Unable to update node name of category", ex);
        }
    }

    public String getLanguage() {
        return getString(HippoNodeType.HIPPO_LANGUAGE);
    }

    public String getDescription() {
        return getString(TaxonomyNodeTypes.HIPPOTAXONOMY_DESCRIPTION, "");
    }

    public void setDescription(String description) throws TaxonomyException {
        checkEditable();
        setString(TaxonomyNodeTypes.HIPPOTAXONOMY_DESCRIPTION, description);
    }

    public String[] getSynonyms() {
        return getStringArray(TaxonomyNodeTypes.HIPPOTAXONOMY_SYNONYMS);
    }

    public void setSynonyms(String[] synonyms) throws TaxonomyException {
        checkEditable();
        setStringArray(TaxonomyNodeTypes.HIPPOTAXONOMY_SYNONYMS, synonyms);
    }

    public Node getNode() throws ItemNotFoundException {
        return super.getNode();
    }

    public String getString(String property, String defaultValue) {
        try {
            Node node = getNode();
            if (node.hasProperty(property)) {
                return node.getProperty(property).getString();
            } else {
                return defaultValue;
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getProperties() {
        Map<String, Object> props = new HashMap<String, Object>();

        return LazyMap.decorate(props, new Transformer() {
            @Override
            public Object transform(Object relPath) {
                try {
                    Node node = getNode();
                    if (node.hasProperty((String) relPath)) {
                        Property prop = node.getProperty((String) relPath);
                        if (prop.getType() == PropertyType.STRING) {
                            if (prop.isMultiple()) {
                                return getStringArray((String) relPath);
                            } else {
                                return getString((String) relPath);
                            }
                        } else {
                            throw new UnsupportedOperationException("The map from JcrCategoryInfo#getProperties() doesn't support non string property values.");
                        }
                    }
                } catch (RepositoryException ex) {
                    log.error(ex.getMessage());
                }
                return null;
            }
        });
    }

    public String getString(String property) {
        try {
            return getNode().getProperty(property).getString();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    public void setString(String property, String value) throws TaxonomyException {
        try {
            getNode().setProperty(property, value);
        } catch (RepositoryException e) {
            throw new TaxonomyException(e);
        }
    }

    public String[] getStringArray(String property) {
        try {
            Node node = getNode();
            if (node.hasProperty(property)) {
                Value[] values = node.getProperty(property).getValues();
                String[] strings = new String[values.length];
                for (int i = 0; i < values.length; i++) {
                    strings[i] = values[i].getString();
                }
                return strings;
            } else {
                return new String[0];
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return new String[0];
    }

    public void setStringArray(String property, String[] values) throws TaxonomyException {
        try {
            getNode().setProperty(property, values);
        } catch (RepositoryException e) {
            throw new TaxonomyException(e);
        }
    }

    @Override
    public void detach() {
        super.detach();
        name = null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JcrCategoryInfo) {
            return ((JcrCategoryInfo) obj).getNodeModel().equals(getNodeModel());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getNodeModel().hashCode() ^ 8803;
    }

}
