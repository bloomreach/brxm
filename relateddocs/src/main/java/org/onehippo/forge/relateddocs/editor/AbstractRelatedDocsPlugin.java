/*
 *  Copyright 2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.relateddocs.editor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.hippoecm.frontend.dialog.IDialogService;
import org.hippoecm.frontend.model.event.IEvent;
import org.hippoecm.frontend.model.event.IObservable;
import org.hippoecm.frontend.model.event.IObserver;
import org.hippoecm.frontend.model.event.JcrEvent;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IEditor;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.onehippo.forge.relateddocs.RelatedDoc;
import org.onehippo.forge.relateddocs.RelatedDocCollection;
import org.onehippo.forge.relateddocs.RelatedDocCollectionModel;
import org.onehippo.forge.relateddocs.dialogs.ClearDeletedDocumentsDialog;
import org.onehippo.forge.relateddocs.dialogs.RelatedDocNotFoundDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.onehippo.forge.relateddocs.RelatedDocsNodeType.NT_RELATABLEDOCS;
import static org.onehippo.forge.relateddocs.RelatedDocsNodeType.NT_RELATEDDOCS;

public abstract class AbstractRelatedDocsPlugin extends RenderPlugin<Node> implements IObserver {
    private static final long serialVersionUID = 1L;

    public static final Logger log = LoggerFactory.getLogger(AbstractRelatedDocsPlugin.class);

    private RelatedDocCollectionModel relatedDocs;
    protected final IEditor.Mode mode;

    public AbstractRelatedDocsPlugin(final IPluginContext context, final IPluginConfig config) {
        super(context, config);

        mode = IEditor.Mode.fromString(config.getString("mode", "view"));
        relatedDocs = createModel(getModelObject());

        if (relatedDocs != null) {
            context.registerService(new IObserver() {

                public IObservable getObservable() {
                    return relatedDocs.getDocument();
                }

                public void onEvent(Iterator events) {
                    onRelatedDocsModelEvent(events);
                }
            }, IObserver.class.getName());

            if (mode == IEditor.Mode.EDIT) {
                List<RelatedDoc> nonExistingDocs = getNonExistingDocuments();
                if (nonExistingDocs != null) {
                    for (RelatedDoc d : nonExistingDocs) {
                        relatedDocs.getObject().remove(d);
                    }
                    showClearedDeletedDocumentsDialog();
                }
            }
        }
    }

    protected RelatedDocCollectionModel createModel(final Node node) {
        try {
            Node relatedDocsNode = null;
            // check if the child node exists, else try to create it
            if (node.hasNode(NT_RELATEDDOCS)) {
                relatedDocsNode = node.getNode(NT_RELATEDDOCS);
            } else if (mode == IEditor.Mode.EDIT) {
                if (log.isDebugEnabled()) {
                    log.debug("Child node " + NT_RELATEDDOCS + " does not exist, attempting to create it.");
                }
                if (!node.isNodeType(NT_RELATABLEDOCS)) {
                    node.addMixin(NT_RELATABLEDOCS);
                }
                relatedDocsNode = node.addNode(NT_RELATEDDOCS, NT_RELATEDDOCS);
                relatedDocsNode.getSession().save();
            }
            return RelatedDocCollectionModel.from(relatedDocsNode);

        } catch (RepositoryException e) {
            log.error("Repository error", e);
        }

        return null;
    }

    protected void onRelatedDocsModelEvent(final Iterator events) {
        while (events.hasNext()) {
            IEvent event = (IEvent) events.next();
            if (event instanceof JcrEvent) {
                onJcrEvent((JcrEvent) event);
            }
        }
    }

    protected void onJcrEvent(final JcrEvent event) {
    }

    protected void onRelatedDocNotFound(final RelatedDoc relatedDoc, final AjaxRequestTarget target) {
        relatedDocs.getObject().remove(relatedDoc);

        if (target != null) {
            target.add(AbstractRelatedDocsPlugin.this);
        }

        showRelatedDocNotFoundDialog();
    }

    protected void showRelatedDocNotFoundDialog() {
        IDialogService dialogService = getPluginContext().
                getService(IDialogService.class.getName(), IDialogService.class);

        dialogService.show(new RelatedDocNotFoundDialog());
    }

    protected void showClearedDeletedDocumentsDialog() {
        IDialogService dialogService = getPluginContext().
                getService(IDialogService.class.getName(), IDialogService.class);

        dialogService.show(new ClearDeletedDocumentsDialog());
    }

    @Override
    protected void onDetach() {
        if (relatedDocs != null) {
            relatedDocs.detach();
        }
        super.onDetach();
    }

    public RelatedDocCollection getRelatedDocs() {
        if (relatedDocs != null) {
            return relatedDocs.getObject();
        }
        return new RelatedDocCollection();
    }

    public List<RelatedDoc> getNonExistingDocuments() {
        List<RelatedDoc> nonExistingDocs = null;
        for (RelatedDoc doc : relatedDocs.getObject()) {
            if (!doc.exists()) {
                if (nonExistingDocs == null) {
                    nonExistingDocs = new LinkedList<RelatedDoc>();
                }
                nonExistingDocs.add(doc);
            }
        }
        return nonExistingDocs;
    }

}
