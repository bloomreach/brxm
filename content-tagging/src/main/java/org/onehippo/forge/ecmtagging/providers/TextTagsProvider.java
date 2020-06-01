/*
 *  Copyright 2008-2013 Hippo B.V. (http://www.onehippo.com)
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
package org.onehippo.forge.ecmtagging.providers;

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.ecmtagging.Tag;
import org.onehippo.forge.ecmtagging.TagCollection;
import org.onehippo.forge.ecmtagging.TaggingNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A provider that can scan a given text property if it contains existing
 * keywords. If one is found, than it also searches for related keywords.
 * 
 * @author Jeroen Tietema
 *
 */
public class TextTagsProvider extends AbstractTagsProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(TextTagsProvider.class);

    private String textSource;

    public static final String TEXT_TAG_SCORE = "text.tag.score";
    public static final String REL_TAG_SCORE = "related.tag.score";
    public static final String TEXT_SOURCE = "text.source";
    public final static String TAGS_INDEX = "tags.index";

    private double textScore;
    private double relatedScore;
    private String tagsIndex;

    public TextTagsProvider(IPluginContext context, IPluginConfig config) {
        super(context, config);
        textScore = config.getDouble(TEXT_TAG_SCORE, 1.0);
        relatedScore = config.getDouble(REL_TAG_SCORE, 0.5);
        textSource = config.getString(TEXT_SOURCE);
        tagsIndex = config.getString(TAGS_INDEX);
        if (tagsIndex.startsWith("/")) {
            tagsIndex = tagsIndex.substring(1);
        }
    }

    public TagCollection getTags(JcrNodeModel nodeModel) throws IllegalStateException, RepositoryException {
        TagCollection tagCollection = new TagCollection();
        if (nodeModel.getNode().hasProperty(textSource)) {
            String text = nodeModel.getNode().getProperty(textSource).getString();
            String[] words = text.split(" ");
            ArrayList<String> currentTags = getCurrentTags(nodeModel);
            Node tagsSearch = nodeModel.getNode().getSession().getRootNode().getNode(tagsIndex);
            for (String word : words) {
                word = word.toLowerCase();
                // filter short (useless) words and already added tags
                // TODO move word length to config in repository
                if (word.length() > 2 && !currentTags.contains(word) && tagsSearch.hasNode(word)) {
                    log.debug("Found tag: {}", word);
                    // add the word as tag if it has results (thus exists as tag)
                    tagCollection.add(new Tag(word, textScore));
                    // add the other tags from the nodes that contain this tag with lower score
                    for (NodeIterator i = tagsSearch.getNode(word).getNode("hippo:resultset").getNodes(); i.hasNext();) {
                        Node node = i.nextNode();
                        Value[] tags = node.getProperty(TaggingNodeType.PROP_TAGS).getValues();
                        for (Value tag : tags) {
                            // make sure it isn't the current tag we added before
                            String foundTag = tag.getString().toLowerCase();
                            if (!word.equalsIgnoreCase(foundTag) && !currentTags.contains(foundTag)) {
                                tagCollection.add(new Tag(foundTag, relatedScore));
                                log.debug("Found related tag: {}", foundTag);
                            }
                        }
                    }
                }
            }
        }
        return tagCollection;
    }

}
