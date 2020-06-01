/*
 *  Copyright 2008-2014 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.ecmtagging.tagcloud;

import java.util.Iterator;
import java.util.TreeMap;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.CssResourceReference;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.service.render.RenderPlugin;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.forge.ecmtagging.Tag;
import org.onehippo.forge.ecmtagging.TagCollection;
import org.onehippo.forge.ecmtagging.common.TagCloud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class renders a tag cloud. It is meant as a frontend component.
 */
public class TagCloudPlugin extends RenderPlugin {
    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(TagCloudPlugin.class);

    public final static String TAGS_INDEX = "tags.index";
    public final static String TAGS_LIMIT = "tags.limit";

    private static final CssResourceReference CSS = new CssResourceReference(TagCloudPlugin.class, "TagCloudPlugin.css");

    private int limit;

    public TagCloudPlugin(IPluginContext context, IPluginConfig config) {
        super(context, config);

        Session session = ((UserSession) getSession()).getJcrSession();
        String tagsIndex = config.getString(TAGS_INDEX);
        if (!tagsIndex.startsWith("/")) {
            tagsIndex = "/" + tagsIndex;
        }
        limit = config.getInt(TAGS_LIMIT, 50);

        final TagCloudView view = new TagCloudView("tagcloud", session, tagsIndex);
        add(view);
    }

    @Override
    public void renderHead(final IHeaderResponse response) {
        super.renderHead(response);
        response.render(CssHeaderItem.forReference(CSS));
    }

    private class TagCloudView extends TagCloud {
        private static final long serialVersionUID = 1L;

        public TagCloudView(final String tagcloud, final Session session, final String tagLocation) {
            super(tagcloud, session, tagLocation);
            setOutputMarkupId(true);

            add(new TagCloudWidgetBehavior());
        }

        @Override
        protected Iterator<IModel> getItemModels() {
            // retrieve the top tags from the index (the index is sorted by count)
            if (this.tagsNodeModel != null) {
                try {
                    Node index = this.tagsNodeModel.getObject();
                    NodeIterator iterator;
                    if (index.getPrimaryNodeType().getName().equals("hippofacnav:facetnavigation") && index.hasNode("hippostd:tags")) {
                        iterator = index.getNode("hippostd:tags").getNodes();
                    }
                    else {
                        iterator = index.getNodes();
                    }
                    TagCollection collection = new TagCollection();
                    for (int i = 0; i < limit && iterator.hasNext(); i++) {
                        Node tag = iterator.nextNode();
                        if ("hippo:resultset".equals(tag.getName())) {
                            // decrease i to get the next result that is not hippo:resultset
                            i--;
                        } else {
                            Tag model = new Tag(tag.getName(), tag.getProperty("hippo:count").getDouble());
                            collection.add(model);
                        }
                    }
                    collection.normalizeScores();
                    // put all the tags in a map to sort them alphabetically
                    TreeMap<String, IModel> map = new TreeMap<String, IModel>();
                    for (IModel t : collection) {
                        String name = ((Tag) t.getObject()).getName();
                        map.put(name, t);
                    }
                    return map.values().iterator();
                } catch (RepositoryException e) {
                    log.error("Repository error: " + e.getMessage(), e);
                }
            }
            // return empty map, null leads to NPE
            return new TreeMap<String, IModel>().values().iterator();
        }

        @SuppressWarnings({"unchecked"})
        @Override
        public void onClick(AjaxRequestTarget target, Tag tag) {
            try {
                JcrNodeModel nodeModel;
                if (tagsNodeModel.getNode().getPrimaryNodeType().getName().equals("hippofacnav:facetnavigation")
                        && tagsNodeModel.getNode().hasNode("hippostd:tags")) {
                    nodeModel = new JcrNodeModel(tagsNodeModel.getNode().getNode("hippostd:tags").getNode(NodeNameCodec.encode(tag.getName(), true)));
                } else {
                    nodeModel = new JcrNodeModel(tagsNodeModel.getNode().getNode(NodeNameCodec.encode(tag.getName(), true)));
                }
                TagCloudPlugin.this.setModel(nodeModel);
            } catch (PathNotFoundException e) {
                log.error("tag not found: " + e.getMessage(), e);
            } catch (RepositoryException e) {
                log.error("Repository error: " + e.getMessage(), e);
            }
        }

    }

}
