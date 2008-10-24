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

import java.util.ArrayList;

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

public class AllTagsProvider extends AbstractTagsProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(AllTagsProvider.class);
    private static final long serialVersionUID = 1L;

    TagCollection tags;
    public final static String SCORE = "score";
    public final static String SERVICE = "service";

    private double score;
    private static int instances = 0;

    public AllTagsProvider(IPluginContext context, IPluginConfig config) {
        super(context, config);
        score = config.getDouble(SCORE, 0.1);
        instances++;
        log.debug("AllTagsProvider instance: " + instances);
    }

    public TagCollection getTags(JcrNodeModel nodeModel) {
        if (tags == null) {
            tags = new TagCollection();
            try {
                ArrayList<String> currentTags = getCurrentTags(nodeModel);
                // iterate over all nodes with mixin taggable
                for (NodeIterator ni = query(nodeModel); ni.hasNext();) {
                    Node node = ni.nextNode();
                    // check if it has the tags property
                    if (node.hasProperty(TagsPlugin.FIELD_NAME)) {
                        Property tagsProperty = node.getProperty(TagsPlugin.FIELD_NAME);
                        Value[] values = tagsProperty.getValues();
                        // add all the tags to the collection
                        for (Value value : values) {
                            // filter already assigned tags
                            if (!currentTags.contains(value.getString())) {
                                Tag tag = new Tag(value.getString(), score);
                                tags.add(tag);
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

    /**
     * Get all nodes with mixin taggable
     * @param nodeModel
     * @return
     * @throws RepositoryException
     */
    private NodeIterator query(JcrNodeModel nodeModel) throws RepositoryException {
        QueryManager queryManager = nodeModel.getNode().getSession().getWorkspace().getQueryManager();
        Query query = queryManager.createQuery("//element(*,hippostd:taggable)", Query.XPATH);
        return query.execute().getNodes();
    }

}
