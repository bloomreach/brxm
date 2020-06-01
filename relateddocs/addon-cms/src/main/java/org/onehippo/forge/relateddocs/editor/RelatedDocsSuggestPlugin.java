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
package org.onehippo.forge.relateddocs.editor;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.dialog.DialogLink;
import org.hippoecm.frontend.dialog.IDialogFactory;
import org.hippoecm.frontend.dialog.IDialogService.Dialog;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.event.JcrEvent;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.IBrowseService;
import org.onehippo.forge.relateddocs.RelatedDoc;
import org.onehippo.forge.relateddocs.RelatedDocCollection;
import org.onehippo.forge.relateddocs.RelatedDocSuggestor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.frontend.service.IEditor.Mode;

/**
 * RelatedDocsSuggestPlugin renders a textarea with a list of suggested documents that may be related to the current
 * document. If the user clicks on a suggestion, the related document will be added to the list of related documents.
 */
public class RelatedDocsSuggestPlugin extends AbstractRelatedDocsPlugin {
    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(RelatedDocsSuggestPlugin.class);
    private static final CssResourceReference CSS = new CssResourceReference(RelatedDocsSuggestPlugin.class, "RelatedDocsSuggestPlugin.css");

    private Form suggestionsForm;
    private AjaxSubmitLink refreshLink;
    private DialogLink browseLink;

    private int limit = 20;
    private int maxAllowed;

    public RelatedDocsSuggestPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        limit = config.getInt("numberOfSuggestions", 20);
        maxAllowed = config.getInt("max.allowed", 0);

        if (Mode.EDIT == mode && getRelatedDocs() != null) {

            refreshLink = new AjaxSubmitLink("refreshlink", suggestionsForm) {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onSubmit(AjaxRequestTarget target, Form form) {
                    redraw();
                }
            };

            browseLink = new DialogLink("browse", new ResourceModel("browse"), new IDialogFactory() {
                private static final long serialVersionUID = 1L;

                public Dialog createDialog() {
                    return new DocumentPickerDialog(context, getPluginConfig(), getModel(), getRelatedDocs());
                }

            }, getDialogService());

            if (maxAllowed > 0) {
                final RelatedDocCollection relatedDocCollection = getRelatedDocs();
                if (relatedDocCollection.size() >= maxAllowed) {
                    refreshLink.setEnabled(false);
                    browseLink.setEnabled(false);
                } else {
                    refreshLink.setEnabled(true);
                    browseLink.setEnabled(true);
                }
            }

            suggestionsForm = new Form("suggestions-form");
            suggestionsForm.add(new TextField<>("count", new PropertyModel<>(this, "limit"), Integer.class));

            final Fragment fragment = new Fragment("relateddoc-view", "relateddoc-suggestions", this);
            fragment.add(new RelatedDocsSuggestView("view"));
            fragment.add(refreshLink);
            fragment.add(browseLink);
            fragment.add(suggestionsForm);

            add(fragment);

        } else {
            add(new EmptyPanel("relateddoc-view").setVisible(false));
        }
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        response.render(CssHeaderItem.forReference(CSS));
    }

    @Override
    protected void onJcrEvent(final JcrEvent event) {
        if (maxAllowed > 0) {
            final RelatedDocCollection relatedDocCollection = getRelatedDocs();
            if (relatedDocCollection.size() >= maxAllowed) {
                refreshLink.setEnabled(false);
                browseLink.setEnabled(false);
            } else {
                refreshLink.setEnabled(true);
                browseLink.setEnabled(true);
            }
        }
        redraw();
    }

    // Used by the propertyModel of the count widget
    @SuppressWarnings("unused")
    public Integer getLimit() {
        return limit;
    }

    // Used by the propertyModel of the count widget
    @SuppressWarnings("unused")
    public void setLimit(final Integer limit) {
        if (limit == null) {
            this.limit = 0;
        } else {
            this.limit = limit;
        }
    }

    private class RelatedDocsSuggestView extends RefreshingView {

        private static final long serialVersionUID = 1L;
        private int size;

        public RelatedDocsSuggestView(String id) {
            super(id);
        }

        @Override
        protected Iterator<IModel> getItemModels() {
            return getItemModelsList().iterator();
        }

        protected List<IModel> getItemModelsList() {

            RelatedDocSuggestor relatedDocSuggester = getPluginContext().getService(RelatedDocSuggestor.SERVICE,
                    RelatedDocSuggestor.class);
            List<IModel> list = new ArrayList<>();

            if (maxAllowed > 0) {
                final RelatedDocCollection relatedDocCollection = getRelatedDocs();
                if (relatedDocCollection.size() >= maxAllowed) {
                    return list;
                }
            }

            JcrNodeModel documentModel = (JcrNodeModel) RelatedDocsSuggestPlugin.this.getModel();
            if (documentModel == null && log.isDebugEnabled()) {
                log.debug("jcrNodeModel is null");
            }
            RelatedDocCollection collection = relatedDocSuggester.getRelatedDocCollection(documentModel);
            if (log.isDebugEnabled()) {
                log.debug("Collection of size: {}", collection.size());
            }
            Iterator<RelatedDoc> iterator = collection.iterator(0, 0);
            for (int i = 0; i < limit && iterator.hasNext(); i++) {
                list.add(collection.model(iterator.next()));
            }
            size = list.size();
            return list;
        }

        @Override
        protected void populateItem(Item item) {
            final RelatedDoc relatedDoc = (RelatedDoc) item.getModelObject();

            AjaxLink link = new AjaxLink("link") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(AjaxRequestTarget target) {
                    if (relatedDoc.exists()) {
                        RelatedDocCollection relatedDocs = getRelatedDocs();
                        // to add the new item to the bottom of the list we have to manipulate its score
                        if (relatedDocs.size() > 0) {
                            relatedDoc.setScore(relatedDocs.lastKey().getScore() / 2);
                        }
                        relatedDocs.add(relatedDoc);
                    } else {
                        onRelatedDocNotFound(relatedDoc, target);
                    }
                }
            };
            NumberFormat nf = NumberFormat.getIntegerInstance();
            nf.setMaximumFractionDigits(0);

            if (item.getIndex() == (size - 1)) {
                item.add(new AttributeAppender("class", Model.of("last"), " "));
            }

            Label label = new Label("link-text", relatedDoc.getName());
            label.add(new AttributeAppender("title", relatedDoc.getPath()));
            label.add(new AttributeAppender("class", Model.of("relateddocsuggest"), " "));
            link.add(label);
            item.add(link);

            AjaxLink previewLink = new AjaxLink("preview") {

                private static final long serialVersionUID = 1L;

                @Override
                public void onClick(final AjaxRequestTarget target) {
                    if (relatedDoc.exists()) {
                        IBrowseService browseService = getPluginContext().
                                getService(getPluginConfig().getString("browser.id"), IBrowseService.class);
                        if (browseService != null) {
                            browseService.browse(relatedDoc.getNodeModel());
                        }
                    } else {
                        onRelatedDocNotFound(relatedDoc, target);
                    }
                }
            };
            item.add(previewLink);
        }
    }
}
