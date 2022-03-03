/*
 *  Copyright 2008-2019 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.ecmtagging.editor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.model.properties.JcrPropertyModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.standards.ClassResourceModel;
import org.onehippo.forge.ecmtagging.Tag;
import org.onehippo.forge.ecmtagging.TagCollection;
import org.onehippo.forge.ecmtagging.TagSuggestor;
import org.onehippo.forge.ecmtagging.TaggingNodeType;
import org.onehippo.forge.ecmtagging.common.TagCloud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Frontend Plugin that displays the tag suggestions to the user.
 */
public class TagSuggestPlugin extends AbstractTagsPlugin {

    private static final Logger log = LoggerFactory.getLogger(TagSuggestPlugin.class);

    private static final CssResourceReference CSS = new CssResourceReference(TagSuggestPlugin.class,
            "TagSuggestPlugin.css");

    public static final int DEFAULT_LIMIT = 20;

    private int limit = DEFAULT_LIMIT;
    private Fragment fragment;

    public TagSuggestPlugin(final IPluginContext context, IPluginConfig config) {
        super(context, config);

        limit = config.getInt("numberOfSuggestions", DEFAULT_LIMIT);
        String mode = config.getString("mode");
        String tagIndexLocation = config.getString("tags.index", "tags");
        if (!tagIndexLocation.startsWith("/")) {
            tagIndexLocation = "/" + tagIndexLocation;
        }
        if ("edit".equals(mode)) {
            fragment = new Fragment("tag-view", "suggestions", this);

            final String defaultCaption = new ClassResourceModel("keyword_suggestions",
                    TagSuggestPlugin.class).getObject();
            fragment.add(new Label("title", getCaptionModel("tagsuggest", defaultCaption)));

            try {
                final Session session = ((JcrNodeModel) getModel()).getNode().getSession();
                fragment.add(new TagSuggestView("view", session, tagIndexLocation));
            } catch (RepositoryException e) {
                log.error("Repository error: " + e.getMessage(), e);
            }
            fragment.add(new AjaxFallbackLink<>("refreshlink") {
                @Override
                public void onClick(final Optional<AjaxRequestTarget> optional) {
                    onEvent(null);
                }
            });
            add(fragment);
        } else {
            add(new Fragment("tag-view", "empty", this));
        }
        log.debug("construct");
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CSS));
    }

    private class TagSuggestView extends TagCloud {

        private int size;

        public TagSuggestView(final String id, final Session session, final String tagLocation) {
            super(id, session, tagLocation);
        }

        @Override
        protected void init(Session session, String tagLocation) {
            add(new TagCloudView("view", session, tagLocation) {

                @Override
                protected void populateItem(Item item) {
                    final Tag tag = (Tag) item.getModelObject();

                    Fragment fragment = new Fragment("fragment", "linkfragment", this);
                    AjaxFallbackLink<Void> link = new AjaxFallbackLink<>("link") {
                        @Override
                        public void onClick(final Optional<AjaxRequestTarget> optional) {
                            optional.ifPresent(target -> TagSuggestView.this.onClick(target, tag));
                        }
                    };

                    String tagName = tag.getName();
                    if (item.getIndex() < size - 1 && StringUtils.isNotBlank(tagName)) {
                        tagName += ",";
                    }

                    Label label = new Label("link-text", tagName);
                    double doubleScore = Math.ceil(tag.getScore() / 10);
                    StringBuilder tagClass = new StringBuilder("tagsuggest");
                    tagClass.append((int) doubleScore);
                    label.add(new AttributeAppender("class", new Model<>(tagClass.toString()), " "));
                    link.add(label);
                    fragment.add(link);
                    item.add(fragment);
                }
            });
        }

        @Override
        protected Iterator<IModel> getItemModels() {
            return getItemModelsList().iterator();
        }

        protected List<IModel> getItemModelsList() {
            TagSuggestor tagSuggestor = getPluginContext().getService(
                    getPluginConfig().getString(TagSuggestor.SERVICE_ID, TagSuggestor.SERVICE_DEFAULT),
                    TagSuggestor.class);
            List<IModel> list = new ArrayList<>();
            if (tagSuggestor != null) {
                try {
                    TagCollection tagCollection = tagSuggestor.getTags((JcrNodeModel) TagSuggestPlugin.this.getModel());
                    log.debug("Collection of size: " + tagCollection.size());
                    Iterator<IModel> iterator = tagCollection.iterator();
                    for (int i = 0; i < limit && iterator.hasNext(); i++) {
                        list.add(iterator.next());
                    }
                } catch (RepositoryException e) {
                    log.error("Repository error: " + e.getMessage(), e);
                }
            } else {
                log.warn("No tag suggestor service found");
            }
            size = list.size();//preserve length for , test in populateitem
            return list;
        }

        @Override
        public void onClick(AjaxRequestTarget target, Tag keyword) {
            JcrNodeModel nodeModel = (JcrNodeModel) TagSuggestPlugin.this.getModel();
            try {
                TagsModel tagsModel = new TagsModel(new JcrPropertyModel(nodeModel.getNode().getProperty(
                        TaggingNodeType.PROP_TAGS)));
                tagsModel.addTag(keyword.getName());
                //getPluginContext().getService(IObserver.class.getName(), IObserver.class).flush(nodeModel);
                log.debug("Send flush");
            } catch (PathNotFoundException e) {
                log.info(TaggingNodeType.PROP_TAGS + " does not exist for this node. Attempting to create it.");
                // the property hippostd:tags does not exist
                String[] tags = {keyword.getName()};
                try {
                    nodeModel.getNode().setProperty(TaggingNodeType.PROP_TAGS, tags);
                } catch (RepositoryException re) {
                    log.error("Creation of " + TaggingNodeType.PROP_TAGS + " failed.", e);
                }
            } catch (RepositoryException e) {
                log.error("Repository error", e);
            }
        }

    }

    @Override
    public void onEvent(final Iterator event) {
        redraw();
    }
}
