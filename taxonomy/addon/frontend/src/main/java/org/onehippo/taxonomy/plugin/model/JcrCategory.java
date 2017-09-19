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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.map.LazyMap;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.taxonomy.api.Category;
import org.onehippo.taxonomy.api.CategoryInfo;
import org.onehippo.taxonomy.api.Taxonomy;
import org.onehippo.taxonomy.api.TaxonomyNodeTypes;
import org.onehippo.taxonomy.plugin.ITaxonomyService;
import org.onehippo.taxonomy.plugin.api.EditableCategory;
import org.onehippo.taxonomy.plugin.api.EditableCategoryInfo;
import org.onehippo.taxonomy.plugin.api.EditableTaxonomy;
import org.onehippo.taxonomy.plugin.api.KeyCodec;
import org.onehippo.taxonomy.plugin.api.TaxonomyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_CATEGORYINFO;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_CATEGORYINFOS;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_NAME;

/**
 * Category model object on top of a JCR node of type hippotaxonomy:category.
 */
public class JcrCategory extends TaxonomyObject implements EditableCategory {

    static final Logger log = LoggerFactory.getLogger(JcrCategory.class);

    public JcrCategory(final IModel<Node> nodeModel,
                       final boolean editable,
                       final ITaxonomyService service) throws TaxonomyException {
        super(nodeModel, editable, service);
        try {
            final Node node = getNode();
            if (!node.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY)) {
                throw new TaxonomyException("Node " + node.getPath() + " is not of type " + TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY);
            }
        } catch (RepositoryException re) {
            throw new TaxonomyException("Error accessing node while creating JcrCategory object", re);
        }
    }

    @Override
    public List<EditableCategory> getChildren() {
        final List<EditableCategory> result = new LinkedList<>();
        try {
            final Node node = getNode();
            final String nodePath = node.getPath();
            for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
                Node child = iter.nextNode();
                if (child != null) {
                    try {
                        if (child.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY)) {

                            final JcrCategory category = toCategory(new JcrNodeModel(child), editable);

                            if (applyCategoryFilters(category)) {
                                result.add(category);
                            }
                        }
                    } catch (RepositoryException re) {
                        if (log.isDebugEnabled()) {
                            log.debug("Can't create a child category below " + nodePath, re);
                        }
                        else {
                            log.warn("Can't create a child category below {}, message is {}", nodePath, re.getMessage());
                        }
                    } catch (TaxonomyException te) {
                        log.warn("TaxonomyException: can't create a child category below {}, message is {}" + nodePath, te.getMessage());
                    }
                }
            }
        } catch (RepositoryException ex) {
            log.error("Failure getting category children", ex);
        }
        return result;
    }

    @Override
    public String getName() {
        try {
            return KeyCodec.decode(getNode().getName());
        } catch (RepositoryException e) {
            log.error("Failed to read name from category node", e);
        }
        return "<unknown>";
    }

    @Override
    public Category getParent() {
        try {
            Node node = getNode();
            Node parent = node.getParent();
            if (parent.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY)) {
                return toCategory(new JcrNodeModel(parent), editable);
            }
        } catch (TaxonomyException te) {
            log.error("Parent not accessible", te);
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public List<? extends EditableCategory> getAncestors() {
        List<JcrCategory> ancestors = new LinkedList<>();
        ancestors.add(this);
        try {
            Node node = getNode();
            while (node.getDepth() > 0) {
                Node parent = node.getParent();
                if (parent.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY)) {
                    ancestors.add(toCategory(new JcrNodeModel(parent), editable));
                } else if (parent.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TAXONOMY)) {
                    break;
                }
                node = parent;
            }
            Collections.reverse(ancestors);
        } catch (TaxonomyException te) {
            log.error("Can't create accurate list of ancestors", te.getMessage());
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return ancestors;
    }

    @Override
    public String getPath() {
        List<? extends Category> ancestors = getAncestors();
        StringBuilder path = new StringBuilder();
        for (Category ancestor : ancestors) {
            path.append(ancestor.getName());
            path.append("/");
        }
        return path.toString();
    }

    @Override
    public JcrTaxonomy getTaxonomy() {
        try {
            Node node = getNode();
            while (node.getDepth() > 0) {
                Node parent = node.getParent();
                if (parent.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_TAXONOMY)) {
                    return toTaxonomy(new JcrNodeModel(parent), editable);
                }
                node = parent;
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @Override
    public String getKey() {
        try {
            Node node = getNode();
            return node.getProperty(TaxonomyNodeTypes.HIPPOTAXONOMY_KEY).getString();
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    /**
     * @deprecated use {@link #getInfo(Locale)} instead
     */
    @Override
    @Deprecated
    public EditableCategoryInfo getInfo(final String language) {
        return getInfo(LocaleUtils.toLocale(language));
    }

    @Override
    public EditableCategoryInfo getInfo(final Locale locale) {
        final String nodeName = JcrHelper.getNodeName(locale);
        try {
            Node node = getNode();
            if (node.hasNode(HIPPOTAXONOMY_CATEGORYINFOS)) {
                final Node infoNodes = node.getNode(HIPPOTAXONOMY_CATEGORYINFOS);
                if (infoNodes.hasNode(nodeName)) {
                    final Node infoNode = infoNodes.getNode(nodeName);
                    return new JcrCategoryInfo(new JcrNodeModel(infoNode), editable);
                }
            }
            if (editable) {
                final Node infoNodes;
                if (!node.hasNode(HIPPOTAXONOMY_CATEGORYINFOS)) {
                    infoNodes = node.addNode(HIPPOTAXONOMY_CATEGORYINFOS, HIPPOTAXONOMY_CATEGORYINFOS);
                } else {
                    infoNodes = node.getNode(HIPPOTAXONOMY_CATEGORYINFOS);
                }
                Node localeNode = infoNodes.addNode(nodeName, HIPPOTAXONOMY_CATEGORYINFO);
                localeNode.setProperty(HIPPOTAXONOMY_NAME, NodeNameCodec.decode(node.getName()));
                return new JcrCategoryInfo(new JcrNodeModel(localeNode), true);
            } else {
                return new EditableCategoryInfo() {

                    public void setDescription(String description) throws TaxonomyException {
                    }

                    public void setName(String name) throws TaxonomyException {
                    }

                    public void setSynonyms(String[] synonyms) throws TaxonomyException {
                    }

                    public Node getNode() throws ItemNotFoundException {
                        return null;
                    }

                    public String getDescription() {
                        return "";
                    }

                    public String getLanguage() {
                        return getLocale().getLanguage();
                    }

                    @Override
                    public Locale getLocale() {
                        return locale;
                    }

                    public String getName() {
                        return JcrCategory.this.getName();
                    }

                    public String[] getSynonyms() {
                        return new String[0];
                    }

                    public Map<String, Object> getProperties() {
                        return Collections.emptyMap();
                    }

                    public String getString(String property) {
                        return "";
                    }

                    public String getString(String property, String defaultValue) {
                        return "";
                    }

                    public String[] getStringArray(String property) {
                        return new String[0];
                    }

                    public void setString(String property, String value) throws TaxonomyException {
                    }

                    public void setStringArray(String property, String[] values) throws TaxonomyException {
                    }
                };
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, ? extends CategoryInfo> getInfos() {
        Map<String, ? extends CategoryInfo> map = new HashMap<>();
        return LazyMap.decorate(map,
                new Transformer() {
                    @Override
                    public Object transform(Object language) {
                        return getInfo((Locale) language); // TODO: check this, must it be Locale?
                    }
                });
    }

    @Override
    public boolean equals(Object obj) {
         if (obj instanceof JcrCategory) {
            return ((JcrCategory) obj).getNodeModel().equals(getNodeModel());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getNodeModel().hashCode() ^ 9887;
    }

    @Override
    @Deprecated
    public JcrCategory addCategory(String key, String name, String locale, final IModel<Taxonomy> taxonomyModel) throws TaxonomyException {
        return addCategory(key, name, LocaleUtils.toLocale(locale), taxonomyModel);
    }

    @Override
    public JcrCategory addCategory(final String key, final String name, Locale locale,
                                   final IModel<Taxonomy> taxonomyModel) throws TaxonomyException {
        try {
            final JcrCategory category = createCategory(getNode(), key, name, locale);
            taxonomyModel.detach();
            return category;
        } catch (RepositoryException e) {
            throw new TaxonomyException("Could not create category with key " + key +
                    ", name " + name + " and locale " + locale, e);
        }
    }

    @Override
    public void remove() throws TaxonomyException {
        checkEditable();

        try {
            getNode().remove();
        } catch (RepositoryException e) {
            throw new TaxonomyException("Could not remove category", e);
        }
    }

    @Override
    public boolean canMoveUp() {
        int index = -1;
        int count = 0;

        try {
            final Node self = getNode();
            final Node parent = self.getParent();
            Node sibling;
            for (NodeIterator nodeIt = parent.getNodes(); nodeIt.hasNext(); ) {
                sibling = nodeIt.nextNode();
                if (sibling != null && sibling.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY)) {
                    if (sibling.isSame(self)) {
                        index = count;
                        return index > 0;
                    }
                    count++;
                }
            }
        } catch (RepositoryException e) {
            log.error("Could not determine the category node can be moved up.", e);
        }

        return false;
    }

    @Override
    public boolean moveUp() throws TaxonomyException {
        try {
            Node prevSibling = null;
            final Node self = getNode();
            final Node parent = self.getParent();
            Node sibling;
            for (NodeIterator nodeIt = parent.getNodes(); nodeIt.hasNext(); ) {
                sibling = nodeIt.nextNode();
                if (sibling != null && sibling.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY)) {
                    if (!sibling.isSame(self)) {
                        prevSibling = sibling;
                    } else {
                        if (prevSibling == null) {
                            return false;
                        } else {
                            parent.orderBefore(
                                    StringUtils.substringAfterLast(self.getPath(), "/"),
                                    StringUtils.substringAfterLast(prevSibling.getPath(), "/"));
                            return true;
                        }
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Could not move up the category node.", e);
        }

        return false;
    }

    @Override
    public boolean canMoveDown() {
        int index = -1;
        int count = 0;

        try {
            final Node self = getNode();
            final Node parent = self.getParent();
            Node sibling;
            for (NodeIterator nodeIt = parent.getNodes(); nodeIt.hasNext(); ) {
                sibling = nodeIt.nextNode();
                if (sibling != null && sibling.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY)) {
                    if (sibling.isSame(self)) {
                        index = count;
                    }
                    count++;
                }
            }
        } catch (RepositoryException e) {
            log.error("Could not determine the category node can be moved down.", e);
        }

        return index != -1 && index < count - 1;
    }

    @Override
    public boolean moveDown() throws TaxonomyException {
        try {
            final Node self = getNode();
            boolean selfFound = false;
            final Node parent = self.getParent();
            Node sibling;
            for (NodeIterator nodeIt = parent.getNodes(); nodeIt.hasNext(); ) {
                sibling = nodeIt.nextNode();
                if (sibling != null && sibling.isNodeType(TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY)) {
                    if (sibling.isSame(self)) {
                        selfFound = true;
                    } else if (selfFound) {
                        parent.orderBefore(
                                StringUtils.substringAfterLast(sibling.getPath(), "/"),
                                StringUtils.substringAfterLast(self.getPath(), "/"));
                        return true;
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error("Could not move down the category node.", e);
        }

        return false;
    }

    @Override
    public void move(EditableCategory destCategory) throws TaxonomyException {
        try {
            Node srcNode = getNode();
            Node destParentNode = ((JcrCategory) destCategory).getNode();
            String destNodePath = StringUtils.removeEnd(destParentNode.getPath(), "/") + "/" + srcNode.getName();
            srcNode.getSession().move(srcNode.getPath(), destNodePath);
        } catch (RepositoryException e) {
            throw new TaxonomyException("Could not move category", e);
        }
    }

    @Override
    public void move(EditableTaxonomy taxonomy) throws TaxonomyException {
        try {
            Node srcNode = getNode();
            Node destParentNode = ((JcrTaxonomy) taxonomy).getNode();
            String destNodePath = StringUtils.removeEnd(destParentNode.getPath(), "/") + "/" + srcNode.getName();
            srcNode.getSession().move(srcNode.getPath(), destNodePath);
        } catch (RepositoryException e) {
            throw new TaxonomyException("Could not move category", e);
        }
    }
}
