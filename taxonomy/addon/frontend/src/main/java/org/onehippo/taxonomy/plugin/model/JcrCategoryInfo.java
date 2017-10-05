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
package org.onehippo.taxonomy.plugin.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.commons.collections.map.LazyMap;
import org.onehippo.taxonomy.api.TaxonomyException;
import org.onehippo.taxonomy.util.TaxonomyUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.onehippo.taxonomy.plugin.api.EditableCategoryInfo;
import org.onehippo.taxonomy.plugin.api.KeyCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_DESCRIPTION;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_NAME;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_SYNONYMS;

public class JcrCategoryInfo extends TaxonomyObject implements EditableCategoryInfo {

    static final Logger log = LoggerFactory.getLogger(JcrCategoryInfo.class);

    private transient String name;

    public JcrCategoryInfo(IModel<Node> nodeModel, boolean editable) {
        super(nodeModel, editable, null);
    }

    public String getName() {
        if (name == null) {
            name = getString(HIPPOTAXONOMY_NAME);
        }
        return name;
    }

    public void setName(String name) throws TaxonomyException {
        checkEditable();

        this.name = name;
        setString(HIPPOTAXONOMY_NAME, name);
        String encoded = "";
        if(StringUtils.isNotBlank(name)) {
            encoded = KeyCodec.encode(name);
        }
        try {
            Node categoryNode = getNode().getParent().getParent();
            if (categoryNode.isNew()) {
                categoryNode.getSession().move(categoryNode.getPath(),
                        categoryNode.getParent().getPath() + "/" + encoded);
            }
        } catch (ItemExistsException ex) {
            log.debug("Could not rename category node; a sibling with name {} already exists", encoded);
        } catch (RepositoryException ex) {
            log.warn("Unable to update node name of category", ex);
        }
    }

    /**
     * @deprecated use {@link #getLocale()} to get the language code from
     */
    @Deprecated
    public String getLanguage() {
        final Locale locale = getLocale();
        if (locale != null) {
            return locale.getLanguage();
        } else {
            return "<unknown>";
        }
    }

    @Override
    public Locale getLocale() {
        try {
            return TaxonomyUtil.toLocale(getNode().getName());
        } catch (RepositoryException e) {
            log.warn("Failed to read name of category info node");
        }
        return null;
    }

    public String getDescription() {
        return getString(HIPPOTAXONOMY_DESCRIPTION, "");
    }

    public void setDescription(String description) throws TaxonomyException {
        checkEditable();
        setString(HIPPOTAXONOMY_DESCRIPTION, description);
    }

    public String[] getSynonyms() {
        return getStringArray(HIPPOTAXONOMY_SYNONYMS);
    }

    public void setSynonyms(String[] synonyms) throws TaxonomyException {
        checkEditable();
        setStringArray(HIPPOTAXONOMY_SYNONYMS, synonyms);
    }

    public Node getNode() throws ItemNotFoundException {
        return super.getNode();
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getProperties() {
        Map<String, Object> props = new HashMap<>();

        return LazyMap.decorate(props, relPath -> {
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
        });
    }

    public String getString(String property, String defaultValue) {
        try {
            Node node = getNode();
            if (node.hasProperty(property)) {
                return node.getProperty(property).getString();
            } else {
                log.debug("Property '{}' does not exist on node at path: {} Returning the default value '{}'", property, getNode().getPath(), defaultValue);
                return defaultValue;
            }
        } catch (RepositoryException ex) {
            log.warn("Failed to retrieve property '" + property + "' from node associated with this category", ex);
        }
        return null;
    }

    public String getString(String property) {
        return getString(property, null);
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
