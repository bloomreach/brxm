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
package org.onehippo.forge.ecmtagging.common;

import java.util.Iterator;
import java.util.Optional;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RefreshingView;
import org.apache.wicket.model.IModel;
import org.hippoecm.frontend.attributes.ClassAttribute;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.onehippo.forge.ecmtagging.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class renders a tag cloud. It is meant as a frontend component.
 */
public abstract class TagCloud extends Panel {
    private static final Logger log = LoggerFactory.getLogger(TagCloud.class);

    protected JcrNodeModel tagsNodeModel;

    public TagCloud(final String id, final Session session, final String tagLocation) {
        super(id);
        init(session, tagLocation);
    }

    protected void init(Session session, String tagLocation) {
        add(new TagCloudView("view", session, tagLocation));
    }

    public class TagCloudView extends RefreshingView {

        public TagCloudView(String id, Session session, String tagLocation) {
            super(id);
            try {
                if (StringUtils.isNotEmpty(tagLocation) && session.itemExists(tagLocation)) {
                    tagsNodeModel = new JcrNodeModel(session.getNode(tagLocation));
                } else {
                    log.error(
                            "Tagging plugin requires to set the correct tags.index property on the tagging-tree-view browseTagcloud section. " +
                                    "Configured value is '{}' which is not an absolute path to an existing node",
                            tagLocation);
                }
            } catch (PathNotFoundException e) {
                log.error("Tags facet not found: " + e.getMessage(), e);
            } catch (RepositoryException e) {
                log.error("Repository error: " + e.getMessage(), e);
            }
        }

        @Override
        protected void populateItem(Item item) {
            final Tag tag = (Tag) item.getModelObject();
            Fragment fragment = new Fragment("fragment", "linkfragment", this);
            AjaxFallbackLink<Void> link = new AjaxFallbackLink<>("link") {
                @Override
                public void onClick(final Optional<AjaxRequestTarget> optional) {
                    optional.ifPresent(target -> TagCloud.this.onClick(target, tag));
                }
            };
            Label label = new Label("link-text", tag.getName());
            double doubleScore = Math.ceil(tag.getScore() / 10);
            StringBuilder tagClass = new StringBuilder("tag");
            tagClass.append((int) doubleScore);
            label.add(ClassAttribute.append(tagClass.toString()));
            link.add(label);
            fragment.add(link);
            item.add(fragment);
        }

        @Override
        protected Iterator<IModel> getItemModels() {
            return TagCloud.this.getItemModels();
        }

    }

    @Override
    protected void onDetach() {
        super.onDetach();
        if (tagsNodeModel != null) {
            tagsNodeModel.detach();
        }
    }

    protected abstract Iterator<IModel> getItemModels();

    public abstract void onClick(AjaxRequestTarget target, Tag tag);
}
