/*
 *  Copyright 2008 Hippo.
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
package org.hippoecm.frontend.plugins.tagging.providers;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.tagging.Tag;
import org.hippoecm.frontend.plugins.tagging.TagCollection;
import org.hippoecm.frontend.plugins.tagging.editor.TagsPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CurrentTagsTagsProvider extends AbstractTagsProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(CurrentTagsTagsProvider.class);
    private static final long serialVersionUID = 1L;

    TagCollection tags;

    public CurrentTagsTagsProvider(IPluginContext context, IPluginConfig config) {
        super(context, config);
        log.debug("CurrentTagsTagsProvider");
    }

    public TagCollection getTags(JcrNodeModel nodeModel) {
        if (tags == null) {
            tags = new TagCollection();
            try {
                // retrieve all tags of the current document
                Value[] tagValues = nodeModel.getNode().getProperty(TagsPlugin.FIELD_NAME).getValues();
                for (Value tagValue : tagValues) {
                    QueryManager queryManager = nodeModel.getNode().getSession().getWorkspace().getQueryManager();
                    // query repository for documents with also this tag
                    Query query = queryManager.createQuery("//element(*,hippostd:taggable)[@" + TagsPlugin.FIELD_NAME
                            + "='" + tagValue.getString().toLowerCase() + "']", Query.XPATH);
                    // iterate over the found nodes
                    for (NodeIterator ni = query.execute().getNodes(); ni.hasNext();) {
                        Node node = ni.nextNode();
                        // some articles may not have a tags field
                        if (node.hasProperty(TagsPlugin.FIELD_NAME)) {
                            Property tagsProperty = node.getProperty(TagsPlugin.FIELD_NAME);
                            Value[] values = tagsProperty.getValues();
                            // iterate over the tags of the found node
                            for (Value value : values) {
                                // check if the found tag isn't the same as the tag we searched for
                                if (!tagValue.getString().equalsIgnoreCase(value.getString())) {
                                    Tag tag = new Tag(value.getString(), 0.5);
                                    tags.add(tag);
                                }
                            }
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.error("Repository error", e);
            }
        }
        return tags;
    }

}
