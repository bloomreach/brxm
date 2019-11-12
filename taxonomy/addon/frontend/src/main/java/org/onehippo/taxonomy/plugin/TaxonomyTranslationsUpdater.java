/*
 *  Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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

import java.util.UUID;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.HippoVersionManager;
import org.hippoecm.repository.util.JcrUtils;
import org.hippoecm.repository.util.NodeIterable;
import org.hippoecm.repository.util.OverwritingCopyHandler;
import org.hippoecm.repository.util.PropInfo;
import org.hippoecm.repository.util.PropertyIterable;
import org.onehippo.repository.update.BaseNodeUpdateVisitor;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_LANGUAGE;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_MESSAGE;
import static org.onehippo.repository.util.JcrConstants.JCR_ROOT_VERSION;
import static org.onehippo.repository.util.JcrConstants.JCR_UUID;
import static org.onehippo.repository.util.JcrConstants.MIX_VERSIONABLE;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_CATEGORYINFO;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_CATEGORYINFOS;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.HIPPOTAXONOMY_NAME;
import static org.onehippo.taxonomy.api.TaxonomyNodeTypes.NODETYPE_HIPPOTAXONOMY_CATEGORY;

public class TaxonomyTranslationsUpdater extends BaseNodeUpdateVisitor {

    private Session session;
    private HippoVersionManager versionManager;

    @Override
    public void initialize(final Session session) throws RepositoryException {
        this.session = session;
        this.versionManager = (HippoVersionManager) session.getWorkspace().getVersionManager();
    }

    @Override
    public boolean doUpdate(final Node taxonomyNode) throws RepositoryException {
        if (taxonomyNode.isNodeType(MIX_VERSIONABLE)) {
            final Node tmpNode = session.getRootNode().addNode(UUID.randomUUID().toString());
            final VersionHistory versionHistory = versionManager.getVersionHistory(taxonomyNode.getPath());
            migrateVersionHistory(tmpNode, versionHistory);
            copy(taxonomyNode, tmpNode);
            updateTaxonomy(tmpNode);
            final String destPath = StringUtils.substringBefore(taxonomyNode.getPath(), "[");
            session.move(tmpNode.getPath(), destPath);
            session.save();
            taxonomyNode.remove();
            session.save();
        } else {
            updateTaxonomy(taxonomyNode);
        }
        return true;
    }

    private void migrateVersionHistory(final Node newTaxonomyNode, final VersionHistory versionHistory)
            throws RepositoryException {
        final VersionIterator versions = versionHistory.getAllVersions();
        while (versions.hasNext()) {
            final Version version = versions.nextVersion();
            if (!version.getName().equals(JCR_ROOT_VERSION)) {
                copy(version.getFrozenNode(), newTaxonomyNode);
                updateTaxonomy(newTaxonomyNode);
                session.save();
                versionManager.checkin(newTaxonomyNode.getPath(), version.getCreated());
                versionManager.checkout(newTaxonomyNode.getPath());
            }
        }
    }

    private void copy(final Node srcNode, final Node destNode) throws RepositoryException {
        JcrUtils.copyTo(srcNode, new OverwritingCopyHandler(destNode) {
            @Override
            public void setProperty(final PropInfo prop) throws RepositoryException {
                final String name = prop.getName();
                if (name.startsWith("jcr:frozen") || name.startsWith(JCR_UUID) ||
                        name.equals(HippoNodeType.HIPPO_RELATED) ||
                        name.equals(HippoNodeType.HIPPO_COMPUTE) ||
                        name.equals(HippoNodeType.HIPPO_PATHS)) {
                    return;
                }
                super.setProperty(prop);
            }
        });
    }

    private void updateTaxonomy(final Node taxonomyNode) throws RepositoryException {
        traverse(taxonomyNode);
    }

    private void traverse(final Node node) throws RepositoryException {
        for (Node child : new NodeIterable(node.getNodes())) {
            if (child.isNodeType(NODETYPE_HIPPOTAXONOMY_CATEGORY)) {
                updateCategory(child);
            }
        }
    }

    private void updateCategory(final Node categoryNode) throws RepositoryException {
        if (categoryNode.isNodeType("hippotaxonomy:translated")) {
            if (!categoryNode.hasNode(HIPPOTAXONOMY_CATEGORYINFOS)) {
                categoryNode.addNode(HIPPOTAXONOMY_CATEGORYINFOS, HIPPOTAXONOMY_CATEGORYINFOS);
            }
            final Node infoNodes = categoryNode.getNode(HIPPOTAXONOMY_CATEGORYINFOS);
            for (Node translationNode : new NodeIterable(categoryNode.getNodes("hippotaxonomy:translation"))) {
                final String language = translationNode.getProperty(HIPPO_LANGUAGE).getString();
                final Node infoNode;
                if (!infoNodes.hasNode(language)) {
                    infoNode = infoNodes.addNode(language, HIPPOTAXONOMY_CATEGORYINFO);
                } else {
                    infoNode = infoNodes.getNode(language);
                }
                for (Property property : new PropertyIterable(translationNode.getProperties())) {
                    if (property.getName().startsWith("jcr:") || property.getName().equals(HIPPO_LANGUAGE)) {
                        continue;
                    }
                    if (property.getName().equals(HIPPO_MESSAGE)) {
                        infoNode.setProperty(HIPPOTAXONOMY_NAME, property.getValue());
                        continue;
                    }
                    if (property.isMultiple()) {
                        infoNode.setProperty(property.getName(), property.getValues());
                    } else {
                        infoNode.setProperty(property.getName(), property.getValue());
                    }
                }
                translationNode.remove();
            }
            categoryNode.removeMixin("hippotaxonomy:translated");
        }
        traverse(categoryNode);
    }

    @Override
    public boolean undoUpdate(final Node node) throws RepositoryException, UnsupportedOperationException {
        return false;
    }
}
