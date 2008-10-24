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
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.plugins.tagging.Tag;
import org.hippoecm.frontend.plugins.tagging.TagCollection;
import org.hippoecm.frontend.plugins.tagging.editor.TagsPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TitleTagsProvider extends AbstractTagsProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(TitleTagsProvider.class);

    TagCollection tags;
    String title;

    public static final String TITLE_TAG_SCORE = "title.tag.score";
    public static final String REL_TAG_SCORE = "related.tag.score";

    private double titleScore;
    private double relatedScore;

    public TitleTagsProvider(IPluginContext context, IPluginConfig config) {
        super(context, config);
        titleScore = config.getDouble(TITLE_TAG_SCORE, 1.0);
        relatedScore = config.getDouble(REL_TAG_SCORE, 0.5);
    }

    public TagCollection getTags(JcrNodeModel nodeModel) {
        tags = new TagCollection();
        try {
            if (nodeModel.getNode().hasProperty("defaultcontent:title")) {
                QueryManager queryManager = nodeModel.getNode().getSession().getWorkspace().getQueryManager();
                String title = nodeModel.getNode().getProperty("defaultcontent:title").getString();
                // make sure we only update when the title changed
                if (title != this.title) {
                    this.title = title;
                    String[] words = title.split(" ");
                    for (String word : words) {
                        word = word.toLowerCase();
                        // filter short (useless) words
                        if (word.length() > 2) {
                            // search for nodes with word as tag
                            /**
                             * @todo retrieve query from node 
                             */
                            Query query = queryManager.createQuery("//element(*,hippostd:taggable)[@"
                                    + TagsPlugin.FIELD_NAME + "='" + word + "'][@hippostd:state='unpublished']",
                                    Query.XPATH);
                            QueryResult result = query.execute();
                            // add the word as tag if it has results (thus exists as tag)
                            if (result.getNodes().hasNext()) {
                                ArrayList<String> currentTags = getCurrentTags(nodeModel);
                                // only add the tag if it is not already added
                                if (!currentTags.contains(word)) {
                                    tags.add(new Tag(word, titleScore));
                                }
                                // add the other tags from the nodes that contain this tag with lower score
                                for (NodeIterator i = result.getNodes(); i.hasNext();) {
                                    Node node = i.nextNode();
                                    Value[] tags = node.getProperty(TagsPlugin.FIELD_NAME).getValues();
                                    for (Value tag : tags) {
                                        // make sure it isn't the current tag we added before
                                        String foundTag = tag.getString().toLowerCase();
                                        if (!word.equalsIgnoreCase(foundTag) && !currentTags.contains(foundTag)) {
                                            this.tags.add(new Tag(foundTag, relatedScore));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                return tags;
            }
        } catch (RepositoryException e) {
            log.error("Repository error", e);
        }
        return null;
    }

}
