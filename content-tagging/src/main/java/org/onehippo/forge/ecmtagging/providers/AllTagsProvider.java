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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.StringCodecFactory;
import org.onehippo.forge.ecmtagging.Tag;
import org.onehippo.forge.ecmtagging.TagCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic tag provider that suggests the most common tags in the whole
 * repository. It does not inspect the given document in any way.
 * 
 * @author Jeroen Tietema
 *
 */
public class AllTagsProvider extends AbstractTagsProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(AllTagsProvider.class);
    private static final long serialVersionUID = 1L;

    public final static String SCORE = "score";
    public final static String TAGS_INDEX = "tags.index";

    private double score;
    private String tagsIndex;

    public AllTagsProvider(IPluginContext context, IPluginConfig config) {
        super(context, config);
        score = config.getDouble(SCORE, 0.1);
        tagsIndex = config.getString(TAGS_INDEX);
        if (tagsIndex.startsWith("/")) {
            tagsIndex = tagsIndex.substring(1);
        }
    }

    public TagCollection getTags(JcrNodeModel nodeModel) throws RepositoryException {
        return AllTagsProvider.getTags(nodeModel.getNode().getSession(), tagsIndex, score);
    }

    public static TagCollection getTags(Session session, String tagsIndex) throws RepositoryException {
        return AllTagsProvider.getTags(session, tagsIndex, 1.0);
    }

    /**
     * @todo Remove use of ISO9075Helper class
     * 
     * @param session
     * @param tagsIndex
     * @param score
     * @return
     * @throws RepositoryException
     */
    public static TagCollection getTags(Session session, String tagsIndex, double score) throws RepositoryException {
        TagCollection tags = new TagCollection();
        Node facetRoot = session.getRootNode().getNode(tagsIndex);

        // retrieve the facet root node; facet navigation
        NodeIterator iterator;
        if (facetRoot.isNodeType("hippofacnav:facetnavigation")) {
            iterator = facetRoot.getNode("hippostd:tags").getNodes();
        } else {
            // backwards compatible; facetsearch
            iterator = facetRoot.getNodes();
        }
        while (iterator.hasNext()) {
            Node tagNode = iterator.nextNode();
            if (!tagNode.isNodeType(HippoNodeType.NT_FACETRESULT)) {
                String name = StringCodecFactory.ISO9075Helper.decodeLocalName(tagNode.getName());
                Tag tag = new Tag(name, tagNode.getProperty("hippo:count").getLong() * score);
                tags.add(tag);
            }
        }
        return tags;
    }
}
