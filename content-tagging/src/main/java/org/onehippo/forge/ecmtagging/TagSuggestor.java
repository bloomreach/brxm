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
package org.onehippo.forge.ecmtagging;

import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.Value;

import org.apache.wicket.util.io.IClusterable;
import org.hippoecm.frontend.model.JcrNodeModel;
import org.hippoecm.frontend.plugin.IPluginContext;
import org.hippoecm.frontend.plugin.Plugin;
import org.hippoecm.frontend.plugin.config.IPluginConfig;
import org.onehippo.forge.ecmtagging.providers.ITagsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Plugin that exposes the tagsuggesting service within the Hippo ECM application.
 * It coordinates all the communication with the tag providers end returns the
 * final TagCollection to the caller / frontend.
 * 
 * @author Jeroen Tietema
 *
 */
public class TagSuggestor extends Plugin implements IClusterable {
    @SuppressWarnings("unused")
    private final static String SVN_ID = "$Id$";

    private static final long serialVersionUID = 1L;
    static final Logger log = LoggerFactory.getLogger(TagSuggestor.class);

    /**
     * The servicename under which the calling resources can
     * retrieve the TagSuggestor
     * @todo make this configurable in the repository
     */
    public final static String SERVICE_ID = "tagging.suggestor.id";
    public final static String SERVICE_DEFAULT = "tagging.suggestor";

    /**
     * Include present tags: if true then the tags present in the document are also shown as suggestions.
     * Otherwise they are filtered from the suggestion list.
     */
    public final static String INCLUDE_PRESENT_TAGS = "include.present.tags";

    public TagSuggestor(IPluginContext context, IPluginConfig config) {
        super(context, config);

        context.registerService(this, config.getString(TagSuggestor.SERVICE_ID, TagSuggestor.SERVICE_DEFAULT));
    }

    /**
     * Will search for tags for the given document. It will do this by
     * passing it on to all the registered TagProviders and combining the
     * results into one TagCollection.
     * 
     * @param nodeModel
     * @return
     * @throws RepositoryException
     */
    public TagCollection getTags(JcrNodeModel nodeModel) throws RepositoryException {
        IPluginConfig config = getPluginConfig();

        // retrieve list of TagProviders
        List<ITagsProvider> providers = getPluginContext().getServices(
                config.getString(ITagsProvider.SERVICE_ID, ITagsProvider.SERVICE_DEFAULT),
                ITagsProvider.class);
        if (log.isDebugEnabled()) {
            log.debug("Loaded  {} providers.", providers.size());
        }
        TagCollection allTags = new TagCollection();
        // query all TagProviders
        for (ITagsProvider provider : providers) {
            TagCollection tags = provider.getTags(nodeModel);
            if (log.isDebugEnabled()) {
                log.debug("Got {} tags from {}", tags.size() , provider.getClass());
            }
            allTags.addAll(tags);
        }
        
        if (!config.getBoolean(INCLUDE_PRESENT_TAGS) && nodeModel.getNode().hasProperty(TaggingNodeType.PROP_TAGS)) {
            Value[] currentTags = nodeModel.getNode().getProperty(TaggingNodeType.PROP_TAGS).getValues();

            for (Value value : currentTags) {
                String tagValue = value.getString();
                if (allTags.containsKey(tagValue)) {
                    allTags.remove(tagValue);
                }
            }
        }
        allTags.normalizeScores();
        return allTags;
    }

}
