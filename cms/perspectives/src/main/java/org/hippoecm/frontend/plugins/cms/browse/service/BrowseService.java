/*
 *  Copyright 2008-2020 Hippo B.V. (http://www.onehippo.com)
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
import org.hippoecm.frontend.model.ObservableModel;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.cms.browse.LastVisited;
import org.hippoecm.frontend.plugins.cms.browse.model.BrowserSections;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollection.DocumentCollectionType;
import org.hippoecm.frontend.plugins.cms.browse.model.DocumentCollectionModel;
import org.hippoecm.frontend.plugins.cms.browse.service.IBrowserSection.Match;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.service.ServiceTracker;
import org.hippoecm.repository.api.HippoNodeType;
import org.onehippo.repository.documentworkflow.DocumentVariant;
import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.HippoNodeType.NT_DOCUMENT;
import static org.hippoecm.repository.api.HippoNodeType.NT_HANDLE;

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
    private final ObservableModel<LastVisited> lastVisitedModel;
    private FolderModelService folderService;

    public BrowseService(final IPluginContext context, final IPluginConfig config, final JcrNodeModel document) {
        this(context, config, document, null);
    }

    public BrowseService(final IPluginContext context, final IPluginConfig config, final JcrNodeModel document, final ObservableModel<LastVisited> lastVisitedModel) {
        documentService = new DocumentModelService(config, context);
        documentService.init(context);

        collectionModel = new DocumentCollectionModel(null);

        this.lastVisitedModel = lastVisitedModel;

        if (config.containsKey("model.folder")) {
            folderService = new FolderModelService(config);
            folderService.init(context);
            context.registerService(new IObserver() {

                @Override
                public IObservable getObservable() {
                    return collectionModel;
                }

                @Override
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
    @Override
    public void browse(final IModel<Node> model) {
        final IModel<Node> document = getHandleOrFolder(model);
        if (document.getObject() == null) {
            return;
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

    private void onModelChanged(final LastVisited lastVisited) {
        if (lastVisitedModel != null) {
            lastVisitedModel.setObject(lastVisited);
        }
    }

    public void selectSection(final IModel<IBrowserSection> model) {
        documentService.updateModel(model.getObject().getCollection().getFolder());
    }

    @Override
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
                            if (parent.isNodeType(NT_HANDLE)) {
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
                    if (parent.isNodeType(NT_HANDLE)) {
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


        private final IPluginContext context;

        DocumentModelService(final IPluginConfig config, final IPluginContext context) {
            super(config.getString("model.document"), new JcrNodeModel((Node) null));
            this.context = context;
        }

        public void updateModel(final IModel<Node> model) {
            super.setModel(updateModelForFrozenNodeWithCurrentBranchId(model));

            onModelChanged(LastVisited.document(model));
            if (lastVisitedModel != null) {
                lastVisitedModel.setObject(LastVisited.document(model));
            }
        }

        @Override
        public void setModel(final IModel<Node> model) {
            if (model == null) {
                throw new IllegalArgumentException("invalid model null");
            }
            if (model.getObject() == null) {
                updateModel(model);
            } else {
                browse(updateModelForFrozenNodeWithCurrentBranchId(model));
            }
        }


        private IModel<Node> updateModelForFrozenNodeWithCurrentBranchId(final IModel<Node> model) {
            final Node node = model.getObject();
            if (node == null){
                return model;
            }
            try {
                if (!node.isNodeType(NT_HANDLE) || node.isNodeType(NT_DOCUMENT)) {
                    return model;
                }
                final Node handle = getHandle(node);
                final BranchIdModel branchIdModel = BranchIdModel.initialize(context, handle);
                final String currentBranchId = branchIdModel.getBranchId();
                log.debug("Current branch id:{}", currentBranchId);
                if (isFrozenNode(node)) {
                    final DocumentVariant documentVariant = new DocumentVariant(node);
                    final String frozenBranchId = documentVariant.getBranchId();
                    log.debug("Branch id of frozen node:{} is {}", handle.getPath(), frozenBranchId);
                    if (branchIdModel.isDefined() && currentBranchId.equals(frozenBranchId)) {
                        log.debug("The documentModel(handle:{}) contains a frozen node:{} that has the same branchId as" +
                                        " the current branch id: {}, updating the documentModel with the associated handle."
                                , currentBranchId);
                        return new JcrNodeModel(handle);
                    }
                }
            } catch (RepositoryException e) {
                log.warn(e.getMessage(), e);
            }
            return model;
        }

        private Node getHandle(final Node node) throws RepositoryException {
            return isFrozenNode(node) ? getVersionHandle(node) : node;
        }

        private Node getVersionHandle(final Node frozenNode) {
            try {
                final String uuid = frozenNode.getProperty(JcrConstants.JCR_FROZEN_UUID).getString();
                final Node variant = frozenNode.getSession().getNodeByIdentifier(uuid);
                return variant.getParent();
            } catch (final RepositoryException e) {
               log.warn(e.getMessage(), e);
               return frozenNode;
            }
        }

        private boolean isFrozenNode(final Node node) throws RepositoryException {
            return node.isNodeType(JcrConstants.NT_FROZEN_NODE);
        }
    }

    private class FolderModelService extends ModelReference<Node> {

        FolderModelService(final IPluginConfig config) {
            super(config.getString("model.folder"), new JcrNodeModel((Node) null));
        }

        public void updateModel(final IModel<Node> model) {
            super.setModel(model);

            onModelChanged(LastVisited.folder(model));
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
