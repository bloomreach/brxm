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
package org.onehippo.forge.ecmtagging.providers;

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.hippoecm.frontend.session.UserSession;
import org.hippoecm.repository.api.HippoNodeType;
import org.hippoecm.repository.api.NodeNameCodec;
import org.onehippo.forge.ecmtagging.Tag;
import org.onehippo.forge.ecmtagging.TagCollection;
import org.onehippo.forge.ecmtagging.TaggingNodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hippoecm.repository.api.StringCodecFactory.ISO9075Helper;

/**
 * Tag provider that suggests tags based on the tags that are already
 * assigned to a document.
 *
 * @author Jeroen Tietema
 */
public class CurrentTagsTagsProvider extends AbstractTagsProvider {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    static final Logger log = LoggerFactory.getLogger(CurrentTagsTagsProvider.class);
    private static final long serialVersionUID = 1L;

    public final static String SCORE = "score";
    public final static String TAGS_INDEX = "tags.index";

    private String tagsIndex;
    private double score;

    public CurrentTagsTagsProvider(IPluginContext context, IPluginConfig config) {
        super(context, config);
        tagsIndex = config.getString(TAGS_INDEX);
        if (tagsIndex.startsWith("/")) {
            tagsIndex = tagsIndex.substring(1);
        }
        score = config.getDouble(SCORE, 1);

        try {
            // retrieve the facet root node; facet navigation is preferred, facetsearch is supported
            Session session = ((UserSession) org.apache.wicket.Session.get()).getJcrSession();
            Node facetRoot = session.getRootNode().getNode(tagsIndex);
            if (!facetRoot.isNodeType("hippofacnav:facetnavigation")) {
                log.warn("Facet search tags node has been deprecated; use facetnavigation instead");
            }
        } catch (RepositoryException ex) {
            log.error("Error locating tags virtual provider by tags location " + tagsIndex, ex);
        }
    }

    @Override
    public TagCollection getTags(JcrNodeModel nodeModel) throws RepositoryException {
        TagCollection tags = new TagCollection();
        ArrayList<String> tagList = getCurrentTags(nodeModel);
        // iterate over all tags of the current document
        for (String tagName : tagList) {
            if (StringUtils.isBlank(tagName)) {
                continue;
            }
            if (log.isDebugEnabled()) {
                log.debug("Searching for tags related to {}", tagName);
            }
            /**
             * This collection contains all tags related to one tag.
             * This is done to be able to normalize at the end. This prevents
             * boosting tags that occur a lot in context of one related tag and
             * enables us to boost tags that relate with multiple tags of the document.
             */
            TagCollection collectionPerTag = new TagCollection();

            Session session = nodeModel.getNode().getSession();
            Node facetRoot = session.getRootNode().getNode(tagsIndex);

            // retrieve the facet root node; facet navigation is preferred, facetsearch is supported
            if (facetRoot.isNodeType("hippofacnav:facetnavigation")) {
                populateFromFacetNavigation(tagList, tagName, collectionPerTag, facetRoot);
            } else {
                populateFromFacetSearch(tagList, tagName, collectionPerTag, facetRoot);
            }

            tags.addAll(collectionPerTag.normalizeScores());
        }
        return tags;
    }

    private void populateFromFacetNavigation(ArrayList<String> tagList, String tagName, TagCollection collectionPerTag,
                                             Node facetRoot) throws RepositoryException {
        Node tagsIndexNode = facetRoot.getNode(TaggingNodeType.PROP_TAGS);
        String tagNodeName = NodeNameCodec.encode(tagName, true);
        if (!tagsIndexNode.hasNode(tagNodeName)) {
            return;
        }
        Node tagNode = tagsIndexNode.getNode(tagNodeName);
        if (!tagNode.hasNode(TaggingNodeType.PROP_TAGS)) {
            return;
        }
        for (NodeIterator ni = tagNode.getNode(TaggingNodeType.PROP_TAGS).getNodes(); ni.hasNext(); ) {
            Node subTagNode = ni.nextNode();
            if (subTagNode.isNodeType(HippoNodeType.NT_FACETRESULT) || StringUtils.isBlank(subTagNode.getName())) {
                continue;
            }

            String name = ISO9075Helper.decodeLocalName(subTagNode.getName());
            if (tagList.contains(name)) {
                continue;
            }

            long count = 1L;
            if (subTagNode.hasProperty(HippoNodeType.HIPPO_COUNT)) {
                count = subTagNode.getProperty(HippoNodeType.HIPPO_COUNT).getLong();
            }

            Tag tag = new Tag(name, count * score);
            collectionPerTag.add(tag);
            if (log.isDebugEnabled()) {
                log.debug("Found tag: {}", tag.getName());
            }
        }
    }

    @Deprecated
    private void populateFromFacetSearch(ArrayList<String> tagList, String tagName, TagCollection collectionPerTag,
                                         Node tagsIndexNode) throws RepositoryException {
        String tagNodeName = NodeNameCodec.encode(tagName, true);
        if (!tagsIndexNode.hasNode(tagNodeName)) {
            return;
        }
        // iterate over all related nodes
        for (NodeIterator ni = tagsIndexNode.getNode(tagNodeName).getNode(HippoNodeType.HIPPO_RESULTSET).getNodes(); ni
                .hasNext(); ) {
            Node node = ni.nextNode();
            Property tagsProperty = node.getProperty(TaggingNodeType.PROP_TAGS);
            Value[] values = tagsProperty.getValues();
            // iterate over the tags of the found node
            for (Value value : values) {
                // check if the found tag isn't the same as already assigned to the document
                if (tagList.contains(value.getString())) {
                    continue;
                }
                Tag tag = new Tag(value.getString(), score);
                collectionPerTag.add(tag);
                if (log.isDebugEnabled()) {
                    log.debug("Found tag: {}", tag.getName());
                }

            }
        }

    }

}
