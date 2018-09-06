/*
 *  Copyright 2008-2018 Hippo B.V. (http://www.onehippo.com)
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
package org.hippoecm.frontend.plugins.cms.browse.service;

import java.util.Iterator;
import java.util.stream.Stream;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.model.BranchIdModel;
import org.hippoecm.frontend.model.IChangeListener;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.ModelReference;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.model.BrowserSections;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection.DocumentCollectionType;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollectionModel;
import org.hippoecm.frontend.plugins.cms.browse.service.IBrowserSection.Match;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.util.JcrUtils;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_ID;
import static org.hippoecm.repository.api.HippoNodeType.HIPPO_PROPERTY_BRANCH_NAME;
import static org.hippoecm.repository.standardworkflow.DocumentVariant.MASTER_BRANCH_ID;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.PUBLISHED;
import static org.hippoecm.repository.util.WorkflowUtils.Variant.UNPUBLISHED;
import static org.hippoecm.repository.util.WorkflowUtils.getDocumentVariantNode;

/**
 * An implementation of IBrowseService that also exposes the document model service.
 * <p>
 * The IBrowseService interface should be used by plugins that do not form
 * part of the "browser".  The model services should be used by plugins that do.
 * <p>
 * The document model is always a JcrNodeModel instance, though the
 * nodes may not exist.  When the document node is null, this implies that no
 * document is selected from the folder.  Setting the folder node to null is
 * not supported.
 */
public class BrowseService implements IBrowseService<IModel<Node>>, IDetachable {

    private static final Logger log = LoggerFactory.getLogger(BrowseService.class);
    private final DocumentCollectionModel collectionModel;
    private final DocumentModelService documentService;
    private final BrowserSections sections;
    private final IPluginContext context;
    private FolderModelService folderService;

    public BrowseService(final IPluginContext context, final IPluginConfig config, final JcrNodeModel document) {
        documentService = new DocumentModelService(config);
        documentService.init(context);
        this.context = context;

        collectionModel = new DocumentCollectionModel(null);

        if (config.containsKey("model.folder")) {
            folderService = new FolderModelService(config);
            folderService.init(context);
            context.registerService(new IObserver() {

                public IObservable getObservable() {
                    return collectionModel;
                }

                public void onEvent(final Iterator events) {
                    if (collectionModel.getObject() != null) {
                        folderService.updateModel(collectionModel.getObject().getFolder());
                    }
                }

            }, IObserver.class.getName());
        }

        this.sections = new BrowserSections();
        sections.addListener((IChangeListener) () -> {
            final IBrowserSection active = sections.getActiveSection();
            if (active != null) {
                collectionModel.setObject(active.getCollection());
            }
        });

        final String[] extensions = config.getStringArray("sections");
        for (final String extension : extensions) {
            context.registerTracker(new ServiceTracker<IBrowserSection>(IBrowserSection.class) {

                @Override
                protected void onServiceAdded(final IBrowserSection service, final String name) {
                    sections.addSection(extension, service);
                    super.onServiceAdded(service, name);
                }

                @Override
                protected void onRemoveService(final IBrowserSection service, final String name) {
                    super.onRemoveService(service, name);
                    sections.removeSection(extension);
                }

            }, config.getString(extension));
        }
        context.registerService(this, config.getString(IBrowseService.BROWSER_ID, BrowseService.class.getName()));


        browse(document);
    }

    public IModel<DocumentCollection> getCollectionModel() {
        return collectionModel;
    }

    public BrowserSections getSections() {
        return sections;
    }

    /**
     * Use the supplied model of a Node (or Version) to set folder and document models.
     * When a Version is supplied from the version storage, the physical node is used.
     */
    public void browse(final IModel<Node> model) {
        final IModel<Node> document = getHandleOrFolder(model);
        final Node node = document.getObject();
        if (node == null) {
            return;
        }

        String identifier = "unknown-identifier";
        try {
            identifier = node.getIdentifier();
        } catch (RepositoryException e) {
            log.warn(e.getMessage(), e);
        }

        try {
            final BranchIdModel branchIdModel = new BranchIdModel(context, identifier);
            branchIdModel.setInitialBranchInfo(MASTER_BRANCH_ID, null);

            final String[] branches = JcrUtils.getMultipleStringProperty(node, HippoNodeType.HIPPO_BRANCHES_PROPERTY, new String[0]);
            if (Stream.of(branches).noneMatch(MASTER_BRANCH_ID::equals)) {
                final Node variant = getDocumentVariantNode(node, UNPUBLISHED)
                        .orElseGet(() -> getDocumentVariantNode(node, PUBLISHED).orElse(null));
                if (variant != null) {
                    final String branchId = JcrUtils.getStringProperty(variant, HIPPO_PROPERTY_BRANCH_ID, MASTER_BRANCH_ID);
                    final String branchName = JcrUtils.getStringProperty(variant, HIPPO_PROPERTY_BRANCH_NAME, null);
                    branchIdModel.setBranchInfo(branchId, branchName);
                }
            }
        } catch (RepositoryException e) {
            log.warn(e.getMessage(), e);
        }

        Match closestMatch = null;
        String closestName = null;
        // Get the match for the active section
        final IBrowserSection activeSection = sections.getActiveSection();
        if (activeSection != null) {
            final Match match = activeSection.contains(document);
            if (match != null) {
                closestMatch = match;
                closestName = sections.getActiveSectionName();
            }
        }
        for (final String name : sections.getSections()) {
            final IBrowserSection section = sections.getSection(name);
            final Match match = section.contains(document);
            if (match != null && (closestMatch == null || match.getDistance() < closestMatch.getDistance())) {
                closestMatch = match;
                closestName = name;
            }
        }
        if (closestName != null) {
            final IBrowserSection section = sections.getSection(closestName);
            section.select(document);
            sections.setActiveSectionByName(closestName);
        }
        IModel<Node> version = null;
        try {
            if (model.getObject().isNodeType(JcrConstants.NT_VERSION)) {
                version = model;
            }
        } catch (final RepositoryException ignore) {
        }

        if (collectionModel.getObject() != null
                && collectionModel.getObject().getType() == DocumentCollectionType.FOLDER) {
            if (collectionModel.getObject().getFolder().equals(document)) {
                documentService.updateModel(new JcrNodeModel((Node) null));
            } else {
                documentService.updateModel(version != null ? version : document);
            }
        } else {
            documentService.updateModel(version != null ? version : document);
        }
        onBrowse();
    }

    protected void onBrowse() {
    }

    public void selectSection(final IModel<IBrowserSection> model) {
        documentService.updateModel(model.getObject().getCollection().getFolder());
    }

    public void detach() {
        documentService.detach();
    }

    // retrieve the (unversioned) handle when the node is versioned,
    // the handle when the node is a document variant or the folder otherwise.
    private IModel<Node> getHandleOrFolder(final IModel<Node> model) {
        final Node node = model.getObject();
        if (node != null) {
            try {
                if (node.isNodeType(JcrConstants.NT_VERSION)) {
                    final Node frozen = node.getNode(JcrConstants.JCR_FROZEN_NODE);
                    String uuid = frozen.getProperty(JcrConstants.JCR_FROZEN_UUID).getString();
                    try {
                        final Node docNode = node.getSession().getNodeByIdentifier(uuid);
                        if (docNode.getDepth() > 0) {
                            final Node parent = docNode.getParent();
                            if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                                return new JcrNodeModel(parent);
                            }
                        }
                        return new JcrNodeModel(docNode);
                    } catch (final ItemNotFoundException infe) {
                        // node doesn't exist anymore.  If it's a document, the handle
                        // should still be available though.
                        if (frozen.hasProperty(HippoNodeType.HIPPO_PATHS)) {
                            final Value[] ancestors = frozen.getProperty(HippoNodeType.HIPPO_PATHS).getValues();
                            if (ancestors.length > 1) {
                                uuid = ancestors[1].getString();
                                return new JcrNodeModel(node.getSession().getNodeByIdentifier(uuid));
                            }
                        }
                        throw infe;
                    }
                } else if (node.isNodeType(HippoNodeType.NT_DOCUMENT)) {
                    final Node parent = node.getParent();
                    if (parent.isNodeType(HippoNodeType.NT_HANDLE)) {
                        return new JcrNodeModel(parent);
                    }
                }
            } catch (final RepositoryException ex) {
                log.error(ex.getMessage());
            }
        }
        return model;
    }

    private class DocumentModelService extends ModelReference<Node> {

        DocumentModelService(final IPluginConfig config) {
            super(config.getString("model.document"), new JcrNodeModel((Node) null));
        }

        public void updateModel(final IModel<Node> model) {
            super.setModel(model);
        }

        @Override
        public void setModel(final IModel<Node> model) {
            if (model == null) {
                throw new IllegalArgumentException("invalid model null");
            }
            if (model.getObject() == null) {
                updateModel(model);
            } else {
                browse(model);
            }
        }
    }

    private class FolderModelService extends ModelReference<Node> {

        FolderModelService(final IPluginConfig config) {
            super(config.getString("model.folder"), new JcrNodeModel((Node) null));
        }

        public void updateModel(final IModel<Node> model) {
            super.setModel(model);
        }

        @Override
        public void setModel(final IModel<Node> model) {
            if (model == null) {
                throw new IllegalArgumentException("invalid model null");
            }
            if (model.getObject() == null) {
                updateModel(model);
            } else {
                browse(model);
            }
        }
    }

}
