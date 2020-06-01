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
package org.onehippo.forge.relateddocs.editor;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.model.IModelReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.JcrEvent;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.diff.LCS;
import org.hippoecm.frontend.plugins.standards.diff.LCS.Change;
import org.hippoecm.frontend.plugins.standards.diff.LCS.ChangeType;
import org.hippoecm.frontend.plugins.standards.icon.HippoIcon;
import org.hippoecm.frontend.service.IBrowseService;
import org.hippoecm.frontend.skin.Icon;
import org.onehippo.forge.relateddocs.RelatedDoc;
import org.onehippo.forge.relateddocs.RelatedDocCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.frontend.service.IEditor.Mode;
import static org.onehippo.forge.relateddocs.RelatedDocsNodeType.NT_RELATEDDOCS;

/**
 * The RelatedDocsPlugin renders the chosen related documents
 */
public class RelatedDocsPlugin extends AbstractRelatedDocsPlugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RelatedDocsPlugin.class);

    private static final CssResourceReference CSS = new CssResourceReference(RelatedDocsPlugin.class, "RelatedDocsPlugin.css");

    private JcrNodeModel baseModel;

    public RelatedDocsPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);


        if (mode == Mode.COMPARE) {
            if (config.containsKey("model.compareTo")) {
                IModelReference baseRef = context.getService(config.getString("model.compareTo"),
                        IModelReference.class);
                if (baseRef != null) {
                    JcrNodeModel baseDocument = (JcrNodeModel) baseRef.getModel();
                    baseModel = new JcrNodeModel(baseDocument.getItemModel().getPath() + "/" + NT_RELATEDDOCS);
                } else {
                    log.warn("no base model service available");
                }
            } else {
                log.warn("no base model service configure");
            }
        }

        if (mode == Mode.COMPARE && baseModel != null) {
            add(createCompareView(getRelatedDocs(), new RelatedDocCollection(baseModel)));
        } else {
            add(createRefreshingView(getRelatedDocs()));
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(CSS));
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        if (baseModel != null) {
            baseModel.detach();
        }
    }

    @Override
    protected void onJcrEvent(final JcrEvent event) {
        if (mode == Mode.COMPARE && baseModel != null) {
            replace(createCompareView(getRelatedDocs(), new RelatedDocCollection(baseModel)));
        } else {
            replace(createRefreshingView(getRelatedDocs()));
        }
        redraw();
    }

    private RefreshingView<RelatedDoc> createRefreshingView(final RelatedDocCollection relatedDocCollection) {

        return new RefreshingView<RelatedDoc>("view") {

            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<IModel<RelatedDoc>> getItemModels() {
                final Iterator<RelatedDoc> base = relatedDocCollection.iterator(0, 0);

                return new Iterator<IModel<RelatedDoc>>() {

                    public boolean hasNext() {
                        return base.hasNext();
                    }

                    public IModel<RelatedDoc> next() {
                        return relatedDocCollection.model(base.next());
                    }

                    public void remove() {
                        base.remove();
                    }
                };
            }

            @Override
            protected void populateItem(Item item) {
                final RelatedDoc relatedDoc = (RelatedDoc) item.getModelObject();
                AjaxLink link = new AjaxLink("link") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        if (relatedDoc.exists()) {
                            IBrowseService browseService = getPluginContext().getService(
                                    getPluginConfig().getString("browser.id"), IBrowseService.class);
                            if (browseService != null) {
                                browseService.browse(relatedDoc.getNodeModel());
                            }
                        } else {
                            onRelatedDocNotFound(relatedDoc, target);
                        }
                    }
                };

                link.add(new Label("link-text", new PropertyModel(relatedDoc, "name")));
                link.add(new AttributeAppender("title", new PropertyModel(relatedDoc, "path")));
                item.add(link);

                if (item.getIndex() == relatedDocCollection.size() - 1) {
                    item.add(new AttributeAppender("class", Model.of("last"), " "));
                }

                AjaxLink deleteLink = new AjaxLink("delete") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        relatedDocCollection.remove(relatedDoc);
                    }
                };

                if (Mode.EDIT != mode) {
                    deleteLink.setVisible(false);
                }

                deleteLink.add(HippoIcon.fromSprite("deleteIcon", Icon.TIMES));
                item.add(deleteLink);
            }
        };
    }

    private RefreshingView createCompareView(final RelatedDocCollection relatedDocCollection,
                                             final RelatedDocCollection baseCollection) {

        return new RefreshingView<Change<RelatedDoc>>("view") {

            @Override
            protected Iterator<IModel<Change<RelatedDoc>>> getItemModels() {
                final List<IModel<Change<RelatedDoc>>> itemModels = new LinkedList<>();

                RelatedDoc[] baseDocs = new RelatedDoc[(int) baseCollection.size()];
                Iterator<RelatedDoc> baseIter = baseCollection.iterator(0, baseCollection.size());
                int i = 0;
                while (baseIter.hasNext()) {
                    baseDocs[i++] = baseIter.next();
                }

                RelatedDoc[] currentDocs = new RelatedDoc[(int) relatedDocCollection.size()];
                Iterator<RelatedDoc> currentIter = relatedDocCollection.iterator(0, relatedDocCollection.size());
                i = 0;
                while (currentIter.hasNext()) {
                    currentDocs[i++] = currentIter.next();
                }

                for (final Change<RelatedDoc> change : LCS.getChangeSet(baseDocs, currentDocs)) {
                    itemModels.add(new AbstractReadOnlyModel<Change<RelatedDoc>>() {

                        @Override
                        public Change<RelatedDoc> getObject() {
                            return change;
                        }

                    });
                }
                return itemModels.iterator();
            }

            @Override
            protected void populateItem(Item<Change<RelatedDoc>> item) {
                Change<RelatedDoc> change = item.getModelObject();
                final RelatedDoc relatedDoc = change.getValue();

                //This link opens the document through the IBrowseService
                AjaxLink link = new AjaxLink("link") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        if (relatedDoc.exists()) {
                            IBrowseService browseService = getPluginContext().getService(
                                    getPluginConfig().getString("browser.id"), IBrowseService.class);
                            if (browseService != null) {
                                browseService.browse(relatedDoc.getNodeModel());
                            }
                        } else {
                            showRelatedDocNotFoundDialog();
                        }
                    }
                };
                Label label = new Label("link-text", new PropertyModel(relatedDoc, "name"));
                label.add(new AttributeAppender("title", new PropertyModel(relatedDoc, "path")));
                if (change.getType() == ChangeType.ADDED) {
                    label.add(new AttributeAppender("class", Model.of("hippo-diff-added"), " "));
                } else if (change.getType() == ChangeType.REMOVED) {
                    label.add(new AttributeAppender("class", Model.of("hippo-diff-removed"), " "));
                }
                link.add(label);
                item.add(link);

                if (item.getIndex() == relatedDocCollection.size() - 1) {
                    item.add(new AttributeAppender("class", Model.of("last"), " "));
                }

                AjaxLink deleteLink = new AjaxLink("delete") {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                    }
                };

                deleteLink.setVisible(false);
                deleteLink.add(HippoIcon.fromSprite("deleteIcon", Icon.TIMES));
                item.add(deleteLink);
            }
        };
    }
}
